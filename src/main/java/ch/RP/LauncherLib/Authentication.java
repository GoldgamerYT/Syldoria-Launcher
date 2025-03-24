package ch.RP.LauncherLib;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import org.json.JSONObject;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Authentication {

    private final File credentialsFile = new File("credentials.json");

    public void handleLogin(Label usernameLabel, Button optionalLoginButton) {
        try {
            Main.simpleLog.log("[Authentication] Versuche Login via Microsoft Auth...");
            HttpClient httpClient = MinecraftAuth.createHttpClient();

            // Blockierender Aufruf: Der Login-Prozess (Device-Code-Flow) wird gestartet.
            StepFullJavaSession.FullJavaSession session =
                    MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(
                            httpClient,
                            new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCode -> {
                                try {
                                    // Öffne den externen Browser mit der von Microsoft vorgegebenen URL.
                                    Desktop.getDesktop().browse(new URI(msaDeviceCode.getDirectVerificationUri()));
                                    Main.simpleLog.log("[Authentication] Browser geöffnet: " + msaDeviceCode.getDirectVerificationUri());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Main.simpleLog.log("[Authentication] Fehler beim Öffnen des Browser-Links: " + e.getMessage());
                                }
                            })
                    );

            // Nach erfolgreichem Login werden folgende Werte gesetzt:
            String user = session.getMcProfile().getName();
            String u = session.getMcProfile().getId().toString();
            String token = session.getMcProfile().getMcToken().getAccessToken();
            
            // Versuche, die Xbox-ID aus dem Session-Objekt zu bekommen
            String xid = "";
            try {
                // Da wir nicht direkt auf die Xbox-ID zugreifen können, 
                // benutzen wir vorerst einen leeren Wert
                // In einer zukünftigen Version könnte diese Information verfügbar sein
                xid = "";
                Main.simpleLog.log("[Authentication] Xbox-ID nicht verfügbar in dieser Implementation.");
            } catch (Exception e) {
                Main.simpleLog.log("[Authentication] Fehler beim Verarbeiten der Xbox-ID: " + e.getMessage());
            }

            Main.username = user;
            Main.uuid = u;
            Main.accessToken = token;
            Main.xboxUserId = xid;

            Platform.runLater(() -> usernameLabel.setText("Angemeldet als: " + user));
            saveCredentials(user, token, u, xid);

            // Login-Button ausblenden, falls vorhanden
            if (optionalLoginButton != null) {
                Platform.runLater(() -> optionalLoginButton.setVisible(false));
            }

            Main.simpleLog.log("[Authentication] Login erfolgreich für " + user);

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Login-Fehler", ex.getMessage(), Alert.AlertType.ERROR);
            Main.simpleLog.log("[Authentication] Login-Fehler: " + ex.getMessage());
        }
    }

    public void handleLogout(Label usernameLabel, Button optionalLoginButton) {
        Main.simpleLog.log("[Authentication] Starte Logout-Prozess...");
        try {
            String absPath = credentialsFile.getAbsolutePath();
            Main.simpleLog.log("[Authentication] credentialsFile Pfad: " + absPath);

            if (credentialsFile.exists()) {
                // Versuche, eventuell noch geöffnete Streams freizugeben
                System.gc();
                try {
                    Thread.sleep(100); // Kurze Pause, um Locks aufzuheben
                } catch (InterruptedException ie) {
                    // Ignorieren
                }

                Main.simpleLog.log("[Authentication] Versuche, credentialsFile zu löschen...");
                boolean deleted = credentialsFile.delete();
                if (deleted) {
                    Main.simpleLog.log("[Authentication] credentialsFile erfolgreich gelöscht.");
                } else {
                    Main.simpleLog.log("[Authentication] credentialsFile konnte nicht gelöscht werden. Versuche, den Inhalt zu leeren...");
                    try (FileWriter writer = new FileWriter(credentialsFile, false)) {
                        writer.write("{}"); // Leeres JSON als Platzhalter
                        Main.simpleLog.log("[Authentication] credentialsFile Inhalt erfolgreich geleert.");
                    } catch (Exception e2) {
                        Main.simpleLog.log("[Authentication] Fehler beim Leeren des credentialsFile-Inhalts: " + e2.getMessage());
                    }
                }

                // Logge den aktuellen Inhalt, falls die Datei noch existiert
                if (credentialsFile.exists()) {
                    try {
                        String content = new String(java.nio.file.Files.readAllBytes(credentialsFile.toPath()), StandardCharsets.UTF_8);
                        Main.simpleLog.log("[Authentication] Aktueller Inhalt der credentialsFile nach Logout: " + content);
                    } catch (Exception e) {
                        Main.simpleLog.log("[Authentication] Fehler beim Lesen der credentialsFile nach Logout: " + e.getMessage());
                    }
                } else {
                    Main.simpleLog.log("[Authentication] credentialsFile existiert nach dem Lösch-/Leerungsversuch nicht mehr.");
                }
            } else {
                Main.simpleLog.log("[Authentication] credentialsFile existiert nicht. Kein Löschen notwendig.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Main.simpleLog.log("[Authentication] Exception im Logout-Prozess: " + e.getMessage());
        }

        // Setze die globalen Variablen zurück und aktualisiere die UI
        Main.username = "";
        Main.accessToken = "";
        Main.uuid = "";
        Main.xboxUserId = "";
        Platform.runLater(() -> {
            usernameLabel.setText("Nicht angemeldet");
            if (optionalLoginButton != null) {
                optionalLoginButton.setVisible(true);
            }
        });
        Main.simpleLog.log("[Authentication] Logout abgeschlossen.");
    }

    // Diese Methode wird bisher im JavaFX-Flow genutzt.
    public void autoLogin(Label usernameLabel, Button optionalLoginButton) {
        if (credentialsFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(credentialsFile.toURI())), StandardCharsets.UTF_8);
                Main.simpleLog.log("[Authentication] Auto-Login liest credentialsFile Inhalt: " + content);
                JSONObject json = new JSONObject(content);

                // Falls das JSON-Objekt keine gültigen Daten enthält, logge dies
                if (!json.has("username") || json.getString("username").isEmpty()) {
                    Main.simpleLog.log("[Authentication] Keine gültigen Credentials in der Datei gefunden.");
                } else {
                    Main.username = json.getString("username");
                    Main.accessToken = json.getString("accessToken");
                    Main.uuid = json.getString("uuid");
                    // Xbox-ID lesen, falls vorhanden (für neuere Versionen)
                    if (json.has("xboxUserId")) {
                        Main.xboxUserId = json.getString("xboxUserId");
                    } else {
                        Main.xboxUserId = "";
                    }

                    Platform.runLater(() -> usernameLabel.setText("Angemeldet als: " + Main.username));

                    if (optionalLoginButton != null) {
                        Platform.runLater(() -> optionalLoginButton.setVisible(false));
                    }

                    Main.simpleLog.log("[Authentication] Auto-Login für " + Main.username + " erfolgreich.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                Main.simpleLog.log("[Authentication] Fehler beim Auto-Login: " + e.getMessage());
            }
        } else {
            Main.simpleLog.log("[Authentication] Kein Auto-Login möglich, keine credentials gefunden.");
        }
    }

    // NEU: Methode für den Web-Flow (Auto-Login) – liest die gespeicherten Daten und gibt sie als JSON zurück.
    public String autoLoginWeb() {
        if (credentialsFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(credentialsFile.toURI())), StandardCharsets.UTF_8);
                if (content.trim().isEmpty()) {
                    Main.simpleLog.log("[Authentication] Credentials file ist leer.");
                    return "{\"status\":\"error\", \"message\":\"Keine Credentials gefunden\"}";
                }
                JSONObject json = new JSONObject(content);
                Main.username = json.getString("username");
                Main.accessToken = json.getString("accessToken");
                Main.uuid = json.getString("uuid");
                // Xbox-ID lesen, falls vorhanden (für neuere Versionen)
                if (json.has("xboxUserId")) {
                    Main.xboxUserId = json.getString("xboxUserId");
                } else {
                    Main.xboxUserId = "";
                }

                Main.simpleLog.log("[Authentication] Auto-Login erfolgreich für " + Main.username);
                return "{\"status\":\"success\", \"username\":\"" + Main.username + "\"}";
            } catch (Exception e) {
                e.printStackTrace();
                Main.simpleLog.log("[Authentication] Fehler beim Auto-Login: " + e.getMessage());
                return "{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}";
            }
        } else {
            Main.simpleLog.log("[Authentication] Keine Credentials gefunden für Auto-Login.");
            return "{\"status\":\"error\", \"message\":\"Keine Credentials gefunden\"}";
        }
    }

    private void saveCredentials(String user, String token, String uuid, String xboxUserId) {
        try (FileWriter writer = new FileWriter(credentialsFile)) {
            JSONObject obj = new JSONObject();
            obj.put("username", user);
            obj.put("accessToken", token);
            obj.put("uuid", uuid);
            obj.put("xboxUserId", xboxUserId);

            writer.write(obj.toString(4));
            Main.simpleLog.log("[Authentication] Credentials gespeichert.");
        } catch (Exception e) {
            e.printStackTrace();
            Main.simpleLog.log("[Authentication] Fehler beim Speichern der Credentials: " + e.getMessage());
        }
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }
}
