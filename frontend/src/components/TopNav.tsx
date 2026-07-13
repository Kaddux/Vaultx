import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { MOCK_USER, formatCurrency } from '../api';

export function TopNav() {
  const location = useLocation();
  const navigate = useNavigate();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const user = MOCK_USER;
  const isLoggedIn = localStorage.getItem('vaultx_logged_in') === 'true';

  const navLinks = [
    { label: 'Dashboard', to: '/' },
    { label: 'Explore Auctions', to: '/explore' },
    { label: 'Seller Portal', to: '/seller', sellerOnly: true },
    { label: 'Transactions', to: '/transactions' },
  ];

  const isActive = (to: string) => {
    if (to === '/') return location.pathname === '/';
    return location.pathname.startsWith(to);
  };

  return (
    <header className="fixed top-0 left-0 right-0 z-50 h-16 bg-white border-b border-border flex items-center">
      <div className="w-full max-w-[1280px] mx-auto px-6 flex items-center gap-8">

        {/* Logo */}
        <Link to="/" className="flex items-center gap-2 shrink-0">
          <span className="text-primary font-bold text-xl tracking-tight">⚡ Vaultx</span>
        </Link>

        {/* Nav Links */}
        <nav className="flex items-center gap-1 flex-1">
          {navLinks.map((link) => {
            if (link.to !== '/explore' && !isLoggedIn) return null;
            if (link.sellerOnly && user.role !== 'SELLER' && user.role !== 'ADMIN') return null;
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
          {isLoggedIn ? (
            <>
              {/* Live Balance Widget */}
              <Link
                to="/wallet"
                className="flex items-center gap-3 px-3 py-2 rounded-lg border border-border bg-white hover:border-gray-300 hover:shadow-card transition-all duration-150 group"
              >
                <div className="text-right">
                  <div className="text-sm font-bold tabular-nums text-text-primary leading-none">
                    {formatCurrency(user.balance)}
                  </div>
                  <div className="text-xs text-text-muted leading-none mt-0.5 flex items-center gap-0.5 justify-end">
                    <span className="material-symbols-outlined" style={{ fontSize: '11px' }}>lock</span>
                    <span className="tabular-nums">{formatCurrency(user.reservedBalance)}</span>
                  </div>
                </div>
                <div className="w-px h-8 bg-border" />
                <div className="flex flex-col items-start">
                  <span className="text-xs font-medium text-text-secondary leading-none">Available</span>
                  <span className="text-xs text-text-muted leading-none mt-0.5">Reserved</span>
                </div>
              </Link>

              {/* User Dropdown */}
              <div className="relative">
                <button
                  id="user-menu-btn"
                  onClick={() => setDropdownOpen((o) => !o)}
                  className="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-gray-100 transition-colors duration-150 cursor-pointer"
                >
                  {/* Avatar */}
                  <div className="w-7 h-7 rounded-full bg-primary flex items-center justify-center text-white text-xs font-bold shrink-0">
                    {user.fullName.charAt(0)}
                  </div>
                  <span className="text-sm font-medium text-text-primary hidden sm:block">{user.username}</span>
                  <span className="material-symbols-outlined text-text-secondary" style={{ fontSize: '18px' }}>
                    expand_more
                  </span>
                </button>

                {dropdownOpen && (
                  <>
                    {/* Backdrop */}
                    <div className="fixed inset-0 z-40" onClick={() => setDropdownOpen(false)} />
                {/* Menu */}
                <div className="absolute right-0 top-full mt-2 w-56 bg-white rounded-lg border border-border shadow-card-hover z-50 py-1 overlay-fade">
                  {/* User Info */}
                  <div className="px-4 py-3 border-b border-border">
                    <div className="text-sm font-semibold text-text-primary">{user.fullName}</div>
                    <div className="text-xs text-text-secondary mt-0.5">{user.email}</div>
                    <div className="mt-2">
                      {user.kycStatus === 'VERIFIED' && (
                        <span className="pill-green text-xs">
                          <span className="material-symbols-outlined" style={{ fontSize: '12px' }}>verified</span>
                          VERIFIED
                        </span>
                      )}
                      {user.kycStatus === 'PENDING' && (
                        <span className="pill-amber text-xs">PENDING</span>
                      )}
                      {user.kycStatus === 'UNVERIFIED' && (
                        <span className="pill-gray text-xs">UNVERIFIED</span>
                      )}
                    </div>
                  </div>
                  {/* Menu items */}
                  <div className="py-1">
                    <button
                      onClick={() => { navigate('/wallet'); setDropdownOpen(false); }}
                      className="w-full text-left flex items-center gap-3 px-4 py-2 text-sm text-text-primary hover:bg-gray-50 transition-colors duration-100"
                    >
                      <span className="material-symbols-outlined text-text-secondary" style={{ fontSize: '18px' }}>account_balance_wallet</span>
                      Wallet
                    </button>
                    <button
                      className="w-full text-left flex items-center gap-3 px-4 py-2 text-sm text-text-primary hover:bg-gray-50 transition-colors duration-100"
                    >
                      <span className="material-symbols-outlined text-text-secondary" style={{ fontSize: '18px' }}>settings</span>
                      Profile Settings
                    </button>
                  </div>
                  <div className="border-t border-border py-1">
                    <button
                      onClick={() => {
                        localStorage.removeItem('vaultx_logged_in');
                        navigate('/login');
                      }}
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
              <span className="hidden sm:flex items-center gap-1.5 text-xs font-semibold text-success bg-success-light px-2.5 py-1 rounded-full">
                <span className="w-1.5 h-1.5 rounded-full bg-success animate-pulse inline-block" />
                4,312 Users Online
              </span>
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
