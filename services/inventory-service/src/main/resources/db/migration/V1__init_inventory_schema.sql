CREATE TABLE inventory_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        VARCHAR(255) NOT NULL,
    seat_id         VARCHAR(255) NOT NULL,
    section_id      VARCHAR(255) NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    user_id         VARCHAR(255),
    price           DECIMAL(10,2),
    currency        VARCHAR(3),
    timestamp       TIMESTAMPTZ  NOT NULL,
    payload         TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inventory_events_event_seat ON inventory_events (event_id, seat_id);
CREATE INDEX idx_inventory_events_type ON inventory_events (event_type);
CREATE INDEX idx_inventory_events_timestamp ON inventory_events (timestamp);

CREATE TABLE seat_snapshots (
    id                       BIGSERIAL PRIMARY KEY,
    event_id                 VARCHAR(255) NOT NULL,
    seat_id                  VARCHAR(255) NOT NULL,
    section_id               VARCHAR(255) NOT NULL,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    current_reservation_id   VARCHAR(255),
    version                  BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uq_seat_event UNIQUE (event_id, seat_id),
    CONSTRAINT chk_status CHECK (status IN ('AVAILABLE', 'BLOCKED', 'RESERVED', 'CONFIRMED', 'RELEASED'))
);

CREATE INDEX idx_seat_snapshots_event_section ON seat_snapshots (event_id, section_id);
CREATE INDEX idx_seat_snapshots_status ON seat_snapshots (status);
