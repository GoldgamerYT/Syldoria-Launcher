package ch.RP.LauncherLib.Gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.scene.image.Image;

import java.io.File;

public class Loader {

    private static Stage downloadStage;
    private static Timeline timeline;
    private static int dotCount = 1;    // Start mit einem Punkt
    private static int direction = 1;   // 1 = ansteigend, -1 = absteigend
    // Für die Drag-Funktionalität des Fensters
    private static double xOffset = 0;
    private static double yOffset = 0;

    public static void showDownloadingWindow() {
        if (downloadStage != null && downloadStage.isShowing()) {
            return;
        }

        // Erstelle das Stage-Objekt vor der weiteren Konfiguration
        downloadStage = new Stage();
        downloadStage.initStyle(StageStyle.TRANSPARENT);

        // Fenstermaße definieren
        double windowWidth = 600;
        double windowHeight = 400;
        double headerHeight = 60; // Höhe des Header-Bereichs

        // Erstelle das Root-Layout mit abgerundeten Ecken via BackgroundFill
        BorderPane root = new BorderPane();
        // Setze einen komplett schwarzen Hintergrund mit abgerundeten Ecken
        root.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(15), Insets.EMPTY)));
        // Optional: Falls du zusätzlich einen leichten DropShadow-Effekt möchtest, kannst du diesen über CSS oder Effects hinzufügen

        // --- Header-Bereich (oben) mit animiertem Text ---
        Label headerLabel = new Label("Launcher starting updating");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #CCCCCC;");
        StackPane headerPane = new StackPane(headerLabel);
        headerPane.setPrefHeight(headerHeight);
        headerPane.setAlignment(Pos.CENTER);
        headerPane.setPadding(new Insets(10));
        // Gestalte den Header passend – hier mit halbtransparentem Schwarz und abgerundeten oberen Ecken
        headerPane.setBackground(new Background(new BackgroundFill(
                Color.rgb(0, 0, 0, 0.8),
                new CornerRadii(15, 15, 0, 0, false),
                Insets.EMPTY
        )));
        root.setTop(headerPane);

        // Timeline zur Animation des Header-Textes (Punkte "bouncen": 1,2,3,2,1, …)
        timeline = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            StringBuilder dots = new StringBuilder();
            for (int i = 0; i < dotCount; i++) {
                dots.append(".");
            }
            headerLabel.setText("Launcher starting updating" + dots.toString());

            // Aktualisiere den Zähler
            dotCount += direction;
            if (dotCount == 3) {
                direction = -1;
            } else if (dotCount == 1) {
                direction = 1;
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // --- Video-Bereich (Center) ---
        String videoPath = "C:/Users/jerom/IdeaProjects/MCLauncher/src/main/resources/icons/loader.mp4";
        File videoFile = new File(videoPath);
        if (!videoFile.exists()) {
            System.err.println("Videodatei nicht gefunden: " + videoPath);
            return;
        }
        Media media = new Media(videoFile.toURI().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Endlosschleife
        MediaView mediaView = new MediaView(mediaPlayer);
        mediaView.setPreserveRatio(true);
        // Video passt sich an die verfügbare Fläche an (Fensterhöhe minus Header)
        mediaView.setFitWidth(windowWidth);
        mediaView.setFitHeight(windowHeight - headerHeight);

        StackPane centerPane = new StackPane(mediaView);
        centerPane.setStyle("-fx-background-color: black;");
        root.setCenter(centerPane);

        // Falls du noch zusätzlich einen exakten Clipping-Effekt möchtest, kannst du optional folgenden Code nutzen:
        /*
        Rectangle clip = new Rectangle(windowWidth, windowHeight);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        root.setClip(clip);
        */

        // Scene mit transparentem Hintergrund (damit keine unerwünschte Farbe außerhalb der abgerundeten Ecken erscheint)
        Scene scene = new Scene(root, windowWidth, windowHeight, Color.TRANSPARENT);

        // Setze Scene, Titel und Icon für das Stage
        downloadStage.setScene(scene);
        downloadStage.setTitle("Launcher starting updating...");
        downloadStage.getIcons().add(new Image(Loader.class.getResourceAsStream("/icons/icon.png")));

        // --- Verschiebbarkeit des Fensters über den Header (Drag-&-Drop) ---
        headerPane.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        headerPane.setOnMouseDragged(event -> {
            downloadStage.setX(event.getScreenX() - xOffset);
            downloadStage.setY(event.getScreenY() - yOffset);
        });

        downloadStage.show();
        mediaPlayer.play();
    }

    public static void closeDownloadingWindow() {
        if (downloadStage != null) {
            downloadStage.close();
            downloadStage = null;
        }
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }
}
