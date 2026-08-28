import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { TopNav } from '../components/TopNav';
import { CountdownClock } from '../components/CountdownTimer';
import {
  api,
  mapAuction,
  mapBid,
  Auction,
  AuctionMedia,
  Bid,
  PaymentStatus,
  formatCurrency,
  formatBidTime,
  ApiError,
} from '../api';
import { useAuth } from '../context/AuthContext';

// ─── Image Carousel ───────────────────────────────────────────────────────────
function ImageCarousel({ auction, media }: { auction: Auction; media: AuctionMedia[] }) {
  const items = media.length > 0 ? media : null;
  const [rawIndex, setRawIndex] = useState(0);
  const count = items ? items.length : 1;
  const current = Math.min(rawIndex, count - 1);

  const prev = () => setRawIndex((c) => (c - 1 + count) % count);
  const next = () => setRawIndex((c) => (c + 1) % count);

  return (
    <div className="relative rounded-xl overflow-hidden group select-none">
      {items ? (
        <div className="w-full h-[400px] bg-black">
          {items[current].mediaType === 'IMAGE' ? (
            <img
              src={items[current].url}
              alt={auction.title}
              className="w-full h-full object-contain"
            />
          ) : (
            <video
              src={items[current].url}
              controls
              playsInline
              className="w-full h-full object-contain"
            />
          )}
        </div>
      ) : (
        <div
          className="w-full h-[400px] flex flex-col items-center justify-center gap-4"
          style={{ backgroundColor: auction.imageColor }}
        >
          <span
            className="material-symbols-outlined text-white/40"
            style={{ fontSize: '96px' }}
          >
            {auction.imageAccent}
          </span>
          <div className="text-white/30 text-xs font-medium uppercase tracking-widest">
            No media added
          </div>
        </div>
      )}

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
          <div className="absolute bottom-3 left-1/2 -translate-x-1/2 text-white/80 text-xs font-medium bg-black/40 px-2.5 py-1 rounded-full">
            {current + 1} / {count}
          </div>
        </>
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
      {isTop && (
        <div className="w-0.5 h-7 bg-primary rounded-full shrink-0 -ml-4 mr-0" />
      )}

      <span className="tabular-nums text-text-muted font-mono text-xs w-14 shrink-0">
        {formatBidTime(bid.timestamp)}
      </span>

      <span className="flex items-center gap-1.5 flex-1 text-xs font-medium text-text-secondary">
        {bid.maskedUsername}
        {bid.isCurrentUser && (
          <span className="pill bg-primary text-white" style={{ fontSize: '9px', padding: '1px 5px' }}>
            you
          </span>
        )}
      </span>

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
  const { user, wallet, isAuthenticated } = useAuth();

  const [auction, setAuction] = useState<Auction | null>(null);
  const [bidHistory, setBidHistory] = useState<Bid[]>([]);
  const [media, setMedia] = useState<AuctionMedia[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [watching, setWatching] = useState(false);

  const [bidAmount, setBidAmount] = useState(0);
  const [autoBid, setAutoBid] = useState(false);
  const [autoBidMax, setAutoBidMax] = useState('');
  const [newBidId, setNewBidId] = useState<string | null>(null);
  const [isWinning, setIsWinning] = useState(false);
  const [bidError, setBidError] = useState('');
  const [placing, setPlacing] = useState(false);
  const [showAllBids, setShowAllBids] = useState(false);
  const [isWinner, setIsWinner] = useState(false);
  const [payError, setPayError] = useState('');
  const [paying, setPaying] = useState(false);
  const [payment, setPayment] = useState<PaymentStatus | null>(null);
  const bidInputRef = useRef<HTMLInputElement>(null);
  // Tracks whether the user has manually edited the bid amount so the 5s poll
  // does not overwrite what they typed with the current minimum.
  const bidEditedRef = useRef(false);
  // Mirror of the auction state used by the persistent-load callbacks so those
  // callbacks don't depend on `auction` (avoiding an infinite effect loop that
  // makes the loading screen jitter). Updated in sync with setAuction.
  const auctionRef = useRef<Auction | null>(null);

  const loadAuction = useCallback(async () => {
    if (!id) return;
    const data = await api.auctions.getById(id);
    const mapped = mapAuction(data);
    auctionRef.current = mapped;
    setAuction(mapped);
    if (!bidEditedRef.current) {
      setBidAmount((mapped.currentBid ?? mapped.startingPrice) + mapped.bidIncrement);
    }
  }, [id]);

  const loadBids = useCallback(async () => {
    if (!id) return;
    try {
      const data = await api.bids.list(id);
      const currentUserId = user?.id;
      setBidHistory(data.map((b) => mapBid(b, currentUserId)));
      const isLeading = data.some((b) => b.currentWinner && b.bidderId === currentUserId);
      setIsWinner(isLeading);
      setIsWinning(isLeading && auctionRef.current?.status === 'ACTIVE');
    } catch {
      // ignore transient polling errors
    }
  }, [id, user]);

  const loadMedia = useCallback(async () => {
    if (!id) return;
    try {
      setMedia(await api.auctions.listMedia(id));
    } catch {
      setMedia([]);
    }
  }, [id]);

  const loadWatchStatus = useCallback(async () => {
    if (!id || !isAuthenticated) return;
    try {
      const watchlist = await api.watchlist.list();
      setWatching(watchlist.some((w) => w.id === id));
    } catch {
      // ignore
    }
  }, [id, isAuthenticated]);

  const loadPayment = useCallback(async () => {
    if (!id || !auctionRef.current || auctionRef.current.status !== 'AWAITING_PAYMENT') {
      setPayment(null);
      return;
    }
    try {
      setPayment(await api.payments.getStatus(id));
    } catch {
      setPayment(null);
    }
  }, [id]);

  useEffect(() => {
    setLoading(true);
    setLoadError('');
    Promise.all([loadAuction(), loadBids(), loadWatchStatus(), loadMedia(), loadPayment()])
      .catch((err) => setLoadError(err instanceof Error ? err.message : 'Failed to load auction'))
      .finally(() => setLoading(false));
  }, [loadAuction, loadBids, loadWatchStatus, loadMedia, loadPayment]);

  // Poll bid history + auction state while active (do not poll on ended/pending).
  // Keyed on the primitive status so it (re)starts when the auction becomes ACTIVE
  // but avoids re-running on every poll-induced re-render.
  useEffect(() => {
    if (!auctionRef.current || auctionRef.current.status !== 'ACTIVE') return;
    const interval = setInterval(async () => {
      await Promise.all([loadBids(), loadAuction()]);
    }, 5000);
    return () => clearInterval(interval);
  }, [auction?.status, loadBids, loadAuction]);

  const toggleWatch = async () => {
    if (!id || !isAuthenticated) {
      navigate('/login');
      return;
    }
    try {
      if (watching) {
        await api.watchlist.remove(id);
        setWatching(false);
      } else {
        await api.watchlist.add(id);
        setWatching(true);
      }
    } catch (err) {
      setBidError(err instanceof Error ? err.message : 'Watchlist update failed');
    }
  };

  const handlePlaceBid = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !auction) return;

    if (!isAuthenticated) {
      navigate('/login');
      return;
    }

    if (auction.sellerId === user?.id) {
      setBidError('You cannot place a bid on your own auction.');
      return;
    }

    setBidError('');
    const currentBid = auction.currentBid ?? auction.startingPrice;
    const minBid = currentBid + auction.bidIncrement;
    if (bidAmount < minBid) {
      setBidError(`Minimum bid is ${formatCurrency(minBid)}`);
      return;
    }
    if (user?.kycStatus !== 'VERIFIED') {
      setBidError('KYC verification is required to place a bid. Please complete verification in your Wallet.');
      return;
    }
    if (wallet && bidAmount > wallet.availableBalance) {
      setBidError('Insufficient available balance');
      return;
    }

    setPlacing(true);
    try {
      const response = await api.bids.place(id, {
        amount: bidAmount,
        maxAutoBid: autoBid && autoBidMax ? Number(autoBidMax) : undefined,
        idempotencyKey: crypto.randomUUID(),
      });

      setIsWinning(response.currentWinner);
      setNewBidId(response.id);
      bidEditedRef.current = false;
      setBidAmount(bidAmount + auction.bidIncrement);
      await Promise.all([loadBids(), loadAuction()]);
      setTimeout(() => setNewBidId(null), 3000);
    } catch (err) {
      if (err instanceof ApiError) {
        setBidError(err.message);
      } else {
        setBidError(err instanceof Error ? err.message : 'Failed to place bid');
      }
    } finally {
      setPlacing(false);
    }
  };

  const handlePayNow = async () => {
    if (!id) return;
    setPayError('');
    setPaying(true);
    try {
      const session = await api.payments.createSession(id);
      window.location.href = session.url;
    } catch (err) {
      setPayError(err instanceof Error ? err.message : 'Failed to start payment');
    } finally {
      setPaying(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-bg-base">
        <TopNav />
        <main className="pt-16 flex items-center justify-center min-h-[60vh]">
          <span className="text-text-muted text-sm">Loading auction…</span>
        </main>
      </div>
    );
  }

  if (!auction) {
    return (
      <div className="min-h-screen bg-bg-base">
        <TopNav />
        <main className="pt-16 max-w-[1280px] mx-auto px-6 py-6">
          <div className="p-6 bg-danger-light border border-danger/20 rounded-xl text-danger text-sm">
            {loadError || 'Auction not found.'}
          </div>
          <Link to="/explore" className="text-primary text-sm font-medium hover:underline mt-4 inline-block">
            Back to Explore
          </Link>
        </main>
      </div>
    );
  }

  const minBid = (auction.currentBid ?? auction.startingPrice) + auction.bidIncrement;

  const endsLabel = (() => {
    const d = auction.endsAt;
    const todayEnd = new Date();
    todayEnd.setHours(23, 59, 59, 999);
    const isToday = d <= todayEnd;
    const hhmm = d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false });
    return isToday ? `ENDS TODAY, ${hhmm} UTC` : `ENDS ${d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }).toUpperCase()}, ${hhmm} UTC`;
  })();

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
              onClick={toggleWatch}
              className={`shrink-0 flex items-center gap-1.5 px-3 py-2 rounded-lg border text-sm font-medium transition-all duration-150 ${
                watching
                  ? 'border-primary bg-primary-light text-primary'
                  : 'border-border bg-white text-text-secondary hover:border-gray-300'
              }`}
            >
              <span className="material-symbols-outlined" style={{ fontSize: '16px', fontVariationSettings: watching ? "'FILL' 1" : "'FILL' 0" }}>
                bookmark
              </span>
              {watching ? 'Watching' : 'Watch'}
            </button>
          </div>

          {/* ── Stats row ── */}
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

              <ImageCarousel auction={auction} media={media} />

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
                <div className="space-y-3">
                  {auction.lotDescription.length > 0 ? (
                    auction.lotDescription.map((para, i) => (
                      <p key={i} className="text-sm text-text-secondary leading-relaxed">
                        {para}
                      </p>
                    ))
                  ) : (
                    <p className="text-sm text-text-muted">No description provided by the seller.</p>
                  )}
                </div>
              </div>

              {/* ── Seller card ── */}
              <div className="card p-5 rounded-xl flex items-center gap-4">
                <div className="w-12 h-12 rounded-full bg-primary flex items-center justify-center text-white text-lg font-bold shrink-0">
                  {auction.sellerInfo.avatarInitial}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-text-primary">{auction.sellerInfo.displayName}</span>
                  </div>
                  <div className="flex items-center gap-3 mt-0.5">
                    <span className="text-xs text-text-muted">Seller ID: {auction.seller}</span>
                  </div>
                </div>
              </div>

            </div>

            {/* ════ RIGHT COLUMN (sticky) ════ */}
            <div className="w-full lg:w-[380px] shrink-0 lg:sticky lg:top-20 space-y-3">

              {/* ── Winning badge + Countdown ── */}
              <div className="card p-5 rounded-xl space-y-4">
                {isWinning && (
                  <div className="flex items-center justify-center gap-2 py-2 bg-success-light rounded-lg border border-success/20">
                    <span className="material-symbols-outlined text-success" style={{ fontSize: '16px', fontVariationSettings: "'FILL' 1" }}>
                      emoji_events
                    </span>
                    <span className="text-success text-sm font-bold uppercase tracking-wide">You are winning!</span>
                  </div>
                )}

                <div className="text-center">
                  <CountdownClock endsAt={auction.endsAt} />
                  <p className="text-[10px] font-semibold uppercase tracking-widest text-text-muted mt-2">
                    {endsLabel}
                  </p>
                </div>

                <div className="section-divider" />

                <div className="flex items-end justify-between">
                  <div>
                    <div className="text-xs text-text-muted font-medium">
                      {auction.currentBid != null ? 'Current Bid' : 'Starting Price'} ({bidHistory.length} {bidHistory.length === 1 ? 'bid' : 'bids'})
                    </div>
                    <div className="text-3xl font-bold tabular-nums text-text-primary leading-none mt-1">
                      {formatCurrency(auction.currentBid ?? auction.startingPrice)}
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

              {/* ── Awaiting payment ── */}
              {auction.status === 'AWAITING_PAYMENT' && (
                <div className="card p-5 rounded-xl space-y-3">
                  <div className="flex items-start gap-2 p-3 bg-primary-light text-primary text-sm rounded-lg border border-primary/20">
                    <span className="material-symbols-outlined shrink-0" style={{ fontSize: '16px' }}>hourglass_top</span>
                    {isWinner
                      ? 'You won this auction! Complete the payment to finalise your purchase.'
                      : 'Awaiting payment from the winning bidder.'}
                  </div>
                  {isWinner && (
                    <>
                      {payError && <div className="text-xs text-danger">{payError}</div>}
                      <button onClick={handlePayNow} disabled={paying} className="btn-primary w-full py-3 text-base gap-2">
                        {paying ? 'Redirecting…' : <>Pay with Stripe</>}
                      </button>
                      <p className="text-xs text-text-muted text-center">Payment is simulated via Stripe in test mode.</p>
                    </>
                  )}
                </div>
              )}

              {/* ── Wallet shortfall (paid via Stripe but internal wallet not debited) ── */}
              {auction.status === 'SOLD' && payment?.walletDebited === false && (
                <div className="card p-5 rounded-xl space-y-2 border border-danger/30 bg-danger-light">
                  <div className="flex items-center gap-2 text-danger font-semibold text-sm">
                    <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>warning</span>
                    Payment received, but wallet shortfall
                  </div>
                  <p className="text-xs text-text-secondary">
                    The buyer paid via Stripe, but the internal wallet balance was insufficient to deduct.
                    {payment.shortfall ? ` (${payment.shortfall})` : ''} Awaiting manual reconciliation.
                  </p>
                </div>
              )}

              {/* ── Bid form ── */}
              {auction.sellerId === user?.id ? (
                <div className="card p-5 rounded-xl space-y-3">
                  <div className="flex items-start gap-2 p-3 bg-danger-light text-danger text-sm rounded-lg border border-danger/20">
                    <span className="material-symbols-outlined shrink-0" style={{ fontSize: '16px' }}>block</span>
                    You are the seller of this auction. You cannot place bids on your own listing.
                  </div>
                  <p className="text-xs text-text-muted text-center">
                    Track activity in your <Link to="/seller" className="text-primary font-semibold">Seller Portal</Link>.
                  </p>
                </div>
              ) : (
              <div className="card p-5 rounded-xl space-y-3">
                {bidError && (
                  <div className="flex items-start gap-2 p-3 bg-danger-light text-danger text-sm rounded-lg border border-danger/20 animate-fadeIn">
                    <span className="material-symbols-outlined shrink-0" style={{ fontSize: '16px' }}>error</span>
                    {bidError}
                  </div>
                )}

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
                      onChange={(e) => { bidEditedRef.current = true; setBidAmount(Number(e.target.value)); }}
                      className="input-field pl-7 text-lg font-bold tabular-nums py-3"
                    />
                  </div>
                  <p className="text-xs text-text-muted mt-1.5">
                    Minimum increment: <strong className="text-text-secondary">{formatCurrency(auction.bidIncrement)}</strong>
                  </p>
                </div>

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
              )}

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
                  {visibleBids.length > 0 ? (
                    visibleBids.map((bid, idx) => (
                      <BidRow
                        key={bid.id}
                        bid={bid}
                        isTop={idx === 0}
                        isNew={bid.id === newBidId}
                      />
                    ))
                  ) : (
                    <div className="px-4 py-6 text-center text-xs text-text-muted">
                      No bids yet. Be the first to bid!
                    </div>
                  )}
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