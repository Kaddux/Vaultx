import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TopNav } from '../components/TopNav';
import { MOCK_AUCTIONS, formatCurrency } from '../api';

type CheckoutView = 'buyer' | 'seller';

export function Checkout() {
  const navigate = useNavigate();
  const [view, setView] = useState<CheckoutView>('buyer');
  const [confirming, setConfirming] = useState(false);
  const [sellerConfirmed, setSellerConfirmed] = useState(false);

  // Use first sold auction
  const soldAuction = MOCK_AUCTIONS.find((a) => a.status === 'SOLD') ?? MOCK_AUCTIONS[0];

  const handleConfirm = () => {
    setConfirming(true);
    setTimeout(() => {
      setConfirming(false);
      setView('seller');
      setSellerConfirmed(false);
    }, 1200);
  };

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
            {/* View switcher for demo */}
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

          {/* Dimmed background simulation */}
          <div className="relative flex items-center justify-center min-h-[420px]">
            {/* Blurred background content */}
            <div className="absolute inset-0 rounded-xl bg-gradient-to-br from-gray-100 to-gray-200 flex items-center justify-center overflow-hidden">
              <div className="w-full h-full opacity-30 p-8 grid grid-cols-3 gap-4">
                {[...Array(6)].map((_, i) => (
                  <div key={i} className="bg-white rounded-lg h-20" />
                ))}
              </div>
              <div className="absolute inset-0 backdrop-blur-sm bg-black/20 rounded-xl" />
            </div>

            {/* Modal overlay */}
            <div className="relative z-10 w-full max-w-md modal-slide">
              {view === 'buyer' && (
                <div className="card p-8 shadow-modal">
                  {/* Header */}
                  <div className="flex items-center gap-3 mb-6">
                    <div className="w-10 h-10 rounded-full bg-success-light flex items-center justify-center">
                      <span className="material-symbols-outlined text-success" style={{ fontSize: '24px' }}>emoji_events</span>
                    </div>
                    <div>
                      <h2 className="text-lg font-bold text-text-primary">Congratulations!</h2>
                      <p className="text-sm text-text-secondary">You won this auction</p>
                    </div>
                  </div>

                  {/* Item thumbnail */}
                  <div
                    className="w-full h-32 rounded-lg mb-4 flex items-center justify-center"
                    style={{ backgroundColor: soldAuction.imageColor }}
                  >
                    <span className="material-symbols-outlined text-white/50" style={{ fontSize: '40px' }}>image</span>
                  </div>

                  {/* Item name */}
                  <div className="text-sm font-semibold text-text-primary mb-4 line-clamp-2">{soldAuction.title}</div>

                  {/* Price breakdown */}
                  <div className="space-y-2 mb-6">
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-text-secondary">Final winning price</span>
                      <span className="font-bold tabular-nums text-2xl text-text-primary">{formatCurrency(soldAuction.currentBid)}</span>
                    </div>
                    <div className="flex items-center justify-between text-xs text-text-muted pt-1 border-t border-border">
                      <span className="flex items-center gap-1">
                        <span className="material-symbols-outlined" style={{ fontSize: '13px' }}>lock_open</span>
                        Released from wallet escrow
                      </span>
                      <span className="tabular-nums text-text-secondary">{formatCurrency(soldAuction.currentBid)}</span>
                    </div>
                  </div>

                  {/* CTA */}
                  <button
                    id="confirm-payment-btn"
                    onClick={handleConfirm}
                    disabled={confirming}
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
                        Confirm & Release Payment
                      </>
                    )}
                  </button>

                  <button
                    onClick={() => navigate('/explore')}
                    className="btn-secondary w-full mt-2"
                  >
                    Review Item Details First
                  </button>
                </div>
              )}

              {view === 'seller' && (
                <div className="card p-8 shadow-modal text-center">
                  {/* Success icon */}
                  <div className="w-16 h-16 rounded-full bg-success-light flex items-center justify-center mx-auto mb-5">
                    <span className="material-symbols-outlined text-success" style={{ fontSize: '36px' }}>check_circle</span>
                  </div>

                  <h2 className="text-xl font-bold text-text-primary mb-1">Payout Released</h2>
                  <p className="text-sm text-text-secondary mb-6">Funds have been deposited to your wallet</p>

                  {/* Amount */}
                  <div className="bg-success-light rounded-xl py-5 mb-6">
                    <div className="text-xs text-success font-semibold uppercase tracking-wider mb-1">Amount Deposited</div>
                    <div className="text-4xl font-bold tabular-nums text-success">{formatCurrency(soldAuction.currentBid)}</div>
                    <div className="text-xs text-success/70 mt-1">from {soldAuction.title}</div>
                  </div>

                  {/* Item row */}
                  <div className="flex items-center gap-3 text-left p-3 bg-gray-50 rounded-lg mb-6">
                    <div
                      className="w-10 h-10 rounded-md shrink-0 flex items-center justify-center"
                      style={{ backgroundColor: soldAuction.imageColor }}
                    >
                      <span className="material-symbols-outlined text-white/60" style={{ fontSize: '18px' }}>image</span>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="text-sm font-medium text-text-primary truncate">{soldAuction.title}</div>
                      <div className="text-xs text-text-muted">{soldAuction.totalBids} bids · Final</div>
                    </div>
                    <span className="pill-green shrink-0">SOLD</span>
                  </div>

                  <button
                    id="view-wallet-btn"
                    onClick={() => navigate('/wallet')}
                    className="btn-secondary w-full"
                  >
                    <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>account_balance_wallet</span>
                    View in Wallet
                  </button>
                </div>
              )}
            </div>
          </div>

        </div>
      </main>
    </div>
  );
}
