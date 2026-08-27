import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { TopNav } from '../components/TopNav';
import {
  api,
  mapAuction,
  Auction,
  PaymentStatus,
  formatCurrency,
  ApiError,
} from '../api';

type CheckoutView = 'buyer' | 'seller';

export function Checkout() {
  const navigate = useNavigate();
  const [view, setView] = useState<CheckoutView>('buyer');
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState('');
  const [released, setReleased] = useState(false);
  const [refunded, setRefunded] = useState(false);

  const [soldAuction, setSoldAuction] = useState<Auction | null>(null);
  const [payment, setPayment] = useState<PaymentStatus | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    api.auctions
      .list({ status: 'SOLD' })
      .then(async (data) => {
        if (cancelled || data.length === 0) return;
        const auction = mapAuction(data[0]);
        setSoldAuction(auction);
        try {
          const status = await api.payments.getStatus(auction.id);
          if (!cancelled) setPayment(status);
        } catch {
          // no escrow record yet — show as pending
        }
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load checkout');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleConfirm = async () => {
    if (!soldAuction) return;
    setError('');
    setConfirming(true);
    try {
      if (view === 'buyer') {
        await api.payments.release(soldAuction.id);
        setReleased(true);
      } else {
        await api.payments.refund(soldAuction.id);
        setRefunded(true);
      }
      const status = await api.payments.getStatus(soldAuction.id);
      setPayment(status);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : err instanceof Error ? err.message : 'Payment action failed');
    } finally {
      setConfirming(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-bg-base">
        <TopNav />
        <main className="pt-16 flex items-center justify-center min-h-[60vh]">
          <span className="text-text-muted text-sm">Loading checkout…</span>
        </main>
      </div>
    );
  }

  if (!soldAuction) {
    return (
      <div className="min-h-screen bg-bg-base">
        <TopNav />
        <main className="pt-16 max-w-[1280px] mx-auto px-6 py-8">
          <div className="card p-8 text-center">
            <span className="material-symbols-outlined text-text-muted mb-3 block" style={{ fontSize: '48px' }}>inbox</span>
            <h2 className="text-lg font-bold text-text-primary">No completed auctions yet</h2>
            <p className="text-sm text-text-secondary mt-1">Won auctions will appear here for settlement.</p>
            <button onClick={() => navigate('/explore')} className="btn-primary mt-6">Browse Auctions</button>
          </div>
        </main>
      </div>
    );
  }

  const escrowStatus = payment?.status ?? 'HELD';
  const amount = payment?.amount ?? soldAuction.currentBid;

  return (
    <div className="min-h-screen bg-bg-base">
      <TopNav />
      <main className="pt-16">
        <div className="max-w-[1280px] mx-auto px-6 py-8">

          {/* Page header */}
          <div className="flex items-center justify-between mb-8">
            <div>
              <h1 className="text-2xl font-bold text-text-primary">Checkout & Settlement</h1>
              <p className="text-sm text-text-secondary mt-1">Escrow confirmation and payout release</p>
            </div>
            <div className="flex items-center gap-1 bg-gray-100 rounded-lg p-1">
              <button
                onClick={() => setView('buyer')}
                className={`px-4 py-2 text-sm font-medium rounded-md transition-all duration-150 ${
                  view === 'buyer' ? 'bg-white shadow-card text-text-primary' : 'text-text-secondary'
                }`}
              >
                Buyer View
              </button>
              <button
                onClick={() => setView('seller')}
                className={`px-4 py-2 text-sm font-medium rounded-md transition-all duration-150 ${
                  view === 'seller' ? 'bg-white shadow-card text-text-primary' : 'text-text-secondary'
                }`}
              >
                Seller View
              </button>
            </div>
          </div>

          {error && (
            <div className="mb-6 p-4 bg-danger-light border border-danger/20 rounded-lg text-sm text-danger">
              {error}
            </div>
          )}

          <div className="relative flex items-center justify-center min-h-[420px]">
            <div className="absolute inset-0 rounded-xl bg-gradient-to-br from-gray-100 to-gray-200 flex items-center justify-center overflow-hidden">
              <div className="w-full h-full opacity-30 p-8 grid grid-cols-3 gap-4">
                {[...Array(6)].map((_, i) => (
                  <div key={i} className="bg-white rounded-lg h-20" />
                ))}
              </div>
              <div className="absolute inset-0 backdrop-blur-sm bg-black/20 rounded-xl" />
            </div>

            <div className="relative z-10 w-full max-w-md modal-slide">
              {released ? (
                <div className="card p-8 shadow-modal text-center">
                  <div className="w-16 h-16 rounded-full bg-success-light flex items-center justify-center mx-auto mb-5">
                    <span className="material-symbols-outlined text-success" style={{ fontSize: '36px' }}>check_circle</span>
                  </div>
                  <h2 className="text-xl font-bold text-text-primary mb-1">Payout Released</h2>
                  <p className="text-sm text-text-secondary mb-6">Funds have been deposited to the seller's wallet</p>
                  <div className="bg-success-light rounded-xl py-5 mb-6">
                    <div className="text-xs text-success font-semibold uppercase tracking-wider mb-1">Amount Released</div>
                    <div className="text-4xl font-bold tabular-nums text-success">{formatCurrency(amount)}</div>
                  </div>
                  <button onClick={() => navigate('/wallet')} className="btn-secondary w-full">View in Wallet</button>
                </div>
              ) : refunded ? (
                <div className="card p-8 shadow-modal text-center">
                  <div className="w-16 h-16 rounded-full bg-warning-light flex items-center justify-center mx-auto mb-5">
                    <span className="material-symbols-outlined text-warning" style={{ fontSize: '36px' }}>currency_exchange</span>
                  </div>
                  <h2 className="text-xl font-bold text-text-primary mb-1">Escrow Refunded</h2>
                  <p className="text-sm text-text-secondary mb-6">Funds returned to the buyer's wallet</p>
                  <button onClick={() => navigate('/wallet')} className="btn-secondary w-full">View in Wallet</button>
                </div>
              ) : view === 'buyer' ? (
                <div className="card p-8 shadow-modal">
                  <div className="flex items-center gap-3 mb-6">
                    <div className="w-10 h-10 rounded-full bg-success-light flex items-center justify-center">
                      <span className="material-symbols-outlined text-success" style={{ fontSize: '24px' }}>emoji_events</span>
                    </div>
                    <div>
                      <h2 className="text-lg font-bold text-text-primary">Congratulations!</h2>
                      <p className="text-sm text-text-secondary">You won this auction</p>
                    </div>
                  </div>

                  <div
                    className="w-full h-32 rounded-lg mb-4 flex items-center justify-center"
                    style={{ backgroundColor: soldAuction.imageColor }}
                  >
                    <span className="material-symbols-outlined text-white/50" style={{ fontSize: '40px' }}>image</span>
                  </div>

                  <div className="text-sm font-semibold text-text-primary mb-4 line-clamp-2">{soldAuction.title}</div>

                  <div className="space-y-2 mb-6">
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-text-secondary">Final winning price</span>
                      <span className="font-bold tabular-nums text-2xl text-text-primary">{formatCurrency(amount)}</span>
                    </div>
                    <div className="flex items-center justify-between text-xs text-text-muted pt-1 border-t border-border">
                      <span className="flex items-center gap-1">
                        <span className="material-symbols-outlined" style={{ fontSize: '13px' }}>lock_open</span>
                        Escrow: {escrowStatus}
                      </span>
                      <span className="tabular-nums text-text-secondary">{formatCurrency(amount)}</span>
                    </div>
                  </div>

                  <button
                    id="confirm-payment-btn"
                    onClick={handleConfirm}
                    disabled={confirming || escrowStatus === 'RELEASED'}
                    className="btn-primary w-full py-3 text-base"
                  >
                    {confirming ? (
                      <span className="flex items-center gap-2">
                        <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                        </svg>
                        Processing…
                      </span>
                    ) : (
                      <>
                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>lock_open</span>
                        {escrowStatus === 'RELEASED' ? 'Payment Already Released' : 'Confirm & Release Payment'}
                      </>
                    )}
                  </button>

                  <button onClick={() => navigate('/explore')} className="btn-secondary w-full mt-2">
                    Review Item Details First
                  </button>
                </div>
              ) : (
                <div className="card p-8 shadow-modal text-center">
                  <div className="w-16 h-16 rounded-full bg-warning-light flex items-center justify-center mx-auto mb-5">
                    <span className="material-symbols-outlined text-warning" style={{ fontSize: '36px' }}>lock</span>
                  </div>
                  <h2 className="text-xl font-bold text-text-primary mb-1">Escrow Status: {escrowStatus}</h2>
                  <p className="text-sm text-text-secondary mb-6">
                    {escrowStatus === 'HELD'
                      ? 'Funds are held in escrow pending delivery confirmation.'
                      : escrowStatus === 'RELEASED'
                      ? 'Funds have been released to your wallet.'
                      : 'Funds have been refunded.'}
                  </p>
                  <div className="bg-warning-light rounded-xl py-5 mb-6">
                    <div className="text-xs text-warning font-semibold uppercase tracking-wider mb-1">Escrow Amount</div>
                    <div className="text-4xl font-bold tabular-nums text-warning">{formatCurrency(amount)}</div>
                  </div>
                  {escrowStatus === 'HELD' && (
                    <button
                      id="refund-btn"
                      onClick={handleConfirm}
                      disabled={confirming}
                      className="btn-danger w-full bg-danger hover:bg-danger/90 border-0"
                    >
                      {confirming ? 'Processing…' : 'Refund Escrow'}
                    </button>
                  )}
                </div>
              )}
            </div>
          </div>

        </div>
      </main>
    </div>
  );
}