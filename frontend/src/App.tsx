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

function RootRoute() {
  const isLoggedIn = localStorage.getItem('vaultx_logged_in') === 'true';
  return isLoggedIn ? <Home /> : <Landing />;
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
        <Route path="/wallet" element={<Wallet />} />
        <Route path="/transactions" element={<Transactions />} />
        <Route path="/seller" element={<SellerPortal />} />
        <Route path="/checkout" element={<Checkout />} />

        {/* Default */}
        <Route path="/" element={<RootRoute />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  );
}
