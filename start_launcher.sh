#!/bin/bash

echo "Syldoria Launcher Startskript"
echo "==========================="

# Setze die JavaFX-Umgebung
# Hier sollten Benutzer ihren eigenen Pfad anpassen
JAVAFX_PATH="$HOME/JavaFX/javafx-sdk-21.0.5/lib"
JCEF_PATH="$HOME/JavaFX/web"

# Überprüfe, ob JavaFX existiert
if [ ! -d "$JAVAFX_PATH" ]; then
    echo "FEHLER: JavaFX nicht gefunden in $JAVAFX_PATH"
    echo "Bitte installieren Sie JavaFX in diesem Pfad oder passen Sie dieses Skript an."
    exit 1
fi

# Überprüfe, ob die JAR-Datei existiert
if [ ! -f "build/libs/SyldoriaLauncher-1.0.jar" ]; then
    echo "FEHLER: SyldoriaLauncher-1.0.jar nicht gefunden."
    echo "Bitte stellen Sie sicher, dass Sie das Projekt gebaut haben mit ./gradlew shadowJar"
    exit 1
fi

echo "JavaFX-Pfad: $JAVAFX_PATH"
echo "JCEF-Pfad: $JCEF_PATH"
echo "Starte Launcher..."
echo ""

java --enable-preview \
--module-path "$JAVAFX_PATH" \
--add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web,javafx.swing \
--add-exports=javafx.base/com.sun.javafx=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.embed.swing=ALL-UNNAMED \
-Djava.library.path="$JCEF_PATH" \
-jar build/libs/SyldoriaLauncher-1.0.jar

echo ""
echo "Launcher beendet."
read -p "Drücken Sie eine Taste, um fortzufahren..." -n1 -s 