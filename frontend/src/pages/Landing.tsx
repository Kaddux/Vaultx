import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';

/* ─────────────────────────── helpers ─────────────────────────── */
function useCountdown(target: Date) {
  const [timeLeft, setTimeLeft] = useState('');
  useEffect(() => {
    const tick = () => {
      const diff = Math.max(0, target.getTime() - Date.now());
      const h = Math.floor(diff / 3600000);
      const m = Math.floor((diff % 3600000) / 60000);
      const s = Math.floor((diff % 60000) / 1000);
      setTimeLeft(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`);
    };
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, [target]);
  return timeLeft;
}

const AUCTION_END = new Date(Date.now() + 4 * 3600000 + 23 * 60000 + 14000);

/* ─────────────────────────── Navbar ─────────────────────────── */
function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  return (
    <header
      className={`fixed top-0 inset-x-0 z-50 h-16 flex items-center transition-all duration-300 ${
        scrolled ? 'bg-white/95 backdrop-blur-md shadow-sm border-b border-gray-100' : 'bg-white border-b border-gray-100'
      }`}
    >
      <div className="max-w-7xl mx-auto px-6 w-full flex items-center justify-between">
        {/* Logo */}
        <Link to="/" className="flex items-center gap-2 font-bold text-lg text-gray-900 tracking-tight">
          <span className="text-primary text-xl">⚡</span>
          <span>Vaultx</span>
        </Link>

        {/* Nav links desktop */}
        <nav className="hidden md:flex items-center gap-8">
          <a href="#features" className="text-sm text-gray-600 hover:text-gray-900 font-medium transition-colors">Features</a>
          <a href="#assets"   className="text-sm text-gray-600 hover:text-gray-900 font-medium transition-colors">Explore Assets</a>
          <a href="#pricing"  className="text-sm text-gray-600 hover:text-gray-900 font-medium transition-colors">Pricing</a>
        </nav>

        {/* Right actions */}
        <div className="hidden md:flex items-center gap-3">
          <span className="flex items-center gap-1.5 text-xs font-semibold text-success bg-success-light px-2.5 py-1 rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-success animate-pulse inline-block" />
            4,312 Users Online
          </span>
          <Link to="/login"    className="text-sm font-medium text-gray-600 hover:text-gray-900 transition-colors">Sign In</Link>
          <Link to="/register" className="btn-primary text-sm px-4 py-2">Get Started</Link>
        </div>

        {/* Hamburger mobile */}
        <button className="md:hidden p-2 rounded-md text-gray-600" onClick={() => setMenuOpen(v => !v)}>
          <span className="material-symbols-outlined">{menuOpen ? 'close' : 'menu'}</span>
        </button>
      </div>

      {menuOpen && (
        <div className="absolute top-16 inset-x-0 bg-white border-b border-gray-100 px-6 py-4 flex flex-col gap-4 md:hidden shadow-lg">
          <a href="#features" className="text-sm text-gray-700 font-medium" onClick={() => setMenuOpen(false)}>Features</a>
          <a href="#assets"   className="text-sm text-gray-700 font-medium" onClick={() => setMenuOpen(false)}>Explore Assets</a>
          <a href="#pricing"  className="text-sm text-gray-700 font-medium" onClick={() => setMenuOpen(false)}>Pricing</a>
          <Link to="/login"    className="text-sm text-gray-700 font-medium">Sign In</Link>
          <Link to="/register" className="btn-primary text-sm text-center">Get Started</Link>
        </div>
      )}
    </header>
  );
}

/* ─────────────────────────── Hero auction card ─────────────────────────── */
function HeroAuctionCard() {
  const navigate = useNavigate();
  const timer = useCountdown(AUCTION_END);

  return (
    <div className="bg-white rounded-2xl border border-gray-200 shadow-[0_8px_40px_rgba(0,0,0,0.10)] overflow-hidden w-full">
      {/* Browser chrome */}
      <div className="flex items-center gap-1.5 px-4 py-3 bg-gray-50 border-b border-gray-100">
        <span className="w-3 h-3 rounded-full bg-red-400" />
        <span className="w-3 h-3 rounded-full bg-yellow-400" />
        <span className="w-3 h-3 rounded-full bg-green-400" />
        <span className="flex-1 mx-3 h-6 bg-white border border-gray-200 rounded text-xs text-gray-400 flex items-center px-2">
          app.vaultx.io/auction/247
        </span>
      </div>

      <div className="p-5">
        <div className="flex items-start justify-between mb-4">
          <div>
            <p className="text-xs text-gray-400 mb-0.5 font-medium">LIVE AUCTION · #247</p>
            <h3 className="text-sm font-semibold text-gray-900 leading-snug">Rare Heritage Patek Philippe<br />Aquanaut Ref. 5168G</h3>
          </div>
          <span className="pill-red text-xs">LIVE</span>
        </div>

        {/* Placeholder image */}
        <div className="relative w-full h-32 bg-gradient-to-br from-gray-100 to-gray-200 rounded-lg mb-4 flex items-center justify-center overflow-hidden">
          <span className="material-symbols-outlined text-4xl text-gray-300">watch</span>
          <div className="absolute inset-0 bg-gradient-to-t from-gray-900/10 to-transparent" />
        </div>

        {/* Live activity */}
        <div className="mb-4 space-y-1.5">
          <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">Live Activity</p>
          {[
            { user: 'j***e', amount: '$239,000', delta: '+$1,000', winner: true },
            { user: 'r***s', amount: '$238,000', delta: '+$500',   winner: false },
          ].map((bid, i) => (
            <div
              key={i}
              className={`flex items-center justify-between px-3 py-2 rounded-lg text-xs ${
                i === 0 ? 'bg-success-light border border-success/20' : 'bg-gray-50'
              }`}
            >
              <span className="font-mono text-gray-600">{bid.user}</span>
              <div className="flex items-center gap-2">
                <span className="font-bold text-gray-900 tabular-nums">{bid.amount}</span>
                <span className={`text-xs font-semibold ${i === 0 ? 'text-success' : 'text-gray-400'}`}>{bid.delta}</span>
                {i === 0 && <span className="w-2 h-2 rounded-full bg-success" />}
              </div>
            </div>
          ))}
        </div>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-3 mb-4">
          {[
            { label: 'Current Bid', value: '$239,000', accent: true },
            { label: 'Total Bids',  value: '12 Active', accent: false },
            { label: 'Ends In',     value: timer,       accent: false },
          ].map(({ label, value, accent }) => (
            <div key={label} className="text-center bg-gray-50 rounded-lg py-2.5 px-1">
              <p className={`text-sm font-bold tabular-nums ${accent ? 'text-primary' : 'text-gray-900'}`}>{value}</p>
              <p className="text-xs text-gray-400 mt-0.5">{label}</p>
            </div>
          ))}
        </div>

        <div className="flex gap-2">
          <button onClick={() => navigate('/login')} className="btn-primary flex-1 text-sm py-2.5">Place Bid</button>
          <button onClick={() => navigate('/login')} className="btn-secondary text-sm px-4 py-2.5">Watch</button>
        </div>
      </div>
    </div>
  );
}

/* ─────────────────────────── Trusted by ─────────────────────────── */
const LOGOS = ["Sotheby's", "Christie's", 'Bonhams', 'Phillips', 'The Plaza', 'Heritage'];

function TrustedBy() {
  return (
    <div className="border-y border-gray-100 py-8 bg-white">
      <div className="max-w-7xl mx-auto px-6">
        <p className="text-center text-xs font-semibold text-gray-400 uppercase tracking-widest mb-6">
          TRUSTED BY GLOBAL LEADERS
        </p>
        <div className="flex flex-wrap items-center justify-center gap-x-12 gap-y-4">
          {LOGOS.map(name => (
            <span key={name} className="text-sm font-semibold text-gray-300 tracking-wide hover:text-gray-400 transition-colors">
              {name}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}

/* ─────────────────────────── Features ─────────────────────────── */
const FEATURES = [
  {
    icon: 'timer',
    color: 'text-primary bg-primary-light',
    title: 'Real-Time Bidding',
    desc: 'Sub-second latency updates ensure you never miss a bid. Our platform refreshes your offers in under 70 ms, giving you the competitive edge.',
  },
  {
    icon: 'security',
    color: 'text-success bg-success-light',
    title: 'Escrow-Powered',
    desc: 'Funds are held securely in escrow and only released upon mutual confirmation — protecting both buyers and sellers throughout the process.',
  },
  {
    icon: 'verified_user',
    color: 'text-warning bg-warning-light',
    title: 'KYC Verified',
    desc: 'Every participant undergoes rigorous identity and valuation process, ensuring every bidder is a validated and verified entity.',
  },
];

function FeaturesSection() {
  return (
    <section id="features" className="py-24 bg-white">
      <div className="max-w-7xl mx-auto px-6">
        <div className="text-center max-w-2xl mx-auto mb-14">
          <p className="text-xs font-semibold text-primary uppercase tracking-widest mb-3">Why Vaultx</p>
          <h2 className="text-3xl font-bold text-gray-900 tracking-tight mb-4">Precision-Engineered Bidding</h2>
          <p className="text-gray-500 text-base leading-relaxed">
            Every component-driven feature on the Vaultx site has been thoughtfully developed and has the most powerful technologies behind it.
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-6">
          {FEATURES.map(({ icon, color, title, desc }) => (
            <div key={title} className="card-hover p-6 rounded-xl">
              <div className={`w-10 h-10 rounded-lg flex items-center justify-center mb-4 ${color}`}>
                <span className="material-symbols-outlined text-lg">{icon}</span>
              </div>
              <h3 className="text-base font-semibold text-gray-900 mb-2">{title}</h3>
              <p className="text-sm text-gray-500 leading-relaxed">{desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ─────────────────────────── Assets ─────────────────────────── */
const ASSETS = [
  {
    tag: 'LIVE', tagColor: 'pill-red',
    title: 'Ferrari 250 GT (1961)', seller: '@rare_motors',
    bid: '$3,450,000', bidLabel: 'Current Bid',
    gradient: 'from-slate-800 to-slate-600', icon: 'directions_car',
    featured: false,
  },
  {
    tag: 'HOT', tagColor: 'pill-amber',
    title: 'Banksy Rock An ATM',    seller: '@street_art_co',
    bid: 'from $10,500',            bidLabel: 'Starting',
    gradient: 'from-zinc-700 to-zinc-500', icon: 'palette',
    featured: true,
  },
  {
    tag: 'LIVE', tagColor: 'pill-red',
    title: 'Penthouse, Skyline West', seller: '@elite_realty',
    bid: '$4,900,000',               bidLabel: 'Current Bid',
    gradient: 'from-amber-800 to-amber-600', icon: 'apartment',
    featured: false,
  },
];

function AssetsSection() {
  return (
    <section id="assets" className="py-24 bg-gray-50">
      <div className="max-w-7xl mx-auto px-6">
        <div className="flex items-end justify-between mb-10">
          <div>
            <p className="text-xs font-semibold text-primary uppercase tracking-widest mb-2">Live Auctions</p>
            <h2 className="text-3xl font-bold text-gray-900 tracking-tight">Institutional-Grade Assets</h2>
            <p className="text-gray-500 text-sm mt-2 max-w-lg">
              From blue-chip art to supercars and prime real estate — bid on assets that matter.
            </p>
          </div>
          <Link to="/explore" className="hidden md:flex items-center gap-1 text-sm font-semibold text-primary hover:text-primary-hover transition-colors">
            Explore all listings
            <span className="material-symbols-outlined text-base">arrow_forward</span>
          </Link>
        </div>

        <div className="grid md:grid-cols-3 gap-5">
          {ASSETS.map(({ tag, tagColor, title, seller, bid, bidLabel, gradient, icon, featured }) => (
            <div key={title} className={`card-hover rounded-xl overflow-hidden ${featured ? 'ring-2 ring-primary/30' : ''}`}>
              {featured && (
                <div className="bg-primary text-white text-xs font-bold text-center py-1.5 tracking-wide">FEATURED AUCTION</div>
              )}
              <div className={`h-40 bg-gradient-to-br ${gradient} relative flex items-center justify-center`}>
                <span className="material-symbols-outlined text-5xl text-white/30">{icon}</span>
                <div className="absolute top-3 left-3">
                  <span className={tagColor}>{tag}</span>
                </div>
              </div>
              <div className="p-4">
                <h3 className="text-sm font-semibold text-gray-900 mb-0.5">{title}</h3>
                <p className="text-xs text-gray-400 mb-3">{seller}</p>
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-xs text-gray-400">{bidLabel}</p>
                    <p className="text-base font-bold text-gray-900 tabular-nums">{bid}</p>
                  </div>
                  <Link to="/explore" className="btn-primary text-xs px-3 py-2">View Bid</Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ─────────────────────────── Pricing ─────────────────────────── */
const PLANS = [
  {
    name: 'Starter', price: '$0', period: '/ month', sub: 'per seller',
    perks: ['2.5% Buyer Premium', '3 Active Auctions', 'Basic Analytics', 'Standard KYC Process'],
    cta: 'Get Started Free', ctaStyle: 'btn-secondary', featured: false,
  },
  {
    name: 'Pro', price: '$499', period: '/ month', sub: 'billed annually',
    perks: ['1.5% Buyer Premium', 'Unlimited Auctions', 'Advanced Analytics', 'Priority KYC Review', 'Dedicated Account Mgr'],
    cta: 'Go Pro Today', ctaStyle: 'btn-primary', featured: true, badge: 'MOST POPULAR',
  },
  {
    name: 'Enterprise', price: 'Custom', period: '', sub: 'contact sales',
    perks: ['Negotiated Buyer Premium', 'White-label Solution', 'Custom Integrations', 'Priority Support (24/7)'],
    cta: 'Contact Sales', ctaStyle: 'btn-secondary', featured: false,
  },
];

function PricingSection() {
  return (
    <section id="pricing" className="py-24 bg-white">
      <div className="max-w-7xl mx-auto px-6">
        <div className="text-center max-w-2xl mx-auto mb-14">
          <p className="text-xs font-semibold text-primary uppercase tracking-widest mb-3">Pricing</p>
          <h2 className="text-3xl font-bold text-gray-900 tracking-tight mb-4">Transparent Pricing</h2>
          <p className="text-gray-500 text-base leading-relaxed">
            Grow your bidding activity with plans designed for every scale of participation.
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-6 items-start">
          {PLANS.map(({ name, price, period, sub, perks, cta, ctaStyle, featured, badge }) => (
            <div
              key={name}
              className={`rounded-xl border p-6 relative transition-all duration-200 ${
                featured
                  ? 'border-primary shadow-[0_0_0_4px_rgba(79,70,229,0.10)] bg-white'
                  : 'border-gray-200 bg-white hover:border-gray-300 hover:shadow-card'
              }`}
            >
              {badge && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2">
                  <span className="bg-primary text-white text-xs font-bold px-3 py-1 rounded-full tracking-wide">{badge}</span>
                </div>
              )}
              <p className="text-xs font-semibold text-gray-400 uppercase tracking-widest mb-3">{name}</p>
              <div className="flex items-baseline gap-1 mb-1">
                <span className="text-3xl font-bold text-gray-900 tabular-nums">{price}</span>
                {period && <span className="text-sm text-gray-400">{period}</span>}
              </div>
              <p className="text-xs text-gray-400 mb-6">{sub}</p>
              <ul className="space-y-2.5 mb-8">
                {perks.map(p => (
                  <li key={p} className="flex items-center gap-2 text-sm text-gray-600">
                    <span className="material-symbols-outlined text-success" style={{ fontSize: 16 }}>check_circle</span>
                    {p}
                  </li>
                ))}
              </ul>
              <Link to="/register" className={`${ctaStyle} w-full text-sm text-center block`}>{cta}</Link>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ─────────────────────────── CTA Banner ─────────────────────────── */
function CtaBanner() {
  return (
    <section className="py-20 bg-gray-950">
      <div className="max-w-7xl mx-auto px-6 text-center">
        <p className="text-xs font-semibold text-gray-500 uppercase tracking-widest mb-4">
          Join 24,000+ traders · Collective bidding experience worth $4B
        </p>
        <h2 className="text-3xl md:text-4xl font-bold text-white tracking-tight mb-8">
          Join the elite bidding community today.
        </h2>
        <Link to="/register" className="btn-primary text-base px-8 py-3">Create Account</Link>
      </div>
    </section>
  );
}

/* ─────────────────────────── Footer ─────────────────────────── */
const FOOTER_LINKS: Record<string, string[]> = {
  Product: ['Auctions', 'Seller Portal', 'Wallet', 'Transactions'],
  Company: ['About Us', 'Careers', 'Press', 'Blog'],
  Legal:   ['Privacy Policy', 'Terms of Use', 'Cookie Policy', 'KYC Policy'],
};

function Footer() {
  return (
    <footer className="bg-gray-950 border-t border-gray-800">
      <div className="max-w-7xl mx-auto px-6 py-12">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8 mb-10">
          <div>
            <div className="flex items-center gap-2 mb-4">
              <span className="text-primary text-xl">⚡</span>
              <span className="text-white font-bold text-base">Vaultx</span>
            </div>
            <p className="text-xs text-gray-500 leading-relaxed max-w-48">
              The professional live auction platform for high-value assets. Secure, fast, trusted.
            </p>
            <div className="flex items-center gap-3 mt-4">
              {['alternate_email', 'language', 'chat'].map(icon => (
                <button key={icon} className="w-8 h-8 rounded-lg bg-gray-800 hover:bg-gray-700 flex items-center justify-center transition-colors">
                  <span className="material-symbols-outlined text-gray-400" style={{ fontSize: 16 }}>{icon}</span>
                </button>
              ))}
            </div>
          </div>

          {Object.entries(FOOTER_LINKS).map(([section, links]) => (
            <div key={section}>
              <p className="text-xs font-semibold text-gray-400 uppercase tracking-widest mb-4">{section}</p>
              <ul className="space-y-2.5">
                {links.map(link => (
                  <li key={link}>
                    <a href="#" className="text-sm text-gray-500 hover:text-gray-300 transition-colors">{link}</a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="border-t border-gray-800 pt-6 flex flex-col md:flex-row items-center justify-between gap-4">
          <p className="text-xs text-gray-600">© 2026 Vaultx Technologies, Inc. All rights reserved.</p>
          <p className="text-xs text-gray-600">Regulated · Secure · Escrow-Protected</p>
        </div>
      </div>
    </footer>
  );
}

/* ─────────────────────────── Page ─────────────────────────── */
export function Landing() {
  const _heroRef = useRef<HTMLDivElement>(null);

  return (
    <div className="min-h-screen bg-white font-sans">
      <Navbar />

      {/* ── Hero ── */}
      <section ref={_heroRef} className="pt-32 pb-20 bg-white relative overflow-hidden">
        {/* Grid bg */}
        <div
          className="absolute inset-0 pointer-events-none opacity-[0.03]"
          style={{
            backgroundImage: 'linear-gradient(#111827 1px,transparent 1px),linear-gradient(90deg,#111827 1px,transparent 1px)',
            backgroundSize: '48px 48px',
          }}
        />
        {/* Glow */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[600px] h-[400px] bg-primary/5 rounded-full blur-3xl pointer-events-none" />

        <div className="max-w-7xl mx-auto px-6 relative">
          <div className="flex flex-col lg:flex-row items-center gap-16">

            {/* Left – copy */}
            <div className="flex-1 text-center lg:text-left">
              <div className="inline-flex items-center gap-2 bg-primary-light border border-primary/20 rounded-full px-3 py-1.5 mb-6">
                <span className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse" />
                <span className="text-xs font-semibold text-primary">678 Auctions Closing This Week</span>
              </div>

              <h1 className="text-5xl md:text-6xl font-extrabold text-gray-900 tracking-tight leading-[1.1] mb-5">
                The Future of{' '}
                <span
                  className="text-transparent bg-clip-text"
                  style={{ backgroundImage: 'linear-gradient(135deg,#4F46E5,#7C3AED)' }}
                >
                  Live Auctions
                </span>
              </h1>

              <p className="text-gray-500 text-lg leading-relaxed mb-8 max-w-xl mx-auto lg:mx-0">
                Secure, real-time bidding for high-value assets. Featuring dynamic lightning-fast automation,
                institutional-grade guarantees for every transaction.
              </p>

              <div className="flex flex-col sm:flex-row gap-3 justify-center lg:justify-start">
                <Link to="/register" className="btn-primary px-6 py-3 text-sm font-semibold rounded-lg">
                  Free Bidding →
                </Link>
                <Link to="/explore" className="btn-secondary px-6 py-3 text-sm font-medium rounded-lg flex items-center justify-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-danger animate-pulse" />
                  4,312 Real-Time Auctions
                </Link>
              </div>
            </div>

            {/* Right – live card */}
            <div className="flex-1 w-full max-w-lg">
              <HeroAuctionCard />
            </div>
          </div>
        </div>
      </section>

      <TrustedBy />
      <FeaturesSection />
      <AssetsSection />
      <PricingSection />
      <CtaBanner />
      <Footer />
    </div>
  );
}
