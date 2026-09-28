CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    category        VARCHAR(100) NOT NULL,
    venue           VARCHAR(255) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    event_date      TIMESTAMPTZ NOT NULL,
    sale_start      TIMESTAMPTZ,
    sale_end        TIMESTAMPTZ,
    min_price       NUMERIC(12,0) NOT NULL DEFAULT 0,
    currency        CHAR(3) NOT NULL DEFAULT 'PYG',
    total_seats     INT NOT NULL DEFAULT 0,
    available_seats INT NOT NULL DEFAULT 0,
    image_url       VARCHAR(512),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,

    search_vector   TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(name, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(description, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(category, '')), 'C') ||
        setweight(to_tsvector('english', coalesce(venue, '')), 'D') ||
        setweight(to_tsvector('english', coalesce(city, '')), 'D')
    ) STORED
);

CREATE INDEX idx_events_category ON events (category);
CREATE INDEX idx_events_city ON events (city);
CREATE INDEX idx_events_event_date ON events (event_date);
CREATE INDEX idx_events_category_city ON events (category, city);
CREATE INDEX idx_events_search_vector ON events USING GIN (search_vector);
CREATE INDEX idx_events_name_trgm ON events USING GIN (name gin_trgm_ops);
