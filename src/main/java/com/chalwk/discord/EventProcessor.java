package com.chalwk.discord;

import com.chalwk.model.DiscordEvent;
import com.chalwk.model.EventEmbed;
import com.chalwk.model.EventMessage;
import com.chalwk.model.EmbedField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;
import java.util.List;

public class EventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(EventProcessor.class);
    private EventListener eventListener;

    public void setEventListener(EventListener listener) {
        this.eventListener = listener;
    }

    public void processEvent(DiscordEvent event, DiscordBot discordBot) {
        if (event.getMessage() != null) {
            processMessageEvent(event.getMessage(), discordBot);
        } else if (event.getEmbed() != null) {
            processEmbedEvent(event.getEmbed(), discordBot);
        } else {
            logger.warn("Received event with neither message nor embed");
        }

        // Notify UI about the processed event
        if (eventListener != null) {
            eventListener.onEventProcessed(event);
        }
    }

    private void processMessageEvent(EventMessage message, DiscordBot discordBot) {
        String channelId = message.getChannel_id();
        String text = message.getText();

        if (channelId == null || channelId.trim().isEmpty()) {
            logger.warn("Message event missing channel ID");
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            logger.warn("Message event missing text content");
            return;
        }

        boolean success = discordBot.sendMessage(channelId, text);
        if (!success) {
            logger.error("Failed to send message to channel: {}", channelId);
        } else {
            logger.info("Message sent to channel {}: {}", channelId, text);
        }
    }

    private void processEmbedEvent(EventEmbed embed, DiscordBot discordBot) {
        String channelId = embed.getChannel_id();

        if (channelId == null || channelId.trim().isEmpty()) {
            logger.warn("Embed event missing channel ID");
            return;
        }

        MessageEmbed discordEmbed = buildDiscordEmbed(embed);
        if (discordEmbed != null) {
            boolean success = discordBot.sendEmbed(channelId, discordEmbed);
            if (!success) {
                logger.error("Failed to send embed to channel: {}", channelId);
            } else {
                logger.info("Embed sent to channel {}: {}", channelId, embed.getTitle());
            }
        }
    }

    private MessageEmbed buildDiscordEmbed(EventEmbed embed) {
        try {
            EmbedBuilder builder = new EmbedBuilder();

            // Set title
            if (embed.getTitle() != null && !embed.getTitle().trim().isEmpty()) {
                builder.setTitle(embed.getTitle());
            }

            // Set description
            if (embed.getDescription() != null && !embed.getDescription().trim().isEmpty()) {
                builder.setDescription(embed.getDescription());
            }

            // Set color
            if (embed.getColor() != null) {
                builder.setColor(new Color(embed.getColor()));
            } else {
                builder.setColor(Color.GRAY); // Default color
            }

            // Set footer
            if (embed.getFooter() != null && !embed.getFooter().trim().isEmpty()) {
                builder.setFooter(embed.getFooter());
            }

            // Set timestamp
            builder.setTimestamp(Instant.now());

            // Add fields
            if (embed.getFields() != null) {
                for (EmbedField field : embed.getFields()) {
                    if (field.getName() != null && field.getValue() != null) {
                        builder.addField(
                                field.getName(),
                                field.getValue(),
                                field.getInline() != null ? field.getInline() : false
                        );
                    }
                }
            }

            return builder.build();
        } catch (Exception e) {
            logger.error("Error building Discord embed", e);
            return null;
        }
    }

    public interface EventListener {
        void onEventProcessed(DiscordEvent event);
    }
}