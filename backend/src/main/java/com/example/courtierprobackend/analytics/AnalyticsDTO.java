package com.example.courtierprobackend.analytics;

import java.util.Map;

/**
 * DTO containing aggregated broker analytics across all categories.
 */
public record AnalyticsDTO(
        // Transaction Overview
        int totalTransactions,
        int activeTransactions,
        int closedTransactions,
        int terminatedTransactions,
        int buyTransactions,
        int sellTransactions,
        double successRate,
        double avgTransactionDurationDays,
        int longestDurationDays,
        int shortestDurationDays,
        Map<String, Integer> transactionsOpenedPerMonth,
        Map<String, Integer> transactionsClosedPerMonth,
        Map<String, Integer> buyerStageDistribution,
        Map<String, Integer> sellerStageDistribution,

        // House Visits (Buy-Side)
        int totalHouseVisits,
        double avgHouseVisitsPerClosedTransaction,

        // Showings (Sell-Side)
        int totalSellShowings,
        double avgSellShowingsPerClosedTransaction,
        int totalSellVisitors,

        // Properties (Buy-Side)
        int totalProperties,
        double avgPropertiesPerBuyTransaction,
        double propertyInterestRate,
        int propertiesNeedingInfo,
        int propertiesWithOffers,
        int propertiesWithoutOffers,

        // Buyer Offers
        int totalBuyerOffers,
        double buyerOfferAcceptanceRate,
        double avgOfferRounds,
        double avgBuyerOfferAmount,
        int expiredOrWithdrawnOffers,
        double buyerCounterOfferRate,

        // Received Offers (Sell-Side)
        int totalOffers,
        double receivedOfferAcceptanceRate,
        double avgReceivedOfferAmount,
        double highestOfferAmount,
        double lowestOfferAmount,
        double avgOffersPerSellTransaction,
        int pendingOrReviewOffers,
        double receivedCounterOfferRate,

        // Documents
        int totalDocuments,
        int pendingDocuments,
        double documentCompletionRate,
        int documentsNeedingRevision,
        double avgDocumentsPerTransaction,

        // Appointments
        int totalAppointments,
        double appointmentConfirmationRate,
        double declinedAppointmentRate,
        double cancelledAppointmentRate,
        int upcomingAppointments,
        double avgAppointmentsPerTransaction,

        // Conditions
        int totalConditions,
        double conditionSatisfiedRate,
        int conditionsApproachingDeadline,
        int overdueConditions,
        double avgConditionsPerTransaction,

        // Client Engagement
        int totalActiveClients,
        int clientsWithMultipleTransactions,
        int appointmentsByBroker,
        int appointmentsByClient,

        // Trends
        String busiestMonth,
        int idleTransactions
) {
}
