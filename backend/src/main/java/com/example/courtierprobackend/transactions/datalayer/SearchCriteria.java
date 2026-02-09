package com.example.courtierprobackend.transactions.datalayer;

import com.example.courtierprobackend.transactions.datalayer.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Search criteria for buy-side transactions.
 * Stores property search preferences for clients.
 */
@Entity
@Table(name = "search_criteria")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "search_criteria_id", nullable = false, unique = true)
    private UUID searchCriteriaId;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private UUID transactionId;

    // ========== Property Types ==========
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "search_criteria_property_types", joinColumns = @JoinColumn(name = "search_criteria_id", referencedColumnName = "id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "property_type")
    @Builder.Default
    private Set<PropertyType> propertyTypes = new HashSet<>();

    // ========== Features ==========
    @Column(name = "min_bedrooms")
    private Integer minBedrooms;

    @Column(name = "min_bathrooms")
    private Integer minBathrooms;

    @Column(name = "min_parking_spaces")
    private Integer minParkingSpaces;

    @Column(name = "min_garages")
    private Integer minGarages;

    @Column(name = "has_pool")
    private Boolean hasPool;

    @Column(name = "has_elevator")
    private Boolean hasElevator;

    @Column(name = "adapted_for_reduced_mobility")
    private Boolean adaptedForReducedMobility;

    @Column(name = "has_waterfront")
    private Boolean hasWaterfront;

    @Column(name = "has_access_to_waterfront")
    private Boolean hasAccessToWaterfront;

    @Column(name = "has_navigable_water")
    private Boolean hasNavigableWater;

    @Column(name = "is_resort")
    private Boolean isResort;

    @Column(name = "pets_allowed")
    private Boolean petsAllowed;

    @Column(name = "smoking_allowed")
    private Boolean smokingAllowed;

    // ========== Building ==========
    @Column(name = "min_living_area")
    private BigDecimal minLivingArea;

    @Column(name = "max_living_area")
    private BigDecimal maxLivingArea;

    @Enumerated(EnumType.STRING)
    @Column(name = "living_area_unit")
    private AreaUnit livingAreaUnit;

    @Column(name = "min_year_built")
    private Integer minYearBuilt;

    @Column(name = "max_year_built")
    private Integer maxYearBuilt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "search_criteria_building_styles", joinColumns = @JoinColumn(name = "search_criteria_id", referencedColumnName = "id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "building_style")
    @Builder.Default
    private Set<BuildingStyle> buildingStyles = new HashSet<>();

    // ========== Plex Types ==========
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "search_criteria_plex_types", joinColumns = @JoinColumn(name = "search_criteria_id", referencedColumnName = "id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "plex_type")
    @Builder.Default
    private Set<PlexType> plexTypes = new HashSet<>();

    // ========== Other Criteria ==========
    @Column(name = "min_land_area")
    private BigDecimal minLandArea;

    @Column(name = "max_land_area")
    private BigDecimal maxLandArea;

    @Enumerated(EnumType.STRING)
    @Column(name = "land_area_unit")
    private AreaUnit landAreaUnit;

    @Column(name = "new_since")
    private LocalDate newSince;

    @Column(name = "move_in_date")
    private LocalDate moveInDate;

    @Column(name = "open_houses_only")
    private Boolean openHousesOnly;

    @Column(name = "repossession_only")
    private Boolean repossessionOnly;

    // ========== Price Range ==========
    @Column(name = "min_price")
    private BigDecimal minPrice;

    @Column(name = "max_price")
    private BigDecimal maxPrice;

    // ========== Regions ==========
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "search_criteria_regions", joinColumns = @JoinColumn(name = "search_criteria_id", referencedColumnName = "id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "region")
    @Builder.Default
    private Set<QuebecRegion> regions = new HashSet<>();

    // ========== Timestamps ==========
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (searchCriteriaId == null) {
            searchCriteriaId = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
