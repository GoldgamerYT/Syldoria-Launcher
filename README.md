# Syldoria Launcher

Ein angepasster Minecraft-Launcher mit erweiterten Funktionen für ein optimales Spielerlebnis.

![Syldoria Launcher Logo](src/main/resources/icons/logo.png)

## Über das Projekt

Der Syldoria Launcher ist ein speziell entwickelter Minecraft-Launcher, der eine verbesserte Benutzeroberfläche und zusätzliche Funktionen bietet. Er ermöglicht eine einfache Installation und Verwaltung von Minecraft-Versionen, Mods und Ressourcenpaketen.

**WICHTIG:** Dieser Launcher darf nur für Open-Source-Projekte genutzt werden. Die kommerzielle Nutzung ist nicht gestattet.

## Funktionen

- Moderne, benutzerfreundliche Oberfläche
- Unterstützung für Microsoft- und Mojang-Konten
- Einfache Installation und Verwaltung von Minecraft-Versionen
- Automatische Updates
- Mod-Integration und -Verwaltung
- Ressourcenpaket-Management
- Discord-Integration

## Voraussetzungen

1. **Java 21 oder neuer** muss installiert sein
2. **JavaFX 21.0.5** muss installiert sein
   - Standard-Installationspfad: `C:/Users/[Benutzername]/JavaFX/javafx-sdk-21.0.5/lib`
3. **JCEF** (Java Chromium Embedded Framework) muss installiert sein
   - Standard-Installationspfad: `C:/Users/[Benutzername]/JavaFX/web`

## Installation

### Voraussetzungen installieren

1. **Java 21+**: Laden Sie Java von [Oracle](https://www.oracle.com/java/technologies/downloads/) oder [AdoptOpenJDK](https://adoptopenjdk.net/) herunter
2. **JavaFX 21.0.5**: Laden Sie JavaFX von [Gluon](https://gluonhq.com/products/javafx/) herunter
3. **JCEF**: Das JCEF-Bundle ist im Repository enthalten

### Launcher starten

#### Windows

1. Öffnen Sie eine Eingabeaufforderung oder PowerShell im Projektverzeichnis
2. Führen Sie das Startskript aus:
   ```
   start_launcher.bat
   ```
   
   Oder die erweiterte Version mit zusätzlichen Prüfungen:
   ```
   start_launcher_erweitert.bat
   ```

#### Linux/Mac

1. Öffnen Sie ein Terminal im Projektverzeichnis
2. Machen Sie das Startskript ausführbar:
   ```
   chmod +x start_launcher.sh
   ```
3. Führen Sie das Startskript aus:
   ```
   ./start_launcher.sh
   ```

## Manueller Start

Sie können den Launcher auch manuell mit folgendem Befehl starten:

```
java --enable-preview --module-path "PFAD_ZU_JAVAFX/lib" --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web,javafx.swing --add-exports=javafx.base/com.sun.javafx=ALL-UNNAMED --add-exports=javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED --add-exports=javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED --add-exports=javafx.graphics/com.sun.javafx.embed.swing=ALL-UNNAMED -Djava.library.path="PFAD_ZU_JCEF" -jar build/libs/SyldoriaLauncher-1.0.jar
```

Ersetzen Sie `PFAD_ZU_JAVAFX` und `PFAD_ZU_JCEF` mit Ihren tatsächlichen Pfaden.

## Entwicklung

### Projekt klonen

```bash
git clone https://github.com/GoldgamerYT/Syldoria-Launcher.git
cd Syldoria-Launcher
```

### Projekt bauen

```bash
./gradlew shadowJar
```

Dies erstellt eine ausführbare JAR-Datei im Verzeichnis `build/libs/`.

## Fehlerbehebung

1. **JavaFX nicht gefunden**: Stellen Sie sicher, dass JavaFX im angegebenen Pfad installiert ist oder passen Sie den Pfad in den Startskripten an.

2. **JAR-Datei nicht gefunden**: Stellen Sie sicher, dass Sie das Projekt mit `./gradlew shadowJar` gebaut haben.

3. **Java-Version**: Vergewissern Sie sich, dass Sie Java 21 oder neuer verwenden, da das Projekt die `--enable-preview`-Flag verwendet.

4. **Anzeigefehler**: Wenn die GUI nicht richtig angezeigt wird, könnte das Problem mit der JCEF-Installation zusammenhängen. Überprüfen Sie, ob die Bibliotheken im richtigen Pfad vorhanden sind.

## Lizenz

Dieser Launcher ist **nur für Open-Source-Projekte** freigegeben. Die kommerzielle Nutzung ist untersagt. Alle Rechte vorbehalten.

## Beiträge

Beiträge zum Projekt sind willkommen! Wenn Sie einen Fehler finden oder eine Verbesserung vorschlagen möchten, erstellen Sie bitte ein Issue oder einen Pull Request.

## Kontakt

Bei Fragen oder Anregungen können Sie sich an das Entwicklerteam wenden:

- GitHub: [GoldgamerYT](https://github.com/GoldgamerYT)
- Discord: (Link zum Discord-Server)

---

2025 Syldoria Launcher - Entwickelt mit ❤️ für die Minecraft-Community 