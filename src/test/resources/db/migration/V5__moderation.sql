CREATE TABLE user_blocks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id  UUID NOT NULL REFERENCES users (id),
    blocked_id  UUID NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_blocks_pair UNIQUE (blocker_id, blocked_id),
    CONSTRAINT chk_user_blocks_no_self_block CHECK (blocker_id != blocked_id)
);

CREATE INDEX idx_user_blocks_blocker ON user_blocks (blocker_id);
CREATE INDEX idx_user_blocks_blocked ON user_blocks (blocked_id);

CREATE TABLE user_reports (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id  UUID NOT NULL REFERENCES users (id),
    reported_id  UUID NOT NULL REFERENCES users (id),
    reason       VARCHAR(50) NOT NULL,
    description  TEXT,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_user_reports_no_self_report CHECK (reporter_id != reported_id)
);

CREATE INDEX idx_user_reports_status ON user_reports (status);
CREATE INDEX idx_user_reports_reporter ON user_reports (reporter_id);
CREATE INDEX idx_user_reports_reported ON user_reports (reported_id);
