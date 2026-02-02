package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.enums.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Response DTO for search criteria.
 */
@Data
@Builder
public class SearchCriteriaResponseDTO {

    private UUID searchCriteriaId;
    private UUID transactionId;

    // Property Types
    private Set<PropertyType> propertyTypes;

    // Features
    private Integer minBedrooms;
    private Integer minBathrooms;
    private Integer minParkingSpaces;
    private Integer minGarages;
    private Boolean hasPool;
    private Boolean hasElevator;
    private Boolean adaptedForReducedMobility;
    private Boolean hasWaterfront;
    private Boolean hasAccessToWaterfront;
    private Boolean hasNavigableWater;
    private Boolean isResort;
    private Boolean petsAllowed;
    private Boolean smokingAllowed;

    // Building
    private BigDecimal minLivingArea;
    private BigDecimal maxLivingArea;
    private AreaUnit livingAreaUnit;
    private Integer minYearBuilt;
    private Integer maxYearBuilt;
    private Set<BuildingStyle> buildingStyles;

    // Plex Types
    private Set<PlexType> plexTypes;

    // Other Criteria
    private BigDecimal minLandArea;
    private BigDecimal maxLandArea;
    private AreaUnit landAreaUnit;
    private LocalDate newSince;
    private LocalDate moveInDate;
    private Boolean openHousesOnly;
    private Boolean repossessionOnly;

    // Price Range
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // Regions
    private Set<QuebecRegion> regions;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
