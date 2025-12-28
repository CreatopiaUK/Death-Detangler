# Death Detangler

A Minecraft Forge mod that fixes compatibility issues between Hardcore Revival and other death-related mods (Corpse, Gravestone, Curios, etc.). Prevents duplicate death handling and ensures revival clones are properly detected and managed.

## Features

- **Automatic Clone Detection**: Scans for orphan revival clones left behind by Hardcore Revival
- **Compatibility Fixes**: Prevents conflicts between Hardcore Revival and other death-related mods
- **Automatic Cleanup**: Optionally runs cleanup on server start and periodically during gameplay
- **Manual Control**: In-game commands for scanning, fixing, and reporting
- **Detailed Reporting**: Generate reports showing detected clones per dimension
- **Configurable**: Extensive configuration options for fine-tuning behavior

## Requirements

- **Minecraft**: 1.20.1
- **Forge**: 47.4.13 or later
- **Hardcore Revival**: 12.0.0 - 12.0.9 (required dependency)

## Installation

1. Download the latest release from the [Releases](https://github.com/CreatopiaUK/Death-Detangler/releases) page
2. Place the `.jar` file in your `mods` folder
3. Ensure Hardcore Revival is installed (required dependency)
4. Launch Minecraft

## Configuration

The mod creates a configuration file at `config/death_detangler-common.toml` with the following options:

- **enableLogNotifications** (default: `true`): Enable or disable log notifications from Death Detangler
- **autoRunOnStart** (default: `true`): Automatically run cleanup when the server starts
- **autoRemove** (default: `true`): If true, periodic cleanup will automatically remove clones. If false, it will only scan and log
- **verboseLogging** (default: `false`): Enable verbose logging for cleanup operations
- **cleanIntervalTicks** (default: `6000`): Interval in ticks between periodic cleanup runs (6000 ticks = 5 minutes at 20 TPS). Set to 0 to disable periodic cleanup

## Commands

All commands require operator level 2 (OP level 2).

- `/death_detangler scan` - Scan for orphan clones without removing them
- `/death_detangler dryrun` - Same as scan (alias)
- `/death_detangler run` - Scan and remove orphan clones
- `/death_detangler report` - Show a report of detected clones per dimension
- `/death_detangler dump` - Generate a detailed report file

## Building from Source

### Prerequisites

- Java 17 or later
- Gradle (included via wrapper)

### Build Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/CreatopiaUK/Death-Detangler.git
   cd Death-Detangler
   ```

2. Build the mod:
   ```bash
   ./gradlew build
   ```

3. The built JAR will be in `build/libs/`

### Development Setup

1. Import the project into your IDE (IntelliJ IDEA or Eclipse)
2. Run `./gradlew genIntellijRuns` or `./gradlew genEclipseRuns` to generate run configurations
3. Use the generated run configurations to test the mod

## How It Works

Death Detangler uses mixins to intercept Hardcore Revival's totem usage and clone creation. It tracks revival clones and detects when they become orphaned (e.g., when other mods handle death events in conflicting ways). The mod then provides tools to scan for and clean up these orphaned clones, preventing issues like duplicate items, ghost entities, and other compatibility problems.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Credits

- **Authors**: @Extra_special_K, @TWGMike
- **Mod ID**: `death_detangler`
- **Version**: 1.1.0

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Issues

If you encounter any bugs or have feature requests, please open an issue on the [GitHub Issues](https://github.com/CreatopiaUK/Death-Detangler/issues) page.

## Acknowledgments

- Built for Minecraft Forge
- Compatible with Hardcore Revival mod
- Designed to work alongside other death-related mods like Corpse, Gravestone, and Curios

