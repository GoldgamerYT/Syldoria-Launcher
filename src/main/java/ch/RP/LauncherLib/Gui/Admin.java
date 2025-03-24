package ch.RP.LauncherLib.Gui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Admin {

    private final Stage adminStage;

    public Admin(Stage parentStage) {
        adminStage = new Stage();
        adminStage.setTitle("Admin Panel");

        // Label
        Label adminLabel = new Label("Admin Einstellungen");
        adminLabel.getStyleClass().add("admin-label-title");

        // Button zum Zurückkehren zu den Settings
        Button backButton = new Button("Zurück zu den Einstellungen");
        backButton.getStyleClass().add("modern-button");
        backButton.setOnAction(e -> {
            adminStage.close();
            parentStage.show();
        });

        // Test-Komponenten
        Label testLabel = new Label("Admin-spezifische Optionen");
        testLabel.getStyleClass().add("admin-label-info");

        Button testButton = new Button("Test-Funktion");
        testButton.getStyleClass().add("modern-button");
        testButton.setOnAction(e -> System.out.println("Test-Funktion ausgeführt"));

        // Layout
        VBox centerBox = new VBox(15, adminLabel, testLabel, testButton, backButton);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.getStyleClass().add("admin-box");

        BorderPane root = new BorderPane(centerBox);
        Scene scene = new Scene(root, 400, 300);
        adminStage.setScene(scene);
    }

    public void show() {
        adminStage.show();
    }
}
