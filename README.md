# SAPPDiscordBot

![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Halo Server Manager](https://img.shields.io/badge/Version-1.0.0-blue.svg)

A powerful Java application that bridges Halo server events with Discord, providing real-time notifications and rich
embeds for enhanced server monitoring and community engagement.

## Features

* **Multi-Server Support**: Monitor multiple Halo servers simultaneously
* **Rich Discord Embeds**: Beautiful embeds with fields, colors, and customization
* **Real-time Event Processing**: Instant processing with configurable polling intervals
* **Modern GUI**: Intuitive Swing-based interface with system tray integration
* **Smart Notifications**: Configurable alerts and status updates
* **Auto-Update Checks**: Built-in update checker for both app and Lua script
* **Comprehensive Logging**: Detailed event logs with server identification
* **Easy Configuration**: User-friendly configuration panel

## Quick Start

### Prerequisites

- **Java 17** or later
- **Discord Bot Token** ([Create one here](https://discord.com/developers/applications))
- **Halo Server** with SAPP mod
- **discord.lua script** from [HALO-SCRIPT-PROJECTS](https://github.com/Chalwk/HALO-SCRIPT-PROJECTS)

### Download and Installation

1. Download the [latest release](https://github.com/Chalwk/SAPPDiscordBot/releases) from the Assets section below
2. Extract the ZIP file to your preferred location
3. Run `SAPPDiscordBot.exe` to launch the application
4. Configure your Discord Bot Token in the Configuration tab
5. Set your watch directory (default: `./discord_events`)
6. Download the required Lua script
   from [HALO-SCRIPT-PROJECTS](https://github.com/Chalwk/HALO-SCRIPT-PROJECTS/blob/master/sapp/utility/discord.lua)

### Configuration

1. **Get Discord Bot Token**

    * Go to [Discord Developer Portal](https://discord.com/developers/applications)
    * Create a new application and bot
    * Copy the bot token

2. **Configure the Bot**

    * Open SAPPDiscordBot
    * Go to Configuration tab
    * Paste the Discord bot token
    * Set watch directory (default: `./discord_events`)
    * Adjust poll interval if needed
    * Save configuration

3. **Setup Lua Script**

    * Download `discord.lua`
      from [HALO-SCRIPT-PROJECTS](https://github.com/Chalwk/HALO-SCRIPT-PROJECTS/blob/master/sapp/utility/discord.lua)
    * Install on your Halo server
    * Configure it to write JSON events to the watch directory

## Usage

### Starting the Bot

1. Launch SAPPDiscordBot
2. Click Start Bot
3. Monitor events in the Event Log tab
4. Minimize to system tray for background operation

### Event Types

#### Message Events

Simple text messages sent to Discord channels:

```json
{
  "message": {
    "channel_id": "123456789012345678",
    "text": "Player John joined the game"
  }
}
```

#### Embed Events

Rich embeds with advanced formatting:

```json
{
  "embed": {
    "channel_id": "123456789012345678",
    "title": "Server Status",
    "description": "All systems operational",
    "color": "0x00FF00",
    "footer": "Halo Server Monitor",
    "fields": [
      {
        "name": "Players Online",
        "value": "12/16",
        "inline": true
      },
      {
        "name": "Map",
        "value": "Blood Gulch",
        "inline": true
      }
    ]
  }
}
```

## Configuration

### Application Settings

* Discord Token
* Watch Directory
* Poll Interval (100 to 10000 ms)
* Auto Start

### File Structure

```
SAPPDiscordBot/
├── sapp_bot_config.json
├── update_config.properties
```

## Lua Script Integration

The app works with `discord.lua` from HALO-SCRIPT-PROJECTS.

It automatically:

* Checks for updates
* Notifies when new versions are available
* Tracks last known commit

## Troubleshooting

### Bot won't start

* Verify token
* Check connection
* Ensure correct Discord permissions

### Events not processing

* Verify watch directory
* Check JSON formatting
* Ensure Halo server write permissions

### Embeds not displaying

* Verify channel IDs
* Check embed fields
* Ensure Discord embed permissions

### Logs

Event Log shows timestamp, server name, event type, channel ID, content preview, and processing status.

## System Requirements

* Java 17 or later
* 512MB RAM minimum
* 50MB storage
* Windows, macOS, or Linux

## Contributing

Bug reports and feature suggestions are welcome.

## [License (MIT)](https://github.com/Chalwk/SAPPDiscordBot/blob/main/LICENSE)

© 2025 SAPPDiscordBot. Jericho Crosby (Chalwk). All rights reserved.

## Links

- [HALO-SCRIPT-PROJECTS](https://github.com/Chalwk/HALO-SCRIPT-PROJECTS) - Companion Lua scripts
- [Discord Developer Portal](https://discord.com/developers/applications) - Create Discord bots
- [Java Downloads](https://www.java.com/download/) - Get Java runtime

### Bug Reports and Feature Requests

Found a bug or have a feature request? Please create an [issue](https://github.com/Chalwk/SAPPDiscordBot/issues) on
GitHub.

---