package ch.RP.LauncherLib;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;
import org.json.JSONArray;

public class ModInstaller {
    private static final String CUSTOM_JSON_URL = "http://45.145.226.15:25564/modpack.json";
    private static final String CURSEFORGE_MANIFEST_URL = "http://45.145.226.15:25564/jar/curseforge/manifest.json";
    private static final String RESOURCE_PACK_URL = "http://45.145.226.15:25564/jar/Texture/Syldoriapack.zip";
    private static final String CURSEFORGE_API_KEY = "$2a$10$CivlQBc/uDat5upwccTYFOxhec0.8rdRer7IL0ovA9QL5J8vuZFCi";

    // Maximale Threads für parallele Downloads (anpassbar)
    private static final int MAX_THREADS = 4;

    public static void main(String[] args) {
        ExecutorService downloadExecutor = Executors.newFixedThreadPool(MAX_THREADS);
        try {
            String userHome = System.getProperty("user.home");
            Path minecraftPath = Paths.get(userHome, "AppData", "Roaming", ".minecraft");

            // Verzeichnisse initialisieren
            initializeDirectory(minecraftPath.resolve("syldoria/mods"));

            // Gemeinsames Set für alle erforderlichen Mods
            Set<String> alleErforderlichenMods = ConcurrentHashMap.newKeySet();

            // CurseForge-Mods installieren
            JSONObject curseForgeManifest = downloadJson(CURSEFORGE_MANIFEST_URL);
            if (curseForgeManifest == null) {
                System.err.println("Fehler beim Herunterladen der CurseForge manifest.json!");
                return;
            }
            Path modsFolderPath = minecraftPath.resolve("syldoria/mods");
            JSONArray curseForgeMods = curseForgeManifest.getJSONArray("files");
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < curseForgeMods.length(); i++) {
                JSONObject mod = curseForgeMods.getJSONObject(i);
                int projectID = mod.getInt("projectID");
                int fileID = mod.getInt("fileID");

                futures.add(downloadExecutor.submit(() -> {
                    try {
                        String downloadUrl = getCurseForgeDownloadUrl(projectID, fileID);
                        String filename = getFilenameFromUrl(downloadUrl);
                        alleErforderlichenMods.add(filename);
                        File modFile = modsFolderPath.resolve(filename).toFile();

                        if (!modFile.exists()) {
                            System.out.println("Lade herunter (CurseForge): " + filename);
                            downloadFile(downloadUrl, modFile);
                        } else {
                            System.out.println("Mod ist aktuell (CurseForge): " + filename);
                        }
                    } catch (Exception e) {
                        System.err.println("Fehler beim Herunterladen der Mod (CurseForge): " + projectID + " - " + fileID + " - " + e.getMessage());
                    }
                }));
            }

            // Custom-Mods installieren
            JSONObject customModpack = downloadJson(CUSTOM_JSON_URL);
            if (customModpack == null) {
                System.err.println("Fehler beim Herunterladen der modpack.json!");
                return;
            }
            JSONArray customMods = customModpack.getJSONArray("mods");
            for (int i = 0; i < customMods.length(); i++) {
                JSONObject mod = customMods.getJSONObject(i);
                String filename = mod.getString("filename");
                String url = mod.getString("url");
                String checksum = mod.optString("checksum", null);

                alleErforderlichenMods.add(filename);
                futures.add(downloadExecutor.submit(() -> {
                    try {
                        File modFile = modsFolderPath.resolve(filename).toFile();
                        if (!modFile.exists() || (checksum != null && !verifyChecksum(modFile, checksum))) {
                            System.out.println("Lade herunter (Custom): " + filename);
                            downloadFile(url, modFile);
                        } else {
                            System.out.println("Mod ist aktuell (Custom): " + filename);
                        }
                    } catch (Exception e) {
                        System.err.println("Fehler beim Herunterladen der Custom-Mod: " + filename + " - " + e.getMessage());
                    }
                }));
            }

            // Ressourcenpaket herunterladen
            Path resourcePacksFolderPath = minecraftPath.resolve("syldoria/resourcepacks");
            initializeDirectory(resourcePacksFolderPath);
            futures.add(downloadExecutor.submit(() -> {
                try {
                    downloadFile(RESOURCE_PACK_URL, resourcePacksFolderPath.resolve("Syldoriapack.zip").toFile());
                    System.out.println("Ressourcenpaket heruntergeladen.");
                } catch (IOException e) {
                    System.err.println("Fehler beim Herunterladen des Ressourcenpakets: " + e.getMessage());
                }
            }));

            // Warten, bis alle Downloads abgeschlossen sind
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    System.err.println("Fehler im parallelen Download: " + e.getMessage());
                }
            }

            // Aufräumen nach der Installation beider Modtypen
            cleanupOldMods(modsFolderPath, alleErforderlichenMods);

            System.out.println("Installation abgeschlossen!");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            downloadExecutor.shutdown();
        }
    }

    private static void initializeDirectory(Path path) throws IOException {
        if (Files.notExists(path)) {
            Files.createDirectories(path);
            System.out.println("Erstelle Verzeichnis: " + path);
        } else {
            System.out.println("Verzeichnis existiert: " + path);
        }
    }

    private static JSONObject downloadJson(String urlString) throws IOException {
        URL url = new URL(urlString);
        try (InputStream is = url.openStream()) {
            String jsonText = new String(is.readAllBytes());
            return new JSONObject(jsonText);
        }
    }

    private static String getCurseForgeDownloadUrl(int projectID, int fileID) throws IOException {
        String apiUrl = String.format("https://api.curseforge.com/v1/mods/%d/files/%d/download-url", projectID, fileID);

        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestProperty("x-api-key", CURSEFORGE_API_KEY);
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Fehler bei der API-Anfrage: " + responseCode);
        }

        try (InputStream is = connection.getInputStream()) {
            String response = new String(is.readAllBytes());
            JSONObject jsonResponse = new JSONObject(response);
            return jsonResponse.getString("data");
        }
    }

    private static String getFilenameFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String path = url.getPath();
            return Paths.get(path).getFileName().toString();
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Extrahieren des Dateinamens aus der URL: " + urlString, e);
        }
    }

    private static void downloadFile(String urlString, File destination) throws IOException {
        URL url = new URL(urlString);
        try (InputStream in = url.openStream()) {
            Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean verifyChecksum(File file, String expectedChecksum) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString().equals(expectedChecksum);
    }

    private static void cleanupOldMods(Path modsFolderPath, Set<String> requiredMods) {
        File modsDir = modsFolderPath.toFile();
        if (modsDir.exists() && modsDir.isDirectory()) {
            for (File file : modsDir.listFiles()) {
                if (!requiredMods.contains(file.getName())) {
                    System.out.println("Lösche alte Mod: " + file.getName());
                    file.delete();
                }
            }
        }
    }
}
