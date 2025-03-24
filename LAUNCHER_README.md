# Syldoria Launcher - Startanleitung

## Voraussetzungen

1. **Java 21 oder neuer** muss installiert sein
2. **JavaFX 21.0.5** muss installiert sein
   - Standard-Installationspfad: `C:/Users/[Benutzername]/JavaFX/javafx-sdk-21.0.5/lib`
3. **JCEF** (Java Chromium Embedded Framework) muss installiert sein
   - Standard-Installationspfad: `C:/Users/[Benutzername]/JavaFX/web`

## Launcher starten

### Windows

1. Öffnen Sie eine Eingabeaufforderung oder PowerShell im Projektverzeichnis
2. Führen Sie das Startskript aus:
   ```
   start_launcher.bat
   ```
   
   Oder die erweiterte Version mit zusätzlichen Prüfungen:
   ```
   start_launcher_erweitert.bat
   ```

### Linux/Mac

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

## Fehlerbehebung

1. **JavaFX nicht gefunden**: Stellen Sie sicher, dass JavaFX im angegebenen Pfad installiert ist oder passen Sie den Pfad in den Startskripten an.

2. **JAR-Datei nicht gefunden**: Stellen Sie sicher, dass Sie das Projekt mit `./gradlew shadowJar` gebaut haben.

3. **Java-Version**: Vergewissern Sie sich, dass Sie Java 21 oder neuer verwenden, da das Projekt die `--enable-preview`-Flag verwendet.

4. **Anzeigefehler**: Wenn die GUI nicht richtig angezeigt wird, könnte das Problem mit der JCEF-Installation zusammenhängen. Überprüfen Sie, ob die Bibliotheken im richtigen Pfad vorhanden sind. 