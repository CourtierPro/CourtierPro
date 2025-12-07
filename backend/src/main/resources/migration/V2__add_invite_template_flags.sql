ALTER TABLE organization_settings_audit
    ADD COLUMN invite_template_en_changed boolean NOT NULL DEFAULT false,
    ADD COLUMN invite_template_fr_changed boolean NOT NULL DEFAULT false;
