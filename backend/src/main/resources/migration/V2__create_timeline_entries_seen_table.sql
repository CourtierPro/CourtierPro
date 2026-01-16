-- Flyway migration: Create table to track which timeline entries each broker has seen
CREATE TABLE timeline_entries_seen (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    broker_id UUID NOT NULL,
    timeline_entry_id UUID NOT NULL REFERENCES timeline_entries(id) ON DELETE CASCADE,
    seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_timeline_seen_broker_entry UNIQUE(broker_id, timeline_entry_id)
);

CREATE INDEX idx_timeline_seen_broker ON timeline_entries_seen(broker_id);
CREATE INDEX idx_timeline_seen_entry ON timeline_entries_seen(timeline_entry_id);

COMMENT ON TABLE timeline_entries_seen IS 'Tracks which timeline entries have been marked as seen by each broker';
