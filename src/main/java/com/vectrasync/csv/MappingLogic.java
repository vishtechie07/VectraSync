package com.vectrasync.csv;

import com.vectrasync.crm.FieldSchema;
import com.vectrasync.crm.FieldSchema.FieldType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MappingLogic {

    public List<MappingSuggestion> suggest(List<String> csvHeaders, List<FieldSchema> crmSchema) {
        List<MappingSuggestion> out = new ArrayList<>();
        Set<String> usedCrm = new HashSet<>();

        for (String csv : csvHeaders) {
            String norm = normalize(csv);
            FieldSchema best = null;
            double bestScore = 0.0;
            String reason = null;

            for (FieldSchema crm : crmSchema) {
                if (usedCrm.contains(crm.name())) continue;
                double s = score(norm, crm);
                if (s > bestScore) {
                    bestScore = s;
                    best = crm;
                    reason = explain(norm, crm, s);
                }
            }

            if (best != null && bestScore >= 0.5) {
                usedCrm.add(best.name());
                out.add(MappingSuggestion.of(csv, best.name(), round(bestScore), reason));
            } else {
                out.add(MappingSuggestion.unmapped(csv, "No confident match"));
            }
        }
        return out;
    }

    private static double score(String csvNorm, FieldSchema crm) {
        String crmNorm = normalize(crm.name());
        if (csvNorm.equals(crmNorm)) return 1.0;

        double semantic = semanticBoost(csvNorm, crm.type());
        if (crmNorm.contains(csvNorm) || csvNorm.contains(crmNorm)) {
            return Math.min(1.0, 0.75 + semantic * 0.1);
        }
        double jaccard = jaccardTokens(csvNorm, crmNorm);
        return Math.min(1.0, jaccard + semantic * 0.2);
    }

    private static double semanticBoost(String csvNorm, FieldType type) {
        return switch (type) {
            case EMAIL -> csvNorm.contains("email") || csvNorm.contains("mail") ? 1.0 : 0.0;
            case PHONE -> csvNorm.contains("phone") || csvNorm.contains("tel") || csvNorm.contains("mobile") ? 1.0 : 0.0;
            case URL -> csvNorm.contains("url") || csvNorm.contains("domain") || csvNorm.contains("website") ? 1.0 : 0.0;
            case DATE -> csvNorm.contains("date") || csvNorm.contains("at") || csvNorm.contains("time") ? 1.0 : 0.0;
            case NUMBER -> csvNorm.contains("amount") || csvNorm.contains("count") || csvNorm.contains("value") ? 0.5 : 0.0;
            default -> 0.0;
        };
    }

    private static String explain(String csvNorm, FieldSchema crm, double score) {
        if (score >= 0.95) return "Exact match";
        if (score >= 0.75) return "Substring match on '" + crm.name() + "'";
        return "Token overlap + type hint (" + crm.type() + ")";
    }

    static String normalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static double jaccardTokens(String a, String b) {
        Set<String> ta = new HashSet<>(List.of(a.split("_")));
        Set<String> tb = new HashSet<>(List.of(b.split("_")));
        if (ta.isEmpty() || tb.isEmpty()) return 0.0;
        Set<String> inter = new HashSet<>(ta);
        inter.retainAll(tb);
        Set<String> union = new HashSet<>(ta);
        union.addAll(tb);
        return (double) inter.size() / union.size();
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
