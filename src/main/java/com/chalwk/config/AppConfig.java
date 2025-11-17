package com.chalwk.config;

import java.util.HashMap;
import java.util.Map;

public class AppConfig {
    private String discordToken = "";
    private String watchDirectory = "./discord_events";
    private int pollInterval = 1000;
    private boolean autoStart = false;

    // Channel configuration
    private Map<String, String> channels = new HashMap<>();

    // Event templates configuration
    private Map<String, EventConfig> eventConfigs = new HashMap<>();

    public AppConfig() {
        initializeDefaults();
    }

    private void initializeDefaults() {
        // Default channels
        channels.put("GENERAL", "");
        channels.put("CHAT", "");
        channels.put("COMMAND", "");

        // Default event configurations
        initializeDefaultEvents();
    }

    private void initializeDefaultEvents() {
        // Game events
        eventConfigs.put("event_start", new EventConfig(
                true,
                "**🗺️ Game Started** → `$map` **-** `$gt ($ffa)`",
                "green", true, "GENERAL"
        ));

        eventConfigs.put("event_end", new EventConfig(
                true,
                "**🏁 Game Ended** → `$map` **-** `$gt ($ffa)`",
                "red", true, "GENERAL"
        ));

        // Player events
        eventConfigs.put("event_join", new EventConfig(
                true,
                "**🟢 Join** → `$name` **-** `$total/16`",
                null, false, "GENERAL"
        ));

        eventConfigs.put("event_leave", new EventConfig(
                true,
                "**🔴 Quit** → `$name` **-** `$total/16`",
                null, false, "GENERAL"
        ));

        eventConfigs.put("event_spawn", new EventConfig(
                false,
                "**✨ Spawn** → `$name` Team: `$team`",
                null, false, "GENERAL"
        ));

        eventConfigs.put("event_team_switch", new EventConfig(
                false,
                "**🔄 Team Switch** → `$name` → `$team`",
                null, false, "GENERAL"
        ));

        eventConfigs.put("event_map_reset", new EventConfig(
                false,
                "**♻️ Map Reset** → `$map` **-** `$gt ($ffa)`",
                null, false, "GENERAL"
        ));

        eventConfigs.put("event_login", new EventConfig(
                false,
                "**🔐 Login** → `$name`\n\n**Admin Level:** `$lvl`",
                "yellow", true, "GENERAL"
        ));

        eventConfigs.put("event_snap", new EventConfig(
                false,
                "**📸 Snap** → `$name`",
                null, false, "GENERAL"
        ));

        // Score events (by mode)
        eventConfigs.put("event_score_1", new EventConfig(
                true,
                "**$name** captured the flag for the **$team** team!\n\n🟥 Red Score: **$redScore**\n🟦 Blue Score: **$blueScore**\n🏁 Scorelimit: **$scorelimit**",
                "green", true, "GENERAL"
        ));

        eventConfigs.put("event_score_2", new EventConfig(
                true,
                "**$name** completed a lap for **$team** team!\n🏁 Team Total Laps: **$totalTeamLaps/$scorelimit**\n🚩 Player Laps: **$score**",
                "green", true, "GENERAL"
        ));

        eventConfigs.put("event_score_3", new EventConfig(
                true,
                "**$name** finished a lap.\n🏆 Total Laps Completed: **$score/$scorelimit**",
                "green", true, "GENERAL"
        ));

        eventConfigs.put("event_score_4", new EventConfig(
                true,
                "**$name** scored for **$team** team!\n\n🟥 Red Score: **$redScore**\n🟦 Blue Score: **$blueScore**\n🏁 Scorelimit: **$scorelimit**",
                "green", true, "GENERAL"
        ));

        eventConfigs.put("event_score_5", new EventConfig(
                true,
                "**$name** scored!\n\n🟥 Red Score: **$redScore**\n🟦 Blue Score: **$blueScore**\n🏁 Scorelimit: **$scorelimit**",
                "green", true, "GENERAL"
        ));

        // Death events
        eventConfigs.put("event_death_1", new EventConfig(true, "**☠️ Death:** `$killerName` drew first blood on `$victimName`", null, false, "GENERAL"));
        eventConfigs.put("event_death_2", new EventConfig(true, "**☠️ Death:** `$victimName` was killed from the grave by `$killerName`", null, false, "GENERAL"));
        eventConfigs.put("event_death_3", new EventConfig(true, "**☠️ Death:** `$victimName` was run over by `$killerName`", null, false, "GENERAL"));
        eventConfigs.put("event_death_4", new EventConfig(true, "**☠️ Death:** `$victimName` was killed by `$killerName`", null, false, "GENERAL"));
        eventConfigs.put("event_death_5", new EventConfig(true, "**☠️ Death:** `$victimName` committed suicide", null, false, "GENERAL"));
        eventConfigs.put("event_death_6", new EventConfig(true, "**☠️ Death:** `$victimName` was betrayed by `$killerName`", null, false, "GENERAL"));
        eventConfigs.put("event_death_7", new EventConfig(true, "**☠️ Death:** `$victimName` was squashed by a vehicle", null, false, "GENERAL"));
        eventConfigs.put("event_death_8", new EventConfig(true, "**☠️ Death:** `$victimName` fell to their death", null, false, "GENERAL"));
        eventConfigs.put("event_death_9", new EventConfig(true, "**☠️ Death:** `$victimName` was killed by the server", null, false, "GENERAL"));
        eventConfigs.put("event_death_10", new EventConfig(true, "**☠️ Death:** `$victimName` died", null, false, "GENERAL"));

        // Chat
        eventConfigs.put("event_chat", new EventConfig(
                true,
                "**💬 Chat** → `$name`: *$msg*",
                null, false, "CHAT"
        ));

        // Commands
        eventConfigs.put("event_command", new EventConfig(
                true,
                "**⌘ Command** → `$name`: `$cmd`",
                "green", true, "COMMAND"
        ));

    }

    // Getters and setters
    public Map<String, String> getChannels() {
        return channels;
    }

    public void setChannels(Map<String, String> channels) {
        this.channels = channels;
    }

    public Map<String, EventConfig> getEventConfigs() {
        return eventConfigs;
    }

    public void setEventConfigs(Map<String, EventConfig> eventConfigs) {
        this.eventConfigs = eventConfigs;
    }

    public String getDiscordToken() {
        return discordToken;
    }

    public void setDiscordToken(String discordToken) {
        this.discordToken = discordToken;
    }

    public String getWatchDirectory() {
        return watchDirectory;
    }

    public void setWatchDirectory(String watchDirectory) {
        this.watchDirectory = watchDirectory;
    }

    public int getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }
}