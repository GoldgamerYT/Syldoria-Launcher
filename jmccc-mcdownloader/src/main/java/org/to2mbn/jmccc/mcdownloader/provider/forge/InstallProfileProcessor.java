package org.to2mbn.jmccc.mcdownloader.provider.forge;

import org.to2mbn.jmccc.internal.org.json.JSONObject;
import org.to2mbn.jmccc.internal.org.json.JSONTokener;
import org.to2mbn.jmccc.mcdownloader.download.tasks.ResultProcessor;
import org.to2mbn.jmccc.mcdownloader.provider.VersionJsonInstaller;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

class InstallProfileProcessor implements ResultProcessor<byte[], String> {

    private MinecraftDirectory mcdir;
    // Path to the custom Minecraft directory (syldoria)
    private Path customMinecraftDir;

    public InstallProfileProcessor(MinecraftDirectory mcdir) {
        this.mcdir = mcdir;
        // Create a path to the syldoria directory
        this.customMinecraftDir = Paths.get(
                System.getProperty("user.home"),
                "AppData", "Roaming", ".minecraft", "syldoria"
        );

        // Ensure the custom directory exists
        try {
            if (!Files.exists(customMinecraftDir)) {
                Files.createDirectories(customMinecraftDir);
                System.out.println("Created custom Minecraft directory: " + customMinecraftDir);
            }
        } catch (Exception e) {
            System.err.println("Failed to create custom directory: " + e.getMessage());
            // Log, but don't throw exception to keep launcher running
            e.printStackTrace();
        }
    }

    @Override
    public String process(byte[] arg) throws Exception {
        // Path to the tweaked installer in the custom directory
        Path tweakedInstaller = customMinecraftDir.resolve("forge-installer.jar");

        // Ensure parent directories exist
        try {
            Files.createDirectories(tweakedInstaller.getParent());
        } catch (Exception e) {
            System.err.println("Failed to create installer directory: " + e.getMessage());
        }

        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(Files.newOutputStream(tweakedInstaller));
        } catch (Exception e) {
            System.err.println("Failed to create output stream: " + e.getMessage());
            return null;
        }

        String version = null;
        String newInstallerVersion = null;

        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(arg))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if ("META-INF/MANIFEST.MF".equals(entry.getName())) {
                    continue;
                }

                try {
                    zos.putNextEntry(new ZipEntry(entry.getName()));

                    if ("install_profile.json".equals(entry.getName())) {
                        byte[] bytes = IOUtils.toByteArray(in);
                        JSONObject installProfile = new JSONObject(new JSONTokener(new String(bytes)));
                        JSONObject versionInfo = processJson(installProfile);

                        if (versionInfo != null) {
                            // Process the version info for the custom directory
                            version = new VersionJsonInstaller(
                                    new MinecraftDirectory(customMinecraftDir.toFile())
                            ).process(versionInfo);
                            in.closeEntry();
                            break;
                        }
                        newInstallerVersion = installProfile.optString("version");
                        zos.write(bytes);
                    } else if ("net/minecraftforge/installer/SimpleInstaller.class".equals(entry.getName())) {
                        // Wir verwenden den originalen SimpleInstaller
                        byte[] origBytes = IOUtils.toByteArray(in);
                        zos.write(origBytes);
                    } else {
                        zos.write(IOUtils.toByteArray(in));
                    }

                    in.closeEntry();
                    zos.closeEntry();
                } catch (IOException e) {
                    System.err.println("Error processing entry: " + entry.getName() + " - " + e.getMessage());
                    // Fortfahren mit dem nächsten Eintrag
                    try { in.closeEntry(); } catch (Exception ex) {}
                }
            }
        } catch (Exception e) {
            System.err.println("Error during ZIP processing: " + e.getMessage());
            e.printStackTrace();

            // Close the ZipOutputStream to avoid resource leaks
            try {
                if (zos != null) zos.close();
            } catch (IOException ioEx) {
                // Ignore
            }

            // Delete the partially created file
            try {
                Files.deleteIfExists(tweakedInstaller);
            } catch (IOException ioEx) {
                // Ignore
            }

            return null;
        }

        try {
            if (zos != null) zos.close();
        } catch (IOException e) {
            System.err.println("Error closing ZipOutputStream: " + e.getMessage());
        }

        if (version != null) {
            try {
                Files.deleteIfExists(tweakedInstaller);
            } catch (IOException e) {
                System.err.println("Could not delete tweaked installer: " + e.getMessage());
            }
            return version;
        }

        // Für neuere Forge-Versionen (1.12.2 2851+)
        String installerVersion = runInstallerSafely(tweakedInstaller);

        try {
            Files.deleteIfExists(tweakedInstaller);
        } catch (IOException e) {
            System.err.println("Could not delete tweaked installer: " + e.getMessage());
        }

        return installerVersion != null ? installerVersion : newInstallerVersion;
    }

    protected JSONObject processJson(JSONObject installprofile) {
        return installprofile.optJSONObject("versionInfo");
    }

    private String runInstallerSafely(Path installerJar) throws Exception {
        // Create default launcher_profiles.json if it doesn't exist
        Path launcherProfile = customMinecraftDir.resolve("launcher_profiles.json");
        if (!Files.exists(launcherProfile)) {
            Files.write(launcherProfile, "{}".getBytes(StandardCharsets.UTF_8));
        }

        // Überprüfe, ob die Installer-Datei existiert
        if (!Files.exists(installerJar)) {
            System.err.println("Installer JAR file does not exist: " + installerJar);
            return null;
        }

        // Führe den Installer mit Timeout und Fehlerbehandlung aus
        final AtomicBoolean success = new AtomicBoolean(false);
        final AtomicBoolean completed = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);
        final StringBuilder errorMessage = new StringBuilder();

        Thread installerThread = new Thread(() -> {
            try {
                // Setze headless-Modus
                System.setProperty("java.awt.headless", "true");

                // Führe den Installer aus
                try (URLClassLoader cl = new URLClassLoader(new URL[]{installerJar.toFile().toURI().toURL()})) {
                    // Versuche zuerst, ClientInstaller zu verwenden
                    try {
                        Class<?> clientInstaller = cl.loadClass("net.minecraftforge.installer.ClientInstaller");
                        Method install = clientInstaller.getMethod("install", File.class, String.class);
                        install.invoke(null, customMinecraftDir.toFile(), null);
                        System.out.println("Forge installation completed using ClientInstaller");
                        success.set(true);
                    } catch (ClassNotFoundException e) {
                        // Fallback auf SimpleInstaller
                        System.out.println("ClientInstaller not found, using SimpleInstaller");
                        Class<?> installer = cl.loadClass("net.minecraftforge.installer.SimpleInstaller");
                        Method main = installer.getMethod("main", String[].class);
                        main.invoke(null, (Object) new String[]{
                                "--installClient",
                                customMinecraftDir.toAbsolutePath().toString()
                        });
                        System.out.println("Forge installation completed using SimpleInstaller");
                        success.set(true);
                    }
                } catch (Exception e) {
                    System.err.println("Error during Forge installation: " + e.getMessage());
                    e.printStackTrace();
                    errorMessage.append(e.getMessage());
                } finally {
                    // Setze headless-Modus zurück
                    System.setProperty("java.awt.headless", "false");
                }
            } catch (Exception e) {
                System.err.println("Critical error in installer thread: " + e.getMessage());
                e.printStackTrace();
                errorMessage.append("Critical error: ").append(e.getMessage());
            } finally {
                completed.set(true);
                latch.countDown();
            }
        });

        installerThread.setDaemon(true);
        installerThread.start();

        // Warte auf Abschluss mit Timeout
        boolean finished = latch.await(5, TimeUnit.MINUTES);

        if (!finished) {
            System.err.println("Forge installation timed out after 5 minutes");
            installerThread.interrupt();
            return null;
        }

        if (!success.get()) {
            String error = errorMessage.length() > 0 ? errorMessage.toString() : "Unknown error during Forge installation";
            System.err.println("Forge installation failed: " + error);
            // Wir geben einen Fehler zurück, aber werfen keine Exception, damit der Launcher nicht abstürzt
            return null;
        }

        // Überprüfe, ob die Installation erfolgreich war
        Path forgeVersionDir = customMinecraftDir.resolve("versions/1.19.2-forge-43.4.0");
        if (!Files.exists(forgeVersionDir)) {
            System.err.println("Forge installation completed, but version directory was not created");
            // Wir geben nur eine Warnung aus, aber werfen keine Exception
            return null;
        }

        return "1.19.2-forge-43.4.0";
    }
}