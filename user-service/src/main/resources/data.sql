-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users
CREATE TABLE users (
                       id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       username        VARCHAR(100) NOT NULL,
                       email           VARCHAR(255) NOT NULL UNIQUE,
                       password_hash   VARCHAR(255) NOT NULL,
                       full_name       VARCHAR(200),
                       phone           VARCHAR(20),
                       kyc_status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- VALUES: 'PENDING', 'VERIFIED', 'REJECTED'
                       user_rating     DECIMAL(3,2) DEFAULT 5.00,
    -- range 1.00 - 5.00
                       role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    -- VALUES: 'USER', 'SELLER', 'ADMIN'
                       created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                       version         BIGINT NOT NULL DEFAULT 0
    -- optimistic lock
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_kyc_status ON users(kyc_status);
CREATE INDEX idx_users_role ON users(role);

-- Wallets
CREATE TABLE wallets (
                         id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         user_id         UUID NOT NULL UNIQUE,
                         balance         DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                         reserved_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    -- amount locked in active bids
                         currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
                         created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                         updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                         version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_wallets_user_id ON wallets(user_id);

-- Refresh Tokens
CREATE TABLE refresh_tokens (
                                id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id         UUID NOT NULL,
                                token           VARCHAR(500) NOT NULL UNIQUE,
                                expires_at      TIMESTAMP NOT NULL,
                                revoked         BOOLEAN NOT NULL DEFAULT FALSE,
                                created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);