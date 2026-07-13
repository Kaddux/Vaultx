import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { MOCK_USER, saveState } from '../api';

export function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPw, setShowPw] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    // Simulate auth — wrong pw triggers 401-style error
    setTimeout(() => {
      setLoading(false);
      if (email === 'demo@vaultx.io' && password === 'Demo1234!') {
        localStorage.setItem('vaultx_logged_in', 'true');
        
        // Restore default demo user profile
        MOCK_USER.username = 'alex_vault';
        MOCK_USER.fullName = 'Alex Morgan';
        MOCK_USER.email = 'alex@vaultx.io';
        MOCK_USER.kycStatus = 'VERIFIED';
        MOCK_USER.balance = 12_480.00;
        MOCK_USER.reservedBalance = 3_200.00;
        saveState();

        navigate('/explore');
      } else if (email === 'suspended@vaultx.io') {
        setError('403: Account suspended — contact support');
      } else {
        localStorage.setItem('vaultx_logged_in', 'true');

        // Create new session for this email
        const userPart = email.split('@')[0];
        MOCK_USER.username = userPart;
        MOCK_USER.fullName = userPart.charAt(0).toUpperCase() + userPart.slice(1);
        MOCK_USER.email = email;
        MOCK_USER.kycStatus = 'UNVERIFIED';
        MOCK_USER.balance = 0.00;
        MOCK_USER.reservedBalance = 0.00;
        saveState();

        navigate('/explore'); // auto-login for demo
      }
    }, 800);
  };

  return (
    <div className="min-h-screen bg-bg-base flex flex-col items-center justify-center px-4 relative">
      {/* Back Button */}
      <button
        onClick={() => navigate('/')}
        className="absolute top-6 left-6 flex items-center gap-1.5 text-sm text-text-secondary hover:text-text-primary transition-colors cursor-pointer"
      >
        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>arrow_back</span>
        Back
      </button>

      {/* Brand mark */}
      <div className="mb-8 text-center">
        <span className="text-2xl font-bold text-text-primary tracking-tight">⚡ Vaultx</span>
        <p className="text-sm text-text-secondary mt-1">Live auction & bidding platform</p>
      </div>

      {/* Card */}
      <div className="w-full max-w-[400px] card p-8">
        <h1 className="text-xl font-bold text-text-primary mb-1">Welcome back</h1>
        <p className="text-sm text-text-secondary mb-6">Sign in to your account to continue</p>

        {/* Error banner */}
        {error && (
          <div className="mb-4 flex items-start gap-2.5 p-3 bg-danger-light border border-danger/20 rounded-lg text-sm text-danger animate-fadeIn">
            <span className="material-symbols-outlined shrink-0" style={{ fontSize: '18px' }}>error</span>
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Email */}
          <div>
            <label className="input-label" htmlFor="email">Email</label>
            <div className="relative">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-text-muted pointer-events-none" style={{ fontSize: '18px' }}>
                mail
              </span>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                required
                className="input-field pl-9"
              />
            </div>
          </div>

          {/* Password */}
          <div>
            <label className="input-label" htmlFor="password">Password</label>
            <div className="relative">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-text-muted pointer-events-none" style={{ fontSize: '18px' }}>
                lock
              </span>
              <input
                id="password"
                type={showPw ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                required
                className="input-field pl-9 pr-10"
              />
              <button
                type="button"
                onClick={() => setShowPw((s) => !s)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-primary transition-colors duration-150"
              >
                <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>
                  {showPw ? 'visibility_off' : 'visibility'}
                </span>
              </button>
            </div>
          </div>

          {/* Submit */}
          <button
            id="login-btn"
            type="submit"
            disabled={loading}
            className="btn-primary w-full py-2.5 text-base mt-2"
          >
            {loading ? (
              <span className="flex items-center gap-2">
                <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                </svg>
                Signing in…
              </span>
            ) : (
              'Log In'
            )}
          </button>
        </form>

        <p className="text-sm text-center text-text-secondary mt-6">
          Don't have an account?{' '}
          <Link to="/register" className="text-primary font-medium hover:underline">
            Register
          </Link>
        </p>
      </div>

      <p className="text-xs text-text-muted mt-6">
        © {new Date().getFullYear()} Vaultx Inc. · Secured by bank-grade escrow
      </p>
    </div>
  );
}
