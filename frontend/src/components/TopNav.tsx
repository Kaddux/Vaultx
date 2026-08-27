import { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { api, formatCurrency } from '../api';
import { useAuth } from '../context/AuthContext';

export function TopNav() {
  const location = useLocation();
  const navigate = useNavigate();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const { user, wallet, isAuthenticated, logout } = useAuth();

  useEffect(() => {
    if (!isAuthenticated) {
      setUnread(0);
      return;
    }
    let cancelled = false;
    const load = () =>
      api.notifications
        .unreadCount()
        .then((res) => {
          if (!cancelled) setUnread(res.unread);
        })
        .catch(() => {});
    load();
    const interval = setInterval(load, 30000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [isAuthenticated]);

  // Refresh the badge immediately whenever notifications are marked as read.
  useEffect(() => {
    const onRead = () => {
      if (!isAuthenticated) return;
      api.notifications
        .unreadCount()
        .then((res) => setUnread(res.unread))
        .catch(() => {});
    };
    window.addEventListener('vaultx:notifications-read', onRead);
    return () => window.removeEventListener('vaultx:notifications-read', onRead);
  }, [isAuthenticated]);

  const navLinks = [
    { label: 'Dashboard', to: '/' },
    { label: 'Explore Auctions', to: '/explore' },
    { label: 'Seller Portal', to: '/seller' },
    { label: 'Transactions', to: '/transactions' },
  ];

  const isActive = (to: string) => {
    if (to === '/') return location.pathname === '/';
    return location.pathname.startsWith(to);
  };

  return (
    <header className="fixed top-0 left-0 right-0 z-50 h-16 bg-white border-b border-border flex items-center">
      <div className="w-full max-w-[1280px] mx-auto px-6 flex items-center gap-4 sm:gap-6">

        {/* Back Button */}
        {location.pathname !== '/' && (
          <button
            onClick={() => navigate(-1)}
            className="flex items-center justify-center w-8 h-8 rounded-full border border-border bg-white text-text-secondary hover:text-text-primary hover:border-gray-300 hover:shadow-sm transition-all duration-150 cursor-pointer shrink-0"
            title="Go back"
          >
            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>arrow_back</span>
          </button>
        )}

        {/* Logo */}
        <Link to="/" className="flex items-center gap-2 shrink-0">
          <span className="text-primary font-bold text-xl tracking-tight">⚡ Vaultx</span>
        </Link>

        {/* Nav Links */}
        <nav className="flex items-center gap-1 flex-1">
          {navLinks.map((link) => {
            if (link.to !== '/explore' && !isAuthenticated) return null;
            const active = isActive(link.to);
            return (
              <Link
                key={link.to}
                to={link.to}
                className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors duration-150 ${
                  active
                    ? 'text-primary bg-primary-light'
                    : 'text-text-secondary hover:text-text-primary hover:bg-gray-100'
                }`}
              >
                {link.label}
              </Link>
            );
          })}
        </nav>

        {/* Right side */}
        <div className="flex items-center gap-4">
          {isAuthenticated && user ? (
            <>
              {/* Live Balance Widget */}
              <Link
                to="/wallet"
                className="flex items-center gap-3 px-3 py-2 rounded-lg border border-border bg-white hover:border-gray-300 hover:shadow-card transition-all duration-150 group"
              >
                <div className="text-right">
                  <div className="text-sm font-bold tabular-nums text-text-primary leading-none">
                    {formatCurrency(wallet?.availableBalance ?? 0)}
                  </div>
                  <div className="text-xs text-text-muted leading-none mt-0.5 flex items-center gap-0.5 justify-end">
                    <span className="material-symbols-outlined" style={{ fontSize: '11px' }}>lock</span>
                    <span className="tabular-nums">{formatCurrency(wallet?.reservedBalance ?? 0)}</span>
                  </div>
                </div>
                <div className="w-px h-8 bg-border" />
                <div className="flex flex-col items-start">
                  <span className="text-xs font-medium text-text-secondary leading-none">Available</span>
                  <span className="text-xs text-text-muted leading-none mt-0.5">Reserved</span>
                </div>
              </Link>

              {/* Notifications bell */}
              <Link to="/" className="relative flex items-center justify-center w-9 h-9 rounded-lg border border-border bg-white text-text-secondary hover:border-gray-300 hover:shadow-card transition-all duration-150">
                <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>notifications</span>
                {unread > 0 && (
                  <span className="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-danger text-white text-[10px] font-bold flex items-center justify-center">
                    {unread > 99 ? '99+' : unread}
                  </span>
                )}
              </Link>

              {/* User Dropdown */}
              <div className="relative">
                <button
                  id="user-menu-btn"
                  onClick={() => setDropdownOpen((o) => !o)}
                  className="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-gray-100 transition-colors duration-150 cursor-pointer"
                >
                  <div className="w-7 h-7 rounded-full bg-primary flex items-center justify-center text-white text-xs font-bold shrink-0">
                    {user.fullName?.charAt(0) || user.username?.charAt(0)}
                  </div>
                  <span className="text-sm font-medium text-text-primary hidden sm:block">{user.username}</span>
                  <span className="material-symbols-outlined text-text-secondary" style={{ fontSize: '18px' }}>
                    expand_more
                  </span>
                </button>

                {dropdownOpen && (
                  <>
                    <div className="fixed inset-0 z-40" onClick={() => setDropdownOpen(false)} />
                    <div className="absolute right-0 top-full mt-2 w-56 bg-white rounded-lg border border-border shadow-card-hover z-50 py-1 overlay-fade">
                      <div className="px-4 py-3 border-b border-border">
                        <div className="text-sm font-semibold text-text-primary">{user.fullName || user.username}</div>
                        <div className="text-xs text-text-secondary mt-0.5">{user.email}</div>
                        <div className="mt-2">
                          {user.kycStatus === 'VERIFIED' && (
                            <span className="pill-green text-xs">
                              <span className="material-symbols-outlined" style={{ fontSize: '12px' }}>verified</span>
                              VERIFIED
                            </span>
                          )}
                          {user.kycStatus !== 'VERIFIED' && (
                            <span className="pill-amber text-xs">PENDING</span>
                          )}
                        </div>
                      </div>
                      <div className="py-1">
                        <button
                          onClick={() => { navigate('/wallet'); setDropdownOpen(false); }}
                          className="w-full text-left flex items-center gap-3 px-4 py-2 text-sm text-text-primary hover:bg-gray-50 transition-colors duration-100"
                        >
                          <span className="material-symbols-outlined text-text-secondary" style={{ fontSize: '18px' }}>account_balance_wallet</span>
                          Wallet
                        </button>
                        <button
                          onClick={() => { navigate('/wallet'); setDropdownOpen(false); }}
                          className="w-full text-left flex items-center gap-3 px-4 py-2 text-sm text-text-primary hover:bg-gray-50 transition-colors duration-100"
                        >
                          <span className="material-symbols-outlined text-text-secondary" style={{ fontSize: '18px' }}>settings</span>
                          Profile & KYC
                        </button>
                      </div>
                      <div className="border-t border-border py-1">
                        <button
                          onClick={() => { logout(); navigate('/login'); }}
                          className="w-full text-left flex items-center gap-3 px-4 py-2 text-sm text-danger hover:bg-danger-light transition-colors duration-100"
                        >
                          <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>logout</span>
                          Log Out
                        </button>
                      </div>
                    </div>
                  </>
                )}
              </div>
            </>
          ) : (
            <>
              <Link to="/login" className="text-sm font-medium text-text-secondary hover:text-text-primary transition-colors">
                Sign In
              </Link>
              <Link to="/register" className="btn-primary text-xs px-3.5 py-1.5">
                Get Started
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}