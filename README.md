# Kinder-Musik – Anleitung

Eine kleine App fürs Handy: Die Kinder sehen nur die Playlists, Alben und Hörspiele, die du freigegeben hast, und spielen sie auf dem Sonos ab. Abgespielt wird über dein Spotify-Premium-Konto.

Zeitaufwand: einmalig etwa 20–30 Minuten. Am PC geht es am bequemsten, am Handy klappt es aber auch.

---

## Schritt 1: Dateien auf GitHub hochladen

1. Auf **github.com** ein kostenloses Konto anlegen (falls noch nicht vorhanden). Merke dir deinen Benutzernamen.
2. Oben rechts auf **+** → **New repository** tippen.
   - Repository name: `kinder-musik`
   - **Public** auswählen
   - **Create repository** tippen
3. Auf der neuen Seite auf **uploading an existing file** tippen.
4. Alle Dateien auswählen und hochladen:
   `index.html`, `manifest.webmanifest`, `sw.js`, `icon-192.png`, `icon-512.png`, `icon-512-maskable.png` (und gern diese `README.md`)
5. Unten auf **Commit changes** tippen.

## Schritt 2: GitHub Pages einschalten

1. Im Repository auf **Settings** → links **Pages**.
2. Bei „Branch“ **main** und **/ (root)** wählen → **Save**.
3. Nach 1–3 Minuten ist die App erreichbar unter:
   `https://DEIN-BENUTZERNAME.github.io/kinder-musik/`
   (Benutzername in Kleinbuchstaben.)

## Schritt 3: Spotify-Entwickler-App anlegen

1. **developer.spotify.com/dashboard** öffnen und mit deinem Spotify-Konto anmelden. Beim ersten Mal die Nutzungsbedingungen bestätigen.
2. **Create app** tippen und ausfüllen:
   - App name: `Kinder-Musik`
   - App description: `Privat für meine Kinder`
   - Redirect URI: **genau** die Adresse aus Schritt 2, mit `/` am Ende, z. B. `https://oliver123.github.io/kinder-musik/`. Danach auf **Add** tippen.
   - Bei „Which API/SDKs are you planning to use?“ **Web API** ankreuzen.
   - Bedingungen bestätigen → **Save**.
3. In der App auf **Settings** gehen und die **Client ID** kopieren (32 Zeichen).
4. Unter **User Management** deine Spotify-E-Mail-Adresse und deinen Namen eintragen, falls sie dort noch fehlt.

## Schritt 4: App auf dem Handy einrichten

1. Die Adresse aus Schritt 2 in **Chrome** öffnen.
2. Die **Client ID** einfügen und eine **Eltern-PIN** (4 Ziffern) festlegen.
3. **Bei Spotify anmelden** und zustimmen.
4. Im Chrome-Menü (⋮) **App installieren** bzw. **Zum Startbildschirm hinzufügen** wählen. Danach die App immer über das neue Icon öffnen.

## Schritt 5: Inhalte freigeben

1. In der App oben rechts auf das **Schloss** tippen und die PIN eingeben.
2. Inhalte hinzufügen, auf einem dieser drei Wege:
   - **Suche** (z. B. „Schlaflieder“) → **Freigeben**
   - **Meine Playlists anzeigen** → **Freigeben**
   - **Spotify-Link einfügen**: lange Links wie `https://open.spotify.com/playlist/…`. Kurzlinks (`spotify.link/…`) funktionieren nicht.
3. Als **Gerät** dein Handy auswählen und bei Bedarf die **Hörzeit** festlegen.
4. **Fertig** tippen. Jetzt sehen die Kinder nur noch die Kacheln.

---

## Gut zu wissen

- **Wiedergabe über Bluetooth:** Die Musik läuft über die Spotify-App auf dem Handy, und das Handy ist per Bluetooth mit dem Sonos verbunden. Als Gerät wählst du im Elternbereich dein Handy aus. Den Sonos selbst kann Spotify über diese Schnittstelle nicht steuern.
- **Handy fehlt in der Liste?** Die Spotify-App einmal öffnen, zurück in die Kinder-App wechseln und auf ↻ tippen. Die Spotify-App muss im Hintergrund weiterlaufen.
- **Lautstärke:** Die Lautstärke regelst du mit den Tasten am Handy. Eine Höchstgrenze legst du in der Sonos-App fest: Einstellungen → Raum → „Lautstärkebegrenzung“. Samsung-Handys haben zusätzlich ein „Medienlautstärke-Limit“ unter Einstellungen → Töne → Lautstärke.
- **Titelübersicht:** Bei Alben, Podcasts und deinen eigenen Playlists siehst du alle Titel. Bei Playlists von Spotify oder anderen Leuten zeigt Spotify keine Titelliste. Abhilfe: In der Spotify-App die Playlist öffnen, „…“ antippen, „Zu anderer Playlist hinzufügen“ → „Neue Playlist“ wählen und diese eigene Kopie freigeben.
- **Nur ein Stream gleichzeitig:** Hörst du selbst gerade Spotify, stoppt das eine, wenn die Kinder etwas starten.
- **Empfehlungen sauber halten:** In der Spotify-App unter Profilbild → Einstellungen und Datenschutz → Inhalte und Darstellung den Schalter „Musik für Kinder und Familien in Geschmacksprofil aufnehmen“ ausschalten.
- **Kinder bleiben in der App:** In den Android-Einstellungen „App anpinnen“ einschalten (meist unter Sicherheit). Dann kommen die Kinder nur mit deiner Bildschirmsperre aus der App heraus.
- **Hörzeit** wird nur geprüft, solange die App geöffnet ist.
- **Datenschutz:** Freigaben, PIN und Anmeldung werden nur auf dem Handy gespeichert, nicht auf GitHub. Die Client-ID ist kein Passwort.
- **Spotify-Fehlermeldung 403:** Prüfen, ob deine E-Mail unter „User Management“ eingetragen ist und ob dein Premium-Abo aktiv ist.

## Als APK (optional)

Liegt die App auf GitHub Pages, kannst du auf **pwabuilder.com** die Adresse aus Schritt 2 eingeben und ein Android-Paket erzeugen lassen. Für die meisten reicht aber „App installieren“ aus Schritt 4.
