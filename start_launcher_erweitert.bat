@echo off
echo Syldoria Launcher Startskript
echo ===========================

rem Setze die JavaFX-Umgebung
set JAVAFX_PATH=%USERPROFILE%\JavaFX\javafx-sdk-21.0.5\lib
set JCEF_PATH=%USERPROFILE%\JavaFX\web

rem Überprüfe, ob JavaFX existiert
if not exist "%JAVAFX_PATH%" (
    echo FEHLER: JavaFX nicht gefunden in %JAVAFX_PATH%
    echo Bitte installieren Sie JavaFX in diesem Pfad oder passen Sie dieses Skript an.
    pause
    exit /b 1
)

rem Überprüfe, ob die JAR-Datei existiert
if not exist "build\libs\SyldoriaLauncher-1.0.jar" (
    echo FEHLER: SyldoriaLauncher-1.0.jar nicht gefunden.
    echo Bitte stellen Sie sicher, dass Sie das Projekt gebaut haben mit ./gradlew shadowJar
    pause
    exit /b 1
)

echo JavaFX-Pfad: %JAVAFX_PATH%
echo JCEF-Pfad: %JCEF_PATH%
echo Starte Launcher...
echo.

java --enable-preview ^
--module-path "%JAVAFX_PATH%" ^
--add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web,javafx.swing ^
--add-exports=javafx.base/com.sun.javafx=ALL-UNNAMED ^
--add-exports=javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED ^
--add-exports=javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED ^
--add-exports=javafx.graphics/com.sun.javafx.embed.swing=ALL-UNNAMED ^
-Djava.library.path="%JCEF_PATH%" ^
-jar build\libs\SyldoriaLauncher-1.0.jar

echo.
echo Launcher beendet.
pause 