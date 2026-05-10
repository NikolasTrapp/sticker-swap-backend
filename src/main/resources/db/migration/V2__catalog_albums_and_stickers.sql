CREATE TABLE albums (
    id           UUID PRIMARY KEY,
    name         VARCHAR(200) NOT NULL,
    description  TEXT,
    year         INTEGER,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_albums_active ON albums (active);

CREATE TABLE stickers (
    id           UUID PRIMARY KEY,
    album_id     UUID NOT NULL REFERENCES albums (id),
    code       VARCHAR(20) NOT NULL,
    name         VARCHAR(200) NOT NULL,
    description  TEXT,
    image_url    VARCHAR(500),
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_stickers_album_code UNIQUE (album_id, code)
);

CREATE INDEX idx_stickers_album_id ON stickers (album_id);
CREATE INDEX idx_stickers_active ON stickers (active);
