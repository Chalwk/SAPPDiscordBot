package com.chalwk.file;

import com.chalwk.model.DiscordEvent;
import com.chalwk.model.EventEmbed;
import com.chalwk.model.EventMessage;
import com.chalwk.model.EmbedField;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JSONParser {

    private static final Logger logger = LoggerFactory.getLogger(JSONParser.class);
    private final ObjectMapper objectMapper;

    public JSONParser() {
        this.objectMapper = new ObjectMapper();
    }

    public List<DiscordEvent> parseEvents(File file) throws IOException {
        List<DiscordEvent> events = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(file);

            if (rootNode.isArray()) {
                for (JsonNode eventNode : rootNode) {
                    DiscordEvent event = parseEvent(eventNode);
                    if (event != null) {
                        events.add(event);
                    }
                }
            } else {
                DiscordEvent event = parseEvent(rootNode);
                if (event != null) {
                    events.add(event);
                }
            }
        } catch (IOException e) {
            logger.error("Error parsing JSON file: {}", file.getName(), e);
            throw e;
        }

        return events;
    }

    private DiscordEvent parseEvent(JsonNode eventNode) {
        try {
            DiscordEvent event = new DiscordEvent();

            // Check for message event
            if (eventNode.has("message")) {
                JsonNode messageNode = eventNode.get("message");
                EventMessage message = objectMapper.treeToValue(messageNode, EventMessage.class);
                event.setMessage(message);
            }

            // Check for embed event
            if (eventNode.has("embed")) {
                JsonNode embedNode = eventNode.get("embed");
                EventEmbed embed = parseEmbed(embedNode);
                event.setEmbed(embed);
            }

            return event;
        } catch (Exception e) {
            logger.warn("Failed to parse event node: {}", eventNode, e);
            return null;
        }
    }

    private EventEmbed parseEmbed(JsonNode embedNode) {
        try {
            EventEmbed embed = new EventEmbed();

            if (embedNode.has("channel_id")) {
                embed.setChannel_id(embedNode.get("channel_id").asText());
            }
            if (embedNode.has("title")) {
                embed.setTitle(embedNode.get("title").asText());
            }
            if (embedNode.has("description")) {
                embed.setDescription(embedNode.get("description").asText());
            }
            if (embedNode.has("color")) {
                JsonNode colorNode = embedNode.get("color");
                if (colorNode.isTextual()) {
                    embed.setColor(colorNode.asText());
                } else if (colorNode.isNumber()) {
                    embed.setColor(colorNode.asInt());
                }
            }
            if (embedNode.has("footer")) {
                embed.setFooter(embedNode.get("footer").asText());
            }
            if (embedNode.has("fields")) {
                JsonNode fieldsNode = embedNode.get("fields");
                if (fieldsNode.isArray()) {
                    List<EmbedField> fields = new ArrayList<>();
                    for (JsonNode fieldNode : fieldsNode) {
                        EmbedField field = new EmbedField();
                        if (fieldNode.has("name")) {
                            field.setName(fieldNode.get("name").asText());
                        }
                        if (fieldNode.has("value")) {
                            field.setValue(fieldNode.get("value").asText());
                        }
                        if (fieldNode.has("inline")) {
                            field.setInline(fieldNode.get("inline").asBoolean());
                        }
                        fields.add(field);
                    }
                    embed.setFields(fields);
                }
            }

            return embed;
        } catch (Exception e) {
            logger.warn("Failed to parse embed node", e);
            return null;
        }
    }
}