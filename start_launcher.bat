@echo off
java --enable-preview ^
--module-path "C:/Users/jerom/JavaFX/javafx-sdk-21.0.5/lib" ^
--add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web,javafx.swing ^
--add-exports=javafx.base/com.sun.javafx=ALL-UNNAMED ^
--add-exports=javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED ^
--add-exports=javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED ^
--add-exports=javafx.graphics/com.sun.javafx.embed.swing=ALL-UNNAMED ^
-Djava.library.path=C:/Users/jerom/JavaFX/web ^
-jar build/libs/SyldoriaLauncher-1.0.jar
pause
