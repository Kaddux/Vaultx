# VaultX — Frontend ⇄ API Connectivity Assessment

> How many of the backend APIs can the frontend actually call today, which existing APIs are insufficient, and which new APIs must be created to reach **100% connectivity** (zero mock data).

---

## 1. Executive Summary

| Metric | Value |
|---|---|
| Backend REST endpoints exposed via the API Gateway | **21** |
| Existing endpoints the frontend can wire up **as-is** (frontend work only) | **18** (86%) |
| Existing endpoints that are **insufficient** for their target page and must be **enhanced** | **3** |
| **New APIs** that must be created (feature gaps with no endpoint at all) | **5** |
| Internal gRPC services that **must NOT** be exposed to the frontend | 3 services / 5 RPCs |
| Frontend ↔ real backend connection today | **0%** (100% mock / localStorage) |
| Target connectivity | **100%** after the work below |

**Bottom line:** Every one of the 21 existing endpoints is *reachable* from the frontend through the gateway. 18 of them map cleanly onto pages with frontend work only. The remaining coverage shortfall is caused by **3 under-powered existing endpoints** (can't filter/list what a page needs) and **5 entirely missing features** (transaction history list, "my bids across auctions", "my auctions" filter, watchlist, KYC submission). Closing those gaps is what gets the frontend to 100% real-API coverage.

---

## 2. Current State of the Frontend

- `frontend/src/api.ts` is **100% mock data** (`MOCK_USER`, `MOCK_AUCTIONS`, `MOCK_BID_HISTORY`, `MOCK_TRANSACTIONS`, `MOCK_SELLER_AUCTIONS`) persisted to `localStorage`. There is **no HTTP client** anywhere in the app.
- Login/Register simulate auth by writing a `vaultx_logged_in` flag to `localStorage` — **no JWT is ever stored or validated**.
- All 10 pages import the `MOCK_*` constants directly and mutate them in place.
- **Vite proxy bug:** `vite.config.ts:11` proxies `/api` → `http://localhost:8000` (User Service). This bypasses the API Gateway (`:8080`), so only user-service paths would even resolve — auctions/payments/notifications would 404. It must target the gateway.

---

## 3. Complete Backend API Inventory

### 3.1 REST APIs exposed through the gateway (`:8080`) — 21 endpoints

**User Service** (`user-service`, via `/api/auth`, `/api/users`, `/api/wallet`)

| # | Method | Path | Auth | Used by page |
|---|---|---|---|---|
| 1 | POST | `/api/auth/register` | public | Register |
| 2 | POST | `/api/auth/login` | public | Login |
| 3 | POST | `/api/auth/refresh` | public | Token refresh interceptor |
| 4 | GET | `/api/users/me` | JWT | Profile / TopNav / Home |
| 5 | PATCH | `/api/users/me` | JWT | Profile Settings |
| 6 | DELETE | `/api/users/me` | JWT | Account deletion |
| 7 | GET | `/api/wallet` | JWT | Wallet / TopNav balance |
| 8 | POST | `/api/wallet/deposit` | JWT | Wallet deposit |

**Bidding Service** (`bidding-service`, via `/api/auctions`)

| # | Method | Path | Auth | Used by page |
|---|---|---|---|---|
| 9 | GET | `/api/auctions` | public | Explore |
| 10 | GET | `/api/auctions/{id}` | public | Auction Detail |
| 11 | POST | `/api/auctions` | JWT + rate-limit | Seller Portal (create) |
| 12 | GET | `/api/auctions/{id}/bids` | public | Auction Detail bid history |
| 13 | POST | `/api/auctions/{id}/bids` | JWT + rate-limit | Auction Detail (place bid) |
| 14 | GET | `/api/auctions/{id}/bids/mine` | JWT | Auction Detail ("my bid") |

**Transaction Service** (`transaction-service`, via `/api/payments`)

| # | Method | Path | Auth | Used by page |
|---|---|---|---|---|
| 15 | POST | `/api/payments/release` | JWT | Checkout (seller) |
| 16 | POST | `/api/payments/refund` | JWT | Checkout (refund) |
| 17 | GET | `/api/payments/{auctionId}` | JWT | Checkout (status) |

**Notification Service** (`notification-service`, via `/api/notifications`)

| # | Method | Path | Auth | Used by page |
|---|---|---|---|---|
| 18 | GET | `/api/notifications` | JWT | Notifications feed |
| 19 | GET | `/api/notifications/unread-count` | JWT | Bell badge |
| 20 | PUT | `/api/notifications/preferences/{eventType}` | JWT | Preferences |
| 21 | POST | `/api/notifications/request` | JWT | Custom notification trigger |

### 3.2 Internal gRPC — DO NOT expose to the browser

| Service | RPCs |
|---|---|
| `UserService` | `GetUserProfile`, `GetWalletBalance`, `UpdateWallet` |
| `BiddingService` | `GetAuctionDetails` |
| `TransactionService` | `GetPaymentStatus` |

These are backend-to-backend synchronous calls. They are not HTTP and must never be called directly from the browser (they also lack gateway auth/CORS). No frontend work is needed here.

---

## 4. Connectivity Assessment Matrix

### 4.1 ✅ Category A — Connectable as-is (frontend work only) — **18 endpoints**

| # | Endpoint | Verdict | Frontend work needed |
|---|---|---|---|
| 1 | POST `/api/auth/register` | ✅ Connect | Replace mock submit with real call |
| 2 | POST `/api/auth/login` | ✅ Connect | Real call + JWT storage |
| 3 | POST `/api/auth/refresh` | ✅ Connect | Refresh interceptor on 401 |
| 4 | GET `/api/users/me` | ✅ Connect | Hydrate profile after login |
| 5 | PATCH `/api/users/me` | ✅ Connect | Profile settings form |
| 6 | DELETE `/api/users/me` | ✅ Connect | Account deletion UI |
| 7 | GET `/api/wallet` | ✅ Connect | Replace `MOCK_USER.balance` |
| 8 | POST `/api/wallet/deposit` | ✅ Connect | Deposit form + idempotencyKey |
| 9 | GET `/api/auctions` | ✅ Connect (browse) | Explore page (see 4.2 for seller filter) |
| 10 | GET `/api/auctions/{id}` | ✅ Connect | Auction detail (map fields) |
| 11 | POST `/api/auctions` | ✅ Connect | Create-auction modal |
| 12 | GET `/api/auctions/{id}/bids` | ✅ Connect | Bid history |
| 13 | POST `/api/auctions/{id}/bids` | ✅ Connect | Place bid + idempotencyKey |
| 14 | GET `/api/auctions/{id}/bids/mine` | ✅ Connect | "Your bid" chip (per auction) |
| 15 | POST `/api/payments/release` | ✅ Connect | Checkout seller release |
| 16 | POST `/api/payments/refund` | ✅ Connect | Refund flow |
| 17 | GET `/api/payments/{auctionId}` | ✅ Connect | Checkout escrow status |
| 18 | GET `/api/notifications` | ✅ Connect | Notifications feed UI |
| 19 | GET `/api/notifications/unread-count` | ✅ Connect | Bell badge polling |
| 20 | PUT `/api/notifications/preferences/{eventType}` | ✅ Connect | Preferences toggles |
| 21 | POST `/api/notifications/request` | ✅ Connect | Custom send |

> **Note on #9:** usable immediately for public browsing. It is listed again in Category B only because the **Seller Portal** needs the same endpoint to support a `sellerId` filter that does not exist yet.

### 4.2 ⚠️ Category B — Existing but insufficient; must be enhanced — **3 endpoints**

| # | Endpoint | Why it fails its page | Required change |
|---|---|---|---|
| 9 | `GET /api/auctions` | Controller only accepts `?status=` (`AuctionService.getAll`, `AuctionController.java:34`). The **Seller Portal "Your Auctions"** table needs auctions owned by the current user. | Add optional `sellerId` query param → `auctionRepository.findBySellerId(...)`. |
| 14 | `GET /api/auctions/{id}/bids/mine` | Requires an `auctionId`. The **Home dashboard "Active Bids Tracker"** needs *all* bids by the current user across auctions. | Add aggregate route `GET /api/auctions/bids/mine` returning the user's bids (each with its auction snapshot). |
| 17 | `GET /api/payments/{auctionId}` | Only returns escrow for one auction. The **Transactions page / Wallet transaction ledger** needs the user's full history. | Add `GET /api/transactions` listing the current user's transactions. (Gateway already routes `/api/transactions/**`, but no controller implements it.) |

### 4.3 ❌ Category C — No API exists; must be created — **5 new APIs**

| # | New API | Method | Purpose | Driven by |
|---|---|---|---|---|
| N1 | `GET /api/transactions` | GET | Full transaction ledger (DEPOSIT / ESCROW_HOLD / ESCROW_RELEASE / REFUND / WITHDRAWAL with status) | Transactions.tsx, Wallet.tsx |
| N2 | `GET /api/auctions/bids/mine` | GET | All my bids across auctions (with auction snapshot + WINNING/OUTBID state) | Home.tsx |
| N3 | `POST /api/auctions/{id}/watchlist` | POST | Add auction to watchlist | AuctionDetail.tsx watch button |
| N4 | `DELETE /api/auctions/{id}/watchlist` | DELETE | Remove from watchlist | AuctionDetail.tsx |
| N5 | `GET /api/watchlist` | GET | List watched auctions | Home.tsx watchlist section |
| N6 | `POST /api/users/me/kyc` | POST | Submit KYC (doc type, full name, address, document/selfie refs) → sets `kycStatus` to `PENDING` | Wallet.tsx KYC wizard |

> Watchlist (N3–N5) and KYC (N6) have **no backing model, repository, or event** anywhere in the backend — they are entirely new features, not just new routes.

---

## 5. Page-by-Page Gap Analysis

| Page | APIs today (mock) | APIs required | Gap severity |
|---|---|---|---|
| **Landing** (`/`) | none | none | ✅ none |
| **Login** | fake localStorage auth | POST `/auth/login` (+ refresh) | ✅ frontend only |
| **Register** | fake localStorage auth | POST `/auth/register` | ✅ frontend only |
| **Explore** | `MOCK_AUCTIONS` | GET `/api/auctions?status=ACTIVE` | ✅ frontend only (field mapping) |
| **Auction Detail** | `MOCK_AUCTIONS`, `MOCK_BID_HISTORY` | GET `/api/auctions/{id}`, GET `.../bids`, POST `.../bids`, POST/DELETE watchlist | ⚠️ needs watchlist APIs (N3–N4) |
| **Home dashboard** | hardcoded arrays | GET `/api/auctions/bids/mine`, GET `/api/watchlist`, GET `/api/wallet`, GET `/api/notifications` | ❌ needs N2, N5 |
| **Wallet** | `MOCK_USER.balance`, KYC simulation | GET/POST `/api/wallet*`, GET `/api/transactions`, POST `/api/users/me/kyc` | ❌ needs N1, N6 |
| **Transactions** | `MOCK_TRANSACTIONS` | GET `/api/transactions` | ❌ needs N1 |
| **Seller Portal** | `MOCK_SELLER_AUCTIONS` | GET `/api/auctions?sellerId=me`, POST `/api/auctions` | ⚠️ needs `sellerId` filter |
| **Checkout** | `MOCK_AUCTIONS.find(SOLD)` | GET `/api/payments/{id}`, POST `/api/payments/release` | ✅ frontend only |

**Features with real UI but no backend counterpart (frontend-only features that will break connectivity):**
- **Watchlist** — the watch button and dashboard section are pure UI state; no model exists.
- **KYC wizard** — a 4-step simulation that mutates `localStorage`; no submission endpoint.
- **Seller display data** — `AuctionResponse` has `sellerId` (a UUID) but no username/rating/verified/avatar. The UI requires rich seller info. Backend must either expose seller details via a user lookup or return them in the auction DTO.
- **Category / images / lotNumber / views / watchers** — present in the mock `Auction` type and the create form, but absent from `AuctionRequest`/`AuctionResponse`. They will render as empty/fallback until fields are added to the DTOs (or derived client-side).

---

## 6. Required Backend Work

### 6.1 Enhancements to existing endpoints (3)

1. **`GET /api/auctions`** — add optional `?sellerId=<uuid>` filter.
   - `AuctionRepository`: add `List<Auction> findBySellerId(UUID sellerId);`
   - `AuctionService.getAll(String status, UUID sellerId)` + `AuctionController` param.
2. **`GET /api/auctions/bids/mine`** — new route on `AuctionController` that queries `BidRepository` by `bidderId` and joins auction snapshots (`auctionId`, `title`, `status`, `currentBid`, `endTime`) into a `MyBidResponse` so the dashboard can render without N+1 polling.
3. **`GET /api/transactions`** — new controller in `transaction-service` under the existing `/api/transactions/**` gateway route (already routed + JWT-protected in `application.yml:62`). Query `TransactionRepository` by the authenticated user (via `X-User-Id`).

### 6.2 New features (watchlist + KYC) — new code

4. **Watchlist (bidding-service)**
   - New entity `WatchlistEntry(id, userId, auctionId, createdAt)` + repository.
   - Routes: `POST /api/auctions/{id}/watchlist`, `DELETE /api/auctions/{id}/watchlist`, `GET /api/watchlist`.
   - All JWT-protected (they mutate per-user state) — note the gateway currently routes `GET /api/auctions/**` through the **public** route; watchlist POST/DELETE use the secure route, and `GET /api/watchlist` needs a new gateway route or inclusion in `user-service-api` path list.
5. **KYC submission (user-service)**
   - `KycRequestDTO(docType, fullName, address, documentRef, selfieRef)` → `POST /api/users/me/kyc` sets `kycStatus=PENDING` and (optionally) emits a `KYC_SUBMITTED` outbox event for notification.
   - Read side already works: `GET /api/users/me` returns `kycStatus`.

### 6.3 Optional (non-blocking, UX polish)

6. **Bid display names** — `BidResponse` exposes only `bidderId`. Add `bidderUsername` (or return masked display) so bid history doesn't show raw UUIDs.
7. **Auction media/category fields** — add `category`, `lotNumber`, `imageUrl` to `AuctionRequest`/`AuctionResponse` so the marketplace grid/detail can show real categories and images instead of gradient placeholders.
8. **Notification preference read** — `GET /api/notifications/preferences` so toggles can be pre-populated.

---

## 7. Required Frontend Work (regardless of backend)

1. **Fix the Vite proxy** — `vite.config.ts` must proxy `/api` → `http://localhost:8080` (API Gateway), not `:8000`.
2. **Add an HTTP client layer** (`src/api/`) — fetch/axios wrapper with:
   - Base URL → gateway, `Authorization: Bearer <accessToken>`.
   - **401 → `/api/auth/refresh` → retry** interceptor (access token 15 min / refresh 7 days).
   - Unified error mapping to `ApiErrorResponse` (`status`, `error`, `message`).
3. **Auth context** — real JWT/refresh token lifecycle, replace the `vaultx_logged_in` flag; hydrate profile + wallet after login (`GET /users/me` + `GET /wallet`).
4. **DTO mapping layer** — map backend DTOs to the UI `Auction`/`Bid`/`Transaction` types (backend uses `sellerId`, `startTime/endTime`, `BigDecimal`; UI expects rich display fields). Derive or fall back for fields the backend doesn't send yet.
5. **Idempotency keys** — every bid/deposit already requires a client-generated UUID; wire `crypto.randomUUID()` into the new request layer.
6. **Polling** — no WebSockets exist; implement adaptive polling (auction detail 3–10s, notifications 30s) as recommended in `FRONTEND_ARCHITECTURE.md` §7.3.
7. **Route guards** — replace localStorage checks with real auth state (protected routes for `/wallet`, `/transactions`, `/seller`, `/checkout`).

---

## 8. Blocking Infrastructure Issues (must fix before 100%)

| # | Issue | Where | Impact |
|---|---|---|---|
| 1 | Vite proxy targets User Service `:8000` instead of Gateway `:8080` | `vite.config.ts:11` | Only `/api/users*` would work; everything else 404s |
| 2 | Rate limiter depends on Redis; bids fail with 429/connection errors if Redis is down | gateway config | Bidding breaks without Redis up |
| 3 | Notification controller falls back to a placeholder user UUID when `X-User-Id` is missing | `NotificationController.java:52` | Must rely on gateway JWT injection (works, but masks auth bugs) |
| 4 | Frontend dev port is `3000` in `vite.config.ts`; docs say `5173` — both are in gateway CORS, so OK, but keep them aligned | docs vs config | minor |

---

## 9. Implementation Roadmap

| Phase | Scope | Effort |
|---|---|---|
| **1. Foundation** | Fix Vite proxy → gateway; add HTTP client + auth context + refresh interceptor; replace Login/Register/Explore/AuctionDetail browsing with real calls | Frontend, small backend touches |
| **2. Core flows** | Wire bid placement, deposit, create-auction, checkout release/refund with idempotency keys + rate-limit UX | Frontend only |
| **3. Backend gaps** | `GET /api/transactions`, `GET /api/auctions/bids/mine`, `?sellerId` filter, bidder usernames | Backend |
| **4. New features** | Watchlist (3 APIs) + KYC submission endpoint + gateway route for `/api/watchlist` | Backend |
| **5. Finish UI** | Transactions ledger, Seller "my auctions", Home dashboard aggregation, Notifications bell/feed/preferences | Frontend |
| **6. Polish** | Auction category/images fields, seller profile enrichment, remove all `MOCK_*` + `saveState()` | Both |

**Definition of 100%:** no page reads from `MOCK_*` / `localStorage` for domain data; every mutation goes through the gateway; every user-visible data block is served by a real endpoint.

---

## 10. Final Scorecard

```
Total existing REST endpoints ......................... 21
  └─ Connectable as-is (frontend work only) ........... 18
  └─ Existing but insufficient (enhance) ...............  3
  └─ Non-existent (new APIs to create) .................  5   (N1–N6)
                                                    ─────
New/existing API surface after work ................. 24 (21 + 5 − 2 merged: sellerId filter & bids/mine reuse existing routes)

Coverage today .........................................   0%  (mock)
Coverage after Phase 1–2 ............................... ~75%
Coverage after Phase 3–5 ............................... 100%  (zero mock)
```

**Key takeaway:** the backend is *not* the bottleneck for most of the app — 18 of 21 endpoints already exist and are correctly routed/authenticated. Reaching 100% connectivity requires fixing the Vite proxy, building the missing frontend HTTP/auth layer, enhancing 3 list/filter endpoints, and creating 5 genuinely new endpoints (transaction history, aggregate "my bids", seller filter support, watchlist ×3, KYC submission).
