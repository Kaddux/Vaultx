import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { TopNav } from '../components/TopNav';
import { CountdownTimer } from '../components/CountdownTimer';
import {
  api,
  MyBidResponse,
  WatchlistResponse,
  NotificationResponse,
  formatCurrency,
  toUtcDate,
} from '../api';
import { useAuth } from '../context/AuthContext';

export function Home() {
  const navigate = useNavigate();
  const { user, wallet } = useAuth();

  const [activeBids, setActiveBids] = useState<MyBidResponse[]>([]);
  const [watchlist, setWatchlist] = useState<WatchlistResponse[]>([]);
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    Promise.all([api.auctions.myBids(), api.watchlist.list(), api.notifications.list(0, 10)])
      .then(([bids, wl, notes]) => {
        if (cancelled) return;
        setActiveBids(bids);
        setWatchlist(wl);
        setNotifications(notes);
        // Mark notifications as read once they are displayed, then refresh the bell badge.
        api.notifications
          .markRead()
          .catch(() => {})
          .finally(() => window.dispatchEvent(new Event('vaultx:notifications-read')));
      })
      .catch(() => {
        // ignore — dashboard degrades gracefully
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen bg-bg-base">
        <TopNav />
        <main className="pt-16 flex items-center justify-center min-h-[60vh]">
          <span className="text-text-muted text-sm">Loading dashboard…</span>
        </main>
      </div>
    );
  }

  const balance = wallet?.balance ?? 0;
  const reservedBalance = wallet?.reservedBalance ?? 0;
  const availableBalance = wallet?.availableBalance ?? 0;
  const winningCount = activeBids.filter((b) => b.myStatus === 'WINNING').length;
  const outbidCount = activeBids.length - winningCount;

  return (
    <div className="min-h-screen bg-bg-base">
      <TopNav />

      <main className="pt-16">
        <div className="max-w-[1280px] mx-auto px-6 py-8">

          {/* Welcome Header */}
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
            <div>
              <div className="flex items-center gap-2.5">
                <h1 className="text-2xl font-bold text-text-primary tracking-tight">Welcome back, {user?.fullName || user?.username}</h1>
                {user?.role && (
                  <span className="pill-indigo uppercase text-[10px] tracking-wider py-0.5 px-2 font-bold">{user.role}</span>
                )}
                {user?.kycStatus === 'VERIFIED' && (
                  <span className="pill-green text-[10px] font-bold py-0.5 px-2 flex items-center gap-0.5">
                    <span className="material-symbols-outlined" style={{ fontSize: '11px' }}>verified</span>
                    KYC VERIFIED
                  </span>
                )}
              </div>
              <p className="text-sm text-text-secondary mt-1">Here's a summary of your live bidding activity and performance.</p>
            </div>

            {/* Quick action buttons */}
            <div className="flex items-center gap-2.5">
              <Link to="/explore" className="btn-secondary py-2 text-xs font-semibold flex items-center gap-1">
                <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>search</span>
                Browse Auctions
              </Link>
              <Link to="/seller" className="btn-primary py-2 text-xs font-semibold flex items-center gap-1">
                <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>add</span>
                Create Auction
              </Link>
            </div>
          </div>

          {/* Quick Metrics Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
            <div className="card p-5 hover:border-gray-300 transition-colors duration-200">
              <div className="flex justify-between items-start mb-3">
                <span className="text-xs font-semibold text-text-secondary uppercase tracking-wider">Available Funds</span>
                <span className="material-symbols-outlined text-primary bg-primary-light p-1.5 rounded-lg" style={{ fontSize: '20px' }}>account_balance_wallet</span>
              </div>
              <div className="text-2xl font-bold text-text-primary tabular-nums">{formatCurrency(availableBalance)}</div>
              <div className="mt-2.5 flex justify-between items-center text-xs">
                <span className="text-text-muted">In escrow: {formatCurrency(reservedBalance)}</span>
                <Link to="/wallet" className="text-primary font-medium hover:underline flex items-center gap-0.5">
                  Manage Wallet
                  <span className="material-symbols-outlined" style={{ fontSize: '12px' }}>chevron_right</span>
                </Link>
              </div>
            </div>

            <div className="card p-5 hover:border-gray-300 transition-colors duration-200">
              <div className="flex justify-between items-start mb-3">
                <span className="text-xs font-semibold text-text-secondary uppercase tracking-wider">Active Bids</span>
                <span className="material-symbols-outlined text-success bg-success-light p-1.5 rounded-lg" style={{ fontSize: '20px' }}>gavel</span>
              </div>
              <div className="text-2xl font-bold text-text-primary tabular-nums">{activeBids.length} Live</div>
              <div className="mt-2.5 text-xs text-text-muted flex items-center gap-1.5">
                <span className="inline-block w-2 h-2 rounded-full bg-success"></span>
                <span>{winningCount} Winning</span>
                <span className="text-text-muted">•</span>
                <span className="inline-block w-2 h-2 rounded-full bg-danger"></span>
                <span>{outbidCount} Outbid</span>
              </div>
            </div>

            <div className="card p-5 hover:border-gray-300 transition-colors duration-200">
              <div className="flex justify-between items-start mb-3">
                <span className="text-xs font-semibold text-text-secondary uppercase tracking-wider">Watchlist</span>
                <span className="material-symbols-outlined text-warning bg-warning-light p-1.5 rounded-lg" style={{ fontSize: '20px' }}>visibility</span>
              </div>
              <div className="text-2xl font-bold text-text-primary tabular-nums">{watchlist.length} items</div>
              <div className="mt-2.5 text-xs">
                <a href="#watchlist-section" className="text-primary font-medium hover:underline flex items-center gap-0.5">
                  View watchlist
                  <span className="material-symbols-outlined" style={{ fontSize: '12px' }}>chevron_right</span>
                </a>
              </div>
            </div>

            <div className="card p-5 hover:border-gray-300 transition-colors duration-200">
              <div className="flex justify-between items-start mb-3">
                <span className="text-xs font-semibold text-text-secondary uppercase tracking-wider">KYC Status</span>
                <span className="material-symbols-outlined text-warning bg-warning-light p-1.5 rounded-lg" style={{ fontSize: '20px' }}>id_card</span>
              </div>
              {user?.kycStatus === 'VERIFIED' ? (
                <div className="text-lg font-bold text-success flex items-center gap-1.5 mt-1">
                  <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>verified_user</span>
                  Verified Account
                </div>
              ) : (
                <div className="text-lg font-bold text-warning flex items-center gap-1.5 mt-1">
                  <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>pending_actions</span>
                  Pending
                </div>
              )}
              <div className="mt-2 text-xs text-text-muted">
                {user?.kycStatus === 'VERIFIED' ? 'Limits: Unlimited Bidding Access' : 'Complete KYC to start bidding'}
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">

            {/* Left: Active Bids Tracker */}
            <div className="lg:col-span-2 space-y-6">
              <div className="card p-6">
                <div className="flex justify-between items-center mb-5 pb-4 border-b border-border">
                  <h2 className="text-base font-bold text-text-primary flex items-center gap-2">
                    <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>gavel</span>
                    Active Bids Tracker
                  </h2>
                  <span className="text-xs text-text-secondary">Real-Time Statuses</span>
                </div>

                <div className="overflow-x-auto">
                  <table className="data-table w-full">
                    <thead>
                      <tr>
                        <th>Lot / Item</th>
                        <th>Your Bid</th>
                        <th>Current Bid</th>
                        <th>Ends In</th>
                        <th>Status</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {activeBids.length === 0 ? (
                        <tr>
                          <td colSpan={6} className="text-center py-8 text-text-muted text-sm">
                            You haven't placed any bids yet.
                            <Link to="/explore" className="text-primary font-medium hover:underline ml-1">Browse auctions</Link>
                          </td>
                        </tr>
                      ) : (
                        activeBids.map((bid) => (
                          <tr key={bid.bidId} className="hover:bg-gray-50/80 transition-colors duration-150">
                            <td className="py-3 px-4">
                              <div>
                                <div className="font-semibold text-text-primary text-sm line-clamp-1">{bid.auctionTitle}</div>
                                <div className="text-xs text-text-muted">Lot · {bid.auctionStatus}</div>
                              </div>
                            </td>
                            <td className="py-3 px-4 font-mono font-semibold tabular-nums text-text-secondary text-sm">
                              {formatCurrency(bid.myBidAmount)}
                            </td>
                            <td className="py-3 px-4 font-mono font-bold tabular-nums text-text-primary text-sm">
                              {formatCurrency(bid.currentBid ?? 0)}
                            </td>
                            <td className="py-3 px-4">
                              <CountdownTimer endsAt={toUtcDate(bid.endTime)} />
                            </td>
                            <td className="py-3 px-4">
                              {bid.myStatus === 'WINNING' ? (
                                <span className="pill-green text-[10px] py-0.5 px-2">WINNING</span>
                              ) : (
                                <span className="pill-red text-[10px] py-0.5 px-2">OUTBID</span>
                              )}
                            </td>
                            <td className="py-3 px-4">
                              <button
                                onClick={() => navigate(`/auction/${bid.auctionId}`)}
                                className="text-xs font-semibold text-primary hover:underline"
                              >
                                {bid.myStatus === 'OUTBID' ? 'Bid Again' : 'View'}
                              </button>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Watchlist Section */}
              <div id="watchlist-section" className="card p-6">
                <div className="flex justify-between items-center mb-5 pb-4 border-b border-border">
                  <h2 className="text-base font-bold text-text-primary flex items-center gap-2">
                    <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>visibility</span>
                    Your Watchlist
                  </h2>
                  <Link to="/explore" className="text-xs text-primary font-semibold hover:underline">Explore More</Link>
                </div>

                {watchlist.length === 0 ? (
                  <div className="text-center py-8 text-text-muted text-sm">
                    No watched auctions yet.
                    <Link to="/explore" className="text-primary font-medium hover:underline ml-1">Explore auctions</Link>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {watchlist.map((w) => (
                      <div
                        key={w.id}
                        onClick={() => navigate(`/auction/${w.id}`)}
                        className="border border-border rounded-lg p-4 hover:border-gray-300 hover:shadow-card cursor-pointer transition-all duration-200 flex gap-4"
                      >
                        <div className="w-16 h-16 rounded-md flex items-center justify-center shrink-0" style={{ backgroundColor: '#2d1a4a' }}>
                          <span className="material-symbols-outlined text-white/80" style={{ fontSize: '24px' }}>
                            {'sell'}
                          </span>
                        </div>
                        <div className="flex-1 min-w-0 flex flex-col justify-between">
                          <div>
                            <h3 className="text-sm font-semibold text-text-primary leading-tight line-clamp-1 hover:text-primary transition-colors">{w.title}</h3>
                            <p className="text-xs text-text-muted mt-0.5">Seller {w.sellerId.slice(0, 6)}</p>
                          </div>
                          <div className="flex items-center justify-between mt-2 pt-2 border-t border-gray-100">
                            <div>
                              <span className="text-[10px] text-text-muted block">Current Bid</span>
                              <span className="font-bold text-text-primary text-xs tabular-nums">{formatCurrency(w.currentBid ?? 0)}</span>
                            </div>
                            <CountdownTimer endsAt={toUtcDate(w.endTime)} />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

            </div>

            {/* Right: Recent Activity feed */}
            <div className="space-y-6">
              <div className="card p-6">
                <div className="flex justify-between items-center mb-5 pb-4 border-b border-border">
                  <h2 className="text-base font-bold text-text-primary flex items-center gap-2">
                    <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>notifications</span>
                    Recent Activity
                  </h2>
                </div>

                {notifications.length === 0 ? (
                  <div className="text-center py-8 text-text-muted text-sm">No notifications yet.</div>
                ) : (
                  <div className="flow-root">
                    <ul className="-mb-8">
                      {notifications.slice(0, 6).map((note, noteIdx) => (
                        <li key={note.id}>
                          <div className="relative pb-8">
                            {noteIdx !== Math.min(notifications.length, 6) - 1 ? (
                              <span className="absolute top-4 left-4 -ml-px h-full w-0.5 bg-gray-200" aria-hidden="true" />
                            ) : null}
                            <div className="relative flex space-x-3">
                              <div>
                                <span className="h-8 w-8 rounded-lg flex items-center justify-center ring-8 ring-white bg-primary-light text-primary">
                                  <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>notifications</span>
                                </span>
                              </div>
                              <div className="flex-1 min-w-0 pt-1.5">
                                <div>
                                  <p className="text-xs font-semibold text-text-primary leading-tight">{note.title}</p>
                                  <p className="text-xs text-text-secondary leading-tight mt-0.5">{note.message}</p>
                                </div>
                                <div className="text-right text-[10px] whitespace-nowrap text-text-muted mt-1">
                                  <span>{new Date(note.createdAt).toLocaleString()}</span>
                                </div>
                              </div>
                            </div>
                          </div>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>

              {/* Escrow Status info widget */}
              <div className="card p-5 bg-primary/[0.02] border-primary/20">
                <h3 className="text-xs font-bold text-primary uppercase tracking-wider mb-2 flex items-center gap-1.5">
                  <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>verified_user</span>
                  Escrow Protection Active
                </h3>
                <p className="text-xs text-text-secondary leading-relaxed">
                  Your funds are protected with bank-grade security during active bids. Payout settlement takes place instantly upon buyer receipt confirmation.
                </p>
                <div className="mt-3 pt-3 border-t border-primary/10 flex items-center justify-between text-xs text-primary font-semibold">
                  <Link to="/wallet" className="hover:underline">View Escrow Policy</Link>
                  <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>security</span>
                </div>
              </div>
            </div>

          </div>

        </div>
      </main>
    </div>
  );
}