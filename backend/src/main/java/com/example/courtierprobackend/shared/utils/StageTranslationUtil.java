package com.example.courtierprobackend.shared.utils;

import java.util.Map;

public class StageTranslationUtil {

    private StageTranslationUtil() {
        throw new UnsupportedOperationException("Utility class");
    }
    private static final Map<String, String> FRENCH_MAPPINGS = Map.ofEntries(
            // Buyer Stages
            Map.entry("BUYER_PREQUALIFY_FINANCIALLY", "Pré-qualification Financière"),
            Map.entry("BUYER_SHOP_FOR_PROPERTY", "Recherche de Propriété"),
            Map.entry("BUYER_SUBMIT_OFFER", "Soumission d'Offre"),
            Map.entry("BUYER_OFFER_ACCEPTED", "Offre Acceptée"),
            Map.entry("BUYER_HOME_INSPECTION", "Inspection Immobilière"),
            Map.entry("BUYER_FINANCING_FINALIZED", "Financement Finalisé"),
            Map.entry("BUYER_FIRST_NOTARY_APPOINTMENT", "Premier Rendez-vous Notaire"),
            Map.entry("BUYER_SECOND_NOTARY_APPOINTMENT", "Deuxième Rendez-vous Notaire"),
            Map.entry("BUYER_OCCUPANCY", "Prise de Possession"),
            Map.entry("BUYER_TERMINATED", "Transaction Terminée"),

            // Seller Stages
            Map.entry("SELLER_INITIAL_CONSULTATION", "Consultation Initiale"),
            Map.entry("SELLER_LISTING_PUBLISHED", "Inscription Publiée"),
            Map.entry("SELLER_REVIEW_OFFERS", "Révision des Offres"),
            Map.entry("SELLER_ACCEPT_BEST_OFFER", "Acceptation de la Meilleure Offre"),
            Map.entry("SELLER_CONDITIONS_MET", "Conditions Remplies"),
            Map.entry("SELLER_NOTARY_COORDINATION", "Coordination Notaire"),
            Map.entry("SELLER_NOTARY_APPOINTMENT", "Rendez-vous Notaire"),
            Map.entry("SELLER_HANDOVER_KEYS", "Remise des Clés"),
            Map.entry("SELLER_TERMINATED", "Transaction Terminée"));

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
