CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    staff_id BIGINT NOT NULL REFERENCES staffs(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    verification_code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP
);

CREATE INDEX idx_password_reset_tokens_staff_id ON password_reset_tokens (staff_id);
CREATE INDEX idx_password_reset_tokens_token_hash ON password_reset_tokens (token_hash);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens (expires_at);