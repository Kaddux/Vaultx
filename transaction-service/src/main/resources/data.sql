-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Transactions
CREATE TABLE transactions (
                              id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              user_id         UUID NOT NULL,
                              auction_id      UUID,
                              type            VARCHAR(30) NOT NULL,
    -- VALUES: 'BID_PLACED', 'BID_REFUND', 'AUCTION_WON', 'PAYMENT',
    --         'WITHDRAWAL', 'DEPOSIT', 'ESCROW_HOLD', 'ESCROW_RELEASE'
                              amount          DECIMAL(15,2) NOT NULL,
                              currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
                              status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- VALUES: 'PENDING', 'COMPLETED', 'FAILED', 'REFUNDED'
                              idempotency_key VARCHAR(255) UNIQUE,
                              description     VARCHAR(500),
                              created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                              completed_at    TIMESTAMP
);

CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_auction_id ON transactions(auction_id);
CREATE INDEX idx_transactions_idempotency ON transactions(idempotency_key);
CREATE INDEX idx_transactions_status ON transactions(status);

-- Payment Intents (for idempotent payment processing)
CREATE TABLE payment_intents (
                                 id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 user_id         UUID NOT NULL,
                                 auction_id      UUID NOT NULL,
                                 amount          DECIMAL(15,2) NOT NULL,
                                 status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- VALUES: 'PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED'
                                 idempotency_key VARCHAR(255) UNIQUE NOT NULL,
                                 failure_reason  TEXT,
                                 created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                                 updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                                 version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_payment_intents_auction_id ON payment_intents(auction_id);
CREATE INDEX idx_payment_intents_idempotency ON payment_intents(idempotency_key);

-- Escrows
CREATE TABLE escrows (
                         id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         auction_id      UUID NOT NULL UNIQUE,
                         buyer_id        UUID NOT NULL,
                         seller_id       UUID NOT NULL,
                         amount          DECIMAL(15,2) NOT NULL,
                         status          VARCHAR(20) NOT NULL DEFAULT 'HELD',
    -- VALUES: 'HELD', 'RELEASED', 'REFUNDED', 'DISPUTED'
                         created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                         released_at     TIMESTAMP,
                         version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_escrows_auction_id ON escrows(auction_id);
CREATE INDEX idx_escrows_buyer_id ON escrows(buyer_id);
CREATE INDEX idx_escrows_seller_id ON escrows(seller_id);