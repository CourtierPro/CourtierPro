CREATE TABLE analytics_export_audit_events (
    id UUID PRIMARY KEY,
    broker_id UUID NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    export_type VARCHAR(255) NOT NULL,
    filters_applied VARCHAR(1000)
);

CREATE INDEX idx_analytics_audit_broker_id ON analytics_export_audit_events(broker_id);
CREATE INDEX idx_analytics_audit_timestamp ON analytics_export_audit_events(timestamp);
