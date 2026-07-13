import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

interface StrengthCheck {
  label: string;
  test: (pw: string) => boolean;
}

const pwChecks: StrengthCheck[] = [
  { label: '8+ characters', test: (pw) => pw.length >= 8 },
  { label: '1 uppercase letter', test: (pw) => /[A-Z]/.test(pw) },
  { label: '1 number', test: (pw) => /[0-9]/.test(pw) },
  { label: '1 special character', test: (pw) => /[^A-Za-z0-9]/.test(pw) },
];

export function Register() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: '', email: '', fullName: '', password: '' });
  const [showPw, setShowPw] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [showKycModal, setShowKycModal] = useState(false);

  const update = (field: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm((f) => ({ ...f, [field]: e.target.value }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      if (form.email === 'taken@vaultx.io') {
        setError('409: Email or username already taken.');
      } else {
        localStorage.setItem('vaultx_logged_in', 'true');
        setSuccess(true);
        setShowKycModal(true);
      }
    }, 900);
  };

  const allPassed = pwChecks.every((c) => c.test(form.password));

  return (
    <div className="min-h-screen bg-bg-base flex flex-col items-center justify-center px-4">
      {/* Success Toast */}
      {success && (
        <div className="fixed top-5 right-5 flex items-center gap-2.5 bg-success text-white text-sm font-medium px-4 py-3 rounded-lg shadow-card-hover animate-fadeIn z-50">
          <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>check_circle</span>
          Account created!
        </div>
      )}

      {/* KYC Modal */}
      {showKycModal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 overlay-fade px-4">
          <div className="card p-8 w-full max-w-md modal-slide text-center">
            <div className="w-12 h-12 rounded-full bg-warning-light flex items-center justify-center mx-auto mb-4">
              <span className="material-symbols-outlined text-warning" style={{ fontSize: '28px' }}>id_card</span>
            </div>
            <h2 className="text-lg font-bold text-text-primary mb-2">Verify your identity</h2>
            <p className="text-sm text-text-secondary mb-6 leading-relaxed">
              Vaultx requires KYC verification to participate in auctions. You can complete this now or later in your profile.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => { setShowKycModal(false); navigate('/explore'); }}
                className="btn-secondary flex-1"
              >
                Do it later
              </button>
              <button
                onClick={() => { setShowKycModal(false); navigate('/wallet'); }}
                className="btn-primary flex-1"
              >
                Start KYC
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Brand */}
      <div className="mb-8 text-center">
        <span className="text-2xl font-bold text-text-primary tracking-tight">⚡ Vaultx</span>
        <p className="text-sm text-text-secondary mt-1">Create your account</p>
      </div>

      <div className="w-full max-w-[440px] card p-8">
        <h1 className="text-xl font-bold text-text-primary mb-1">Create account</h1>
        <p className="text-sm text-text-secondary mb-6">Join thousands of buyers and sellers on Vaultx</p>

        {error && (
          <div className="mb-4 flex items-start gap-2.5 p-3 bg-danger-light border border-danger/20 rounded-lg text-sm text-danger animate-fadeIn">
            <span className="material-symbols-outlined shrink-0" style={{ fontSize: '18px' }}>error</span>
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Username */}
          <div>
            <label className="input-label" htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              value={form.username}
              onChange={update('username')}
              placeholder="your_handle"
              required
              className="input-field"
            />
          </div>

          {/* Email */}
          <div>
            <label className="input-label" htmlFor="reg-email">Email</label>
            <input
              id="reg-email"
              type="email"
              value={form.email}
              onChange={update('email')}
              placeholder="you@example.com"
              required
              className="input-field"
            />
          </div>

          {/* Full Name */}
          <div>
            <label className="input-label" htmlFor="fullName">Full Name</label>
            <input
              id="fullName"
              type="text"
              value={form.fullName}
              onChange={update('fullName')}
              placeholder="Alex Morgan"
              required
              className="input-field"
            />
          </div>

          {/* Password */}
          <div>
            <label className="input-label" htmlFor="reg-password">Password</label>
            <div className="relative">
              <input
                id="reg-password"
                type={showPw ? 'text' : 'password'}
                value={form.password}
                onChange={update('password')}
                placeholder="••••••••"
                required
                className="input-field pr-10"
              />
              <button
                type="button"
                onClick={() => setShowPw((s) => !s)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-primary"
              >
                <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>
                  {showPw ? 'visibility_off' : 'visibility'}
                </span>
              </button>
            </div>

            {/* Strength checklist */}
            {form.password.length > 0 && (
              <div className="mt-3 space-y-1.5">
                {pwChecks.map((check) => {
                  const passed = check.test(form.password);
                  return (
                    <div key={check.label} className="flex items-center gap-2 text-xs">
                      <span
                        className={`material-symbols-outlined transition-colors duration-200 ${passed ? 'text-success' : 'text-text-muted'}`}
                        style={{ fontSize: '14px' }}
                      >
                        {passed ? 'check_circle' : 'radio_button_unchecked'}
                      </span>
                      <span className={passed ? 'text-success font-medium' : 'text-text-secondary'}>
                        {check.label}
                      </span>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          <button
            id="register-btn"
            type="submit"
            disabled={loading || !allPassed}
            className="btn-primary w-full py-2.5 text-base mt-2"
          >
            {loading ? (
              <span className="flex items-center gap-2">
                <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                </svg>
                Creating account…
              </span>
            ) : (
              'Create Account'
            )}
          </button>
        </form>

        <p className="text-sm text-center text-text-secondary mt-6">
          Already have an account?{' '}
          <Link to="/login" className="text-primary font-medium hover:underline">
            Log In
          </Link>
        </p>
      </div>
    </div>
  );
}
