CREATE TABLE organization_settings (
    id UUID PRIMARY KEY,
    default_language VARCHAR(2) NOT NULL,
    invite_subject_en VARCHAR(255) NOT NULL,
    invite_body_en TEXT NOT NULL,
    invite_subject_fr VARCHAR(255) NOT NULL,
    invite_body_fr TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE organization_settings_audit (
    id UUID PRIMARY KEY,
    timestamp TIMESTAMP,
    admin_user_id VARCHAR(255),
    admin_email VARCHAR(255),
    action VARCHAR(255),
    previous_default_language VARCHAR(255),
    new_default_language VARCHAR(255)
);
