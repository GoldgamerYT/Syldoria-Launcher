package ch.RP.LauncherLib.Features.Log;

import ch.RP.LauncherLib.Launcher.Var;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.output.TeeOutputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Fix für Paketname-Warnung - das kann ignoriert werden, solange der Code funktioniert

public class SimpleLogWindow {
    private static SimpleLogWindow instance;
    private TextArea textArea;
    private Stage stage;
    private CheckBox autoScrollCheckBox;
    private Label statusLabel;
    private TextField searchField;
    private ComboBox<LogLevel> logLevelFilter;
    private TabPane logTabs;
    private int entryCount = 0;
    private int maxEntries = 5000; // Maximale Anzahl von Log-Einträgen
    private final ConcurrentLinkedQueue<LogEntry> logQueue = new ConcurrentLinkedQueue<>();
    private ScheduledExecutorService logProcessor;
    private boolean darkMode = true;

    // Konstanten für Styling
    private static final String DARK_BACKGROUND = "#1E1E1E";
    private static final String LIGHT_BACKGROUND = "#F5F5F5";
    private static final String DARK_TEXT = "#E0E0E0";
    private static final String LIGHT_TEXT = "#333333";

    public enum LogLevel {
        INFO("Info", Color.WHITE),
        DEBUG("Debug", Color.LIGHTBLUE),
        WARNING("Warning", Color.ORANGE),
        ERROR("Error", Color.RED);

        private final String label;
        private final Color color;

        LogLevel(String label, Color color) {
            this.label = label;
            this.color = color;
        }

        @Override
        public String toString() {
            return label;
        }

        public Color getColor() {
            return color;
        }
    }

    private static class LogEntry {
        private final String message;
        private final LogLevel level;
        private final LocalDateTime timestamp;

        public LogEntry(String message, LogLevel level) {
            this.message = message;
            this.level = level;
            this.timestamp = LocalDateTime.now();
        }

        public String getFormattedMessage() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            return String.format("[%s] [%s] %s",
                    formatter.format(timestamp),
                    level.toString().toUpperCase(),
                    message);
        }

        public LogLevel getLevel() {
            return level;
        }
    }

    private SimpleLogWindow() {
        initializeUI();
        startLogProcessor();
        redirectSystemStreams();
    }

    private void initializeUI() {
        // Erstellen der Tabs für verschiedene Log-Kategorien
        logTabs = new TabPane();

        // Hauptlog Tab
        Tab mainLogTab = new Tab("Hauptlog");
        mainLogTab.setClosable(false);
        textArea = createLogTextArea();
        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        mainLogTab.setContent(scrollPane);

        // Fehler Tab
        Tab errorTab = new Tab("Fehler");
        errorTab.setClosable(false);
        TextArea errorTextArea = createLogTextArea();
        ScrollPane errorScrollPane = new ScrollPane(errorTextArea);
        errorScrollPane.setFitToWidth(true);
        errorScrollPane.setFitToHeight(true);
        errorTab.setContent(errorScrollPane);

        logTabs.getTabs().addAll(mainLogTab, errorTab);

        // Suchleiste
        searchField = new TextField();
        searchField.setPromptText("Suchen...");
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                highlightSearchResults(searchField.getText());
            }
        });
        Button searchButton = createStyledButton("Suchen", "#3A86FF");
        searchButton.setOnAction(e -> highlightSearchResults(searchField.getText()));

        // Log-Level Filter
        logLevelFilter = new ComboBox<>();
        logLevelFilter.getItems().addAll(LogLevel.values());
        logLevelFilter.setValue(LogLevel.INFO);
        logLevelFilter.setOnAction(e -> applyLogLevelFilter());

        // AutoScroll Checkbox
        autoScrollCheckBox = new CheckBox("Autoscroll");
        autoScrollCheckBox.setSelected(true);

        // Buttons
        Button copyButton = createStyledButton("Kopieren", "#3A86FF");
        copyButton.setOnAction(e -> {
            textArea.selectAll();
            textArea.copy();
        });

        Button clearButton = createStyledButton("Löschen", "#E53935");
        clearButton.setOnAction(e -> clearLogs());

        Button exportButton = createStyledButton("Exportieren", "#4CAF50");
        exportButton.setOnAction(e -> exportLogToFile());

        Button themeToggleButton = createStyledButton("Designwechsel", "#9C27B0");
        themeToggleButton.setOnAction(e -> toggleTheme());

        // Status Label
        statusLabel = new Label("0 Einträge");

        // Layout für Steuerelemente
        HBox searchBox = new HBox(5, new Label("Suche:"), searchField, searchButton);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        HBox filterBox = new HBox(5, new Label("Log-Level:"), logLevelFilter, autoScrollCheckBox);
        filterBox.setAlignment(Pos.CENTER);

        HBox buttonBox = new HBox(5, copyButton, clearButton, exportButton, themeToggleButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        // Obere Toolbar
        HBox topControls = new HBox(10, searchBox, filterBox, buttonBox);
        topControls.setPadding(new Insets(10));
        topControls.setAlignment(Pos.CENTER);

        // Statusleiste
        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-background-color: #333333;");

        // Hauptlayout
        VBox layout = new VBox(10, topControls, logTabs, statusBar);
        layout.setPadding(new Insets(10));
        VBox.setVgrow(logTabs, Priority.ALWAYS);

        // Tastenkombinationen für Shortcuts
        Scene scene = new Scene(layout, 900, 700);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN),
                () -> searchField.requestFocus()
        );

        // Stage konfigurieren
        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Minecraft Logs");
        stage.setOnCloseRequest(e -> {
            Var.loggingEnabled = false;
            stopLogProcessor();
        });

        // Initial Styling anwenden
        applyTheme();
    }

    private TextArea createLogTextArea() {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(true);
        area.setFont(javafx.scene.text.Font.font("Monospaced", 12));
        return area;
    }

    private Button createStyledButton(String text, String baseColor) {
        Button button = new Button(text);
        button.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-padding: 5 15;",
                baseColor));
        return button;
    }

    private void highlightSearchResults(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return;
        }

        String content = textArea.getText();
        textArea.clear();

        // Einfacher Fall: Text hervorheben (in einer echten Anwendung
        // würde man hier eine richtige TextFlow-Implementierung verwenden)
        Pattern pattern = Pattern.compile(searchText, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        StringBuilder highlightedText = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            highlightedText.append(content, lastEnd, matcher.start());
            // In einer echten Implementierung würden wir hier TextFlow mit
            // verschiedenen TextFlow-Nodes verwenden
            highlightedText.append(">>>").append(matcher.group()).append("<<<");
            lastEnd = matcher.end();
        }

        highlightedText.append(content.substring(lastEnd));
        textArea.setText(highlightedText.toString());

        // Update Status
        statusLabel.setText(String.format("Suchergebnisse für '%s'", searchText));
    }

    private void applyLogLevelFilter() {
        LogLevel selectedLevel = logLevelFilter.getValue();
        if (selectedLevel == null) {
            return;
        }

        // In einer echten Implementierung würden wir alle Logs filtern
        // Hier fügen wir nur einen Hinweis ein, dass der Filter angewendet wurde
        log("Log-Level Filter auf " + selectedLevel + " gesetzt", LogLevel.INFO);
    }

    private void clearLogs() {
        textArea.clear();
        ((TextArea)((ScrollPane)logTabs.getTabs().get(1).getContent()).getContent()).clear();
        entryCount = 0;
        updateStatusLabel();
    }

    private void exportLogToFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Log speichern");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Log Dateien", "*.log"),
                new FileChooser.ExtensionFilter("Textdateien", "*.txt"),
                new FileChooser.ExtensionFilter("Alle Dateien", "*.*")
        );

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                Files.writeString(file.toPath(), textArea.getText(), StandardCharsets.UTF_8);
                log("Log erfolgreich gespeichert: " + file.getAbsolutePath(), LogLevel.INFO);
            } catch (IOException e) {
                log("Fehler beim Speichern: " + e.getMessage(), LogLevel.ERROR);
            }
        }
    }

    private void toggleTheme() {
        darkMode = !darkMode;
        applyTheme();
    }

    private void applyTheme() {
        String backgroundColor = darkMode ? DARK_BACKGROUND : LIGHT_BACKGROUND;
        String textColor = darkMode ? DARK_TEXT : LIGHT_TEXT;
        String controlBg = darkMode ? "#252525" : "#E0E0E0";

        String textAreaStyle = String.format(
                "-fx-control-inner-background: %s; -fx-font-family: 'Consolas'; -fx-font-size: 14px; -fx-text-fill: %s;",
                backgroundColor, textColor);

        textArea.setStyle(textAreaStyle);
        ((TextArea)((ScrollPane)logTabs.getTabs().get(1).getContent()).getContent()).setStyle(textAreaStyle);

        stage.getScene().getRoot().setStyle(String.format("-fx-background-color: %s;", darkMode ? "#121212" : "#F0F0F0"));

        // Styling für Steuerelemente anwenden
        VBox root = (VBox)stage.getScene().getRoot();
        if (root.getChildren().size() > 0 && root.getChildren().get(0) instanceof HBox) {
            HBox topBar = (HBox)root.getChildren().get(0);
            for (Node node : topBar.getChildren()) {
                if (node instanceof HBox) {
                    ((HBox)node).setStyle(String.format("-fx-background-color: %s;", controlBg));
                }
            }
        }

        statusLabel.setStyle(String.format("-fx-text-fill: %s;", textColor));
        ((HBox)((VBox)stage.getScene().getRoot()).getChildren().get(2)).setStyle(
                String.format("-fx-background-color: %s;", controlBg));
    }

    private void startLogProcessor() {
        logProcessor = Executors.newSingleThreadScheduledExecutor();
        logProcessor.scheduleAtFixedRate(this::processLogQueue, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void stopLogProcessor() {
        if (logProcessor != null && !logProcessor.isShutdown()) {
            logProcessor.shutdown();
            try {
                logProcessor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processLogQueue() {
        if (logQueue.isEmpty()) {
            return;
        }

        // Sammele Logs für einen Batch-Update
        StringBuilder mainLogBatch = new StringBuilder();
        StringBuilder errorLogBatch = new StringBuilder();

        int processedCount = 0;
        LogEntry entry;

        while ((entry = logQueue.poll()) != null && processedCount < 100) {
            String formattedMsg = entry.getFormattedMessage() + "\n";
            mainLogBatch.append(formattedMsg);

            // Fehler-Logs auch im Fehler-Tab anzeigen
            if (entry.getLevel() == LogLevel.ERROR || entry.getLevel() == LogLevel.WARNING) {
                errorLogBatch.append(formattedMsg);
            }

            processedCount++;
        }

        if (processedCount > 0) {
            final String mainUpdate = mainLogBatch.toString();
            final String errorUpdate = errorLogBatch.toString();

            int finalProcessedCount = processedCount;
            Platform.runLater(() -> {
                // Hauptlog aktualisieren
                textArea.appendText(mainUpdate);
                trimLogIfNeeded(textArea);

                // Fehlerlog aktualisieren
                TextArea errorArea = (TextArea)((ScrollPane)logTabs.getTabs().get(1).getContent()).getContent();
                errorArea.appendText(errorUpdate);
                trimLogIfNeeded(errorArea);

                // Autoscroll
                if (autoScrollCheckBox.isSelected()) {
                    textArea.setScrollTop(Double.MAX_VALUE);
                    errorArea.setScrollTop(Double.MAX_VALUE);
                }

                // Status aktualisieren
                entryCount += finalProcessedCount;
                updateStatusLabel();
            });
        }
    }

    private void trimLogIfNeeded(TextArea logArea) {
        if (entryCount > maxEntries) {
            String text = logArea.getText();
            int cutIndex = text.indexOf('\n', text.length() / 2);
            if (cutIndex > 0) {
                logArea.setText(text.substring(cutIndex + 1));
            }
        }
    }

    private void updateStatusLabel() {
        statusLabel.setText(String.format("%d Einträge", entryCount));
    }

    public static SimpleLogWindow getInstance() {
        if (instance == null) {
            instance = new SimpleLogWindow();
        }
        return instance;
    }

    public void log(String msg) {
        log(msg, LogLevel.INFO);
    }

    public void log(String msg, LogLevel level) {
        logQueue.add(new LogEntry(msg, level));
    }

    public void debug(String msg) {
        log(msg, LogLevel.DEBUG);
    }

    public void warn(String msg) {
        log(msg, LogLevel.WARNING);
    }

    public void error(String msg) {
        log(msg, LogLevel.ERROR);
    }

    public void showLogWindow() {
        System.out.println("SimpleLogWindow.showLogWindow() aufgerufen");
        
        Platform.runLater(() -> {
            try {
                if (stage == null || !stage.isShowing()) {
                    System.out.println("Erstelle neues Log-Fenster oder zeige existierendes");
                    
                    if (stage == null) {
                        stage = new Stage();
                        VBox root = new VBox(10);
                        root.setPadding(new Insets(10));
                        
                        textArea = new TextArea();
                        textArea.setEditable(false);
                        textArea.setFont(javafx.scene.text.Font.font("Monospaced", 12));
                        
                        Button closeButton = new Button("Schließen");
                        closeButton.setOnAction(e -> stage.hide());
                        
                        root.getChildren().addAll(textArea, closeButton);
                        VBox.setVgrow(textArea, Priority.ALWAYS);
                        
                        Scene scene = new Scene(root, 800, 600);
                        stage.setScene(scene);
                        stage.setTitle("Syldoria Launcher - Logs");
                    }
                    
                    // Aktualisiere den Text
                    if (textArea != null) {
                        textArea.setText(getLogText());
                        textArea.setScrollTop(Double.MAX_VALUE);
                    }
                    
                    // Zeige das Fenster an
                    System.out.println("Mache Log-Fenster sichtbar");
                    stage.show();
                    stage.toFront();
                } else {
                    System.out.println("Log-Fenster ist bereits sichtbar");
                    stage.toFront();
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Anzeigen des Log-Fensters: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void closeLogWindow() {
        Platform.runLater(() -> {
            if (stage.isShowing()) {
                stage.hide();
            }
        });
    }

    public String getLogText() {
        return textArea.getText();
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    // Umleitung der System-Streams mit Verbesserter Performance
    private void redirectSystemStreams() {
        // Originale Streams speichern
        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;

        // Einen neuen OutputStream für System.out
        OutputStream logOut = new BufferedOutputStream(new OutputStream() {
            private final StringBuilder lineBuilder = new StringBuilder();

            @Override
            public void write(int b) throws IOException {
                char c = (char) b;
                if (c == '\n') {
                    // Bei Zeilenumbruch das gesammelte Log ausgeben
                    log(lineBuilder.toString());
                    lineBuilder.setLength(0);
                } else {
                    lineBuilder.append(c);
                }

                // Zum Original-Stream weiterleiten
                originalOut.write(b);
            }
        });

        // Einen neuen OutputStream für System.err
        OutputStream logErr = new BufferedOutputStream(new OutputStream() {
            private final StringBuilder lineBuilder = new StringBuilder();

            @Override
            public void write(int b) throws IOException {
                char c = (char) b;
                if (c == '\n') {
                    // Bei Zeilenumbruch das gesammelte Log ausgeben
                    error(lineBuilder.toString());
                    lineBuilder.setLength(0);
                } else {
                    lineBuilder.append(c);
                }

                // Zum Original-Stream weiterleiten
                originalErr.write(b);
            }
        });

        // System.out und System.err umleiten
        System.setOut(new PrintStream(logOut, true));
        System.setErr(new PrintStream(logErr, true));
    }

    // Helper-Methode zum Speichern aller Logs automatisch
    public void autoSaveLogEvery(int minutes, String directory) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Erstelle Verzeichnis falls notwendig
                Path dir = Paths.get(directory);
                if (!Files.exists(dir)) {
                    Files.createDirectories(dir);
                }

                // Dateiname mit Zeitstempel
                String timestamp = LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                Path logFile = dir.resolve("minecraft_log_" + timestamp + ".log");

                // Log speichern
                Files.writeString(logFile, getLogText(), StandardCharsets.UTF_8);
                log("Log automatisch gespeichert: " + logFile, LogLevel.INFO);
            } catch (IOException e) {
                error("Fehler beim automatischen Speichern: " + e.getMessage());
            }
        }, minutes, minutes, TimeUnit.MINUTES);
    }
}