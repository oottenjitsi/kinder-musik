package de.oottenjitsi.kindermusik;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Kinder-Musik als eigene Android-App – ganz ohne Chrome.
 * Zeigt die Web-App in einer eingebauten Ansicht und ergänzt, was nur eine echte App kann:
 * Titel/Cover auf dem Sperrbildschirm, Bildschirm anlassen, Zurück-Taste, Datei speichern.
 */
public class MainActivity extends Activity {
    private static final String START = "https://oottenjitsi.github.io/kinder-musik/";
    private static final int FILE_REQ = 7, SAVE_REQ = 8;
    private static final String CHANNEL = "wiedergabe";
    private static final int NOTE_ID = 1;
    private static final String ACT_PREV = "de.oottenjitsi.kindermusik.PREV";
    private static final String ACT_TOGGLE = "de.oottenjitsi.kindermusik.TOGGLE";
    private static final String ACT_NEXT = "de.oottenjitsi.kindermusik.NEXT";

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingSave;
    private MediaSession session;
    private NotificationManager notes;
    private String artUrl = "";
    private Bitmap artBitmap;
    private String lastTitle = "", lastArtist = "";
    private boolean lastPlaying = false;
    private long lastPos = 0, lastDur = 0;

    private final BroadcastReceiver buttons = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if (ACT_PREV.equals(a)) js("previous");
            else if (ACT_NEXT.equals(a)) js("next");
            else if (ACT_TOGGLE.equals(a)) js(lastPlaying ? "pause" : "play");
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.parseColor("#FFF8EE"));
        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        // Ohne den WebView-Hinweis "; wv" – sonst lehnen manche Dienste die Ansicht ab.
        s.setUserAgentString(s.getUserAgentString().replace("; wv)", ")") + " KinderMusikApp/1");

        web.addJavascriptInterface(new Bridge(), "KMNative");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                if (!req.isForMainFrame()) return false;
                Uri u = req.getUrl();
                String scheme = u.getScheme(), host = u.getHost();
                if (scheme == null || host == null) return true;
                if (!scheme.equals("https")) return true;
                // Nur die eigene Seite und die Spotify-Anmeldung – kein freier Browser fürs Kind.
                return !(host.endsWith("github.io") || host.endsWith("spotify.com") || host.endsWith("scdn.co")
                        || host.endsWith("spotifycdn.com") || host.endsWith("google.com") || host.endsWith("gstatic.com")
                        || host.endsWith("recaptcha.net"));
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) {
                    view.loadDataWithBaseURL(null,
                        "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>"
                        + "<body style='font-family:sans-serif;text-align:center;padding:40px;background:#FFF8EE;color:#1F2937'>"
                        + "<h2>Keine Internetverbindung</h2><p>Kinder-Musik braucht Internet, um Musik von Spotify abzuspielen.</p>"
                        + "<button style='font-size:18px;padding:14px 22px;border-radius:14px;border:0;background:#1E3A8A;color:#fff'"
                        + " onclick=\"location.href='" + START + "'\">Nochmal versuchen</button></body></html>",
                        "text/html", "utf-8", null);
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // Spotify spielt geschützte Musik ab (Widevine) – das muss die App erlauben.
                for (String r : request.getResources()) {
                    if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(r)) {
                        request.grant(new String[]{PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID});
                        return;
                    }
                }
                request.deny();
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(i, "Datei auswählen"), FILE_REQ);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        setupMedia();

        if (state != null) web.restoreState(state); else web.loadUrl(START);
    }

    // ---------- Sperrbildschirm & Benachrichtigung ----------
    private void setupMedia() {
        notes = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Wiedergabe", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notes.createNotificationChannel(ch);
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        session = new MediaSession(this, "KinderMusik");
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { js("play"); }
            @Override public void onPause() { js("pause"); }
            @Override public void onSkipToNext() { js("next"); }
            @Override public void onSkipToPrevious() { js("previous"); }
            @Override public void onFastForward() { js("fwd15"); }
            @Override public void onRewind() { js("back15"); }
            @Override public void onSeekTo(long pos) { js("seek:" + pos); }
        });
        IntentFilter f = new IntentFilter();
        f.addAction(ACT_PREV); f.addAction(ACT_TOGGLE); f.addAction(ACT_NEXT);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(buttons, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(buttons, f);
    }

    private void js(String action) {
        runOnUiThread(() -> web.evaluateJavascript(
            "window.kmMediaAction && window.kmMediaAction(" + quote(action) + ")", null));
    }

    private static String quote(String s) {
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private PendingIntent button(String action, int code) {
        Intent i = new Intent(action).setPackage(getPackageName());
        return PendingIntent.getBroadcast(this, code, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void showNowPlaying() {
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE
                | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                | PlaybackState.ACTION_SEEK_TO | PlaybackState.ACTION_FAST_FORWARD | PlaybackState.ACTION_REWIND;
        session.setPlaybackState(new PlaybackState.Builder()
                .setActions(actions)
                .setState(lastPlaying ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, lastPos, lastPlaying ? 1f : 0f)
                .build());
        MediaMetadata.Builder m = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, lastTitle)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, lastArtist)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, lastDur);
        if (artBitmap != null) m.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artBitmap);
        session.setMetadata(m.build());
        session.setActive(true);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        b.setSmallIcon(android.R.drawable.ic_media_play)
         .setContentTitle(lastTitle)
         .setContentText(lastArtist)
         .setVisibility(Notification.VISIBILITY_PUBLIC)
         .setOngoing(lastPlaying)
         .setShowWhen(false)
         .setContentIntent(PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
         .addAction(android.R.drawable.ic_media_previous, "Zurück", button(ACT_PREV, 1))
         .addAction(lastPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                    lastPlaying ? "Pause" : "Abspielen", button(ACT_TOGGLE, 2))
         .addAction(android.R.drawable.ic_media_next, "Weiter", button(ACT_NEXT, 3))
         .setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0, 1, 2));
        if (artBitmap != null) b.setLargeIcon(artBitmap);
        try { notes.notify(NOTE_ID, b.build()); } catch (Exception ignored) { }
    }

    private void loadArt(final String url) {
        new Thread(() -> {
            Bitmap bmp = null;
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(8000); c.setReadTimeout(8000);
                try (InputStream in = c.getInputStream()) { bmp = BitmapFactory.decodeStream(in); }
            } catch (Exception ignored) { }
            final Bitmap result = bmp;
            runOnUiThread(() -> {
                if (url.equals(artUrl)) { artBitmap = result; showNowPlaying(); }
            });
        }).start();
    }

    /** Wird von der Web-App als window.KMNative aufgerufen. */
    private class Bridge {
        @JavascriptInterface public void keepAwake(final boolean on) {
            runOnUiThread(() -> {
                if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }

        @JavascriptInterface public void setNowPlaying(final String title, final String artist, final String art,
                                                       final boolean playing, final double pos, final double dur) {
            runOnUiThread(() -> {
                lastTitle = title == null ? "" : title;
                lastArtist = artist == null ? "" : artist;
                lastPlaying = playing;
                lastPos = (long) pos; lastDur = (long) dur;
                String a = art == null ? "" : art;
                if (!a.equals(artUrl)) {
                    artUrl = a; artBitmap = null;
                    if (a.startsWith("https://")) loadArt(a);
                }
                showNowPlaying();
            });
        }

        @JavascriptInterface public void clearNowPlaying() {
            runOnUiThread(() -> {
                notes.cancel(NOTE_ID);
                session.setActive(false);
            });
        }

        @JavascriptInterface public void saveFile(final String name, final String data) {
            runOnUiThread(() -> {
                pendingSave = data;
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("application/json");
                i.putExtra(Intent.EXTRA_TITLE, name);
                try { startActivityForResult(i, SAVE_REQ); } catch (Exception e) { pendingSave = null; }
            });
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == FILE_REQ && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            fileCallback = null;
        } else if (req == SAVE_REQ) {
            if (res == RESULT_OK && data != null && data.getData() != null && pendingSave != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    out.write(pendingSave.getBytes(StandardCharsets.UTF_8));
                    web.evaluateJavascript("typeof toast==='function'&&toast('Einstellungen gespeichert.')", null);
                } catch (Exception e) {
                    web.evaluateJavascript("typeof toast==='function'&&toast('Speichern fehlgeschlagen.')", null);
                }
            }
            pendingSave = null;
        } else {
            super.onActivityResult(req, res, data);
        }
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.kmBack && window.kmBack()) ? '1' : '0'", v -> {
            if (v == null || !v.contains("1")) moveTaskToBack(true);
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onDestroy() {
        try { unregisterReceiver(buttons); } catch (Exception ignored) { }
        if (notes != null) notes.cancel(NOTE_ID);
        if (session != null) session.release();
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
