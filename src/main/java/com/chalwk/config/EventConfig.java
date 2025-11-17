package com.chalwk.config;

public class EventConfig {
    private boolean enabled;
    private String template;
    private String color;
    private boolean useEmbed;
    private String channelId;

    public EventConfig() {
    }

    public EventConfig(boolean enabled, String template, String color, boolean useEmbed, String channelId) {
        this.enabled = enabled;
        this.template = template;
        this.color = color;
        this.useEmbed = useEmbed;
        this.channelId = channelId;
    }

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isUseEmbed() {
        return useEmbed;
    }

    public void setUseEmbed(boolean useEmbed) {
        this.useEmbed = useEmbed;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
}