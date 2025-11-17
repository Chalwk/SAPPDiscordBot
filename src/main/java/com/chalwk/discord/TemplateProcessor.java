package com.chalwk.discord;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateProcessor {

    public static String processTemplate(String template, Map<String, Object> data) {
        if (template == null) return "";

        String result = template;
        Pattern pattern = Pattern.compile("\\$(\\w+)");
        Matcher matcher = pattern.matcher(template);

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = getReplacement(placeholder, data);
            result = result.replace("$" + placeholder, replacement);
        }

        return result;
    }

    private static String getReplacement(String placeholder, Map<String, Object> data) {
        Object value = data.get(placeholder);
        if (value == null) {
            // Try common variations
            value = switch (placeholder) {
                case "gt" -> data.get("gametype");
                case "killerName" -> data.get("killer_name");
                case "victimName" -> data.get("victim_name");
                default -> null;
            };
        }
        return value != null ? value.toString() : "[MISSING:" + placeholder + "]";
    }
}