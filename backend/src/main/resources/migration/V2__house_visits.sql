-- =============================================================================
-- V2: House Visits for Buying (CP-29)
-- Add property_id to appointments for linking house visit appointments to properties
-- =============================================================================

ALTER TABLE appointments ADD COLUMN property_id UUID;

ALTER TABLE appointments ADD CONSTRAINT fk_appointments_property
    FOREIGN KEY (property_id) REFERENCES properties(property_id)
    ON DELETE SET NULL;

CREATE INDEX idx_appointments_property_id ON appointments(property_id);
