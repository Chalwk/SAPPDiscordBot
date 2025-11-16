package com.chalwk.model;

public class DiscordEvent {
    private EventMessage message;
    private EventEmbed embed;

    // Getters and setters
    public EventMessage getMessage() {
        return message;
    }

    public void setMessage(EventMessage message) {
        this.message = message;
    }

    public EventEmbed getEmbed() {
        return embed;
    }

    public void setEmbed(EventEmbed embed) {
        this.embed = embed;
    }
}