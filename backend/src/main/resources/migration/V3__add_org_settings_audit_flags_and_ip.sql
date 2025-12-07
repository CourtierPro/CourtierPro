ALTER TABLE organization_settings_audit
    ADD COLUMN IF NOT EXISTS invite_template_en_changed BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS invite_template_fr_changed BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(255);
