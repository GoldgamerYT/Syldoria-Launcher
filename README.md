# Syldoria Launcher

A custom Minecraft launcher with enhanced features for an optimal gaming experience, specifically designed for the Syldoria RP (Roleplay) server project.

![Syldoria Launcher Logo](src/main/resources/icons/logo.png)

## About the Project

The Syldoria Launcher is a specially developed Minecraft launcher that offers an improved user interface and additional features. It enables easy installation and management of Minecraft versions, mods, and resource packs required for the Syldoria roleplay server.

### Automatic Mod Download

**Main Features:**
- **Automatic Mod Download:** The launcher automatically downloads all required mods from the Syldoria server and installs them
- **CurseForge API Integration:** Uses the CurseForge API to obtain mods directly from their repository
- **Custom Mod Downloader:** Contains a specially developed mod downloader for optimal compatibility and performance

**IMPORTANT:** 
- This launcher may only be used for open-source projects. Commercial use is not permitted.
- The launcher is **Windows-only** and does not support Linux or macOS.
- This launcher is specifically designed for the Syldoria roleplay server project.

## Features

- Modern, user-friendly interface
- Support for Microsoft and Mojang accounts
- Easy installation and management of Minecraft versions
- Automatic updates
- Mod integration and management with automatic download from the server
- Resource pack management
- Discord integration
- Special features for the Syldoria RP server
- Custom mod downloader with direct CurseForge API connection

## Requirements

1. **Windows Operating System** - The launcher does not work on Linux or macOS
2. **Java 21 or newer** must be installed
3. **JavaFX 21.0.5** must be installed
   - Default installation path: `C:/Users/[Username]/JavaFX/javafx-sdk-21.0.5/lib`
4. **JCEF** (Java Chromium Embedded Framework) must be installed
   - Default installation path: `C:/Users/[Username]/JavaFX/web`

## Installation

### Installing Prerequisites

1. **Java 21+**: Download Java from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [AdoptOpenJDK](https://adoptopenjdk.net/)
2. **JavaFX 21.0.5**: Download JavaFX from [Gluon](https://gluonhq.com/products/javafx/)
3. **JCEF**: The JCEF bundle is included in the repository

### Starting the Launcher

#### Windows

1. Open a Command Prompt or PowerShell in the project directory
2. Run the startup script:
   ```
   start_launcher.bat
   ```
   
   Or the extended version with additional checks:
   ```
   start_launcher_erweitert.bat
   ```

### Manual Start

You can also start the launcher manually with the following command:

```
java --enable-preview --module-path "PATH_TO_JAVAFX/lib" --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web,javafx.swing --add-exports=javafx.base/com.sun.javafx=ALL-UNNAMED --add-exports=javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED --add-exports=javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED --add-exports=javafx.graphics/com.sun.javafx.embed.swing=ALL-UNNAMED -Djava.library.path="PATH_TO_JCEF" -jar build/libs/SyldoriaLauncher-1.0.jar
```

Replace `PATH_TO_JAVAFX` and `PATH_TO_JCEF` with your actual paths.

## Development

### Cloning the Project

```bash
git clone https://github.com/GoldgamerYT/Syldoria-Launcher.git
cd Syldoria-Launcher
```

### Building the Project

```bash
./gradlew shadowJar
```

This creates an executable JAR file in the `build/libs/` directory.

## Troubleshooting

1. **JavaFX not found**: Make sure JavaFX is installed in the specified path or adjust the path in the startup scripts.

2. **JAR file not found**: Make sure you have built the project with `./gradlew shadowJar`.

3. **Java version**: Make sure you are using Java 21 or newer, as the project uses the `--enable-preview` flag.

4. **Display errors**: If the GUI is not displayed correctly, the problem could be related to the JCEF installation. Check if the libraries are in the correct path.

5. **Platform compatibility**: Remember that this launcher only works on Windows systems.

## License

This launcher is **only released for open-source projects**. Commercial use is prohibited. All rights reserved.

## Contributions

Contributions to the project are welcome! If you find a bug or want to suggest an improvement, please create an issue or a pull request.

## Contact

For questions or suggestions, you can contact the development team:

- GitHub: [GoldgamerYT](https://github.com/GoldgamerYT)
- Discord: (Link to Discord server)

---

2025 Syldoria Launcher - Developed with ❤️ for the Syldoria RP server community
