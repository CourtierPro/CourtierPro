package com.example.courtierprobackend.shared.utils;

import java.util.Map;

public class StageTranslationUtil {

    private static final Map<String, String> FRENCH_MAPPINGS = Map.ofEntries(
            // Buyer Stages
            Map.entry("OFFER_SUBMITTED", "Offre Soumise"),
            Map.entry("OFFER_ACCEPTED", "Offre Acceptée"),
            Map.entry("OFFER_DECLINED", "Offre Refusée"),
            Map.entry("IN_ESCROW", "En Dépôt"), // Assuming "In Escrow" -> "En Dépôt" or similar context
            Map.entry("INSPECTION_SCHEDULED", "Inspection Planifiée"),
            Map.entry("INSPECTION_COMPLETED", "Inspection Terminée"),
            Map.entry("FINANCING_PENDING", "Financement en Attente"),
            Map.entry("FINANCING_APPROVED", "Financement Approuvé"),
            Map.entry("CLOSING_SCHEDULED", "Clôture Planifiée"),
            Map.entry("CLOSED", "Vendu"), // or "Clôturé"

            // Seller Stages
            Map.entry("LISTED", "Inscrit"),
            Map.entry("SHOWING_SCHEDULED", "Visite Planifiée"),
            Map.entry("OFFER_RECEIVED", "Offre Reçue")
    // Add more as needed based on Enum values
    );

    public static String getTranslatedStage(String stageEnum, String languageCode) {
        if (stageEnum == null || stageEnum.isBlank()) {
            return "";
        }

        boolean isFrench = languageCode != null && languageCode.equalsIgnoreCase("fr");

        if (isFrench) {
            String translated = FRENCH_MAPPINGS.get(stageEnum);
            if (translated != null) {
                return translated;
            }
            // Fallback to English formatting if no mapping found
        }

        return formatEnglish(stageEnum);
    }

    private static String formatEnglish(String stage) {
        if (stage == null || stage.isBlank()) {
            return "";
        }
        String[] parts = stage.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (formatted.length() > 0) {
                    formatted.append(" ");
                }
                formatted.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    formatted.append(part.substring(1).toLowerCase());
                }
            }
        }
        return formatted.toString();
    }
}
