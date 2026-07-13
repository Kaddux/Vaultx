import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { TopNav } from '../components/TopNav';
import { CountdownTimer } from '../components/CountdownTimer';
import { MOCK_USER, MOCK_AUCTIONS, formatCurrency, formatDate } from '../api';

export function Home() {
  const navigate = useNavigate();
  const user = MOCK_USER;

  // Derive mock dashboard state
  // Let's assume the user has bid on Mickey Mantle Baseball Card (id: auc_001) and Rolex Submariner (id: auc_002)
  const [activeBids] = useState([
    {
      auction: MOCK_AUCTIONS.find(a => a.id === 'auc_001')!,
      userBid: 8800,
      status: 'OUTBID' as const, // Alex Morgan bid $8,800, currentBid is $9,400
    },
    {
      auction: MOCK_AUCTIONS.find(a => a.id === 'auc_002')!,
      userBid: 14750,
      status: 'WINNING' as const, // Alex Morgan bid is the highest bid (e.g. currentBid is $14,750)
    }
  ]);

  // Let's assume the user is watching Fender Stratocaster (id: auc_003) and Abstract Oil Painting (id: auc_004)
  const [watchlist] = useState([
    MOCK_AUCTIONS.find(a => a.id === 'auc_003')!,
    MOCK_AUCTIONS.find(a => a.id === 'auc_004')!,
  ]);

  // Recent activity logs
  const [activityLogs] = useState([
    { id: 1, type: 'OUTBID', text: 'You were outbid on 1952 Topps Mickey Mantle Baseball Card', time: '5m ago', icon: 'warning', color: 'text-danger bg-danger-light' },
    { id: 2, type: 'BID', text: 'Placed bid of $14,750 on Rolex Submariner Date 126610LN', time: '12m ago', icon: 'gavel', color: 'text-primary bg-primary-light' },
    { id: 3, type: 'DEPOSIT', text: 'Wire transfer deposit of $5,000.00 completed successfully', time: '1h ago', icon: 'add_card', color: 'text-success bg-success-light' },
    { id: 4, type: 'ESCROW_HOLD', text: '$3,200.00 locked in active bid escrow hold', time: '3h ago', icon: 'lock', color: 'text-warning bg-warning-light' },
  ]);

  return (
    <div className="min-h-screen bg-bg-base">
      <TopNav />

      <main className="pt-16">
        <div className="max-w-[1280px] mx-auto px-6 py-8">
          
          {/* Welcome Header */}
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
            <div>
              <div className="flex items-center gap-2.5">
                <h1 className="text-2xl font-bold text-text-primary tracking-tight">Welcome back, {user.fullName}</h1>
                <span className="pill-indigo uppercase text-[10px] tracking-wider py-0.5 px-2 font-bold">{user.role}</span>
                {user.kycStatus === 'VERIFIED' && (
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
              {user.role === 'SELLER' && (
                <Link to="/seller" className="btn-primary py-2 text-xs font-semibold flex items-center gap-1">
                  <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>add</span>
                  Create Auction
                </Link>
              )}
            </div>
          </div>

          {/* Quick Metrics Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
            {/* Metric: Available Balance */}
            <div className="card p-5 hover:border-gray-300 transition-colors duration-200">
              <div className="flex justify-between items-start mb-3">
                <span className="text-xs font-semibold text-text-secondary uppercase tracking-wider">Available Funds</span>
                <span className="material-symbols-outlined text-primary bg-primary-light p-1.5 rounded-lg" style={{ fontSize: '20px' }}>account_balance_wallet</span>
              </div>
              <div className="text-2xl font-bold text-text-primary tabular-nums">{formatCurrency(user.balance)}</div>
              <div className="mt-2.5 flex justify-between items-center text-xs">
                <span className="text-text-muted">In escrow: {formatCurrency(user.reservedBalance)}</span>
                <Link to="/wallet" className="text-primary font-medium hover:underline flex items-center gap-0.5">
                  Manage Wallet
                  <span className="material-symbols-outlined" style={{ fontSize: '12px' }}>chevron_right</span>
                </Link>
              </div>
            </div>

            {/* Metric: Active Bids */}
            <div className="card p-5 hover:border-gray-300 transition-colors duration-200">
              <div className="flex justify-between items-start mb-3">
                <span className="text-xs font-semibold text-text-secondary uppercase tracking-wider">Active Bids</span>
                <span className="material-symbols-outlined text-success bg-success-light p-1.5 rounded-lg" style={{ fontSize: '20px' }}>gavel</span>
              </div>
              <div className="text-2xl font-bold text-text-primary tabular-nums">{activeBids.length} Live</div>
              <div className="mt-2.5 text-xs text-text-muted flex items-center gap-1.5">
                <span className="inline-block w-2 h-2 rounded-full bg-success"></span>
                <span>1 Winning</span>
                <span className="text-text-muted">•</span>
                <span className="inline-block w-2 h-2 rounded-full bg-danger"></span>
                <span>1 Outbid</span>
              </div>
            </div>

            {/* Metric: Watchlist */}
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

            {/* Metric: KYC Status */}
            <div className="card p-5 hover:border-gray-300 transition-colors duration-200">
              <div className="flex justify-between items-start mb-3">
                <span className="text-xs font-semibold text-text-secondary uppercase tracking-wider">KYC Status</span>
                <span className="material-symbols-outlined text-warning bg-warning-light p-1.5 rounded-lg" style={{ fontSize: '20px' }}>id_card</span>
              </div>
              <div className="text-lg font-bold text-success flex items-center gap-1.5 mt-1">
                <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>verified_user</span>
                Verified Account
              </div>
              <div className="mt-2 text-xs text-text-muted">Limits: Unlimited Bidding Access</div>
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
                      {activeBids.map(({ auction, userBid, status }) => (
                        <tr key={auction.id} className="hover:bg-gray-50/80 transition-colors duration-150">
                          <td className="py-3 px-4">
                            <div>
                              <div className="font-semibold text-text-primary text-sm line-clamp-1">{auction.title}</div>
                              <div className="text-xs text-text-muted">Lot #{auction.lotNumber} · {auction.category}</div>
                            </div>
                          </td>
                          <td className="py-3 px-4 font-mono font-semibold tabular-nums text-text-secondary text-sm">
                            {formatCurrency(userBid)}
                          </td>
                          <td className="py-3 px-4 font-mono font-bold tabular-nums text-text-primary text-sm">
                            {formatCurrency(auction.currentBid)}
                          </td>
                          <td className="py-3 px-4">
                            <CountdownTimer endsAt={auction.endsAt} />
                          </td>
                          <td className="py-3 px-4">
                            {status === 'WINNING' ? (
                              <span className="pill-green text-[10px] py-0.5 px-2">WINNING</span>
                            ) : (
                              <span className="pill-red text-[10px] py-0.5 px-2">OUTBID</span>
                            )}
                          </td>
                          <td className="py-3 px-4">
                            <button
                              onClick={() => navigate(`/auction/${auction.id}`)}
                              className="text-xs font-semibold text-primary hover:underline"
                            >
                              {status === 'OUTBID' ? 'Bid Again' : 'View'}
                            </button>
                          </td>
                        </tr>
                      ))}
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

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {watchlist.map((auction) => (
                    <div
                      key={auction.id}
                      onClick={() => navigate(`/auction/${auction.id}`)}
                      className="border border-border rounded-lg p-4 hover:border-gray-300 hover:shadow-card cursor-pointer transition-all duration-200 flex gap-4"
                    >
                      <div className="w-16 h-16 rounded-md flex items-center justify-center shrink-0" style={{ backgroundColor: auction.imageColor }}>
                        <span className="material-symbols-outlined text-white/80" style={{ fontSize: '24px' }}>
                          {auction.imageAccent || 'sell'}
                        </span>
                      </div>
                      <div className="flex-1 min-w-0 flex flex-col justify-between">
                        <div>
                          <h3 className="text-sm font-semibold text-text-primary leading-tight line-clamp-1 hover:text-primary transition-colors">{auction.title}</h3>
                          <p className="text-xs text-text-muted mt-0.5">@{auction.seller}</p>
                        </div>
                        <div className="flex items-center justify-between mt-2 pt-2 border-t border-gray-100">
                          <div>
                            <span className="text-[10px] text-text-muted block">Current Bid</span>
                            <span className="font-bold text-text-primary text-xs tabular-nums">{formatCurrency(auction.currentBid)}</span>
                          </div>
                          <CountdownTimer endsAt={auction.endsAt} />
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
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
                  <button className="text-[10px] font-bold text-text-muted uppercase tracking-wider hover:text-text-primary transition-colors">Clear</button>
                </div>

                <div className="flow-root">
                  <ul className="-mb-8">
                    {activityLogs.map((log, logIdx) => (
                      <li key={log.id}>
                        <div className="relative pb-8">
                          {logIdx !== activityLogs.length - 1 ? (
                            <span className="absolute top-4 left-4 -ml-px h-full w-0.5 bg-gray-200" aria-hidden="true" />
                          ) : null}
                          <div className="relative flex space-x-3">
                            <div>
                              <span className={`h-8 w-8 rounded-lg flex items-center justify-center ring-8 ring-white ${log.color}`}>
                                <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>{log.icon}</span>
                              </span>
                            </div>
                            <div className="flex-1 min-w-0 pt-1.5 flex justify-between space-x-4">
                              <div>
                                <p className="text-xs text-text-primary leading-tight">{log.text}</p>
                              </div>
                              <div className="text-right text-[10px] whitespace-nowrap text-text-muted">
                                <span>{log.time}</span>
                              </div>
                            </div>
                          </div>
                        </div>
                      </li>
                    ))}
                  </ul>
                </div>
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
