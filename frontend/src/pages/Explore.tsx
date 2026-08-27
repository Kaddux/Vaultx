import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { TopNav } from '../components/TopNav';
import { CountdownTimer } from '../components/CountdownTimer';
import { api, mapAuction, Auction, formatCurrency } from '../api';

type SortKey = 'price_asc' | 'price_desc' | 'ending_soon';
type StatusFilter = 'ALL' | 'ACTIVE' | 'PENDING' | 'AWAITING_PAYMENT' | 'SOLD' | 'UNSOLD';

// Category icon color placeholder
function AuctionImagePlaceholder({ color, category }: { color: string; category: string }) {
  return (
    <div className="w-full h-44 rounded-t-lg flex flex-col items-center justify-center gap-2" style={{ backgroundColor: color }}>
      <span className="material-symbols-outlined text-white/70" style={{ fontSize: '40px' }}>
        {getCategoryIcon(category)}
      </span>
      <span className="text-white/60 text-xs font-medium uppercase tracking-widest">{category}</span>
    </div>
  );
}

function getCategoryIcon(cat: string): string {
  const map: Record<string, string> = {
    Sports: 'sports_soccer',
    Watches: 'watch',
    Music: 'music_note',
    Art: 'palette',
    Technology: 'computer',
    Fashion: 'checkroom',
    Collectibles: 'star',
  };
  return map[cat] || 'sell';
}

function AuctionCard({ auction }: { auction: Auction }) {
  const navigate = useNavigate();

  return (
    <div className="card-hover flex flex-col overflow-hidden group cursor-pointer" onClick={() => navigate(`/auction/${auction.id}`)}>
      {auction.coverImageUrl ? (
        <div className="w-full h-44 bg-black overflow-hidden rounded-t-lg">
          <img src={auction.coverImageUrl} alt={auction.title} className="w-full h-full object-cover" />
        </div>
      ) : (
        <AuctionImagePlaceholder color={auction.imageColor} category={auction.category} />
      )}

      <div className="p-4 flex flex-col flex-1">
        {/* Title */}
        <h3 className="text-sm font-semibold text-text-primary leading-snug line-clamp-2 group-hover:text-primary transition-colors duration-150">
          {auction.title}
        </h3>
        <p className="text-xs text-text-muted mt-0.5">@{auction.seller}</p>

        {/* Current bid */}
        <div className="mt-3">
          <div className="text-xs text-text-secondary font-medium">Current Bid</div>
          <div className="text-xl font-bold tabular-nums text-text-primary mt-0.5">
            {formatCurrency(auction.currentBid)}
          </div>
        </div>

        {/* Stats row */}
        <div className="flex items-center gap-3 mt-2">
          <div className="text-xs text-text-muted flex items-center gap-1">
            <span className="material-symbols-outlined" style={{ fontSize: '13px' }}>gavel</span>
            {auction.totalBids} bids
          </div>
          {auction.status !== 'SOLD' && auction.status !== 'UNSOLD' && (
            <CountdownTimer endsAt={auction.endsAt} />
          )}
          {auction.status === 'SOLD' && <span className="pill-green">SOLD</span>}
          {auction.status === 'UNSOLD' && <span className="pill-gray">UNSOLD</span>}
          {auction.status === 'PENDING' && <span className="pill-amber">PENDING</span>}
          {auction.status === 'AWAITING_PAYMENT' && <span className="pill-amber">AWAITING PAYMENT</span>}
        </div>

        {/* Reserve met */}
        {auction.reservePrice && (
          <div className="mt-2">
            {auction.reserveMet ? (
              <span className="text-xs text-success font-medium flex items-center gap-1">
                <span className="material-symbols-outlined" style={{ fontSize: '13px' }}>check_circle</span>
                Reserve met
              </span>
            ) : (
              <span className="text-xs text-text-muted flex items-center gap-1">
                <span className="material-symbols-outlined" style={{ fontSize: '13px' }}>cancel</span>
                Reserve not met
              </span>
            )}
          </div>
        )}

        {/* Bid Now button */}
        <div className="mt-4 pt-4 border-t border-border">
          <button
            id={`bid-now-${auction.id}`}
            onClick={(e) => { e.stopPropagation(); navigate(`/auction/${auction.id}`); }}
            disabled={auction.status !== 'ACTIVE'}
            className="btn-primary w-full"
          >
            {auction.status === 'ACTIVE' ? (
              <>
                <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>gavel</span>
                Bid Now
              </>
            ) : (
              'View Details'
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

export function Explore() {
  const [auctions, setAuctions] = useState<Auction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [sort, setSort] = useState<SortKey>('ending_soon');

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    api.auctions
      .list()
      .then((data) => {
        if (!cancelled) setAuctions(data.map(mapAuction));
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load auctions');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const filteredAuctions = useMemo(() => {
    let list = [...auctions];

    // Search
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(
        (a) => a.title.toLowerCase().includes(q) || a.seller.toLowerCase().includes(q) || a.category.toLowerCase().includes(q)
      );
    }

    // Status filter
    if (statusFilter !== 'ALL') {
      list = list.filter((a) => a.status === statusFilter);
    }

    // Sort
    if (sort === 'price_asc') list.sort((a, b) => a.currentBid - b.currentBid);
    else if (sort === 'price_desc') list.sort((a, b) => b.currentBid - a.currentBid);
    else if (sort === 'ending_soon') list.sort((a, b) => a.endsAt.getTime() - b.endsAt.getTime());

    return list;
  }, [auctions, search, statusFilter, sort]);

  const sortButtons: { key: SortKey; label: string }[] = [
    { key: 'price_asc', label: 'Price: Low → High' },
    { key: 'price_desc', label: 'Price: High → Low' },
    { key: 'ending_soon', label: 'Ending Soonest' },
  ];

  return (
    <div className="min-h-screen bg-bg-base">
      <TopNav />
      <main className="pt-16">
        <div className="max-w-[1280px] mx-auto px-6 py-8">

          {/* Page header */}
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-text-primary">Explore Auctions</h1>
            <p className="text-sm text-text-secondary mt-1">Browse live and upcoming auctions</p>
          </div>

          {/* Filter bar */}
          <div className="flex flex-wrap items-center gap-3 mb-6 p-4 card">
            {/* Search */}
            <div className="relative flex-1 min-w-[200px]">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-text-muted pointer-events-none" style={{ fontSize: '18px' }}>
                search
              </span>
              <input
                id="explore-search"
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search auctions, sellers, categories…"
                className="input-field pl-9 py-2"
              />
            </div>

            {/* Status dropdown */}
            <div className="relative">
              <select
                id="status-filter"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
                className="input-field py-2 pr-8 appearance-none cursor-pointer min-w-[140px]"
              >
                <option value="ALL">All Status</option>
                <option value="ACTIVE">Active</option>
                <option value="PENDING">Pending</option>
                <option value="AWAITING_PAYMENT">Awaiting Payment</option>
                <option value="SOLD">Sold</option>
                <option value="UNSOLD">Unsold</option>
              </select>
              <span className="material-symbols-outlined absolute right-2 top-1/2 -translate-y-1/2 text-text-muted pointer-events-none" style={{ fontSize: '16px' }}>
                expand_more
              </span>
            </div>

            {/* Sort buttons */}
            <div className="flex items-center gap-1 bg-gray-100 rounded-lg p-1">
              {sortButtons.map((btn) => (
                <button
                  key={btn.key}
                  id={`sort-${btn.key}`}
                  onClick={() => setSort(btn.key)}
                  className={`px-3 py-1.5 text-xs font-medium rounded-md transition-all duration-150 ${
                    sort === btn.key
                      ? 'bg-white text-text-primary shadow-card'
                      : 'text-text-secondary hover:text-text-primary'
                  }`}
                >
                  {btn.label}
                </button>
              ))}
            </div>
          </div>

          {/* Results count */}
          <div className="text-sm text-text-secondary mb-4">
            {loading ? 'Loading…' : `${filteredAuctions.length} ${filteredAuctions.length === 1 ? 'auction' : 'auctions'} found`}
          </div>

          {/* Error */}
          {error && (
            <div className="mb-6 p-4 bg-danger-light border border-danger/20 rounded-lg text-sm text-danger">
              {error}
            </div>
          )}

          {/* Grid */}
          {!loading && filteredAuctions.length === 0 ? (
            <div className="text-center py-16 text-text-muted">
              <span className="material-symbols-outlined mb-3 block" style={{ fontSize: '48px' }}>search_off</span>
              <p className="font-medium">No auctions match your filters</p>
              <p className="text-sm mt-1">Try adjusting your search or filter</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
              {filteredAuctions.map((auction) => (
                <AuctionCard key={auction.id} auction={auction} />
              ))}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}