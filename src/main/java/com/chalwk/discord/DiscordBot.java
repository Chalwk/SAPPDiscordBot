package com.chalwk.discord;

import com.chalwk.config.ConfigManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordBot {

    private static final Logger logger = LoggerFactory.getLogger(DiscordBot.class);

    private final ConfigManager configManager;
    private JDA jda;
    private boolean isRunning;

    public DiscordBot(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public boolean start() {
        String token = configManager.getConfig().getDiscordToken();
        if (token == null || token.trim().isEmpty()) {
            logger.error("Discord bot token is not configured");
            return false;
        }

        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER)
                    .build();

            jda.awaitReady();
            isRunning = true;
            logger.info("Discord bot started successfully. Connected to {} servers", jda.getGuilds().size());
            return true;

        } catch (InterruptedException e) {
            logger.error("Bot startup interrupted", e);
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            logger.error("Failed to start Discord bot", e);
            return false;
        }
    }

    public void stop() {
        if (jda != null) {
            jda.shutdown();
            jda = null;
        }
        isRunning = false;
        logger.info("Discord bot stopped");
    }

    public boolean sendMessage(String channelId, String message) {
        if (!isRunning || jda == null) {
            logger.warn("Discord bot is not running");
            return false;
        }

        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.error("Channel not found: {}", channelId);
                return false;
            }

            // Split long messages to avoid Discord's 2000 character limit
            if (message.length() > 2000) {
                message = message.substring(0, 1997) + "...";
            }

            channel.sendMessage(message).queue();
            logger.debug("Message sent to channel {}: {}", channelId, message);
            return true;

        } catch (Exception e) {
            logger.error("Failed to send message to channel {}", channelId, e);
            return false;
        }
    }

    public boolean sendEmbed(String channelId, MessageEmbed embed) {
        if (!isRunning || jda == null) {
            logger.warn("Discord bot is not running");
            return false;
        }

        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.error("Channel not found: {}", channelId);
                return false;
            }

            channel.sendMessageEmbeds(embed).queue();
            logger.debug("Embed sent to channel {}: {}", channelId, embed.getTitle());
            return true;

        } catch (Exception e) {
            logger.error("Failed to send embed to channel {}", channelId, e);
            return false;
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public JDA getJDA() {
        return jda;
    }
}