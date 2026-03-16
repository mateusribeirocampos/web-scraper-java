CREATE TABLE persistent_queue_messages (
    id BIGSERIAL PRIMARY KEY,
    queue_name VARCHAR(40) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    available_at TIMESTAMP WITH TIME ZONE NOT NULL,
    claimed_at TIMESTAMP WITH TIME ZONE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_queue_messages_status_available
    ON persistent_queue_messages (status, available_at);

CREATE INDEX idx_queue_messages_queue_status
    ON persistent_queue_messages (queue_name, status, available_at);
