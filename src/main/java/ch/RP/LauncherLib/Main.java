package ch.RP.LauncherLib;

import ch.RP.LauncherLib.Features.Log.SimpleLogWindow;
import ch.RP.LauncherLib.Gui.Loader;
import ch.RP.LauncherLib.ModInstaller;
import ch.RP.LauncherLib.Gui.SettingsGUI;
import ch.RP.LauncherLib.Gui.LauncherWebGUI; // Startet den internen Webserver

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.handler.CefJSDialogHandlerAdapter;
import org.cef.callback.CefJSDialogCallback;
import org.cef.misc.BoolRef;
import org.cef.handler.CefJSDialogHandler.JSDialogType;

import org.json.JSONArray;
import org.json.JSONObject;
import org.to2mbn.jmccc.auth.AuthInfo;
import org.to2mbn.jmccc.auth.AuthenticationException;
import org.to2mbn.jmccc.auth.Authenticator;
import org.to2mbn.jmccc.launch.Launcher;
import org.to2mbn.jmccc.launch.LauncherBuilder;
import org.to2mbn.jmccc.mcdownloader.AssetOption;
import org.to2mbn.jmccc.mcdownloader.MinecraftDownloader;
import org.to2mbn.jmccc.mcdownloader.MinecraftDownloaderBuilder;
import org.to2mbn.jmccc.mcdownloader.download.concurrent.CallbackAdapter;
import org.to2mbn.jmccc.mcdownloader.provider.DownloadProviderChain;
import org.to2mbn.jmccc.mcdownloader.provider.forge.ForgeDownloadProvider;
import org.to2mbn.jmccc.option.LaunchOption;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.version.Version;

import java.awt.Desktop;

public class Main extends Application {

    public static String username = "";
    public static String uuid = "";
    public static String accessToken = "";
    public static String xboxUserId = "";

    public static final SimpleLogWindow simpleLog = SimpleLogWindow.getInstance();
    public static boolean loggingEnabled = true; // Logging standardmäßig aktivieren

    private static Launcher jmcccLauncher;
    private static MinecraftDownloader downloader;

    public static int MaxRam;
    public static SettingsGUI settingsGUI;

    public static boolean isAdmin = false;
    public static volatile boolean minecraftRunning = false;
    public static volatile Process minecraftProcess = null;

    // Referenz auf das JCEF-Fenster
    private static JFrame cefFrame;

    // Zusätzliche Flags für die Fortschrittsverfolgung
    private static volatile boolean forgeInstallationStarted = false;
    private static volatile boolean forgeInstallationCompleted = false;
    private static volatile boolean modInstallationCompleted = false;
    private static volatile boolean launcherGUIStarted = false;

    @Override
    public void start(Stage primaryStage) {
        try {
            simpleLog.log("[Main] Launcher wird gestartet...");
            // Lade-Fenster anzeigen
            Loader.showDownloadingWindow();

            // Initialisierung in separatem Thread durchführen
            Thread initThread = new Thread(() -> {
                try {
                    // 1. Forge installieren (falls nötig) und Mods installieren
                    installForgeIfNeeded();

                    // 2. Den Launcher initialisieren
                    jmcccLauncher = LauncherBuilder.create().build();
                    simpleLog.log("[Main] JMCCC Launcher initialisiert.");

                    // 3. Die GUI im JavaFX-Thread starten
                    Platform.runLater(() -> startGUI(primaryStage));

                } catch (Exception e) {
                    e.printStackTrace();
                    simpleLog.log("[Main] Kritischer Fehler bei der Initialisierung: " + e.getMessage());
                    Platform.runLater(() -> {
                        Loader.closeDownloadingWindow();
                        showError("Fehler beim Starten", e.getMessage());
                    });
                }
            }, "LauncherInitThread");

            initThread.setDaemon(false); // Damit der Thread die App am Laufen hält
            initThread.start();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Fehler beim Starten", e.getMessage());
        }
    }

    /**
     * Startet die GUI-Komponenten der Anwendung
     */
    private void startGUI(Stage primaryStage) {
        try {
            // Lade-Fenster schließen
            Loader.closeDownloadingWindow();
            simpleLog.log("[Main] Lade-Fenster geschlossen.");

            // WebGUI-Server starten
            new LauncherWebGUI();
            simpleLog.log("[Main] WebGUI-Server gestartet.");

            // Kurze Wartezeit, damit der Server vollständig starten kann
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                simpleLog.log("[Main] Wartezeit für Server-Start unterbrochen: " + e.getMessage());
            }

            // JCEF-GUI starten
            launchJCEF();
            simpleLog.log("[Main] JCEF-GUI gestartet.");
            launcherGUIStarted = true;

            // JavaFX-Hauptfenster ausblenden
            primaryStage.hide();
            simpleLog.log("[Main] Launcher-Initialisierung abgeschlossen.");
        } catch (Exception e) {
            e.printStackTrace();
            simpleLog.log("[Main] Fehler beim Starten der GUI: " + e.getMessage());
            showError("Fehler beim Starten der GUI", e.getMessage());
        }
    }

    /**
     * Startet die JCEF-basierte GUI.
     */
    private static void launchJCEF() {
        System.setProperty("jcef.disable_osr", "true");
        CountDownLatch jcefLatch = new CountDownLatch(1);

        SwingUtilities.invokeLater(() -> {
            try {
                CefSettings settings = new CefSettings();
                settings.windowless_rendering_enabled = false;

                CefApp.addAppHandler(new CefAppHandlerAdapter(new String[]{}) {
                    @Override
                    public void onBeforeCommandLineProcessing(String processType, org.cef.callback.CefCommandLine commandLine) {
                        commandLine.appendSwitch("disable-gpu");
                        commandLine.appendSwitch("disable-gpu-compositing");
                        commandLine.appendSwitch("disable-webgl");
                    }
                });

                CefApp cefApp = CefApp.getInstance(new String[]{}, settings);
                CefClient client = cefApp.createClient();

                client.addJSDialogHandler(new CefJSDialogHandlerAdapter() {
                    @Override
                    public boolean onJSDialog(CefBrowser browser, String origin_url, JSDialogType dialogType,
                                              String message_text, String default_prompt_text, CefJSDialogCallback callback, BoolRef suppress_message) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(null, message_text, "Alert", JOptionPane.INFORMATION_MESSAGE);
                        });
                        callback.Continue(true, null);
                        return true;
                    }
                });

                String url = "http://localhost:8080/";
                CefBrowser browser = client.createBrowser(url, false, false);

                cefFrame = new JFrame("Syldoria Launcher");
                cefFrame.getContentPane().add(browser.getUIComponent(), BorderLayout.CENTER);
                cefFrame.setSize(900, 700);
                cefFrame.setMinimumSize(new Dimension(900, 700));
                cefFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                cefFrame.setResizable(true);
                cefFrame.setLocationRelativeTo(null);

                // Icon setzen
                cefFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/icons/icon.png")));

                // Fenster-Schließen abfangen
                cefFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                        handleCloseLauncher();
                    }
                });

                cefFrame.setVisible(true);
                jcefLatch.countDown();
                simpleLog.log("[Main] JCEF-Fenster erfolgreich erstellt und angezeigt.");
            } catch (Exception e) {
                e.printStackTrace();
                simpleLog.log("[Main] Fehler beim Starten des JCEF-Fensters: " + e.getMessage());
                jcefLatch.countDown();
            }
        });

        try {
            jcefLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            simpleLog.log("[Main] Warten auf JCEF-Fenster wurde unterbrochen.");
        }
    }

    /**
     * Schließt den Launcher.
     */
    public static void handleCloseLauncher() {
        simpleLog.log("[Main] Schließe Launcher...");

        // Minecraft-Prozess beenden, falls aktiv
        if (minecraftRunning && minecraftProcess != null) {
            simpleLog.log("[Main] Beende laufende Minecraft-Instanz...");
            minecraftProcess.destroy();
            minecraftProcess = null;
            minecraftRunning = false;
        }

        // JCEF-Fenster schließen
        if (cefFrame != null) {
            cefFrame.dispose();
            cefFrame = null;
        }

        // JavaFX-Plattform beenden und JVM beenden
        Platform.exit();
        System.exit(0);
    }

    /**
     * Zeigt ein Logfenster an oder schreibt Logs in eine Datei.
     */
    public static void handleShowLogs() {
        System.out.println("Schreibe Logs in Datei und öffne diese...");
        
        new Thread(() -> {
            try {
                // Hole den Log-Text
                String logText = simpleLog.getLogText();
                if (logText == null || logText.isEmpty()) {
                    logText = "Keine Logs verfügbar.";
                }
                
                // Schreibe in temporäre Datei
                File tempFile = File.createTempFile("syldoria_logs_", ".txt");
                try (FileWriter writer = new FileWriter(tempFile)) {
                    writer.write(logText);
                }
                
                // Öffne die Datei mit dem Standardprogramm
                Desktop.getDesktop().open(tempFile);
                
                System.out.println("Logs wurden in Datei geschrieben und geöffnet: " + tempFile.getAbsolutePath());
                
            } catch (Exception e) {
                System.err.println("Fehler beim Schreiben/Öffnen der Log-Datei: " + e.getMessage());
                e.printStackTrace();
                
                // Fallback: Zeige zumindest eine einfache Meldung an
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        JOptionPane.showMessageDialog(null, 
                            "Fehler beim Anzeigen der Logs: " + e.getMessage(), 
                            "Fehler", 
                            JOptionPane.ERROR_MESSAGE);
                    });
                } catch (Exception ex) {
                    System.err.println("Auch Fallback-Meldung konnte nicht angezeigt werden: " + ex.getMessage());
                }
            }
        }).start();
    }

    /**
     * Installiert Forge, falls noch nicht vorhanden, und führt anschließend den Mod-Installer aus.
     */
    private void installForgeIfNeeded() {
        File mcDir = new File(System.getProperty("user.home"), "/AppData/Roaming/.minecraft/syldoria");
        File forgeFolder = new File(mcDir, "versions/1.19.2-forge-43.4.0");

        // Sicherstellen, dass das Minecraft-Verzeichnis existiert
        if (!mcDir.exists()) {
            mcDir.mkdirs();
            simpleLog.log("[Main] Minecraft-Verzeichnis erstellt: " + mcDir.getAbsolutePath());
        }

        // Prüfen, ob Forge vorhanden ist
        if (!forgeFolder.exists()) {
            simpleLog.log("[Main] Forge nicht gefunden. Starte Installation...");
            forgeInstallationStarted = true;
            final CountDownLatch forgeLatch = new CountDownLatch(1);
            final AtomicBoolean forgeSuccess = new AtomicBoolean(false);
            final StringBuilder forgeError = new StringBuilder();

            // Downloader für Forge konfigurieren
            downloader = MinecraftDownloaderBuilder.create()
                    .providerChain(
                            DownloadProviderChain.create()
                                    .addProvider(new ForgeDownloadProvider())
                    )
                    .build();

            simpleLog.log("[Main] Starte Forge-Download...");
            downloader.downloadIncrementally(
                    new MinecraftDirectory(mcDir),
                    "1.19.2-forge-43.4.0",
                    new CallbackAdapter<Version>() {
                        @Override
                        public void done(Version result) {
                            simpleLog.log("[Main] Forge erfolgreich heruntergeladen und installiert.");
                            forgeSuccess.set(true);
                            forgeLatch.countDown();
                        }

                        @Override
                        public void failed(Throwable e) {
                            simpleLog.log("[Main] Forge-Download fehlgeschlagen: " + e.getMessage());
                            forgeError.append(e.getMessage());
                            forgeLatch.countDown();
                        }

                        @Override
                        public void cancelled() {
                            simpleLog.log("[Main] Forge-Download abgebrochen.");
                            forgeError.append("Download wurde abgebrochen");
                            forgeLatch.countDown();
                        }
                    }
            );

            try {
                if (!forgeLatch.await(5, TimeUnit.MINUTES)) {
                    simpleLog.log("[Main] Forge-Installation hat das Zeitlimit von 5 Minuten überschritten.");
                    forgeInstallationCompleted = false;
                } else if (!forgeSuccess.get()) {
                    simpleLog.log("[Main] Forge-Installation fehlgeschlagen: " +
                            (forgeError.length() > 0 ? forgeError.toString() : "Unbekannter Fehler"));
                    forgeInstallationCompleted = false;
                } else {
                    // Kurze Pause, um Forge-Prozesse abzuschließen
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        simpleLog.log("[Main] Wartezeit nach Forge-Installation unterbrochen.");
                    }
                    simpleLog.log("[Main] Forge-Installation erfolgreich abgeschlossen.");
                    forgeInstallationCompleted = true;
                }
            } catch (InterruptedException e) {
                simpleLog.log("[Main] Fehler beim Warten auf Forge-Installation: " + e.getMessage());
                forgeInstallationCompleted = false;
            }
        } else {
            simpleLog.log("[Main] Forge ist bereits vorhanden.");
            forgeInstallationCompleted = true;
        }

        // Mods installieren (auch wenn Forge fehlgeschlagen ist)
        runModInstaller();

        // Falls das Forge-Verzeichnis nicht vorhanden ist, Warnung ausgeben
        if (!forgeFolder.exists()) {
            simpleLog.log("[Main] WARNUNG: Forge-Verzeichnis nicht gefunden, obwohl Installation abgeschlossen sein sollte!");
        }
    }

    /**
     * Führt den Mod-Installer aus.
     */
    private void runModInstaller() {
        try {
            simpleLog.log("[Main] Mods werden installiert...");
            ModInstaller.main(new String[]{});
            simpleLog.log("[Main] Mods erfolgreich installiert.");
            modInstallationCompleted = true;
        } catch (Exception e) {
            simpleLog.log("[Main] Fehler beim ModInstaller: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() ->
                    showError("Fehler bei der Mod-Installation",
                            "Es gab ein Problem bei der Installation der Mods: " + e.getMessage() +
                                    "\n\nDer Launcher wird trotzdem gestartet, aber Minecraft wird möglicherweise nicht korrekt funktionieren.")
            );
            modInstallationCompleted = false;
        }
    }

    /**
     * Zeigt einen Dialog mit Neuversuch-Option an.
     */
    private boolean showRetryDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(response -> response == javafx.scene.control.ButtonType.OK).isPresent();
    }

    /**
     * Löscht ein Verzeichnis rekursiv.
     */
    private void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if (!file.delete()) {
                            throw new IOException("Konnte Datei nicht löschen: " + file);
                        }
                    }
                }
            }
            if (!directory.delete()) {
                throw new IOException("Konnte Verzeichnis nicht löschen: " + directory);
            }
        }
    }

    /**
     * Startet Minecraft.
     */
    public static void handleStartMinecraft() {
        if (minecraftRunning) {
            simpleLog.log("[Main] Minecraft läuft bereits.");
            return;
        }

        try {
            simpleLog.log("[Main] handleStartMinecraft() aufgerufen.");

            if (username.isEmpty() || uuid.isEmpty() || accessToken.isEmpty()) {
                throw new IllegalArgumentException("Fehlende Anmeldedaten. Bitte zuerst einloggen!");
            }

            updateAdminStatus();

            File mcDir = new File(System.getProperty("user.home"), "/AppData/Roaming/.minecraft/syldoria");
            UUID uuidObj = UUID.fromString(uuid);

            // Downloader initialisieren, falls er null ist
            if (downloader == null) {
                downloader = MinecraftDownloaderBuilder.create()
                    .providerChain(
                        DownloadProviderChain.create()
                            .addProvider(new ForgeDownloadProvider())
                    )
                    .build();
                simpleLog.log("[Main] Downloader initialisiert.");
            }

            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long totalRamBytes = osBean.getTotalPhysicalMemorySize();
            long totalRamMBLong = totalRamBytes / (1024 * 1024);

            double ramPercentage = 0.75;
            long calculatedRamMBLong = (long) (totalRamMBLong * ramPercentage);
            long minRamMB = 1024;
            long maxRamMB = 327680;
            long tempRamMB = Math.max(minRamMB, Math.min(calculatedRamMBLong, maxRamMB));

            if (tempRamMB > Integer.MAX_VALUE) {
                tempRamMB = Integer.MAX_VALUE;
            } else if (tempRamMB < Integer.MIN_VALUE) {
                tempRamMB = Integer.MIN_VALUE;
            }

            int selectedRamMB = (int) tempRamMB;

            simpleLog.log("[Main] Gesamt-RAM: " + totalRamMBLong + " MB");
            simpleLog.log("[Main] Zugewiesener RAM für Minecraft: " + selectedRamMB + " MB");

            minecraftRunning = true;

            // Prüfen, ob Vanilla Minecraft existiert, falls nicht herunterladen
            File vanillaVersionFile = new File(mcDir, "versions/1.19.2/1.19.2.json");
            if (!vanillaVersionFile.exists()) {
                simpleLog.log("[Main] Vanilla Minecraft 1.19.2 wird heruntergeladen...");
                final CountDownLatch vanillaLatch = new CountDownLatch(1);
                final AtomicBoolean vanillaSuccess = new AtomicBoolean(false);
                
                downloader.downloadIncrementally(
                    new MinecraftDirectory(mcDir),
                    "1.19.2",
                    new CallbackAdapter<Version>() {
                        @Override
                        public void done(Version result) {
                            simpleLog.log("[Main] Vanilla Minecraft erfolgreich heruntergeladen.");
                            vanillaSuccess.set(true);
                            vanillaLatch.countDown();
                        }

                        @Override
                        public void failed(Throwable e) {
                            simpleLog.log("[Main] Vanilla Minecraft-Download fehlgeschlagen: " + e.getMessage());
                            vanillaLatch.countDown();
                        }
                    }
                );
                
                try {
                    if (!vanillaLatch.await(5, TimeUnit.MINUTES)) {
                        throw new RuntimeException("Zeitüberschreitung beim Herunterladen von Vanilla Minecraft");
                    }
                    if (!vanillaSuccess.get()) {
                        throw new RuntimeException("Fehler beim Herunterladen von Vanilla Minecraft");
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException("Download von Vanilla Minecraft wurde unterbrochen", e);
                }
            }

            LaunchOption option = new LaunchOption(
                "1.19.2-forge-43.4.0",  // Forge-Version verwenden statt "1.19.2"
                new Authenticator() {
                    @Override
                    public AuthInfo auth() throws AuthenticationException {
                        return new AuthInfo(username, accessToken, uuidObj, Map.of(), "msa", xboxUserId);
                    }
                },
                new MinecraftDirectory(mcDir)
            );
            option.setMaxMemory(selectedRamMB);

            Process process = jmcccLauncher.launch(option);
            minecraftProcess = process;

            process.onExit().thenAccept(p -> {
                int exitCode = p.exitValue();
                if (exitCode == 0) {
                    simpleLog.log("[Main] Minecraft wurde normal beendet.");
                } else {
                    simpleLog.log("[Main] Minecraft ist abgestürzt. Exit-Code: " + exitCode);
                }
                minecraftRunning = false;
                minecraftProcess = null;
            });

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (loggingEnabled) {
                            final String logLine = line;
                            Platform.runLater(() -> simpleLog.log("[Minecraft] " + logLine));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, "MinecraftOutputThread").start();

            new Thread(() -> {
                try (BufferedReader errReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errReader.readLine()) != null) {
                        if (loggingEnabled) {
                            final String logLine = line;
                            Platform.runLater(() -> simpleLog.log("[Minecraft-Error] " + logLine));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, "MinecraftErrorThread").start();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Fehler beim Starten", ex.getMessage());
            if (loggingEnabled) {
                simpleLog.log("[Main] Fehler beim Starten: " + ex.getMessage());
            }
        }
    }

    /**
     * Stoppt Minecraft.
     */
    public static void handleStopMinecraft() {
        if (minecraftRunning && minecraftProcess != null) {
            simpleLog.log("[Main] Stoppe Minecraft...");
            minecraftProcess.destroy();
        } else {
            simpleLog.log("[Main] Kein Minecraft-Prozess zum Stoppen vorhanden.");
        }
    }

    /**
     * Öffnet das Admin-Menü.
     */
    public static void openAdminWindow() {
        Platform.runLater(() -> {
            Stage adminStage = new Stage();
            adminStage.setTitle("Admin Menü");
            javafx.scene.control.Label adminLabel = new javafx.scene.control.Label("Willkommen im Admin Menü!");
            adminStage.setScene(new javafx.scene.Scene(new javafx.scene.layout.StackPane(adminLabel), 400, 300));
            adminStage.show();
        });
    }

    /**
     * Aktualisiert den Admin-Status.
     */
    public static void updateAdminStatus() {
        isAdmin = checkUuidWithAdminlist();
        simpleLog.log("[Main] Admin-Rechte: " + isAdmin);
    }

    /**
     * Zeigt einen Fehler-Dialog an.
     */
    private static void showError(String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    /**
     * Prüft die Admin-Liste.
     */
    private static boolean checkUuidWithAdminlist() {
        try {
            URL url = new URL("http://45.145.226.15:25564/jar/launcher/adminlist.json");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8)) {
                    String jsonResponse = scanner.useDelimiter("\\A").next();
                    JSONArray adminlist = new JSONObject(jsonResponse).getJSONArray("users");

                    for (int i = 0; i < adminlist.length(); i++) {
                        JSONObject user = adminlist.getJSONObject(i);
                        if (user.getString("uuid").equals(uuid)) {
                            return true;
                        }
                    }
                }
            } else {
                System.err.println("Fehler beim Abrufen der Admin-Liste: HTTP " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Fehler beim Überprüfen der Admin-Liste: " + e.getMessage());
        }
        return false;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
