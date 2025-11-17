package com.chalwk.discord;

import com.chalwk.config.ConfigManager;
import com.chalwk.config.EventConfig;
import com.chalwk.model.RawEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.time.Instant;

public class EventProcessor {
    private final ConfigManager configManager;
    private EventListener eventListener;

    public EventProcessor(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void setEventListener(EventListener listener) {
        this.eventListener = listener;
    }

    public void processRawEvent(RawEvent rawEvent, DiscordBot discordBot) {
        String eventType = rawEvent.getEvent_type();
        EventConfig eventConfig = configManager.getConfig().getEventConfigs().get(eventType);

        if (eventConfig == null || !eventConfig.isEnabled()) {
            return; // Event not configured or disabled
        }

        // Get actual channel ID from channel mapping
        String channelType = eventConfig.getChannelId();
        String channelId = configManager.getConfig().getChannels().get(channelType);

        if (channelId == null || channelId.trim().isEmpty()) {
            return; // Channel not configured
        }

        // Process template with event data
        String processedContent = TemplateProcessor.processTemplate(
                eventConfig.getTemplate(),
                rawEvent.getData()
        );

        if (eventConfig.isUseEmbed()) {
            sendEmbed(channelId, processedContent, eventConfig.getColor(), discordBot);
        } else {
            discordBot.sendMessage(channelId, processedContent);
        }

        // Notify UI
        if (eventListener != null) {
            eventListener.onEventProcessed(rawEvent);
        }
    }

    private void sendEmbed(String channelId, String description, String colorName, DiscordBot discordBot) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setDescription(description);
        builder.setTimestamp(Instant.now());

        // Set color
        Color color = getColorFromName(colorName);
        if (color != null) {
            builder.setColor(color);
        }

        MessageEmbed embed = builder.build();
        discordBot.sendEmbed(channelId, embed);
    }

    private Color getColorFromName(String colorName) {
        if (colorName == null) return null;

        return switch (colorName.toLowerCase()) {
            case "red" -> Color.RED;
            case "green" -> Color.GREEN;
            case "blue" -> Color.BLUE;
            case "yellow" -> Color.YELLOW;
            case "orange" -> Color.ORANGE;
            case "purple" -> new Color(128, 0, 128);
            case "cyan" -> Color.CYAN;
            case "pink" -> Color.PINK;
            case "white" -> Color.WHITE;
            case "black" -> Color.BLACK;
            default -> Color.GRAY;
        };
    }

    public interface EventListener {
        void onEventProcessed(RawEvent event);
    }
}