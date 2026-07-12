import { useState } from 'react';

export default function App() {
  const [balance] = useState(1500.0);
  const [reserved] = useState(300.0);
  const [auctions, setAuctions] = useState([
    { id: '1', title: 'Omega Seamaster 1960s Watch', bid: 520, time: '1h 24m' },
    { id: '2', title: 'Autographed MJ Jersey (1998)', bid: 1450, time: '4h 12m' },
  ]);

  const placeBid = (id: string) => {
    setAuctions(auctions.map(a => a.id === id ? { ...a, bid: a.bid + 10 } : a));
  };

  return (
    <div className="app-container">
      <header className="glass-panel header">
        <div className="logo">⚡ <span className="gradient-text">Vaultx</span></div>
        <div className="wallet-badge">
          <span>Wallet: <strong className="text-success">${balance}</strong></span>
          <span>Reserved: <strong className="text-warning">${reserved}</strong></span>
        </div>
      </header>

      <main className="main-content">
        <section className="hero">
          <span className="badge">Real-Time Bidding</span>
          <h2>Bid Live, <span className="gradient-text">Secure Instantly</span></h2>
        </section>

        <section className="auctions-grid">
          {auctions.map(a => (
            <div key={a.id} className="glass-panel auction-card">
              <div className="card-header">
                <span>Active</span>
                <span className="timer">{a.time} remaining</span>
              </div>
              <h3>{a.title}</h3>
              <div className="card-footer">
                <div>
                  <small>CURRENT BID</small>
                  <h4>${a.bid}</h4>
                </div>
                <button className="btn-primary" onClick={() => placeBid(a.id)}>Bid +$10</button>
              </div>
            </div>
          ))}
        </section>
      </main>
    </div>
  );
}
