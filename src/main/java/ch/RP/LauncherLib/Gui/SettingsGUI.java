package ch.RP.LauncherLib.Gui;

import ch.RP.LauncherLib.Main;
import ch.RP.LauncherLib.Authentication;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class SettingsGUI extends BorderPane {

    private final Authentication auth;
    private int selectedRam;
    private CheckBox consoleCheckBox;
    private Button adminButton;

    public SettingsGUI(LauncherGUI launcherGUI) {
        this.auth = new Authentication();
        // Anwenden der CSS-Klasse für das Panel
        this.getStyleClass().add("settings-panel");

        Label settingsLabel = new Label("Einstellungen");
        settingsLabel.getStyleClass().add("settings-label-title");
        setTop(settingsLabel);
        BorderPane.setAlignment(settingsLabel, Pos.CENTER);

        // Berechnung des RAM-Werts
        long totalRam = ((com.sun.management.OperatingSystemMXBean)
                java.lang.management.ManagementFactory.getOperatingSystemMXBean())
                .getTotalPhysicalMemorySize() / (1024L * 1024L * 1024L);
        int installedRam = (int) totalRam;
        selectedRam = Math.max(1, (int) (installedRam * 0.75));

        Label ramInfoLabel = new Label(
                "Zugewiesener RAM: " + selectedRam + " GB (75% des verfügbaren RAM)"
        );
        ramInfoLabel.getStyleClass().add("settings-label-info");

        // Log-Checkbox (Debug)
        consoleCheckBox = new CheckBox("Konsole anzeigen (Debug)");
        consoleCheckBox.getStyleClass().add("settings-checkbox");
        consoleCheckBox.setSelected(false);
        consoleCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            Main.loggingEnabled = newVal;
            Main.simpleLog.log("[SettingsGUI] Log-Anzeige aktiviert: " + newVal);
        });

        // Logout-Button
        Button logoutButton = new Button("Logout");
        logoutButton.getStyleClass().addAll("modern-button", "button-red");
        logoutButton.setOnAction(e -> {
            //auth.handleLogout(launcherGUI.getUsernameLabel(), launcherGUI.getFooterLoginButton());
            Main.simpleLog.log("[SettingsGUI] Logout ausgeführt.");
        });

        // Show Log-Button
        Button showLogButton = new Button("Show Log");
        showLogButton.getStyleClass().add("modern-button");
        showLogButton.setOnAction(e -> {
            Main.simpleLog.log("[SettingsGUI] User klickt auf Show Log");
            Main.simpleLog.showLogWindow();
        });

        // Admin-Button
        adminButton = new Button("Admin");
        adminButton.getStyleClass().addAll("modern-button", "button-yellow");
        adminButton.setVisible(Main.isAdmin);
        adminButton.setManaged(Main.isAdmin);
        adminButton.setOnAction(e -> {
            Main.simpleLog.log("[SettingsGUI] Admin-Button geklickt.");
        });

        // VBox für zentrale Elemente
        VBox centerBox = new VBox(15, ramInfoLabel, consoleCheckBox, logoutButton, showLogButton, adminButton);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.getStyleClass().add("dark-box");
        setCenter(centerBox);
    }

    public int getSelectedRam() {
        return selectedRam;
    }
}
