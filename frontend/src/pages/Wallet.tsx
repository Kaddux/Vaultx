import { useState, useEffect } from 'react';
import { TopNav } from '../components/TopNav';
import {
  api,
  mapTransaction,
  Transaction,
  formatCurrency,
  formatDate,
  ApiError,
  WalletResponse,
} from '../api';
import { useAuth } from '../context/AuthContext';

type KycState = 'PENDING' | 'VERIFIED' | 'UNVERIFIED';

function TransactionTypePill({ type }: { type: Transaction['type'] }) {
  const configs: Record<Transaction['type'], { cls: string; label: string }> = {
    DEPOSIT: { cls: 'pill-green', label: 'DEPOSIT' },
    ESCROW_HOLD: { cls: 'pill-amber', label: 'ESCROW HOLD' },
    ESCROW_RELEASE: { cls: 'pill-indigo', label: 'ESCROW RELEASE' },
    REFUND: { cls: 'pill-blue', label: 'REFUND' },
    WITHDRAWAL: { cls: 'pill-purple', label: 'WITHDRAWAL' },
    RESERVE: { cls: 'pill-amber', label: 'RESERVE' },
    RELEASE: { cls: 'pill-indigo', label: 'RELEASE' },
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
  const { user, refresh } = useAuth();

  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);

  const [depositAmount, setDepositAmount] = useState('');
  const [depositing, setDepositing] = useState(false);
  const [depositSuccess, setDepositSuccess] = useState(false);
  const [depositError, setDepositError] = useState('');

  const kycStatus = (user?.kycStatus ?? 'UNVERIFIED') as KycState;
  const [kycStep, setKycStep] = useState(1);
  const [docType, setDocType] = useState('Passport');
  const [fullName, setFullName] = useState(user?.fullName ?? '');
  const [address, setAddress] = useState('');
  const [uploadedDoc, setUploadedDoc] = useState<string | null>(null);
  const [uploadedSelfie, setUploadedSelfie] = useState<string | null>(null);
  const [scanProgress, setScanProgress] = useState(0);
  const [scanStatusMsg, setScanStatusMsg] = useState('');
  const [kycError, setKycError] = useState('');

  useEffect(() => {
    let cancelled = false;
    Promise.all([api.wallet.get(), api.transactions.list()])
      .then(([w, txns]) => {
        if (cancelled) return;
        setWallet(w);
        setTransactions(txns.map(mapTransaction));
      })
      .catch(() => {
        // context refresh will surface auth errors
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // Stripe Checkout redirects here with ?session_id=cs_... — confirm & refresh.
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const sid = params.get('session_id');
    if (!sid) return;
    api.payments
      .confirm(sid)
      .then(() =>
        Promise.all([api.wallet.get(), api.transactions.list()]).then(([w, txns]) => {
          setWallet(w);
          setTransactions(txns.map(mapTransaction));
        })
      )
      .catch(() => {});
    window.history.replaceState({}, '', '/wallet');
  }, [refresh]);

  const handleDeposit = async (e: React.FormEvent) => {
    e.preventDefault();
    const amount = Number(depositAmount);
    if (!depositAmount || amount <= 0) return;
    setDepositError('');
    setDepositing(true);
    try {
      const updated = await api.wallet.deposit(amount, crypto.randomUUID());
      setWallet(updated);
      setDepositSuccess(true);
      setDepositAmount('');
      await refresh();
      const txns = await api.transactions.list();
      setTransactions(txns.map(mapTransaction));
      setTimeout(() => setDepositSuccess(false), 3000);
    } catch (err) {
      setDepositError(err instanceof Error ? err.message : 'Deposit failed');
    } finally {
      setDepositing(false);
    }
  };

  const startVerificationScan = () => {
    setKycStep(4);
    setScanProgress(0);
    setScanStatusMsg('Scanning document layout...');

    let currentProgress = 0;
    const interval = setInterval(async () => {
      currentProgress += 5;
      setScanProgress(currentProgress);

      if (currentProgress <= 30) {
        setScanStatusMsg('Scanning document layout...');
      } else if (currentProgress <= 65) {
        setScanStatusMsg('Extracting OCR metadata...');
      } else if (currentProgress < 100) {
        setScanStatusMsg('Running face biometric match...');
      } else {
        clearInterval(interval);
        setScanStatusMsg('Submitting verification...');
        try {
          await api.users.submitKyc({
            docType,
            fullName,
            address,
            documentRef: uploadedDoc ?? undefined,
            selfieRef: uploadedSelfie ?? undefined,
          });
          await refresh();
          setScanStatusMsg('Verification submitted!');
        } catch (err) {
          setKycError(err instanceof ApiError ? err.message : 'KYC submission failed');
          setScanStatusMsg('Submission failed');
        }
      }
    }, 150);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-bg-base">
        <TopNav />
        <main className="pt-16 flex items-center justify-center min-h-[60vh]">
          <span className="text-text-muted text-sm">Loading wallet…</span>
        </main>
      </div>
    );
  }

  const balance = wallet?.balance ?? 0;
  const reservedBalance = wallet?.reservedBalance ?? 0;
  const availableBalance = wallet?.availableBalance ?? 0;

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
                <div className="flex-1">
                  <div className="text-xs text-text-secondary font-medium mb-1">Available</div>
                  <div className="text-3xl font-bold tabular-nums text-primary leading-none">
                    {formatCurrency(availableBalance)}
                  </div>
                  <div className="text-xs text-text-muted mt-1">Ready to use</div>
                </div>
                <div className="w-px h-14 bg-border shrink-0" />
                <div className="flex-1">
                  <div className="text-xs text-text-secondary font-medium mb-1 flex items-center gap-1">
                    <span className="material-symbols-outlined" style={{ fontSize: '13px' }}>lock</span>
                    Reserved
                  </div>
                  <div className="text-2xl font-semibold tabular-nums text-text-secondary leading-none">
                    {formatCurrency(reservedBalance)}
                  </div>
                  <div className="text-xs text-text-muted mt-1">Locked in active bids</div>
                </div>
              </div>
              <div className="mt-4 pt-4 border-t border-border">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-text-secondary">Total balance</span>
                  <span className="font-bold tabular-nums">{formatCurrency(balance)}</span>
                </div>
              </div>
            </div>

            {/* Deposit Form */}
            <div className="card p-6">
              <div className="text-xs font-semibold text-text-muted uppercase tracking-wider mb-4">Deposit Funds</div>

              {depositSuccess && (
                <div className="mb-4 flex items-center gap-2 p-3 bg-success-light border border-success/20 rounded-lg text-sm text-success animate-fadeIn">
                  <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>check_circle</span>
                  Deposit completed successfully
                </div>
              )}
              {depositError && (
                <div className="mb-4 flex items-start gap-2 p-3 bg-danger-light border border-danger/20 rounded-lg text-sm text-danger animate-fadeIn">
                  <span className="material-symbols-outlined shrink-0" style={{ fontSize: '16px' }}>error</span>
                  {depositError}
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
                      min="0.01"
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
            <div className="card p-6 flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-4 border-b border-border pb-2">
                  <div className="text-xs font-semibold text-text-muted uppercase tracking-wider">KYC Verification</div>
                  {kycStatus === 'VERIFIED' && (
                    <span className="pill-green text-[10px]">VERIFIED</span>
                  )}
                  {kycStatus === 'PENDING' && (
                    <span className="pill-amber text-[10px]">PENDING</span>
                  )}
                  {kycStatus === 'UNVERIFIED' && (
                    <span className="pill-gray text-[10px]">UNVERIFIED</span>
                  )}
                </div>

                {kycError && (
                  <div className="mb-4 p-3 bg-danger-light border border-danger/20 rounded-lg text-xs text-danger animate-fadeIn">
                    {kycError}
                  </div>
                )}

                {/* VERIFIED State */}
                {kycStatus === 'VERIFIED' && (
                  <div className="flex flex-col items-center text-center py-6 gap-3">
                    <div className="w-14 h-14 rounded-full bg-success-light flex items-center justify-center">
                      <span className="material-symbols-outlined text-success" style={{ fontSize: '32px' }}>verified</span>
                    </div>
                    <div>
                      <div className="text-sm font-bold text-success">Identity verified</div>
                      <div className="text-xs text-text-secondary mt-0.5">Your account is fully verified and in good standing</div>
                    </div>
                  </div>
                )}

                {/* PENDING + UNVERIFIED State (Interactive Wizard) */}
                {kycStatus !== 'VERIFIED' && (
                  <div className="space-y-4">
                    {/* Progress Dots */}
                    <div className="flex items-center justify-center gap-1.5 mb-4">
                      {[1, 2, 3, 4].map((s) => (
                        <div
                          key={s}
                          className={`h-1.5 rounded-full transition-all duration-300 ${
                            kycStep === s ? 'w-6 bg-primary' : 'w-1.5 bg-gray-200'
                          }`}
                        />
                      ))}
                    </div>

                    {/* Step 1: Personal Info */}
                    {kycStep === 1 && (
                      <div className="space-y-3 animate-fadeIn">
                        <h3 className="text-sm font-bold text-text-primary">Step 1: Personal Details</h3>
                        <div>
                          <label className="input-label" htmlFor="doc-type">Document Type</label>
                          <select
                            id="doc-type"
                            value={docType}
                            onChange={(e) => setDocType(e.target.value)}
                            className="input-field py-2 cursor-pointer"
                          >
                            <option value="Passport">Passport</option>
                            <option value="Driver License">Driver's License</option>
                            <option value="National ID">National ID Card</option>
                          </select>
                        </div>
                        <div>
                          <label className="input-label" htmlFor="kyc-fullname">Full Name</label>
                          <input
                            id="kyc-fullname"
                            type="text"
                            value={fullName}
                            onChange={(e) => setFullName(e.target.value)}
                            className="input-field py-2"
                            placeholder="Alex Morgan"
                          />
                        </div>
                        <div>
                          <label className="input-label" htmlFor="kyc-address">Residential Address</label>
                          <input
                            id="kyc-address"
                            type="text"
                            value={address}
                            onChange={(e) => setAddress(e.target.value)}
                            className="input-field py-2"
                            placeholder="123 Bidding Ave, Austin, TX"
                          />
                        </div>
                        <button
                          type="button"
                          onClick={() => setKycStep(2)}
                          className="btn-primary w-full py-2 text-xs font-semibold mt-2"
                        >
                          Continue to Upload
                        </button>
                      </div>
                    )}

                    {/* Step 2: Doc Upload */}
                    {kycStep === 2 && (
                      <div className="space-y-3 animate-fadeIn">
                        <div className="flex items-center gap-1">
                          <button onClick={() => setKycStep(1)} className="text-text-muted hover:text-text-primary">
                            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>arrow_back</span>
                          </button>
                          <h3 className="text-sm font-bold text-text-primary">Step 2: Upload {docType}</h3>
                        </div>

                        {!uploadedDoc ? (
                          <div
                            onClick={() => setUploadedDoc(`${docType.toLowerCase().replace(' ', '_')}_scan.jpg`)}
                            className="border-2 border-dashed border-border rounded-lg p-6 text-center cursor-pointer hover:border-primary transition-colors hover:bg-primary/[0.01]"
                          >
                            <span className="material-symbols-outlined text-text-muted text-3xl mb-2">upload_file</span>
                            <p className="text-xs font-semibold text-text-primary">Click to upload scan of {docType}</p>
                            <p className="text-[10px] text-text-muted mt-1">Supports PDF, PNG, JPG up to 10MB</p>
                          </div>
                        ) : (
                          <div className="border border-success/30 bg-success-light rounded-lg p-4 flex items-center justify-between">
                            <div className="flex items-center gap-2">
                              <span className="material-symbols-outlined text-success">check_circle</span>
                              <div className="text-left">
                                <p className="text-xs font-bold text-success truncate max-w-[150px]">{uploadedDoc}</p>
                                <p className="text-[10px] text-success/70">842 KB · Ready</p>
                              </div>
                            </div>
                            <button onClick={() => setUploadedDoc(null)} className="text-text-muted hover:text-danger">
                              <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>delete</span>
                            </button>
                          </div>
                        )}

                        <button
                          type="button"
                          disabled={!uploadedDoc}
                          onClick={() => setKycStep(3)}
                          className="btn-primary w-full py-2 text-xs font-semibold mt-2"
                        >
                          Continue to Selfie Scan
                        </button>
                      </div>
                    )}

                    {/* Step 3: Selfie Verification */}
                    {kycStep === 3 && (
                      <div className="space-y-3 animate-fadeIn">
                        <div className="flex items-center gap-1">
                          <button onClick={() => setKycStep(2)} className="text-text-muted hover:text-text-primary">
                            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>arrow_back</span>
                          </button>
                          <h3 className="text-sm font-bold text-text-primary">Step 3: Biometric Match</h3>
                        </div>

                        {!uploadedSelfie ? (
                          <div
                            onClick={() => setUploadedSelfie('selfie_capture.png')}
                            className="border-2 border-dashed border-border rounded-lg p-6 text-center cursor-pointer hover:border-primary transition-colors hover:bg-primary/[0.01]"
                          >
                            <span className="material-symbols-outlined text-text-muted text-3xl mb-2">face</span>
                            <p className="text-xs font-semibold text-text-primary">Click to simulate selfie capture</p>
                            <p className="text-[10px] text-text-muted mt-1">Make sure your face is clearly visible</p>
                          </div>
                        ) : (
                          <div className="border border-success/30 bg-success-light rounded-lg p-4 flex items-center justify-between">
                            <div className="flex items-center gap-2">
                              <span className="material-symbols-outlined text-success">check_circle</span>
                              <div className="text-left">
                                <p className="text-xs font-bold text-success truncate max-w-[150px]">{uploadedSelfie}</p>
                                <p className="text-[10px] text-success/70">412 KB · Ready</p>
                              </div>
                            </div>
                            <button onClick={() => setUploadedSelfie(null)} className="text-text-muted hover:text-danger">
                              <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>delete</span>
                            </button>
                          </div>
                        )}

                        <button
                          type="button"
                          disabled={!uploadedSelfie}
                          onClick={startVerificationScan}
                          className="btn-primary w-full py-2 text-xs font-semibold mt-2"
                        >
                          Submit For Verification
                        </button>
                      </div>
                    )}

                    {/* Step 4: Scanning simulator */}
                    {kycStep === 4 && (
                      <div className="space-y-4 py-4 text-center animate-fadeIn">
                        <div className="relative w-16 h-16 mx-auto mb-2 flex items-center justify-center">
                          <svg className="absolute inset-0 w-full h-full transform -rotate-90">
                            <circle cx="32" cy="32" r="28" stroke="#E5E7EB" strokeWidth="4" fill="transparent" />
                            <circle cx="32" cy="32" r="28" stroke="#4F46E5" strokeWidth="4" fill="transparent"
                              strokeDasharray={175}
                              strokeDashoffset={175 - (175 * scanProgress) / 100}
                              className="transition-all duration-150"
                            />
                          </svg>
                          <span className="text-sm font-bold text-primary font-mono tabular-nums">{scanProgress}%</span>
                        </div>
                        <div>
                          <p className="text-xs font-semibold text-text-primary">{scanStatusMsg}</p>
                          <p className="text-[10px] text-text-muted mt-0.5">Please do not close this window</p>
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Transactions Table */}
          <div className="card overflow-hidden">
            <div className="px-5 py-4 border-b border-border flex items-center justify-between">
              <h2 className="text-base font-semibold text-text-primary">Transaction History</h2>
              <span className="text-xs text-text-muted">{transactions.length} transactions</span>
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
                  {transactions.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="text-center py-8 text-text-muted text-sm">No transactions yet.</td>
                    </tr>
                  ) : (
                    transactions.map((tx) => (
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
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>

        </div>
      </main>
    </div>
  );
}