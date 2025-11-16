package com.chalwk.model;

import java.util.List;

public class EventEmbed {
    private String channel_id;
    private String title;
    private String description;
    private Integer color;
    private String footer;
    private List<EmbedField> fields;

    // Getters and setters
    public String getChannel_id() {
        return channel_id;
    }

    public void setChannel_id(String channel_id) {
        this.channel_id = channel_id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getColor() {
        return color;
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    public void setColor(String color) {
        if (color != null && !color.trim().isEmpty()) {
            try {
                // Handle hex colors (0xFFFFFF format from Lua)
                if (color.startsWith("0x")) {
                    this.color = Integer.parseInt(color.substring(2), 16);
                } else {
                    this.color = Integer.parseInt(color);
                }
            } catch (NumberFormatException e) {
                this.color = null;
            }
        }
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public List<EmbedField> getFields() {
        return fields;
    }

    public void setFields(List<EmbedField> fields) {
        this.fields = fields;
    }
}