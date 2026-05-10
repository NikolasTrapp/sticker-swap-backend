CREATE TABLE albums (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(200) NOT NULL,
    description  TEXT,
    year         INTEGER,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_albums_active ON albums (active);

CREATE TABLE stickers (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    album_id     UUID NOT NULL REFERENCES albums (id),
    number       VARCHAR(20) NOT NULL,
    code         VARCHAR(200) NOT NULL,
    description  TEXT,
    image_url    VARCHAR(500),
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_stickers_album_number UNIQUE (album_id, number)
);

CREATE INDEX idx_stickers_album_id ON stickers (album_id);
CREATE INDEX idx_stickers_active ON stickers (active);
