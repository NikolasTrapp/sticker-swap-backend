CREATE TABLE user_repeated_stickers (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id),
    album_id    UUID NOT NULL REFERENCES albums (id),
    sticker_id  UUID NOT NULL REFERENCES stickers (id),
    quantity    INTEGER NOT NULL CHECK (quantity >= 0),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_repeated_sticker UNIQUE (user_id, sticker_id)
);

CREATE INDEX idx_repeated_user_album ON user_repeated_stickers (user_id, album_id);
CREATE INDEX idx_repeated_sticker_quantity ON user_repeated_stickers (sticker_id, quantity);

CREATE TABLE user_wanted_stickers (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id),
    album_id    UUID NOT NULL REFERENCES albums (id),
    sticker_id  UUID NOT NULL REFERENCES stickers (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_wanted_sticker UNIQUE (user_id, sticker_id)
);

CREATE INDEX idx_wanted_user_album ON user_wanted_stickers (user_id, album_id);
CREATE INDEX idx_wanted_sticker ON user_wanted_stickers (sticker_id);
