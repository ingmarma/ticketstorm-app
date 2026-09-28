CREATE TABLE IF NOT EXISTS ticket_sections (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    price           NUMERIC(12, 0) NOT NULL DEFAULT 0,
    currency        CHAR(3) NOT NULL DEFAULT 'PYG',
    total_capacity  INT NOT NULL DEFAULT 0,
    available_capacity INT NOT NULL DEFAULT 0,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX idx_ticket_sections_event_id ON ticket_sections (event_id);
CREATE INDEX idx_ticket_sections_sort_order ON ticket_sections (event_id, sort_order);
