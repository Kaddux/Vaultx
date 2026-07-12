import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { TopNav } from '../components/TopNav';
import { CountdownClock } from '../components/CountdownTimer';
import { MOCK_AUCTIONS, MOCK_BID_HISTORY, Bid, formatCurrency, formatTime, MOCK_USER } from '../api';

export function AuctionDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const auction = MOCK_AUCTIONS.find((a) => a.id === id) ?? MOCK_AUCTIONS[0];

  const [bidAmount, setBidAmount] = useState(auction.currentBid + auction.bidIncrement);
  const [autoBid, setAutoBid] = useState(false);
  const [autoBidMax, setAutoBidMax] = useState('');
  const [bidHistory, setBidHistory] = useState<Bid[]>(MOCK_BID_HISTORY);
  const [justBid, setJustBid] = useState(false);
  const [bidError, setBidError] = useState('');
  const [placing, setPlacing] = useState(false);
  const [isWinning, setIsWinning] = useState(false);

  const minBid = auction.currentBid + auction.bidIncrement;

  const handlePlaceBid = (e: React.FormEvent) => {
    e.preventDefault();
    if (bidAmount < minBid) {
      setBidError(`Minimum bid is ${formatCurrency(minBid)}`);
      return;
    }
    if (bidAmount > MOCK_USER.balance) {
      setBidError('Insufficient available balance');
      return;
    }
    setBidError('');
    setPlacing(true);
    setTimeout(() => {
      setPlacing(false);
      setIsWinning(true);
      setJustBid(true);
      const newBid: Bid = {
        id: `b_new_${Date.now()}`,
        username: MOCK_USER.username,
        maskedUsername: `${MOCK_USER.username.charAt(0)}***${MOCK_USER.username.slice(-1)}`,
        amount: bidAmount,
        timestamp: new Date(),
      };
      setBidHistory((prev) => [newBid, ...prev]);
      setBidAmount(bidAmount + auction.bidIncrement);
      setTimeout(() => setJustBid(false), 3000);
    }, 1000);
  };

  return (
    <div className="min-h-screen bg-bg-base">
      <TopNav />
      <main className="pt-16">
        <div className="max-w-[1280px] mx-auto px-6 py-8">

          {/* Breadcrumb */}
          <div className="flex items-center gap-2 text-sm text-text-secondary mb-6">
            <button onClick={() => navigate('/explore')} className="hover:text-primary transition-colors duration-150">
              Explore
            </button>
            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>chevron_right</span>
            <span className="text-text-primary font-medium truncate">{auction.title}</span>
          </div>

          {/* Two-column layout */}
          <div className="flex flex-col lg:flex-row gap-8 items-start">

            {/* LEFT COLUMN — 60% */}
            <div className="flex-1 min-w-0 space-y-6">
              {/* Image */}
              <div
                className="w-full h-80 rounded-xl flex flex-col items-center justify-center gap-3"
                style={{ backgroundColor: auction.imageColor }}
              >
                <span className="material-symbols-outlined text-white/50" style={{ fontSize: '72px' }}>image</span>
                <span className="text-white/40 text-sm font-medium uppercase tracking-widest">{auction.category}</span>
              </div>

              {/* Title & seller */}
              <div>
                <h1 className="text-2xl font-bold text-text-primary leading-snug">{auction.title}</h1>
                <div className="flex items-center gap-3 mt-2">
                  <span className="text-sm text-text-secondary">@{auction.seller}</span>
                  <span className="pill-gray text-xs">{auction.category}</span>
                </div>
              </div>

              {/* Description */}
              <p className="text-sm text-text-secondary leading-relaxed">{auction.description}</p>

              {/* Stat chips */}
              <div className="flex flex-wrap gap-3">
                {[
                  { label: 'Starting Price', value: formatCurrency(auction.startingPrice) },
                  { label: 'Reserve Price', value: auction.reservePrice ? formatCurrency(auction.reservePrice) : 'None' },
                  { label: 'Bid Increment', value: formatCurrency(auction.bidIncrement) },
                ].map((stat) => (
                  <div key={stat.label} className="flex flex-col gap-0.5 px-4 py-3 card rounded-lg min-w-[120px]">
                    <span className="text-xs text-text-muted font-medium">{stat.label}</span>
                    <span className="text-sm font-bold tabular-nums text-text-primary">{stat.value}</span>
                  </div>
                ))}
                <div className="flex flex-col gap-0.5 px-4 py-3 card rounded-lg">
                  <span className="text-xs text-text-muted font-medium">Reserve</span>
                  <span className={`text-sm font-bold ${auction.reserveMet ? 'text-success' : 'text-danger'}`}>
                    {auction.reserveMet ? '✓ Met' : '✗ Not met'}
                  </span>
                </div>
              </div>

              {/* Seller mini-card */}
              <div className="card p-4 flex items-center gap-4">
                <div className="w-10 h-10 rounded-full bg-primary flex items-center justify-center text-white font-bold text-sm shrink-0">
                  {auction.seller.charAt(0).toUpperCase()}
                </div>
                <div className="flex-1">
                  <div className="text-sm font-semibold text-text-primary">@{auction.seller}</div>
                  <div className="flex items-center gap-1 text-xs text-warning mt-0.5">
                    ⭐⭐⭐⭐⭐ <span className="text-text-secondary font-medium">4.85 · 218 reviews</span>
                  </div>
                </div>
                <span className="pill-green text-xs">Trusted Seller</span>
              </div>
            </div>

            {/* RIGHT COLUMN — sticky 40% */}
            <div className="w-full lg:w-[420px] shrink-0 lg:sticky lg:top-20 space-y-4">

              {/* Countdown card */}
              <div className="card p-5 text-center">
                <div className="text-xs font-semibold text-text-muted uppercase tracking-wider mb-3">Auction ends in</div>
                <CountdownClock endsAt={auction.endsAt} />
              </div>

              {/* Status & bid form */}
              <div className="card p-5 space-y-4">
                {/* Status badge */}
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-xs text-text-muted font-medium">Current Highest Bid</div>
                    <div className="text-3xl font-bold tabular-nums text-text-primary mt-0.5">
                      {formatCurrency(auction.currentBid)}
                    </div>
                    <div className="text-xs text-text-secondary mt-1">{auction.totalBids} bids placed</div>
                  </div>
                  <div>
                    {isWinning ? (
                      <span className="pill-green text-sm font-bold">
                        <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>emoji_events</span>
                        You are winning!
                      </span>
                    ) : (
                      <span className="pill-gray">Not winning</span>
                    )}
                  </div>
                </div>

                <div className="section-divider" />

                {/* Bid form */}
                <form onSubmit={handlePlaceBid} className="space-y-3">
                  {bidError && (
                    <div className="p-3 bg-danger-light text-danger text-sm rounded-lg border border-danger/20 flex items-center gap-2">
                      <span className="material-symbols-outlined shrink-0" style={{ fontSize: '16px' }}>error</span>
                      {bidError}
                    </div>
                  )}

                  <div>
                    <label className="input-label" htmlFor="bid-amount">Your Bid (USD)</label>
                    <div className="relative">
                      <span className="absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary font-medium text-sm">$</span>
                      <input
                        id="bid-amount"
                        type="number"
                        min={minBid}
                        step={auction.bidIncrement}
                        value={bidAmount}
                        onChange={(e) => setBidAmount(Number(e.target.value))}
                        className="input-field pl-7 tabular-nums"
                      />
                    </div>
                    <p className="text-xs text-text-muted mt-1.5">
                      Minimum next bid: <strong className="tabular-nums text-text-secondary">{formatCurrency(minBid)}</strong>
                      {' '}· increment {formatCurrency(auction.bidIncrement)}
                    </p>
                  </div>

                  {/* Auto-bid toggle */}
                  <div className="rounded-lg border border-border overflow-hidden">
                    <label className="flex items-center gap-3 px-4 py-3 cursor-pointer hover:bg-gray-50 transition-colors duration-100">
                      <input
                        id="auto-bid-toggle"
                        type="checkbox"
                        checked={autoBid}
                        onChange={(e) => setAutoBid(e.target.checked)}
                        className="w-4 h-4 accent-primary"
                      />
                      <div>
                        <div className="text-sm font-medium text-text-primary">Enable Auto-Bid</div>
                        <div className="text-xs text-text-muted">We'll bid for you up to your maximum</div>
                      </div>
                    </label>
                    {autoBid && (
                      <div className="px-4 pb-3 border-t border-border bg-primary-light/40">
                        <label className="input-label mt-3 text-primary" htmlFor="auto-bid-max">Maximum Bid Limit (USD)</label>
                        <div className="relative">
                          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary font-medium text-sm">$</span>
                          <input
                            id="auto-bid-max"
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
                  </div>

                  <button
                    id="place-bid-btn"
                    type="submit"
                    disabled={placing || auction.status !== 'ACTIVE'}
                    className="btn-primary w-full py-3 text-base"
                  >
                    {placing ? (
                      <span className="flex items-center gap-2">
                        <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                        </svg>
                        Placing Bid…
                      </span>
                    ) : (
                      <>
                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>gavel</span>
                        Place Bid — {formatCurrency(bidAmount)}
                      </>
                    )}
                  </button>
                </form>
              </div>

              {/* Live Bid History */}
              <div className="card overflow-hidden">
                <div className="px-4 py-3 border-b border-border flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-text-primary">Live Bid History</h3>
                  <div className="flex items-center gap-1.5">
                    <div className="w-2 h-2 rounded-full bg-success animate-pulse" />
                    <span className="text-xs text-text-muted">Live</span>
                  </div>
                </div>
                <div className="divide-y divide-border max-h-64 overflow-y-auto scrollbar-thin">
                  {bidHistory.map((bid, idx) => (
                    <div
                      key={bid.id}
                      className={`flex items-center justify-between px-4 py-2.5 transition-colors duration-300 ${
                        idx === 0 && justBid ? 'bg-success-light' : idx === 0 ? 'bg-gray-50' : ''
                      }`}
                    >
                      <div className="flex items-center gap-2.5">
                        {idx === 0 && (
                          <div className="w-5 h-5 rounded-full bg-primary flex items-center justify-center shrink-0">
                            <span className="material-symbols-outlined text-white" style={{ fontSize: '12px' }}>arrow_upward</span>
                          </div>
                        )}
                        <div>
                          <div className="text-xs font-semibold text-text-primary">{bid.maskedUsername}</div>
                          <div className="text-xs text-text-muted tabular-nums">{formatTime(bid.timestamp)}</div>
                        </div>
                      </div>
                      <div className={`text-sm font-bold tabular-nums ${idx === 0 ? 'text-success' : 'text-text-primary'}`}>
                        {formatCurrency(bid.amount)}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
