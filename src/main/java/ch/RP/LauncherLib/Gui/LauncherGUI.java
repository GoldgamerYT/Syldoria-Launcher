package ch.RP.LauncherLib.Gui;

import ch.RP.LauncherLib.Authentication;
import ch.RP.LauncherLib.Main;
import ch.RP.LauncherLib.ModInstaller;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.awt.Desktop;
import java.net.URI;

public class LauncherGUI {

    private Stage primaryStage;
    private Authentication auth;
    private double xOffset = 0, yOffset = 0;
    private Label usernameLabel;
    private Button footerLoginButton, settingsToggleButton;
    private boolean isSettingsOpen = false;
    private AnchorPane mainPanel;
    private SettingsGUI settingsPanel;

    public LauncherGUI(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.auth = new Authentication();
        setupStage();
        BorderPane root = createRootPane();
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/css/launcher.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();

        auth.autoLogin(usernameLabel, null);
        if (!Main.username.isEmpty() && footerLoginButton != null) {
            footerLoginButton.setVisible(false);
        }
        Main.simpleLog.log("[LauncherGUI] Konstruktor fertig.");
    }

    private void setupStage() {
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Syldoria Launcher");
        primaryStage.setWidth(900);
        primaryStage.setHeight(700);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/icon.png")));
    }

    private BorderPane createRootPane() {
        BorderPane root = new BorderPane();
        root.setBackground(Background.EMPTY);

        // Top-Bereich (eigene TitleBar + Header)
        VBox topContainer = new VBox(createCustomTitleBar(), createHeader());
        topContainer.setBackground(new Background(new BackgroundFill(Color.web("#2C3E50"), new CornerRadii(15, 15, 0, 0, false), Insets.EMPTY)));
        root.setTop(topContainer);

        // Mitte: Hauptpanel & Settings (als StackPane übereinander)
        StackPane contentStack = new StackPane();
        contentStack.setBackground(new Background(new BackgroundFill(Color.web("#34495E"), CornerRadii.EMPTY, Insets.EMPTY)));
        mainPanel = createMainPanel();
        settingsPanel = new SettingsGUI(this);
        settingsPanel.setVisible(false);
        contentStack.getChildren().addAll(mainPanel, settingsPanel);
        root.setCenter(contentStack);

        // Footer
        HBox footer = createFooter();
        footer.setBackground(new Background(new BackgroundFill(Color.web("#2C3E50"), CornerRadii.EMPTY, Insets.EMPTY)));
        root.setBottom(footer);

        return root;
    }

    private HBox createCustomTitleBar() {
        HBox titleBar = new HBox();
        titleBar.setStyle("-fx-background-color: #333333; -fx-background-radius: 15 15 0 0;");
        titleBar.setPrefHeight(30);
        titleBar.setAlignment(Pos.CENTER_RIGHT);

        Button minimizeButton = createStyledButton("-", 40, 30, "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 15;");
        minimizeButton.setOnAction(e -> playMinimizeAnimation());
        minimizeButton.setOnMouseEntered(e -> minimizeButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 15;"));
        minimizeButton.setOnMouseExited(e -> minimizeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 15;"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        titleBar.getChildren().addAll(spacer, minimizeButton);

        // Ermöglicht Fenster ziehen
        titleBar.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
        titleBar.setOnMouseDragged(e -> {
            primaryStage.setX(e.getScreenX() - xOffset);
            primaryStage.setY(e.getScreenY() - yOffset);
        });
        return titleBar;
    }

    private void playMinimizeAnimation() {
        ScaleTransition scale = new ScaleTransition(Duration.millis(300), primaryStage.getScene().getRoot());
        scale.setFromX(1.0); scale.setFromY(1.0); scale.setToX(0.1); scale.setToY(0.1);
        FadeTransition fade = new FadeTransition(Duration.millis(300), primaryStage.getScene().getRoot());
        fade.setFromValue(1.0); fade.setToValue(0.0);
        ParallelTransition pt = new ParallelTransition(scale, fade);
        pt.setOnFinished(e -> {
            primaryStage.setIconified(true);
            primaryStage.getScene().getRoot().setOpacity(1.0);
            primaryStage.getScene().getRoot().setScaleX(1.0);
            primaryStage.getScene().getRoot().setScaleY(1.0);
        });
        pt.play();
    }

    private VBox createHeader() {
        VBox header = new VBox();
        header.getStyleClass().add("header-box");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 10, 10, 20));

        Label titleLabel = new Label("Syldoria Launcher");
        titleLabel.getStyleClass().add("header-title");

        usernameLabel = new Label("Nicht angemeldet");
        usernameLabel.getStyleClass().add("header-username");

        header.getChildren().addAll(titleLabel, usernameLabel);
        return header;
    }

    private HBox createFooter() {
        HBox footer = new HBox();
        footer.getStyleClass().add("footer-box");
        footer.setPadding(new Insets(10, 20, 10, 20));
        footer.setAlignment(Pos.CENTER);

        settingsToggleButton = createStyledButton("\u2699", 80, 50, "-fx-background-radius: 25; -fx-font-size: 18px; -fx-background-color: #3A86FF; -fx-text-fill: white; -fx-cursor: hand;");
        settingsToggleButton.setOnAction(e -> toggleSettings());

        footerLoginButton = createStyledButton("Login", 160, -1, "");
        footerLoginButton.getStyleClass().addAll("modern-button", "button-green");
        footerLoginButton.setOnAction(e -> {
            Main.simpleLog.log("[LauncherGUI] User klickt auf Login...");
            auth.handleLogin(usernameLabel, footerLoginButton);
            Main.simpleLog.log("[LauncherGUI] Login fertig.");
        });
        if (!Main.username.isEmpty()) {
            footerLoginButton.setVisible(false);
        }

        Button closeButton = createStyledButton("Close Launcher", 190, 50, "-fx-background-radius: 25; -fx-background-color: #E53935; -fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand;");
        closeButton.setOnAction(e -> {
            Main.simpleLog.log("[LauncherGUI] Schließe Launcher...");
            primaryStage.close();
        });

        Region spacerLeft = new Region(), spacerRight = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        HBox.setHgrow(spacerRight, Priority.ALWAYS);
        HBox.setMargin(closeButton, new Insets(0, 0, 0, 20));

        footer.getChildren().addAll(settingsToggleButton, spacerLeft, footerLoginButton, spacerRight, closeButton);
        return footer;
    }

    private Button createStyledButton(String text, double prefWidth, double prefHeight, String style) {
        Button btn = new Button(text);
        if (prefWidth > 0) btn.setPrefWidth(prefWidth);
        if (prefHeight > 0) btn.setPrefHeight(prefHeight);
        if (style != null && !style.isEmpty()) btn.setStyle(style);
        return btn;
    }

    private void toggleSettings() {
        isSettingsOpen = !isSettingsOpen;
        mainPanel.setVisible(!isSettingsOpen);
        settingsPanel.setVisible(isSettingsOpen);
        if (isSettingsOpen) {
            animateSettingsButton(0, 180, "\u2699", "X");
            Main.simpleLog.log("[LauncherGUI] Öffne Settings.");
        } else {
            animateSettingsButton(180, 360, "X", "\u2699");
            Main.simpleLog.log("[LauncherGUI] Schließe Settings.");
        }
    }

    private void animateSettingsButton(double startAngle, double endAngle, String oldSymbol, String newSymbol) {
        settingsToggleButton.setRotate(startAngle);
        settingsToggleButton.setText(oldSymbol);
        RotateTransition rt = new RotateTransition(Duration.millis(400), settingsToggleButton);
        rt.setFromAngle(startAngle);
        rt.setToAngle(endAngle);
        rt.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (newTime.toMillis() >= 200) {
                settingsToggleButton.setText(newSymbol);
                String color = "X".equals(newSymbol) ? "#E53935" : "#3A86FF";
                settingsToggleButton.setStyle("-fx-background-radius: 25; -fx-font-size: 20px; -fx-background-color: " + color + "; -fx-text-fill: white; -fx-cursor: hand;");
            }
        });
        rt.play();
    }

    private AnchorPane createMainPanel() {
        AnchorPane anchor = new AnchorPane();
        anchor.getStyleClass().add("main-panel");

        VBox darkBox = new VBox(20);
        darkBox.getStyleClass().add("dark-box");
        darkBox.setAlignment(Pos.CENTER);

        double btnWidth = 250;
        Button startBtn = new Button("Start Minecraft");
        startBtn.getStyleClass().addAll("modern-button", "button-green");
        startBtn.setPrefWidth(btnWidth);
        startBtn.setOnAction(e -> {
            Main.simpleLog.log("[LauncherGUI] User klickt auf Start Minecraft...");
            Main.handleStartMinecraft();
        });

        Button updateModsBtn = new Button("Mods aktualisieren");
        updateModsBtn.getStyleClass().add("modern-button");
        updateModsBtn.setPrefWidth(btnWidth);
        updateModsBtn.setOnAction(e -> {
            Main.simpleLog.log("[LauncherGUI] Mods aktualisieren angefordert.");
            try {
                ModInstaller.main(new String[]{});
                showAlert("Update", "Mods wurden erfolgreich aktualisiert.", Alert.AlertType.INFORMATION);
                Main.simpleLog.log("[LauncherGUI] Mods Update erfolgreich.");
            } catch (Exception ex) {
                showAlert("Fehler beim Aktualisieren", ex.getMessage(), Alert.AlertType.ERROR);
                ex.printStackTrace();
                Main.simpleLog.log("[LauncherGUI] Mods Update FEHLER: " + ex.getMessage());
            }
        });

        VBox btnBox = new VBox(15, startBtn, updateModsBtn);
        btnBox.setAlignment(Pos.CENTER);

        HBox iconBox = new HBox(20);
        iconBox.setAlignment(Pos.CENTER);
        iconBox.getChildren().addAll(
                createIcon("/icons/discord.png", "https://discord.com"),
                createIcon("/icons/youtube.png", "https://youtube.com"),
                createIcon("/icons/tiktok.png", "https://tiktok.com")
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        darkBox.getChildren().addAll(btnBox, spacer, iconBox);
        anchor.getChildren().add(darkBox);
        AnchorPane.setTopAnchor(darkBox, 20.0);
        AnchorPane.setLeftAnchor(darkBox, 20.0);
        AnchorPane.setBottomAnchor(darkBox, 20.0);
        return anchor;
    }

    private ImageView createIcon(String path, String url) {
        ImageView iv = new ImageView(new Image(getClass().getResourceAsStream(path)));
        iv.setFitWidth(50);
        iv.setFitHeight(50);
        iv.setPreserveRatio(true);
        iv.setStyle("-fx-cursor: hand;");
        iv.setOnMouseClicked(e -> {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                ex.printStackTrace();
                Main.simpleLog.log("[LauncherGUI] Fehler beim Öffnen des Links: " + ex.getMessage());
            }
        });
        return iv;
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public Button getFooterLoginButton() {
        return footerLoginButton;
    }

    public Label getUsernameLabel() {
        return usernameLabel;
    }
}
