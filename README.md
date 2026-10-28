# Windy Client

Windy is a free Minecraft 1.8.9 client project with a custom interface, HUD, visual effects, and combat, movement, and player modules.

I am sharing the project so others can explore the code, learn from it, and find inspiration. Some features are experimental, and compatibility with server anticheats is not guaranteed.

## Requirements

- Minecraft Java Edition account
- Windows with 64-bit Java 17
- Official Minecraft Launcher for the supplied version profile
- Maven for building from source

## Download and installation

Download `Windy-B4.zip` from this repository's **Releases** section.

1. Close Minecraft Launcher.
2. Extract the `Windy-B4` folder into `%APPDATA%\.minecraft\versions\`.
3. Reopen the launcher and create a new installation using `release Windy-B4`.
4. Under **More Options**, select a 64-bit Java 17 executable if needed.
5. Use a separate game directory for Windy and start the game.

The JAR belongs in the version folder alongside its JSON. It is not a Forge mod and should not be placed in `mods`.

The supplied launcher profile has been checked structurally; a full launch through the official launcher still needs testing before marking the release stable.

## Building

Open `pom.xml` as a Maven project and use JDK 17, then run:

```powershell
mvn clean package -DskipTests
```

The bundled client is `target/MCP-1.8.9-jar-with-dependencies.jar`.

To build and export a launcher ZIP, run the included script from the project root:

```powershell
powershell -ExecutionPolicy Bypass -File .\build-release.ps1
```

The result is `release/Windy-B4.zip`.

## Credits

- Windy development and integration: Matyl.
- Minecraft: Mojang Studios.
- This project uses or adapts components from other projects, including OpenMyau, Rise, Tenacity, Rockstar, and ViaVersion-related projects. Attribution here does not imply endorsement.
- AI assistance was used for parts of development, debugging, and review.

Before publishing, complete the upstream links, authors, and license notices for the components actually included. This README does not grant rights to Minecraft or third-party code; preserve their applicable terms and notices. Choose a license for your own contributions separately.
