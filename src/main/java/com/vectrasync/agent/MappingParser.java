package com.vectrasync.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vectrasync.csv.MappingSuggestion;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MappingParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern JSON_ARRAY = Pattern.compile("\\[[\\s\\S]*\\]");

    private MappingParser() {}

    public static List<MappingSuggestion> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Model returned empty mapping response");
        }
        String json = extractJsonArray(raw);
        try {
            return MAPPER.readValue(json, new TypeReference<List<MappingSuggestion>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse mapping JSON: " + e.getMessage()
                    + "\nRaw: " + truncate(raw), e);
        }
    }

    private static String extractJsonArray(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            int first = s.indexOf('\n');
            int last = s.lastIndexOf("```");
            if (first > 0 && last > first) {
                s = s.substring(first + 1, last).trim();
            }
        }
        if (s.startsWith("[")) return s;
        Matcher m = JSON_ARRAY.matcher(s);
        if (m.find()) return m.group();
        throw new IllegalStateException("No JSON array found in model output: " + truncate(raw));
    }

    private static String truncate(String s) {
        return s.length() <= 512 ? s : s.substring(0, 512) + "...";
    }
}
