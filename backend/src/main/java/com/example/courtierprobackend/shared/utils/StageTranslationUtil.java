package com.example.courtierprobackend.shared.utils;

import java.util.Map;

public class StageTranslationUtil {

    private StageTranslationUtil() {
        throw new UnsupportedOperationException("Utility class");
    }
    private static final Map<String, String> FRENCH_MAPPINGS = Map.ofEntries(
            // Buyer Stages
            Map.entry("BUYER_FINANCIAL_PREPARATION", "Préparation Financière"),
            Map.entry("BUYER_PROPERTY_SEARCH", "Recherche de Propriété"),
            Map.entry("BUYER_OFFER_AND_NEGOTIATION", "Offre et Négociation"),
            Map.entry("BUYER_FINANCING_AND_CONDITIONS", "Financement et Conditions"),
            Map.entry("BUYER_NOTARY_AND_SIGNING", "Notaire et Signature"),
            Map.entry("BUYER_POSSESSION", "Possession"),

            // Seller Stages
            Map.entry("SELLER_INITIAL_CONSULTATION", "Consultation Initiale"),
            Map.entry("SELLER_PUBLISH_LISTING", "Publication de l'Inscription"),
            Map.entry("SELLER_OFFER_AND_NEGOTIATION", "Offre et Négociation"),
            Map.entry("SELLER_FINANCING_AND_CONDITIONS", "Financement et Conditions"),
            Map.entry("SELLER_NOTARY_AND_SIGNING", "Notaire et Signature"),
            Map.entry("SELLER_HANDOVER", "Remise des Clés"));

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
        }

        return formatEnglish(stageEnum);
    }

    public static String constructNotificationMessage(String stage, String brokerName, String propertyAddress,
            String languageCode) {
        String translatedStage = getTranslatedStage(stage, languageCode);
        boolean isFrench = languageCode != null && languageCode.equalsIgnoreCase("fr");

        if (isFrench) {
            return String.format("Le stade a été mis à jour à %s par %s pour %s", translatedStage, brokerName,
                    propertyAddress);
        } else {
            return String.format("Stage updated to %s by %s for %s", translatedStage, brokerName, propertyAddress);
        }
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
