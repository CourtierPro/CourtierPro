-- Create search_criteria table for buy-side transaction property search preferences
CREATE TABLE IF NOT EXISTS search_criteria (
    id BIGSERIAL PRIMARY KEY,
    search_criteria_id UUID NOT NULL UNIQUE,
    transaction_id UUID NOT NULL UNIQUE,

    -- Features
    min_bedrooms INTEGER,
    min_bathrooms INTEGER,
    min_parking_spaces INTEGER,
    min_garages INTEGER,
    has_pool BOOLEAN,
    has_elevator BOOLEAN,
    adapted_for_reduced_mobility BOOLEAN,
    has_waterfront BOOLEAN,
    has_access_to_waterfront BOOLEAN,
    has_navigable_water BOOLEAN,
    is_resort BOOLEAN,
    pets_allowed BOOLEAN,
    smoking_allowed BOOLEAN,

    -- Building
    min_living_area DECIMAL(12, 2),
    max_living_area DECIMAL(12, 2),
    living_area_unit VARCHAR(10),
    min_year_built INTEGER,
    max_year_built INTEGER,

    -- Other Criteria
    min_land_area DECIMAL(12, 2),
    max_land_area DECIMAL(12, 2),
    land_area_unit VARCHAR(10),
    new_since DATE,
    move_in_date DATE,
    open_houses_only BOOLEAN,
    repossession_only BOOLEAN,

    -- Price Range
    min_price DECIMAL(15, 2),
    max_price DECIMAL(15, 2),

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    -- Foreign key constraint
    CONSTRAINT fk_search_criteria_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE CASCADE
);

-- Create index on transaction_id for quick lookups
CREATE INDEX IF NOT EXISTS idx_search_criteria_transaction_id ON search_criteria(transaction_id);

-- Create collection table for property types (Set<PropertyType>)
CREATE TABLE IF NOT EXISTS search_criteria_property_types (
    search_criteria_id BIGINT NOT NULL,
    property_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (search_criteria_id, property_type),
    CONSTRAINT fk_sc_property_types_search_criteria FOREIGN KEY (search_criteria_id) REFERENCES search_criteria(id) ON DELETE CASCADE
);

-- Create collection table for building styles (Set<BuildingStyle>)
CREATE TABLE IF NOT EXISTS search_criteria_building_styles (
    search_criteria_id BIGINT NOT NULL,
    building_style VARCHAR(50) NOT NULL,
    PRIMARY KEY (search_criteria_id, building_style),
    CONSTRAINT fk_sc_building_styles_search_criteria FOREIGN KEY (search_criteria_id) REFERENCES search_criteria(id) ON DELETE CASCADE
);

-- Create collection table for plex types (Set<PlexType>)
CREATE TABLE IF NOT EXISTS search_criteria_plex_types (
    search_criteria_id BIGINT NOT NULL,
    plex_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (search_criteria_id, plex_type),
    CONSTRAINT fk_sc_plex_types_search_criteria FOREIGN KEY (search_criteria_id) REFERENCES search_criteria(id) ON DELETE CASCADE
);

-- Create collection table for Quebec regions (Set<QuebecRegion>)
CREATE TABLE IF NOT EXISTS search_criteria_regions (
    search_criteria_id BIGINT NOT NULL,
    region VARCHAR(50) NOT NULL,
    PRIMARY KEY (search_criteria_id, region),
    CONSTRAINT fk_sc_regions_search_criteria FOREIGN KEY (search_criteria_id) REFERENCES search_criteria(id) ON DELETE CASCADE
);
