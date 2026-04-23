package com.vectrasync.csv;

public record MappingSuggestion(
        String csvField,
        String crmField,
        double confidence,
        String reason
) {
    public static MappingSuggestion of(String csv, String crm, double confidence, String reason) {
        return new MappingSuggestion(csv, crm, confidence, reason);
    }

    public static MappingSuggestion unmapped(String csv, String reason) {
        return new MappingSuggestion(csv, null, 0.0, reason);
    }

    public boolean isMapped() {
        return crmField != null && !crmField.isBlank();
    }
}
