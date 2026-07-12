import { useState } from 'react';
import { TopNav } from '../components/TopNav';
import { MOCK_USER, MOCK_TRANSACTIONS, Transaction, formatCurrency, formatDate } from '../api';

type KycState = 'PENDING' | 'VERIFIED' | 'REJECTED';

function TransactionTypePill({ type }: { type: Transaction['type'] }) {
  const configs: Record<Transaction['type'], { cls: string; label: string }> = {
    DEPOSIT: { cls: 'pill-green', label: 'DEPOSIT' },
    ESCROW_HOLD: { cls: 'pill-amber', label: 'ESCROW HOLD' },
    ESCROW_RELEASE: { cls: 'pill-indigo', label: 'ESCROW RELEASE' },
    REFUND: { cls: 'pill-blue', label: 'REFUND' },
    WITHDRAWAL: { cls: 'pill-purple', label: 'WITHDRAWAL' },
  };
  const { cls, label } = configs[type];
  return <span className={cls}>{label}</span>;
}

function StatusPill({ status }: { status: Transaction['status'] }) {
  if (status === 'COMPLETED') return <span className="pill-green">COMPLETED</span>;
  if (status === 'PENDING') return <span className="pill-amber">PENDING</span>;
  return <span className="pill-red">FAILED</span>;
}

export function Wallet() {
  const user = MOCK_USER;
  const [depositAmount, setDepositAmount] = useState('');
  const [depositing, setDepositing] = useState(false);
  const [depositSuccess, setDepositSuccess] = useState(false);
  const [kycState, setKycState] = useState<KycState>('VERIFIED');

  const handleDeposit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!depositAmount || Number(depositAmount) <= 0) return;
    setDepositing(true);
    setTimeout(() => {
      setDepositing(false);
      setDepositSuccess(true);
      setTimeout(() => setDepositSuccess(false), 3000);
      setDepositAmount('');
    }, 1000);
  };

  return (
    <div className="min-h-screen bg-bg-base">
      <TopNav />
      <main className="pt-16">
        <div className="max-w-[1280px] mx-auto px-6 py-8 space-y-6">

          {/* Page header */}
          <div>
            <h1 className="text-2xl font-bold text-text-primary">Wallet & Transactions</h1>
            <p className="text-sm text-text-secondary mt-1">Manage your funds and view transaction history</p>
          </div>

          {/* Top grid: balances + deposit + kyc */}
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">

            {/* Balance Card */}
            <div className="card p-6 xl:col-span-1">
              <div className="text-xs font-semibold text-text-muted uppercase tracking-wider mb-4">Account Balances</div>
              <div className="flex items-end gap-6">
                {/* Available */}
                <div className="flex-1">
                  <div className="text-xs text-text-secondary font-medium mb-1">Available</div>
                  <div className="text-3xl font-bold tabular-nums text-primary leading-none">
                    {formatCurrency(user.balance)}
                  </div>
                  <div className="text-xs text-text-muted mt-1">Ready to use</div>
                </div>
                <div className="w-px h-14 bg-border shrink-0" />
                {/* Reserved */}
                <div className="flex-1">
                  <div className="text-xs text-text-secondary font-medium mb-1 flex items-center gap-1">
                    <span className="material-symbols-outlined" style={{ fontSize: '13px' }}>lock</span>
                    Reserved
                  </div>
                  <div className="text-2xl font-semibold tabular-nums text-text-secondary leading-none">
                    {formatCurrency(user.reservedBalance)}
                  </div>
                  <div className="text-xs text-text-muted mt-1">Locked in active bids</div>
                </div>
              </div>
              <div className="mt-4 pt-4 border-t border-border">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-text-secondary">Total balance</span>
                  <span className="font-bold tabular-nums">{formatCurrency(user.balance + user.reservedBalance)}</span>
                </div>
              </div>
            </div>

            {/* Deposit Form */}
            <div className="card p-6">
              <div className="text-xs font-semibold text-text-muted uppercase tracking-wider mb-4">Deposit Funds</div>

              {depositSuccess && (
                <div className="mb-4 flex items-center gap-2 p-3 bg-success-light border border-success/20 rounded-lg text-sm text-success animate-fadeIn">
                  <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>check_circle</span>
                  Deposit initiated successfully
                </div>
              )}

              <form onSubmit={handleDeposit} className="space-y-3">
                <div>
                  <label className="input-label" htmlFor="deposit-amount">Amount (USD)</label>
                  <div className="relative">
                    <span className="absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary font-medium text-sm">$</span>
                    <input
                      id="deposit-amount"
                      type="number"
                      min="1"
                      step="0.01"
                      value={depositAmount}
                      onChange={(e) => setDepositAmount(e.target.value)}
                      placeholder="0.00"
                      className="input-field pl-7 tabular-nums"
                    />
                  </div>
                </div>
                <div className="flex gap-2">
                  {[100, 500, 1000, 5000].map((amt) => (
                    <button
                      key={amt}
                      type="button"
                      onClick={() => setDepositAmount(String(amt))}
                      className="flex-1 text-xs py-1.5 border border-border rounded-md hover:border-primary hover:text-primary transition-colors duration-150 font-medium"
                    >
                      ${amt >= 1000 ? `${amt / 1000}k` : amt}
                    </button>
                  ))}
                </div>
                <button
                  id="deposit-btn"
                  type="submit"
                  disabled={depositing || !depositAmount}
                  className="btn-primary w-full"
                >
                  {depositing ? (
                    <span className="flex items-center gap-2">
                      <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                      </svg>
                      Processing…
                    </span>
                  ) : (
                    <>
                      <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>add_card</span>
                      Deposit Funds
                    </>
                  )}
                </button>
              </form>
            </div>

            {/* KYC Panel */}
            <div className="card p-6">
              <div className="flex items-center justify-between mb-4">
                <div className="text-xs font-semibold text-text-muted uppercase tracking-wider">KYC Verification</div>
                {/* State switcher for demo */}
                <div className="flex items-center gap-1 bg-gray-100 rounded-full p-0.5">
                  {(['VERIFIED', 'PENDING', 'REJECTED'] as KycState[]).map((s) => (
                    <button
                      key={s}
                      onClick={() => setKycState(s)}
                      className={`px-2 py-0.5 text-xs font-medium rounded-full transition-all duration-150 ${
                        kycState === s ? 'bg-white shadow-card text-text-primary' : 'text-text-muted'
                      }`}
                    >
                      {s.charAt(0)}
                    </button>
                  ))}
                </div>
              </div>

              {kycState === 'VERIFIED' && (
                <div className="flex flex-col items-center text-center py-4 gap-3">
                  <div className="w-14 h-14 rounded-full bg-success-light flex items-center justify-center">
                    <span className="material-symbols-outlined text-success" style={{ fontSize: '32px' }}>verified</span>
                  </div>
                  <div>
                    <div className="text-sm font-bold text-success">Identity verified</div>
                    <div className="text-xs text-text-secondary mt-0.5">Your account is fully verified and in good standing</div>
                  </div>
                  <span className="pill-green">VERIFIED</span>
                </div>
              )}

              {kycState === 'PENDING' && (
                <div className="flex flex-col items-center text-center py-4 gap-3">
                  <div className="w-14 h-14 rounded-full bg-warning-light flex items-center justify-center">
                    <svg className="animate-spin h-8 w-8 text-warning" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                    </svg>
                  </div>
                  <div>
                    <div className="text-sm font-bold text-warning">Verification in progress</div>
                    <div className="text-xs text-text-secondary mt-0.5">We're reviewing your documents — typically takes 1–2 business days</div>
                  </div>
                  <span className="pill-amber">PENDING</span>
                </div>
              )}

              {kycState === 'REJECTED' && (
                <div className="flex flex-col items-center text-center py-4 gap-3">
                  <div className="w-14 h-14 rounded-full bg-danger-light flex items-center justify-center">
                    <span className="material-symbols-outlined text-danger" style={{ fontSize: '32px' }}>cancel</span>
                  </div>
                  <div>
                    <div className="text-sm font-bold text-danger">Verification failed</div>
                    <div className="text-xs text-text-secondary mt-0.5">Verification failed — resubmit documents to continue bidding</div>
                  </div>
                  <span className="pill-red">REJECTED</span>
                  <button className="btn-danger text-xs px-3 py-1.5 mt-1">
                    <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>upload</span>
                    Resubmit Documents
                  </button>
                </div>
              )}
            </div>
          </div>

          {/* Transactions Table */}
          <div className="card overflow-hidden">
            <div className="px-5 py-4 border-b border-border flex items-center justify-between">
              <h2 className="text-base font-semibold text-text-primary">Transaction History</h2>
              <span className="text-xs text-text-muted">{MOCK_TRANSACTIONS.length} transactions</span>
            </div>
            <div className="overflow-x-auto">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Transaction ID</th>
                    <th>Type</th>
                    <th className="text-right">Amount</th>
                    <th>Status</th>
                    <th>Description</th>
                  </tr>
                </thead>
                <tbody>
                  {MOCK_TRANSACTIONS.map((tx) => (
                    <tr key={tx.id}>
                      <td className="text-text-secondary whitespace-nowrap">{formatDate(tx.date)}</td>
                      <td>
                        <span className="font-mono text-xs text-text-secondary">{tx.id}</span>
                      </td>
                      <td><TransactionTypePill type={tx.type} /></td>
                      <td className={`text-right font-bold tabular-nums ${tx.amount < 0 ? 'text-danger' : 'text-success'}`}>
                        {tx.amount > 0 ? '+' : ''}{formatCurrency(tx.amount)}
                      </td>
                      <td><StatusPill status={tx.status} /></td>
                      <td className="text-text-secondary">{tx.description}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

        </div>
      </main>
    </div>
  );
}
