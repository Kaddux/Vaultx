import { MOCK_TRANSACTIONS, Transaction, formatCurrency, formatDate } from '../api';
import { TopNav } from '../components/TopNav';

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
              { label: 'Total Deposits', value: formatCurrency(17000), icon: 'add_card', color: 'text-success' },
              { label: 'Total Escrow', value: formatCurrency(4000), icon: 'lock', color: 'text-warning' },
              { label: 'Total Refunds', value: formatCurrency(1500), icon: 'currency_exchange', color: 'text-primary' },
              { label: 'Failed Txns', value: '1', icon: 'error', color: 'text-danger' },
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

          {/* Full transactions table */}
          <div className="card overflow-hidden">
            <div className="px-5 py-4 border-b border-border flex items-center justify-between">
              <h2 className="text-base font-semibold text-text-primary">All Transactions</h2>
              <button className="btn-secondary text-xs py-1.5 px-3">
                <span className="material-symbols-outlined" style={{ fontSize: '15px' }}>download</span>
                Export CSV
              </button>
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
                      <td className="text-text-secondary text-sm">{tx.description}</td>
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
