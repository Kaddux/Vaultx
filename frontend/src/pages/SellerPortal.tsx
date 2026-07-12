import { useState } from 'react';
import { TopNav } from '../components/TopNav';
import { CountdownTimer } from '../components/CountdownTimer';
import { MOCK_SELLER_AUCTIONS, Auction, formatCurrency, formatDate } from '../api';

function AuctionStatusPill({ status }: { status: Auction['status'] }) {
  if (status === 'ACTIVE') return <span className="pill-green">ACTIVE</span>;
  if (status === 'PENDING') return <span className="pill-amber">PENDING</span>;
  if (status === 'SOLD') return <span className="pill-indigo">SOLD</span>;
  return <span className="pill-red">UNSOLD</span>;
}

interface CreateAuctionForm {
  title: string;
  description: string;
  startingPrice: string;
  reservePrice: string;
  bidIncrement: string;
  startTime: string;
  endTime: string;
  extensionPeriod: string;
}

const DEFAULT_FORM: CreateAuctionForm = {
  title: '',
  description: '',
  startingPrice: '',
  reservePrice: '',
  bidIncrement: '1.00',
  startTime: '',
  endTime: '',
  extensionPeriod: '120',
};

export function SellerPortal() {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [form, setForm] = useState<CreateAuctionForm>(DEFAULT_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [successToast, setSuccessToast] = useState(false);

  const update = (field: keyof CreateAuctionForm) => (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setTimeout(() => {
      setSubmitting(false);
      setShowCreateModal(false);
      setSuccessToast(true);
      setForm(DEFAULT_FORM);
      setTimeout(() => setSuccessToast(false), 3500);
    }, 1000);
  };

  return (
    <div className="min-h-screen bg-bg-base">
      <TopNav />
      <main className="pt-16">
        <div className="max-w-[1280px] mx-auto px-6 py-8">

          {/* Success toast */}
          {successToast && (
            <div className="fixed top-5 right-5 z-50 flex items-center gap-2 bg-success text-white text-sm font-medium px-4 py-3 rounded-lg shadow-card-hover animate-fadeIn">
              <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>check_circle</span>
              Auction created successfully!
            </div>
          )}

          {/* Header */}
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-2xl font-bold text-text-primary">Seller Portal</h1>
              <p className="text-sm text-text-secondary mt-1">Manage your auction listings</p>
            </div>
            <button
              id="create-auction-btn"
              onClick={() => setShowCreateModal(true)}
              className="btn-primary"
            >
              <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>add</span>
              Create Auction
            </button>
          </div>

          {/* Stats bar */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
            {[
              { label: 'Total Listings', value: MOCK_SELLER_AUCTIONS.length, color: 'text-text-primary' },
              { label: 'Active', value: MOCK_SELLER_AUCTIONS.filter((a) => a.status === 'ACTIVE').length, color: 'text-success' },
              { label: 'Sold', value: MOCK_SELLER_AUCTIONS.filter((a) => a.status === 'SOLD').length, color: 'text-primary' },
              { label: 'Total Bids', value: MOCK_SELLER_AUCTIONS.reduce((s, a) => s + a.totalBids, 0), color: 'text-warning' },
            ].map((s) => (
              <div key={s.label} className="card p-4">
                <div className="text-xs text-text-muted font-medium">{s.label}</div>
                <div className={`text-2xl font-bold mt-1 ${s.color}`}>{s.value}</div>
              </div>
            ))}
          </div>

          {/* Auctions table */}
          <div className="card overflow-hidden">
            <div className="px-5 py-4 border-b border-border">
              <h2 className="text-base font-semibold text-text-primary">Your Auctions</h2>
            </div>
            <div className="overflow-x-auto">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Title</th>
                    <th className="text-center">Bids</th>
                    <th className="text-right">Current Bid</th>
                    <th>Reserve</th>
                    <th>Status</th>
                    <th>Payout</th>
                    <th>Ends</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {MOCK_SELLER_AUCTIONS.map((auction) => (
                    <tr key={auction.id}>
                      <td>
                        <div className="font-medium text-text-primary max-w-[220px] truncate">{auction.title}</div>
                        <div className="text-xs text-text-muted">{auction.category}</div>
                      </td>
                      <td className="text-center tabular-nums">{auction.totalBids}</td>
                      <td className="text-right font-bold tabular-nums">{formatCurrency(auction.currentBid)}</td>
                      <td>
                        {auction.reserveMet ? (
                          <span className="flex items-center gap-1 text-success text-xs font-medium">
                            <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>check_circle</span> Met
                          </span>
                        ) : (
                          <span className="flex items-center gap-1 text-danger text-xs font-medium">
                            <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>cancel</span> Not met
                          </span>
                        )}
                      </td>
                      <td><AuctionStatusPill status={auction.status} /></td>
                      <td>
                        {auction.payout === 'RELEASED' ? (
                          <span className="pill-green">RELEASED</span>
                        ) : auction.status === 'SOLD' ? (
                          <span className="pill-amber">PENDING</span>
                        ) : (
                          <span className="text-text-muted text-xs">—</span>
                        )}
                      </td>
                      <td className="whitespace-nowrap">
                        {auction.status === 'ACTIVE' ? (
                          <CountdownTimer endsAt={auction.endsAt} />
                        ) : (
                          <span className="text-xs text-text-muted">{formatDate(auction.endsAt)}</span>
                        )}
                      </td>
                      <td>
                        <button className="btn-secondary text-xs py-1 px-2.5">
                          <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>open_in_new</span>
                          View
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </main>

      {/* Create Auction Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/40 flex items-start justify-center z-50 overlay-fade overflow-y-auto py-10 px-4">
          <div className="card w-full max-w-lg modal-slide">
            {/* Header */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-border">
              <h2 className="text-base font-bold text-text-primary">Create New Auction</h2>
              <button
                onClick={() => setShowCreateModal(false)}
                className="text-text-muted hover:text-text-primary transition-colors duration-150"
              >
                <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>close</span>
              </button>
            </div>

            <form onSubmit={handleCreate} className="p-6 space-y-5">
              {/* Title */}
              <div>
                <label className="input-label" htmlFor="auction-title">Title</label>
                <input id="auction-title" type="text" value={form.title} onChange={update('title')} required placeholder="e.g. 1962 Fender Stratocaster" className="input-field" />
              </div>

              {/* Description */}
              <div>
                <label className="input-label" htmlFor="auction-desc">Description</label>
                <textarea
                  id="auction-desc"
                  value={form.description}
                  onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                  rows={3}
                  placeholder="Describe the item, condition, provenance…"
                  className="input-field resize-none"
                />
              </div>

              {/* Pricing */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="input-label" htmlFor="starting-price">Starting Price ($)</label>
                  <div className="relative">
                    <span className="absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary text-sm">$</span>
                    <input id="starting-price" type="number" min="1" step="0.01" value={form.startingPrice} onChange={update('startingPrice')} required placeholder="0.00" className="input-field pl-7 tabular-nums" />
                  </div>
                </div>
                <div>
                  <label className="input-label" htmlFor="reserve-price">
                    Reserve Price ($)
                    <span className="text-text-muted font-normal ml-1">(optional)</span>
                  </label>
                  <div className="relative">
                    <span className="absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary text-sm">$</span>
                    <input id="reserve-price" type="number" min="0" step="0.01" value={form.reservePrice} onChange={update('reservePrice')} placeholder="Threshold to sell" className="input-field pl-7 tabular-nums" />
                  </div>
                  <p className="text-xs text-text-muted mt-1">Minimum price threshold to sell</p>
                </div>
              </div>

              {/* Bid increment */}
              <div>
                <label className="input-label" htmlFor="bid-increment">Bid Increment ($)</label>
                <div className="relative">
                  <span className="absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary text-sm">$</span>
                  <input id="bid-increment" type="number" min="0.01" step="0.01" value={form.bidIncrement} onChange={update('bidIncrement')} className="input-field pl-7 tabular-nums" />
                </div>
                <p className="text-xs text-text-muted mt-1">Each new bid must exceed the previous by this amount</p>
              </div>

              {/* Times */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="input-label" htmlFor="start-time">Start Time</label>
                  <input id="start-time" type="datetime-local" value={form.startTime} onChange={update('startTime')} required className="input-field" />
                </div>
                <div>
                  <label className="input-label" htmlFor="end-time">End Time</label>
                  <input id="end-time" type="datetime-local" value={form.endTime} onChange={update('endTime')} required className="input-field" />
                </div>
              </div>

              {/* Extension period */}
              <div>
                <label className="input-label" htmlFor="extension-period">
                  Extension Period (seconds)
                  <span className="ml-1 text-text-muted font-normal">— soft-close window</span>
                </label>
                <input id="extension-period" type="number" min="0" step="1" value={form.extensionPeriod} onChange={update('extensionPeriod')} className="input-field tabular-nums" />
                <p className="text-xs text-text-muted mt-1">If a bid is placed in the final {form.extensionPeriod}s, the auction extends by this duration</p>
              </div>

              {/* Actions */}
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowCreateModal(false)} className="btn-secondary flex-1">Cancel</button>
                <button id="submit-auction-btn" type="submit" disabled={submitting} className="btn-primary flex-1">
                  {submitting ? (
                    <span className="flex items-center gap-2">
                      <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                      </svg>
                      Creating…
                    </span>
                  ) : (
                    <>
                      <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>gavel</span>
                      Create Auction
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
