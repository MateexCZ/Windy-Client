# Windy Client

A free Minecraft 1.8.9 client with a custom interface, HUD, visual effects, and combat, movement, and player modules.

![Windy Client screenshot](docs/images/windy.png)

Windy is shared for others to explore, learn from, and find inspiration. Some modules are experimental, and anticheat compatibility is not guaranteed.

## Download & Install

Download **Windy-B4.zip** from [Releases](../../releases).

1. Close Minecraft Launcher.
2. Extract the **Windy-B4** folder into `%APPDATA%\.minecraft\versions\`.
3. Open the launcher → **Installations → New installation** → select **release Windy-B4**.
4. Under **More Options**, select **64-bit Java 17**.
5. Choose a separate game directory and launch.

Keep the folder, JAR, and JSON names identical: **Windy-B4**. This is a standalone client, so do not put it in `mods`.

**Open ClickGUI:** Right Shift.

## Build from Source

Requires **JDK 17** and **Maven**. Run from the project root:

```powershell
mvn clean package -DskipTests
```

Output: `target/MCP-1.8.9-jar-with-dependencies.jar`.

## Feedback & Contact

Found a bug or have a suggestion? Open an issue.

**Discord:** mateex_

If you enjoy the project, consider giving it a ⭐!

## Credits

Developed and integrated by **Mateex** (me), using code and inspiration from [OpenMyau](https://github.com/IamNespola/OpenMyau-Plus), Tenacity, Rise, with ViaVersion-related libraries and AI assistance during development and debugging.
