CREATE TABLE chat_conversations (
    id          UUID PRIMARY KEY,
    user_a_id   UUID NOT NULL REFERENCES users (id),
    user_b_id   UUID NOT NULL REFERENCES users (id),
    sticker_id  UUID NOT NULL REFERENCES stickers (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_chat_conversations_pair_sticker UNIQUE (user_a_id, user_b_id, sticker_id)
);

CREATE INDEX idx_chat_conversations_user_a ON chat_conversations (user_a_id);
CREATE INDEX idx_chat_conversations_user_b ON chat_conversations (user_b_id);
CREATE INDEX idx_chat_conversations_sticker ON chat_conversations (sticker_id);

CREATE TABLE chat_messages (
    id               UUID PRIMARY KEY,
    conversation_id  UUID NOT NULL REFERENCES chat_conversations (id),
    sender_user_id   UUID REFERENCES users (id),
    type             VARCHAR(20) NOT NULL,
    body             TEXT NOT NULL,
    sent_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_messages_conversation_sent ON chat_messages (conversation_id, sent_at);
CREATE INDEX idx_chat_messages_sender ON chat_messages (sender_user_id);
