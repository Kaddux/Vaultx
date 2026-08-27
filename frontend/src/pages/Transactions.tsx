import { useState, useEffect } from 'react';
import { TopNav } from '../components/TopNav';
import { api, mapTransaction, Transaction, formatCurrency, formatDate } from '../api';

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

export function Transactions() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    api.transactions
      .list()
      .then((data) => {
        if (!cancelled) setTransactions(data.map(mapTransaction));
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load transactions');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const totals = transactions.reduce(
    (acc, tx) => {
      if (tx.type === 'DEPOSIT' && tx.status === 'COMPLETED') acc.deposits += tx.amount;
      if (tx.type === 'ESCROW_HOLD') acc.escrow += Math.abs(tx.amount);
      if (tx.type === 'REFUND' && tx.status === 'COMPLETED') acc.refunds += Math.abs(tx.amount);
      if (tx.status === 'FAILED') acc.failed += 1;
      return acc;
    },
    { deposits: 0, escrow: 0, refunds: 0, failed: 0 }
  );

  return (
    <div className="min-h-screen bg-bg-base">
      <TopNav />
      <main className="pt-16">
        <div className="max-w-[1280px] mx-auto px-6 py-8">
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-text-primary">Transaction History</h1>
            <p className="text-sm text-text-secondary mt-1">All your financial activity on Vaultx</p>
          </div>

          {/* Summary cards */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
            {[
              { label: 'Total Deposits', value: formatCurrency(totals.deposits), icon: 'add_card', color: 'text-success' },
              { label: 'Total Escrow', value: formatCurrency(totals.escrow), icon: 'lock', color: 'text-warning' },
              { label: 'Total Refunds', value: formatCurrency(totals.refunds), icon: 'currency_exchange', color: 'text-primary' },
              { label: 'Failed Txns', value: String(totals.failed), icon: 'error', color: 'text-danger' },
            ].map((s) => (
              <div key={s.label} className="card p-4 flex items-start gap-3">
                <span className={`material-symbols-outlined ${s.color}`} style={{ fontSize: '22px' }}>{s.icon}</span>
                <div>
                  <div className="text-xs text-text-muted font-medium">{s.label}</div>
                  <div className={`text-lg font-bold tabular-nums mt-0.5 ${s.color}`}>{s.value}</div>
                </div>
              </div>
            ))}
          </div>

          {error && (
            <div className="mb-4 p-4 bg-danger-light border border-danger/20 rounded-lg text-sm text-danger">
              {error}
            </div>
          )}

          {/* Full transactions table */}
          <div className="card overflow-hidden">
            <div className="px-5 py-4 border-b border-border flex items-center justify-between">
              <h2 className="text-base font-semibold text-text-primary">All Transactions</h2>
              <span className="text-xs text-text-muted">{loading ? 'Loading…' : `${transactions.length} transactions`}</span>
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
                  {loading ? (
                    <tr>
                      <td colSpan={6} className="text-center py-8 text-text-muted text-sm">Loading transactions…</td>
                    </tr>
                  ) : transactions.length === 0 ? (
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
                        <td className="text-text-secondary text-sm">{tx.description}</td>
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