-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Auctions
CREATE TABLE auctions (
                          id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          title           VARCHAR(200) NOT NULL,
                          description     TEXT,
                          seller_id       UUID NOT NULL,
                          starting_price  DECIMAL(15,2) NOT NULL,
                          reserve_price   DECIMAL(15,2),
    -- minimum price to sell; if not met, auction fails
                          current_bid     DECIMAL(15,2),
                          bid_increment   DECIMAL(15,2) NOT NULL DEFAULT 1.00,
    -- minimum increment over current bid
                          status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- VALUES: 'PENDING', 'ACTIVE', 'SOLD', 'UNSOLD', 'CANCELLED'
                          start_time      TIMESTAMP NOT NULL,
                          end_time        TIMESTAMP NOT NULL,
                          extended_at     TIMESTAMP,
    -- last time extended due to late bidding
                          extension_period_seconds INT NOT NULL DEFAULT 120,
    -- auto-extend if bid placed within this window before end
                          currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
                          created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                          updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                          version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_auctions_seller_id ON auctions(seller_id);
CREATE INDEX idx_auctions_status ON auctions(status);
CREATE INDEX idx_auctions_end_time ON auctions(end_time) WHERE status = 'ACTIVE';
CREATE INDEX idx_auctions_start_time ON auctions(start_time) WHERE status = 'PENDING';

-- Bids
CREATE TABLE bids (
                      id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                      auction_id      UUID NOT NULL,
                      bidder_id       UUID NOT NULL,
                      amount          DECIMAL(15,2) NOT NULL,
                      max_auto_bid    DECIMAL(15,2),
    -- if set, system auto-bids up to this max
                      is_auto_bid     BOOLEAN NOT NULL DEFAULT FALSE,
                      status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    -- VALUES: 'ACTIVE', 'OUTBID', 'WINNING', 'WON', 'LOST', 'WITHDRAWN'
                      created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                      idempotency_key VARCHAR(255) UNIQUE
);

CREATE INDEX idx_bids_auction_id ON bids(auction_id);
CREATE INDEX idx_bids_bidder_id ON bids(bidder_id);
CREATE INDEX idx_bids_auction_id_amount ON bids(auction_id, amount DESC);
CREATE INDEX idx_bids_idempotency ON bids(idempotency_key);
CREATE INDEX idx_bids_created_at ON bids(auction_id, created_at DESC);

-- Snapshot of current highest bid (denormalized for performance)

-- Outbox events (for transactional outbox pattern)
CREATE TABLE outbox_events (
                               id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               aggregate_type  VARCHAR(100) NOT NULL,
                               aggregate_id    VARCHAR(100) NOT NULL,
                               event_type      VARCHAR(100) NOT NULL,
                               payload         JSONB NOT NULL,
                               created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                               published       BOOLEAN NOT NULL DEFAULT FALSE,
                               published_at    TIMESTAMP
);

CREATE INDEX idx_outbox_unpublished ON outbox_events(published, created_at)
    WHERE published = FALSE;