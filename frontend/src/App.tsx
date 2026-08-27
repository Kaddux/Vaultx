import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Landing } from './pages/Landing';
import { Home } from './pages/Home';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Explore } from './pages/Explore';
import { AuctionDetail } from './pages/AuctionDetail';
import { Wallet } from './pages/Wallet';
import { Transactions } from './pages/Transactions';
import { SellerPortal } from './pages/SellerPortal';
import { Checkout } from './pages/Checkout';
import { useAuth } from './context/AuthContext';

function RootRoute() {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Home /> : <Landing />;
}

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, loading } = useAuth();
  if (loading) {
    return (
      <div className="min-h-screen bg-bg-base flex items-center justify-center">
        <span className="text-text-muted text-sm">Loading…</span>
      </div>
    );
  }
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Router>
      <Routes>
        {/* Auth */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* Dashboard pages */}
        <Route path="/explore" element={<Explore />} />
        <Route path="/auction/:id" element={<AuctionDetail />} />
        <Route path="/wallet" element={<RequireAuth><Wallet /></RequireAuth>} />
        <Route path="/transactions" element={<RequireAuth><Transactions /></RequireAuth>} />
        <Route path="/seller" element={<RequireAuth><SellerPortal /></RequireAuth>} />
        <Route path="/checkout" element={<RequireAuth><Checkout /></RequireAuth>} />

        {/* Default */}
        <Route path="/" element={<RootRoute />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  );
}
