# Syldoria Launcher - Custom Minecraft Launcher with Automatic Mod Installation

A powerful custom Minecraft launcher featuring automatic mod downloading through CurseForge API integration. The Syldoria Launcher provides an enhanced gaming experience specifically designed for the Syldoria RP (Roleplay) server project.

![Syldoria Launcher Logo](src/main/resources/icons/logo.png)

## About Syldoria Launcher - The Ultimate Custom Minecraft Mod Manager

The Syldoria Launcher is an advanced, custom-built Minecraft launcher that transforms your gaming experience with its superior user interface and powerful mod management capabilities. It simplifies the installation and management of Minecraft versions, mods, and resource packs required for the immersive Syldoria roleplay server environment.

### Key Features: Automated Minecraft Mod Management

**Core Functionality:**
- **Automatic Mod Synchronization:** The launcher automatically downloads, updates, and installs all required mods from the Syldoria server
- **Advanced CurseForge API Integration:** Directly accesses the CurseForge repository to ensure you always have the latest compatible mods
- **Proprietary Mod Downloader Engine:** Features a custom-developed mod downloading system for maximum performance and compatibility
- **Streamlined Minecraft Modpack Management:** Handles complete modpack installation with a single click

**IMPORTANT:** 
- This custom Minecraft launcher is available exclusively for open-source projects. Commercial use is not permitted.
- The launcher is **Windows-compatible only** and does not support Linux or macOS.
- This Minecraft mod manager is specifically optimized for the Syldoria roleplay server project.

## Complete Feature Set

- Modern, user-friendly game launcher interface
- Full support for Microsoft and Mojang Minecraft accounts
- One-click Minecraft version installation and management
- Smart automatic updates for both launcher and game content
- Advanced mod integration with server-synchronized automatic downloads
- Comprehensive resource pack management system
- Seamless Discord integration for community features
- Special enhanced features for the Syldoria RP Minecraft server
- Cutting-edge mod downloader with direct CurseForge API connection
- Custom modpack profile management

## System Requirements

1. **Windows Operating System** - The custom launcher is Windows-exclusive
2. **Java 21 or newer** must be installed
3. **JavaFX 21.0.5** must be installed
   - Default installation path: `C:/Users/[Username]/JavaFX/javafx-sdk-21.0.5/lib`
4. **JCEF** (Java Chromium Embedded Framework) must be installed
   - Default installation path: `C:/Users/[Username]/JavaFX/web`

## Installation Guide

### Installing Prerequisites

1. **Java 21+**: Download Java from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [AdoptOpenJDK](https://adoptopenjdk.net/)
2. **JavaFX 21.0.5**: Download JavaFX from [Gluon](https://gluonhq.com/products/javafx/)
3. **JCEF**: The JCEF bundle is included in the repository

### Launching Your Minecraft Mod Manager

#### Windows Installation

1. Open a Command Prompt or PowerShell in the project directory
2. Run the startup script:
   ```
   start_launcher.bat
   ```
   
   Or the extended version with additional checks:
   ```
   start_launcher_erweitert.bat
   ```

### Manual Launch Configuration

You can also launch the Minecraft mod manager manually with the following command:

```
java --enable-preview --module-path "PATH_TO_JAVAFX/lib" --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web,javafx.swing --add-exports=javafx.base/com.sun.javafx=ALL-UNNAMED --add-exports=javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED --add-exports=javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED --add-exports=javafx.graphics/com.sun.javafx.embed.swing=ALL-UNNAMED -Djava.library.path="PATH_TO_JCEF" -jar build/libs/SyldoriaLauncher-1.0.jar
```

Replace `PATH_TO_JAVAFX` and `PATH_TO_JCEF` with your actual paths.

## Developer Documentation

### Cloning the Minecraft Launcher Project

```bash
git clone https://github.com/GoldgamerYT/Syldoria-Launcher.git
cd Syldoria-Launcher
```

### Building the Custom Launcher

```bash
./gradlew shadowJar
```

This creates an executable JAR file in the `build/libs/` directory.

## Troubleshooting Common Issues

1. **JavaFX not found**: Make sure JavaFX is installed in the specified path or adjust the path in the startup scripts.

2. **JAR file not found**: Make sure you have built the project with `./gradlew shadowJar`.

3. **Java version**: Make sure you are using Java 21 or newer, as the project uses the `--enable-preview` flag.

4. **Display errors**: If the GUI is not displayed correctly, the problem could be related to the JCEF installation. Check if the libraries are in the correct path.

5. **Platform compatibility**: Remember that this custom Minecraft launcher only works on Windows systems.

## License Information

This Minecraft launcher with automatic mod downloading capabilities is **only released for open-source projects**. Commercial use is prohibited. All rights reserved.

## Community Contributions

Contributions to the Syldoria Launcher project are welcome! If you find a bug or want to suggest an improvement, please create an issue or a pull request.

## Contact Information

For questions or suggestions about this custom Minecraft launcher, you can contact the development team:

- GitHub: [GoldgamerYT](https://github.com/GoldgamerYT)
- Discord: (Link to Discord server)

---

## Keywords
custom minecraft launcher, minecraft mod manager, automatic mod downloader, curseforge api integration, minecraft modpack manager, syldoria launcher, minecraft server launcher, custom game launcher, minecraft mod installer, windows minecraft launcher

---

2025 Syldoria Launcher - Developed with ❤️ for the Syldoria RP server community
