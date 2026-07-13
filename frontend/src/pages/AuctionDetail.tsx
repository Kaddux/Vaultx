import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { TopNav } from '../components/TopNav';
import { CountdownClock } from '../components/CountdownTimer';
import {
  MOCK_AUCTIONS,
  MOCK_BID_HISTORY,
  Bid,
  Auction,
  formatCurrency,
  formatBidTime,
  MOCK_USER,
  MOCK_TRANSACTIONS,
  Transaction,
  saveState,
} from '../api';

// ─── Image Carousel ───────────────────────────────────────────────────────────
function ImageCarousel({ auction }: { auction: Auction }) {
  const [current, setCurrent] = useState(0);
  const count = auction.imageCount;

  const prev = () => setCurrent((c) => (c - 1 + count) % count);
  const next = () => setCurrent((c) => (c + 1) % count);

  return (
    <div className="relative rounded-xl overflow-hidden group select-none">
      {/* Main image placeholder */}
      <div
        className="w-full h-[400px] flex flex-col items-center justify-center gap-4 transition-all duration-500"
        style={{ backgroundColor: auction.imageColor }}
      >
        <span
          className="material-symbols-outlined text-white/40"
          style={{ fontSize: '96px' }}
        >
          {auction.imageAccent}
        </span>
        <div className="text-white/30 text-xs font-medium uppercase tracking-widest">
          Image {current + 1} of {count}
        </div>
      </div>

      {/* Prev / Next buttons */}
      {count > 1 && (
        <>
          <button
            id="carousel-prev"
            onClick={prev}
            className="absolute left-3 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-white/90 hover:bg-white shadow-card flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-200"
          >
            <span className="material-symbols-outlined text-text-primary" style={{ fontSize: '20px' }}>
              chevron_left
            </span>
          </button>
          <button
            id="carousel-next"
            onClick={next}
            className="absolute right-3 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-white/90 hover:bg-white shadow-card flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-200"
          >
            <span className="material-symbols-outlined text-text-primary" style={{ fontSize: '20px' }}>
              chevron_right
            </span>
          </button>
        </>
      )}

      {/* Dot indicators */}
      {count > 1 && (
        <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1.5">
          {Array.from({ length: count }).map((_, i) => (
            <button
              key={i}
              onClick={() => setCurrent(i)}
              className={`h-1.5 rounded-full transition-all duration-200 ${
                i === current ? 'w-5 bg-white' : 'w-1.5 bg-white/50 hover:bg-white/80'
              }`}
            />
          ))}
        </div>
      )}

      {/* Thumbnail strip */}
      {count > 1 && (
        <div className="flex gap-2 mt-2">
          {Array.from({ length: count }).map((_, i) => (
            <button
              key={i}
              onClick={() => setCurrent(i)}
              className={`flex-1 h-14 rounded-lg overflow-hidden flex items-center justify-center transition-all duration-150 ${
                i === current ? 'ring-2 ring-primary' : 'opacity-60 hover:opacity-80'
              }`}
              style={{ backgroundColor: auction.imageColor }}
            >
              <span className="material-symbols-outlined text-white/40" style={{ fontSize: '20px' }}>
                {auction.imageAccent}
              </span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Bid Row ─────────────────────────────────────────────────────────────────
function BidRow({ bid, isTop, isNew }: { bid: Bid; isTop: boolean; isNew: boolean }) {
  return (
    <div
      className={`flex items-center gap-3 px-4 py-2.5 text-sm transition-all duration-500 border-b border-border last:border-0 ${
        isNew ? 'bg-success-light' : isTop ? 'bg-gray-50/80' : 'hover:bg-gray-50/60'
      }`}
    >
      {/* Left accent bar for top bid */}
      {isTop && (
        <div className="w-0.5 h-7 bg-primary rounded-full shrink-0 -ml-4 mr-0" />
      )}

      {/* Time */}
      <span className="tabular-nums text-text-muted font-mono text-xs w-14 shrink-0">
        {formatBidTime(bid.timestamp)}
      </span>

      {/* Username + YOU badge */}
      <span className="flex items-center gap-1.5 flex-1 text-xs font-medium text-text-secondary">
        {bid.maskedUsername}
        {bid.isCurrentUser && (
          <span className="pill bg-primary text-white" style={{ fontSize: '9px', padding: '1px 5px' }}>
            you
          </span>
        )}
      </span>

      {/* Amount */}
      <span className={`tabular-nums font-bold ${isTop ? 'text-primary' : 'text-text-primary'}`}>
        {formatCurrency(bid.amount)}
      </span>
    </div>
  );
}

// ─── Main Component ───────────────────────────────────────────────────────────
export function AuctionDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const auction: Auction = MOCK_AUCTIONS.find((a) => a.id === id) ?? MOCK_AUCTIONS[0];

  const minBid = auction.currentBid + auction.bidIncrement;
  const [bidAmount, setBidAmount] = useState(minBid);
  const [autoBid, setAutoBid] = useState(false);
  const [autoBidMax, setAutoBidMax] = useState('');
  const [bidHistory, setBidHistory] = useState<Bid[]>(MOCK_BID_HISTORY);
  const [newBidId, setNewBidId] = useState<string | null>(null);
  const [isWinning, setIsWinning] = useState(false);
  const [bidError, setBidError] = useState('');
  const [placing, setPlacing] = useState(false);
  const [showAllBids, setShowAllBids] = useState(false);
  const [watchlisted, setWatchlisted] = useState(false);
  const bidInputRef = useRef<HTMLInputElement>(null);

  // Format ends-at for display
  const endsLabel = (() => {
    const d = auction.endsAt;
    const todayEnd = new Date();
    todayEnd.setHours(23, 59, 59, 999);
    const isToday = d <= todayEnd;
    const hhmm = d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false });
    return isToday ? `ENDS TODAY, ${hhmm} UTC` : `ENDS ${d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }).toUpperCase()}, ${hhmm} UTC`;
  })();

  const handlePlaceBid = (e: React.FormEvent) => {
    e.preventDefault();

    const isLoggedIn = localStorage.getItem('vaultx_logged_in') === 'true';
    if (!isLoggedIn) {
      navigate('/login');
      return;
    }

    setBidError('');
    if (bidAmount < minBid) {
      setBidError(`Minimum bid is ${formatCurrency(minBid)}`);
      return;
    }
    if (MOCK_USER.kycStatus !== 'VERIFIED') {
      setBidError('KYC verification is required to place a bid. Please complete verification in your Wallet.');
      return;
    }
    if (bidAmount > MOCK_USER.balance) {
      setBidError('Insufficient available balance');
      return;
    }
    setPlacing(true);
    setTimeout(() => {
      setPlacing(false);
      setIsWinning(true);
      const newBid: Bid = {
        id: `b_new_${Date.now()}`,
        username: MOCK_USER.username,
        maskedUsername: `${MOCK_USER.username.charAt(0)}***${MOCK_USER.username.slice(-1)}`,
        isCurrentUser: true,
        amount: bidAmount,
        timestamp: new Date(),
      };

      // Mutate global data and persist
      MOCK_USER.balance -= bidAmount;
      MOCK_USER.reservedBalance += bidAmount;

      auction.currentBid = bidAmount;
      auction.totalBids += 1;
      if (auction.reservePrice && auction.currentBid >= auction.reservePrice) {
        auction.reserveMet = true;
      }

      MOCK_BID_HISTORY.unshift(newBid);

      const newTxn: Transaction = {
        id: `txn_bid_${Date.now()}`,
        date: new Date(),
        type: 'ESCROW_HOLD',
        amount: -bidAmount,
        status: 'COMPLETED',
        description: `Bid placed on ${auction.title}`,
      };
      MOCK_TRANSACTIONS.unshift(newTxn);

      saveState();

      setNewBidId(newBid.id);
      setBidHistory((prev) => [newBid, ...prev]);
      setBidAmount(bidAmount + auction.bidIncrement);
      setTimeout(() => setNewBidId(null), 3000);
    }, 900);
  };

  const visibleBids = showAllBids ? bidHistory : bidHistory.slice(0, 5);

  return (
    <div className="min-h-screen bg-bg-base">
      <TopNav />

      <main className="pt-16">
        <div className="max-w-[1280px] mx-auto px-6 py-6">

          {/* ── Breadcrumb ── */}
          <nav className="flex items-center gap-1.5 text-sm text-text-secondary mb-4">
            <button onClick={() => navigate('/explore')} className="hover:text-primary transition-colors">
              Auctions
            </button>
            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>chevron_right</span>
            <Link to="/explore" className="hover:text-primary transition-colors">{auction.category}</Link>
            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>chevron_right</span>
            {auction.subcategory && (
              <>
                <Link to="/explore" className="hover:text-primary transition-colors">{auction.subcategory}</Link>
                <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>chevron_right</span>
              </>
            )}
            <span className="text-primary font-medium">Lot #{auction.lotNumber}</span>
          </nav>

          {/* ── Title row ── */}
          <div className="flex items-start justify-between gap-4 mb-2">
            <div>
              <h1 className="text-[22px] font-bold text-text-primary leading-snug max-w-2xl">
                {auction.title}
              </h1>
              {auction.subtitle && (
                <p className="text-sm text-text-secondary mt-0.5">{auction.subtitle}</p>
              )}
            </div>
            <button
              id="watchlist-btn"
              onClick={() => setWatchlisted((w) => !w)}
              className={`shrink-0 flex items-center gap-1.5 px-3 py-2 rounded-lg border text-sm font-medium transition-all duration-150 ${
                watchlisted
                  ? 'border-primary bg-primary-light text-primary'
                  : 'border-border bg-white text-text-secondary hover:border-gray-300'
              }`}
            >
              <span className="material-symbols-outlined" style={{ fontSize: '16px', fontVariationSettings: watchlisted ? "'FILL' 1" : "'FILL' 0" }}>
                bookmark
              </span>
              {watchlisted ? 'Watching' : 'Watch'}
            </button>
          </div>

          {/* ── Stats row (views / watchers / escrow) ── */}
          <div className="flex items-center gap-4 text-xs text-text-muted mb-6">
            <span className="flex items-center gap-1">
              <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>visibility</span>
              {auction.views.toLocaleString()} Views
            </span>
            <span className="flex items-center gap-1">
              <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>bookmark</span>
              {auction.watchers} Watchers
            </span>
            <span className="flex items-center gap-1 text-success font-medium">
              <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>verified_user</span>
              Authenticated via escrow
            </span>
          </div>

          {/* ── Two-column layout ── */}
          <div className="flex flex-col lg:flex-row gap-8 items-start">

            {/* ════ LEFT COLUMN ════ */}
            <div className="flex-1 min-w-0 space-y-6">

              {/* Image carousel */}
              <ImageCarousel auction={auction} />

              {/* Stat chips */}
              <div className="flex flex-wrap gap-3">
                <div className="flex flex-col gap-0.5 px-4 py-3 card rounded-xl min-w-[130px]">
                  <span className="text-[10px] text-text-muted font-semibold uppercase tracking-wider">Starting Price</span>
                  <span className="text-base font-bold tabular-nums text-text-primary">
                    {formatCurrency(auction.startingPrice)}
                  </span>
                </div>
                <div className="flex flex-col gap-0.5 px-4 py-3 card rounded-xl min-w-[130px]">
                  <span className="text-[10px] text-text-muted font-semibold uppercase tracking-wider">Reserve</span>
                  {auction.reservePrice ? (
                    <span className={`text-base font-bold flex items-center gap-1.5 ${auction.reserveMet ? 'text-success' : 'text-danger'}`}>
                      <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>
                        {auction.reserveMet ? 'check_circle' : 'cancel'}
                      </span>
                      {auction.reserveMet ? 'Met' : 'Not met'}
                    </span>
                  ) : (
                    <span className="text-base font-bold text-text-muted">None</span>
                  )}
                </div>
                <div className="flex flex-col gap-0.5 px-4 py-3 card rounded-xl min-w-[130px]">
                  <span className="text-[10px] text-text-muted font-semibold uppercase tracking-wider">Bid Increment</span>
                  <span className="text-base font-bold tabular-nums text-text-primary">
                    {formatCurrency(auction.bidIncrement)}
                  </span>
                </div>
              </div>

              {/* ── Lot Description ── */}
              <div className="card p-6 rounded-xl space-y-4">
                <h2 className="text-base font-bold text-text-primary border-b border-border pb-3">
                  Lot Description
                </h2>

                {/* Rich text paragraphs */}
                <div className="space-y-3">
                  {auction.lotDescription.map((para, i) => (
                    <p key={i} className="text-sm text-text-secondary leading-relaxed">
                      {para}
                    </p>
                  ))}
                </div>

                {/* Spec bullets */}
                {auction.specs.length > 0 && (
                  <ul className="space-y-1.5 pt-2">
                    {auction.specs.map((spec) => (
                      <li key={spec.label} className="flex items-start gap-2 text-sm">
                        <span className="text-primary mt-0.5">•</span>
                        <span>
                          <strong className="text-text-primary font-semibold">{spec.label}:</strong>{' '}
                          <span className="text-text-secondary">{spec.value}</span>
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              {/* ── Seller card ── */}
              <div className="card p-5 rounded-xl flex items-center gap-4">
                {/* Avatar */}
                <div className="w-12 h-12 rounded-full bg-primary flex items-center justify-center text-white text-lg font-bold shrink-0">
                  {auction.sellerInfo.avatarInitial}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-text-primary">{auction.sellerInfo.displayName}</span>
                    {auction.sellerInfo.verified && (
                      <span className="material-symbols-outlined text-primary" style={{ fontSize: '16px', fontVariationSettings: "'FILL' 1" }}>
                        verified
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-3 mt-0.5">
                    <span className="text-xs text-text-muted">
                      Member since {auction.sellerInfo.memberSince} · {auction.sellerInfo.location}
                    </span>
                  </div>
                </div>

                {/* Rating */}
                <div className="text-right shrink-0">
                  <div className="flex items-center gap-1 justify-end">
                    <span className="text-warning text-sm">★</span>
                    <span className="text-sm font-bold text-text-primary">{auction.sellerInfo.rating}</span>
                  </div>
                  <div className="text-xs text-text-muted">
                    ({auction.sellerInfo.reviewCount} Reviews)
                  </div>
                </div>
              </div>

            </div>

            {/* ════ RIGHT COLUMN (sticky) ════ */}
            <div className="w-full lg:w-[380px] shrink-0 lg:sticky lg:top-20 space-y-3">

              {/* ── Winning badge + Countdown ── */}
              <div className="card p-5 rounded-xl space-y-4">
                {/* "You are winning" badge */}
                {isWinning && (
                  <div className="flex items-center justify-center gap-2 py-2 bg-success-light rounded-lg border border-success/20">
                    <span className="material-symbols-outlined text-success" style={{ fontSize: '16px', fontVariationSettings: "'FILL' 1" }}>
                      emoji_events
                    </span>
                    <span className="text-success text-sm font-bold uppercase tracking-wide">You are winning!</span>
                  </div>
                )}

                {/* Countdown clock */}
                <div className="text-center">
                  <CountdownClock endsAt={auction.endsAt} />
                  <p className="text-[10px] font-semibold uppercase tracking-widest text-text-muted mt-2">
                    {endsLabel}
                  </p>
                </div>

                <div className="section-divider" />

                {/* Current Bid row */}
                <div className="flex items-end justify-between">
                  <div>
                    <div className="text-xs text-text-muted font-medium">
                      Current Bid ({auction.totalBids} bids)
                    </div>
                    <div className="text-3xl font-bold tabular-nums text-text-primary leading-none mt-1">
                      {formatCurrency(auction.currentBid)}
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-xs text-text-muted font-medium">Next Min. Bid</div>
                    <div className="text-base font-bold tabular-nums text-text-secondary mt-1">
                      {formatCurrency(minBid)}
                    </div>
                  </div>
                </div>
              </div>

              {/* ── Bid form ── */}
              <div className="card p-5 rounded-xl space-y-3">
                {bidError && (
                  <div className="flex items-start gap-2 p-3 bg-danger-light text-danger text-sm rounded-lg border border-danger/20 animate-fadeIn">
                    <span className="material-symbols-outlined shrink-0" style={{ fontSize: '16px' }}>error</span>
                    {bidError}
                  </div>
                )}

                {/* Bid input */}
                <div>
                  <div className="relative">
                    <span className="absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary font-semibold">$</span>
                    <input
                      id="bid-amount-input"
                      ref={bidInputRef}
                      type="number"
                      min={minBid}
                      step={auction.bidIncrement}
                      value={bidAmount}
                      onChange={(e) => setBidAmount(Number(e.target.value))}
                      className="input-field pl-7 text-lg font-bold tabular-nums py-3"
                    />
                  </div>
                  <p className="text-xs text-text-muted mt-1.5">
                    Minimum increment: <strong className="text-text-secondary">{formatCurrency(auction.bidIncrement)}</strong>
                  </p>
                </div>

                {/* Auto-bid toggle */}
                <label className="flex items-center gap-2.5 cursor-pointer group">
                  <input
                    id="auto-bid-checkbox"
                    type="checkbox"
                    checked={autoBid}
                    onChange={(e) => setAutoBid(e.target.checked)}
                    className="w-4 h-4 accent-primary"
                  />
                  <span className="text-sm font-medium text-text-primary group-hover:text-primary transition-colors duration-150">
                    Enable Auto-Bid
                  </span>
                </label>

                {autoBid && (
                  <div className="animate-fadeIn">
                    <label className="input-label text-primary" htmlFor="auto-bid-max-input">
                      Maximum Bid Limit ($)
                    </label>
                    <div className="relative">
                      <span className="absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary font-semibold">$</span>
                      <input
                        id="auto-bid-max-input"
                        type="number"
                        min={bidAmount}
                        step={auction.bidIncrement}
                        value={autoBidMax}
                        onChange={(e) => setAutoBidMax(e.target.value)}
                        placeholder={String(bidAmount + auction.bidIncrement * 5)}
                        className="input-field pl-7 tabular-nums border-primary/40 focus:border-primary"
                      />
                    </div>
                  </div>
                )}

                {/* Place Bid button */}
                <form onSubmit={handlePlaceBid}>
                  <button
                    id="place-bid-btn"
                    type="submit"
                    disabled={placing || auction.status !== 'ACTIVE'}
                    className="btn-primary w-full py-3 text-base gap-2"
                  >
                    {placing ? (
                      <>
                        <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                        </svg>
                        Placing Bid…
                      </>
                    ) : (
                      <>
                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>gavel</span>
                        Place Bid for {formatCurrency(bidAmount)}
                      </>
                    )}
                  </button>
                </form>

                <p className="text-xs text-text-muted text-center">
                  By placing a bid, you commit to buy this item.
                </p>
              </div>

              {/* ── Live Bid History ── */}
              <div className="card rounded-xl overflow-hidden">
                <div className="flex items-center justify-between px-4 py-3 border-b border-border">
                  <h3 className="text-sm font-semibold text-text-primary">Live Bid History</h3>
                  <div className="flex items-center gap-1.5">
                    <div className="w-1.5 h-1.5 rounded-full bg-success animate-pulse" />
                    <span className="text-xs text-success font-medium">Live</span>
                  </div>
                </div>

                <div className="divide-y divide-border">
                  {visibleBids.map((bid, idx) => (
                    <BidRow
                      key={bid.id}
                      bid={bid}
                      isTop={idx === 0}
                      isNew={bid.id === newBidId}
                    />
                  ))}
                </div>

                {bidHistory.length > 5 && (
                  <div className="px-4 py-2.5 border-t border-border bg-gray-50">
                    <button
                      id="view-all-bids-btn"
                      onClick={() => setShowAllBids((s) => !s)}
                      className="text-xs text-primary font-medium hover:underline"
                    >
                      {showAllBids
                        ? 'Show less'
                        : `View all ${bidHistory.length} bids`}
                    </button>
                  </div>
                )}
              </div>

            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
