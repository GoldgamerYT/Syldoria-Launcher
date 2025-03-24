package ch.RP.LauncherLib.Gui;

import ch.RP.LauncherLib.Authentication;
import ch.RP.LauncherLib.Features.Log.SimpleLogWindow;
import ch.RP.LauncherLib.Main;
import ch.RP.LauncherLib.ModInstaller;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.BorderLayout;
import javax.swing.JOptionPane;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

public class LauncherWebGUI {

    private static final int PORT = 8080;
    private HttpServer server;
    private Authentication auth;
    private static final Logger LOGGER = Logger.getLogger("LauncherWebGUI");

    public LauncherWebGUI() {
        try {
            auth = new Authentication();
            server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Statische Dateien ausliefern – achte darauf, dass diese im Ressourcenpfad liegen.
            server.createContext("/", new StaticFileHandler("/web/index.html", "text/html"));
            server.createContext("/css/style.css", new StaticFileHandler("/web/css/style.css", "text/css"));
            // Admin‑Seite als statische Datei (z. B. für eine reine Web‑Oberfläche)
            server.createContext("/adminPage", new StaticFileHandler("/web/admin.html", "text/html"));

            // Endpunkte für Aktionen
            server.createContext("/login", new LoginHandler());
            server.createContext("/start", new StartHandler());
            server.createContext("/stop", new StopHandler());
            server.createContext("/updateMods", new UpdateModsHandler());

            // Neuer Endpunkt für Auto‑Login (Web‑Flow)
            server.createContext("/autologin", new AutoLoginHandler());

            // Endpunkte für die Settings‑WebGUI
            server.createContext("/settings", new StaticFileHandler("/web/settings.html", "text/html"));
            server.createContext("/settings/info", new SettingsInfoHandler());
            server.createContext("/toggleConsole", new ToggleConsoleHandler());
            server.createContext("/logout", new LogoutHandler());
            server.createContext("/showLog", new ShowLogHandler());
            server.createContext("/admin", new AdminHandler());

            // Neuer Endpunkt, um den Minecraft‑Status abzufragen
            server.createContext("/minecraftStatus", new MinecraftStatusHandler());

            // Neuer Endpunkt, um den Launcher zu schließen
            server.createContext("/closeLauncher", new CloseLauncherHandler());

            server.setExecutor(null); // Standard‑Executor verwenden
            server.start();
            LOGGER.info("Server gestartet auf Port " + PORT);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Liefert statische Dateien aus dem Klassenpfad.
     */
    class StaticFileHandler implements HttpHandler {
        private final String filePath;
        private final String contentType;

        public StaticFileHandler(String filePath, String contentType) {
            this.filePath = filePath;
            this.contentType = contentType;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            InputStream is = getClass().getResourceAsStream(filePath);
            if (is == null) {
                LOGGER.warning("Ressource nicht gefunden: " + filePath);
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            byte[] data = readAllBytes(is);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, data.length);
            OutputStream os = exchange.getResponseBody();
            os.write(data);
            os.close();
        }
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    /**
     * Login‑Handler: Ruft Authentication.handleLogin auf.
     * Dabei wird der externe Browser gestartet – nach erfolgreichem Login sollten die Nutzer‑Daten gesetzt werden.
     */
    class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                LOGGER.info("Login‑Request empfangen.");
                // Dummy‑Komponenten, da der Callback in Authentication Labels/Buttons benötigt.
                Label dummyLabel = new Label("Nicht angemeldet");
                Button dummyButton = new Button("Login");
                auth.handleLogin(dummyLabel, dummyButton);
                String response = "{\"status\":\"success\",\"message\":\"Login angestoßen. Bitte schließe das Browserfenster, sobald du angemeldet bist.\"}";
                sendJsonResponse(exchange, response);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    /**
     * AutoLogin‑Handler: Ruft Authentication.autoLoginWeb() auf, um gespeicherte Credentials zu laden.
     */
    class AutoLoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = auth.autoLoginWeb();
                sendJsonResponse(exchange, response);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    /**
     * StartHandler: Startet Minecraft.
     */
    class StartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                LOGGER.info("Start‑Minecraft‑Request empfangen.");
                Main.handleStartMinecraft();
                String response = "{\"status\":\"success\", \"message\":\"Minecraft gestartet\"}";
                sendJsonResponse(exchange, response);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    /**
     * StopHandler: Stoppt Minecraft.
     */
    class StopHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                LOGGER.info("Stop‑Minecraft‑Request empfangen.");
                Main.handleStopMinecraft();
                String response = "{\"status\":\"success\", \"message\":\"Minecraft wird gestoppt\"}";
                sendJsonResponse(exchange, response);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    /**
     * UpdateModsHandler: Aktualisiert die Mods.
     */
    class UpdateModsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                LOGGER.info("UpdateMods‑Request empfangen.");
                try {
                    ModInstaller.main(new String[]{});
                    String response = "{\"status\":\"success\", \"message\":\"Mods erfolgreich aktualisiert\"}";
                    sendJsonResponse(exchange, response);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    String response = "{\"status\":\"error\", \"message\":\"" + ex.getMessage() + "\"}";
                    sendJsonResponse(exchange, response);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    /**
     * SettingsInfoHandler: Liefert Infos zu den Einstellungen (z. B. RAM, Debug‑Status, Admin‑Rechte).
     */
    class SettingsInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                long totalRam = ((com.sun.management.OperatingSystemMXBean)
                        java.lang.management.ManagementFactory.getOperatingSystemMXBean())
                        .getTotalPhysicalMemorySize() / (1024L * 1024L * 1024L);
                int installedRam = (int) totalRam;
                int selectedRam = Math.max(1, (int) (installedRam * 0.75));
                boolean loggingEnabled = Main.loggingEnabled;
                boolean isAdmin = Main.isAdmin;
                String response = "{\"selectedRam\":\"" + selectedRam + " GB\", " +
                        "\"loggingEnabled\":" + loggingEnabled + ", " +
                        "\"isAdmin\":" + isAdmin + "}";
                sendJsonResponse(exchange, response);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    /**
     * ToggleConsoleHandler: Schaltet den Debug‑Modus (Konsole anzeigen) um.
     */
    class ToggleConsoleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                boolean enabled = body.contains("\"enabled\":true");
                Main.loggingEnabled = enabled;
                Main.simpleLog.log("[ToggleConsoleHandler] Logging enabled: " + enabled);
                String response = "{\"status\":\"success\", \"loggingEnabled\":" + enabled + "}";
                sendJsonResponse(exchange, response);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    /**
     * LogoutHandler: Meldet den Nutzer ab.
     */
    class LogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Dummy‑Komponenten für die UI (werden hier nur benötigt, um die Methode aufzurufen)
                Label dummyLabel = new Label("Nicht angemeldet");
                Button dummyButton = new Button("Login");
                auth.handleLogout(dummyLabel, dummyButton);

                String response = "{\"status\":\"success\", \"message\":\"Logout erfolgreich\"}";
                sendJsonResponse(exchange, response);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    /**
     * ShowLogHandler: Öffnet das Log-Fenster mit erweiterter Funktionalität.
     */
    class ShowLogHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    // Direkt eine Antwort senden, bevor wir versuchen, das Log-Fenster zu öffnen
                    String response = "{\"status\":\"success\", \"message\":\"Log window opened in background\"}";
                    sendJsonResponse(exchange, response);
                    
                    // Dann in einem separaten Thread versuchen, das Log-Fenster zu öffnen
                    new Thread(() -> {
                        try {
                            System.out.println("Erstelle verbessertes Log-Fenster...");
                            
                            // Erstelle ein modernes Swing-Fenster für die Logs
                            JFrame frame = new JFrame("Syldoria Launcher - Logs");
                            
                            // Versuche das Icon zu laden, aber fange Fehler ab
                            try {
                                InputStream iconStream = getClass().getResourceAsStream("/web/images/favicon.ico");
                                if (iconStream != null) {
                                    frame.setIconImage(javax.imageio.ImageIO.read(iconStream));
                                }
                            } catch (Exception e) {
                                System.out.println("Konnte Icon nicht laden: " + e.getMessage());
                                // Kein Abbruch, wenn das Icon nicht geladen werden kann
                            }
                            
                            // Haupttextbereich für Logs
                            JTextArea textArea = new JTextArea();
                            textArea.setEditable(false);
                            textArea.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 12));
                            textArea.setBackground(new Color(18, 18, 18));  // Dunkleres Hintergrundfarbe
                            textArea.setForeground(new Color(220, 220, 220));
                            textArea.setCaretColor(Color.WHITE);
                            
                            // Fülle das TextArea mit den Logs
                            textArea.setText(Main.simpleLog.getLogText());
                            
                            // Scrollpane mit dunklem Design
                            JScrollPane scrollPane = new JScrollPane(textArea);
                            scrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder());
                            scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
                                @Override
                                protected void configureScrollBarColors() {
                                    this.thumbColor = new Color(60, 60, 60);
                                    this.trackColor = new Color(30, 30, 30);
                                }
                            });
                            
                            // Erstelle einen Timer, der die Logs regelmäßig aktualisiert
                            Timer timer = new Timer(1000, e -> {
                                String newText = Main.simpleLog.getLogText();
                                if (!textArea.getText().equals(newText)) {
                                    boolean shouldScroll = textArea.getCaretPosition() == textArea.getText().length();
                                    textArea.setText(newText);
                                    if (shouldScroll) {
                                        textArea.setCaretPosition(textArea.getText().length());
                                    }
                                }
                            });
                            timer.start();
                            
                            // Suchfunktion
                            JPanel searchPanel = new JPanel(new BorderLayout());
                            searchPanel.setBackground(new Color(25, 25, 25));
                            
                            javax.swing.JTextField searchField = new javax.swing.JTextField();
                            searchField.setBackground(new Color(40, 40, 40));
                            searchField.setForeground(Color.WHITE);
                            searchField.setCaretColor(Color.WHITE);
                            searchField.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                                    javax.swing.BorderFactory.createLineBorder(new Color(60, 60, 60)),
                                    javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5)));
                            
                            JButton searchButton = createStyledButton("Suchen");
                            searchButton.addActionListener(e -> searchInLogs(searchField.getText(), textArea));
                            
                            JLabel searchLabel = new JLabel("  Suche: ");
                            searchLabel.setForeground(Color.WHITE);
                            searchPanel.add(searchLabel, BorderLayout.WEST);
                            searchPanel.add(searchField, BorderLayout.CENTER);
                            searchPanel.add(searchButton, BorderLayout.EAST);
                            searchPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
                            
                            // Erstelle ein Panel für die Buttons
                            JPanel buttonPanel = new JPanel();
                            buttonPanel.setBackground(new Color(25, 25, 25));
                            buttonPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
                            
                            // Erstelle Buttons mit einheitlichem Stil
                            JButton copyButton = createStyledButton("Kopieren");
                            copyButton.addActionListener(e -> {
                                textArea.selectAll();
                                textArea.copy();
                                textArea.setCaretPosition(textArea.getText().length());
                            });
                            
                            JButton saveButton = createStyledButton("Speichern");
                            saveButton.addActionListener(e -> saveLogsToFile(textArea.getText()));
                            
                            JButton clearButton = createStyledButton("Löschen");
                            clearButton.addActionListener(e -> {
                                // Statt clearLogs() zu verwenden, setzen wir den Text direkt
                                // und loggen eine Nachricht, dass die Logs gelöscht wurden
                                Main.simpleLog.log("[Log] Logs wurden gelöscht");
                                textArea.setText(Main.simpleLog.getLogText());
                            });
                            
                            JButton closeButton = createStyledButton("Schließen");
                            closeButton.addActionListener(e -> {
                                timer.stop();
                                frame.dispose();
                            });
                            
                            // Filter-Dropdown
                            String[] filterOptions = {"Alle Logs", "Nur Fehler", "Nur Warnungen", "Nur Info"};
                            javax.swing.JComboBox<String> filterComboBox = new javax.swing.JComboBox<>(filterOptions);
                            filterComboBox.setBackground(new Color(40, 40, 40));
                            filterComboBox.setForeground(Color.WHITE);
                            filterComboBox.addActionListener(e -> filterLogs(filterComboBox.getSelectedIndex(), textArea));
                            
                            // Füge die Buttons zum Button-Panel hinzu
                            JLabel filterLabel = new JLabel("Filter: ");
                            filterLabel.setForeground(Color.WHITE);
                            buttonPanel.add(filterLabel);
                            buttonPanel.add(filterComboBox);
                            buttonPanel.add(copyButton);
                            buttonPanel.add(saveButton);
                            buttonPanel.add(clearButton);
                            buttonPanel.add(closeButton);
                            
                            // Erstelle ein Haupt-Panel für die Anordnung
                            JPanel mainPanel = new JPanel(new BorderLayout());
                            mainPanel.setBackground(new Color(25, 25, 25));
                            mainPanel.add(searchPanel, BorderLayout.NORTH);
                            mainPanel.add(scrollPane, BorderLayout.CENTER);
                            mainPanel.add(buttonPanel, BorderLayout.SOUTH);
                            
                            // Füge alles zum Frame hinzu
                            frame.getContentPane().add(mainPanel);
                            
                            frame.setSize(900, 600);
                            frame.setLocationRelativeTo(null);
                            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                                @Override
                                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                                    timer.stop();
                                }
                            });
                            
                            SwingUtilities.invokeLater(() -> {
                                frame.setVisible(true);
                                frame.toFront();
                                textArea.setCaretPosition(textArea.getText().length());
                            });
                            
                            System.out.println("Verbessertes Log-Fenster sollte jetzt sichtbar sein!");
                        } catch (Exception e) {
                            System.err.println("Fehler beim Anzeigen der Logs: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }).start();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    String response = "{\"status\":\"error\", \"message\":\"Error opening log window: " + e.getMessage() + "\"}";
                    sendJsonResponse(exchange, response);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
        
        private JButton createStyledButton(String text) {
            JButton button = new JButton(text);
            button.setBackground(new Color(45, 45, 45));
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(new Color(60, 60, 60), 1),
                    javax.swing.BorderFactory.createEmptyBorder(5, 10, 5, 10)));
            return button;
        }
        
        private void searchInLogs(String searchText, JTextArea textArea) {
            if (searchText == null || searchText.isEmpty()) {
                return;
            }
            
            String text = textArea.getText();
            textArea.setCaretPosition(0);
            
            int index = text.indexOf(searchText);
            if (index != -1) {
                textArea.select(index, index + searchText.length());
                textArea.requestFocus();
            } else {
                JOptionPane.showMessageDialog(null, 
                        "Text '" + searchText + "' nicht gefunden", 
                        "Suche", 
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
        
        private void filterLogs(int filterIndex, JTextArea textArea) {
            String fullLog = Main.simpleLog.getLogText();
            
            switch (filterIndex) {
                case 0: // Alle Logs
                    textArea.setText(fullLog);
                    break;
                case 1: // Nur Fehler
                    filterLogsByKeyword(textArea, fullLog, "ERROR", "FEHLER", "Exception");
                    break;
                case 2: // Nur Warnungen
                    filterLogsByKeyword(textArea, fullLog, "WARN", "WARNUNG");
                    break;
                case 3: // Nur Info
                    filterLogsByKeyword(textArea, fullLog, "INFO");
                    break;
            }
            
            textArea.setCaretPosition(0);
        }
        
        private void filterLogsByKeyword(JTextArea textArea, String fullLog, String... keywords) {
            StringBuilder filteredLog = new StringBuilder();
            String[] lines = fullLog.split("\n");
            
            for (String line : lines) {
                for (String keyword : keywords) {
                    if (line.contains(keyword)) {
                        filteredLog.append(line).append("\n");
                        break;
                    }
                }
            }
            
            textArea.setText(filteredLog.toString());
        }
        
        private void saveLogsToFile(String logText) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
                String timestamp = java.time.LocalDateTime.now().format(formatter);
                
                File logsDir = new File(System.getProperty("user.home"), "SyldoriaLauncher/logs");
                if (!logsDir.exists()) {
                    logsDir.mkdirs();
                }
                
                File logFile = new File(logsDir, "launcher_log_" + timestamp + ".txt");
                
                try (FileWriter writer = new FileWriter(logFile)) {
                    writer.write(logText);
                }
                
                JOptionPane.showMessageDialog(null, 
                        "Log gespeichert unter:\n" + logFile.getAbsolutePath(), 
                        "Log gespeichert", 
                        JOptionPane.INFORMATION_MESSAGE);
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, 
                        "Fehler beim Speichern: " + e.getMessage(), 
                        "Fehler", 
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * AdminHandler: Führt Admin‑spezifische Aktionen aus.
     * Bei erfolgreicher Prüfung (Main.isAdmin == true) wird z. B. ein Admin‑Fenster (Stage) geöffnet.
     */
    class AdminHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                if (Main.isAdmin) {
                    Main.simpleLog.log("[AdminHandler] Admin Aktion ausgeführt.");
                    // Öffne Admin‑Fenster in der JavaFX‑Anwendung
                    Main.openAdminWindow();
                    String response = "{\"status\":\"success\", \"message\":\"Admin Aktion erfolgreich\"}";
                    sendJsonResponse(exchange, response);
                } else {
                    String response = "{\"status\":\"error\", \"message\":\"Keine Admin Rechte\"}";
                    sendJsonResponse(exchange, response);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    /**
     * MinecraftStatusHandler: Gibt den Status zurück, ob Minecraft läuft.
     */
    class MinecraftStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                boolean running = Main.minecraftRunning;
                String response = "{\"running\":" + running + "}";
                sendJsonResponse(exchange, response);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    /**
     * CloseLauncherHandler: Schließt den Launcher.
     */
    class CloseLauncherHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                Main.handleCloseLauncher();
                String response = "{\"status\":\"success\", \"message\":\"Launcher wird geschlossen\"}";
                sendJsonResponse(exchange, response);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private void sendJsonResponse(HttpExchange exchange, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }

    public static void main(String[] args) {
        new LauncherWebGUI();
    }
}
