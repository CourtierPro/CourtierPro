CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(255) NOT NULL UNIQUE,
    recipient_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE NOT NULL,
    related_transaction_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);
