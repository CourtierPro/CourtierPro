export interface AnalyticsData {
    // Transaction Overview
    totalTransactions: number;
    activeTransactions: number;
    closedTransactions: number;
    terminatedTransactions: number;
    buyTransactions: number;
    sellTransactions: number;
    successRate: number;
    avgTransactionDurationDays: number;
    longestDurationDays: number;
    shortestDurationDays: number;
    transactionsOpenedPerMonth: Record<string, number>;
    transactionsClosedPerMonth: Record<string, number>;
    buyerStageDistribution: Record<string, number>;
    sellerStageDistribution: Record<string, number>;
    buyerPipeline: PipelineStageDTO[];
    sellerPipeline: PipelineStageDTO[];

    // House Visits (Buy-Side)
    totalHouseVisits: number;
    avgHouseVisitsPerClosedTransaction: number;

    // Showings (Sell-Side)
    totalSellShowings: number;
    avgSellShowingsPerClosedTransaction: number;
    totalSellVisitors: number;

    // Properties (Buy-Side)
    totalProperties: number;
    avgPropertiesPerBuyTransaction: number;
    propertyInterestRate: number;
    propertiesNeedingInfo: number;
    propertiesWithOffers: number;
    propertiesWithoutOffers: number;

    // Buyer Offers
    totalBuyerOffers: number;
    buyerOfferAcceptanceRate: number;
    avgOfferRounds: number;
    avgBuyerOfferAmount: number;
    expiredOrWithdrawnOffers: number;
    buyerCounterOfferRate: number;

    // Received Offers (Sell-Side)
    totalOffers: number;
    receivedOfferAcceptanceRate: number;
    avgReceivedOfferAmount: number;
    highestOfferAmount: number;
    lowestOfferAmount: number;
    avgOffersPerSellTransaction: number;
    pendingOrReviewOffers: number;
    receivedCounterOfferRate: number;

    // Documents
    totalDocuments: number;
    pendingDocuments: number;
    documentCompletionRate: number;
    documentsNeedingRevision: number;
    avgDocumentsPerTransaction: number;

    // Appointments
    totalAppointments: number;
    appointmentConfirmationRate: number;
    declinedAppointmentRate: number;
    cancelledAppointmentRate: number;
    upcomingAppointments: number;
    avgAppointmentsPerTransaction: number;

    // Conditions
    totalConditions: number;
    conditionSatisfiedRate: number;
    conditionsApproachingDeadline: number;
    overdueConditions: number;
    avgConditionsPerTransaction: number;

    // Client Engagement
    totalActiveClients: number;
    clientsWithMultipleTransactions: number;
    appointmentsByBroker: number;
    appointmentsByClient: number;

    // Trends
    busiestMonth: string;
    idleTransactions: number;
}

export interface PipelineStageDTO {
    stageName: string;
    count: number;
    avgDays: number;
    clients: ClientStageInfoDTO[];
}

export interface ClientStageInfoDTO {
    clientName: string;
    daysInStage: number;
}

export interface AnalyticsFilter {
    startDate?: string; // ISO date string (YYYY-MM-DD)
    endDate?: string;   // ISO date string (YYYY-MM-DD)
    transactionType?: 'BUY_SIDE' | 'SELL_SIDE';
    clientName?: string;
}
