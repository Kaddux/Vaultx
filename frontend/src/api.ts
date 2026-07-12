// ──────────────────────────────────────────────────────────────────────────────
// api.ts — Shared types and mock data for Vaultx frontend
// ──────────────────────────────────────────────────────────────────────────────

export interface AuthResponse {
  id: string;
  username: string;
  email: string;
  fullName: string;
  role: 'BUYER' | 'SELLER' | 'ADMIN';
  balance: number;
  reservedBalance: number;
  kycStatus: 'VERIFIED' | 'PENDING' | 'UNVERIFIED';
}

export interface Auction {
  id: string;
  title: string;
  description: string;
  seller: string;
  category: string;
  startingPrice: number;
  reservePrice?: number;
  bidIncrement: number;
  currentBid: number;
  totalBids: number;
  status: 'ACTIVE' | 'PENDING' | 'SOLD' | 'UNSOLD';
  endsAt: Date;
  imageColor: string; // placeholder gradient
  reserveMet: boolean;
  payout?: 'RELEASED' | 'PENDING' | null;
}

export interface Bid {
  id: string;
  username: string;
  maskedUsername: string;
  amount: number;
  timestamp: Date;
}

export interface Transaction {
  id: string;
  date: Date;
  type: 'DEPOSIT' | 'ESCROW_HOLD' | 'ESCROW_RELEASE' | 'REFUND' | 'WITHDRAWAL';
  amount: number;
  status: 'COMPLETED' | 'PENDING' | 'FAILED';
  description: string;
}

// ─── Mock current user ───────────────────────────────────────────────────────
export const MOCK_USER: AuthResponse = {
  id: 'usr_8402',
  username: 'alex_vault',
  email: 'alex@vaultx.io',
  fullName: 'Alex Morgan',
  role: 'SELLER',
  balance: 12_480.00,
  reservedBalance: 3_200.00,
  kycStatus: 'VERIFIED',
};

// ─── Mock Auctions ───────────────────────────────────────────────────────────
const now = new Date();
const in90s = new Date(now.getTime() + 90 * 1000);
const in5m = new Date(now.getTime() + 5 * 60 * 1000);
const in2h = new Date(now.getTime() + 2 * 60 * 60 * 1000);
const in1d = new Date(now.getTime() + 24 * 60 * 60 * 1000);

export const MOCK_AUCTIONS: Auction[] = [
  {
    id: 'auc_001',
    title: '1952 Topps Mickey Mantle Baseball Card',
    description: 'PSA graded 8 NM-MT condition. One of the most iconic baseball cards ever produced.',
    seller: 'sports_memorabilia',
    category: 'Sports',
    startingPrice: 5000,
    reservePrice: 8000,
    bidIncrement: 100,
    currentBid: 9_400,
    totalBids: 47,
    status: 'ACTIVE',
    endsAt: in90s,
    imageColor: '#312E81',
    reserveMet: true,
    payout: null,
  },
  {
    id: 'auc_002',
    title: 'Rolex Submariner Date 126610LN',
    description: '2023 Rolex Submariner in perfect condition with full original box and papers.',
    seller: 'luxury_watch_co',
    category: 'Watches',
    startingPrice: 12000,
    reservePrice: 14000,
    bidIncrement: 250,
    currentBid: 14_750,
    totalBids: 32,
    status: 'ACTIVE',
    endsAt: in5m,
    imageColor: '#065F46',
    reserveMet: true,
    payout: null,
  },
  {
    id: 'auc_003',
    title: 'Vintage Fender Stratocaster 1962',
    description: 'All original, sunburst finish, lightweight body. Incredible tone and playability.',
    seller: 'guitar_vault',
    category: 'Music',
    startingPrice: 18000,
    reservePrice: 22000,
    bidIncrement: 500,
    currentBid: 19_500,
    totalBids: 14,
    status: 'ACTIVE',
    endsAt: in2h,
    imageColor: '#78350F',
    reserveMet: false,
    payout: null,
  },
  {
    id: 'auc_004',
    title: 'Abstract Oil Painting — "Chromatic Drift"',
    description: 'Original oil on linen, 60x80cm, signed and authenticated. Contemporary artist.',
    seller: 'arthaus_berlin',
    category: 'Art',
    startingPrice: 1500,
    reservePrice: 2000,
    bidIncrement: 50,
    currentBid: 2_200,
    totalBids: 28,
    status: 'ACTIVE',
    endsAt: in1d,
    imageColor: '#7C3AED',
    reserveMet: true,
    payout: null,
  },
  {
    id: 'auc_005',
    title: 'Apple Mac Pro M2 Ultra — Studio Config',
    description: 'Brand new in sealed box. 192GB RAM, 8TB SSD, Afterburner card.',
    seller: 'tech_resellers',
    category: 'Technology',
    startingPrice: 8000,
    reservePrice: 9500,
    bidIncrement: 200,
    currentBid: 8_400,
    totalBids: 11,
    status: 'ACTIVE',
    endsAt: in2h,
    imageColor: '#1E3A5F',
    reserveMet: false,
    payout: null,
  },
  {
    id: 'auc_006',
    title: 'Hermes Birkin 30 — Togo Leather Gold',
    description: 'Pristine condition, authentic authentication papers, Hermes orange box included.',
    seller: 'maison_luxe',
    category: 'Fashion',
    startingPrice: 22000,
    reservePrice: 28000,
    bidIncrement: 500,
    currentBid: 31_000,
    totalBids: 63,
    status: 'SOLD',
    endsAt: new Date(now.getTime() - 60000),
    imageColor: '#9A3412',
    reserveMet: true,
    payout: 'RELEASED',
  },
];

export const MOCK_BID_HISTORY: Bid[] = [
  { id: 'b1', username: 'jade_collector', maskedUsername: 'j***e', amount: 9400, timestamp: new Date(now.getTime() - 5000) },
  { id: 'b2', username: 'bidmaster99', maskedUsername: 'b***9', amount: 9200, timestamp: new Date(now.getTime() - 18000) },
  { id: 'b3', username: 'rare_finds', maskedUsername: 'r***s', amount: 9000, timestamp: new Date(now.getTime() - 45000) },
  { id: 'b4', username: 'alex_vault', maskedUsername: 'a***t', amount: 8800, timestamp: new Date(now.getTime() - 120000) },
  { id: 'b5', username: 'sports_fan42', maskedUsername: 's***2', amount: 8500, timestamp: new Date(now.getTime() - 300000) },
  { id: 'b6', username: 'collector_pro', maskedUsername: 'c***o', amount: 8200, timestamp: new Date(now.getTime() - 600000) },
  { id: 'b7', username: 'bidder_x', maskedUsername: 'b***x', amount: 7900, timestamp: new Date(now.getTime() - 1200000) },
];

export const MOCK_TRANSACTIONS: Transaction[] = [
  { id: 'txn_001', date: new Date(now.getTime() - 1 * 3600000), type: 'DEPOSIT', amount: 5000, status: 'COMPLETED', description: 'Bank transfer deposit' },
  { id: 'txn_002', date: new Date(now.getTime() - 3 * 3600000), type: 'ESCROW_HOLD', amount: -3200, status: 'COMPLETED', description: 'Bid placed on Rolex Submariner' },
  { id: 'txn_003', date: new Date(now.getTime() - 6 * 3600000), type: 'ESCROW_RELEASE', amount: 2500, status: 'COMPLETED', description: 'Outbid on Stratocaster auction' },
  { id: 'txn_004', date: new Date(now.getTime() - 24 * 3600000), type: 'REFUND', amount: 1500, status: 'COMPLETED', description: 'Auction cancelled by seller' },
  { id: 'txn_005', date: new Date(now.getTime() - 48 * 3600000), type: 'DEPOSIT', amount: 10000, status: 'PENDING', description: 'Wire transfer pending verification' },
  { id: 'txn_006', date: new Date(now.getTime() - 72 * 3600000), type: 'ESCROW_HOLD', amount: -800, status: 'FAILED', description: 'Insufficient funds' },
  { id: 'txn_007', date: new Date(now.getTime() - 96 * 3600000), type: 'DEPOSIT', amount: 2000, status: 'COMPLETED', description: 'Card deposit' },
];

export const MOCK_SELLER_AUCTIONS: Auction[] = [
  {
    id: 'sel_001',
    title: 'Signed Michael Jordan Jersey 1996',
    description: '',
    seller: 'alex_vault',
    category: 'Sports',
    startingPrice: 3000,
    reservePrice: 5000,
    bidIncrement: 100,
    currentBid: 6_200,
    totalBids: 24,
    status: 'ACTIVE',
    endsAt: in2h,
    imageColor: '#1D4ED8',
    reserveMet: true,
    payout: null,
  },
  {
    id: 'sel_002',
    title: 'First Edition Pokémon Card Set',
    description: '',
    seller: 'alex_vault',
    category: 'Collectibles',
    startingPrice: 8000,
    reservePrice: 12000,
    bidIncrement: 250,
    currentBid: 9_500,
    totalBids: 18,
    status: 'ACTIVE',
    endsAt: in1d,
    imageColor: '#D97706',
    reserveMet: false,
    payout: null,
  },
  {
    id: 'sel_003',
    title: 'Vintage Omega Speedmaster 1969',
    description: '',
    seller: 'alex_vault',
    category: 'Watches',
    startingPrice: 10000,
    reservePrice: 15000,
    bidIncrement: 500,
    currentBid: 16_800,
    totalBids: 41,
    status: 'SOLD',
    endsAt: new Date(now.getTime() - 86400000),
    imageColor: '#374151',
    reserveMet: true,
    payout: 'RELEASED',
  },
  {
    id: 'sel_004',
    title: 'Rare Gibson Les Paul 1959 Reissue',
    description: '',
    seller: 'alex_vault',
    category: 'Music',
    startingPrice: 15000,
    reservePrice: 20000,
    bidIncrement: 500,
    currentBid: 14_500,
    totalBids: 6,
    status: 'UNSOLD',
    endsAt: new Date(now.getTime() - 3600000),
    imageColor: '#7C2D12',
    reserveMet: false,
    payout: null,
  },
];

// ─── Helpers ─────────────────────────────────────────────────────────────────
export function formatCurrency(n: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 2 }).format(n);
}

export function formatDate(d: Date): string {
  return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(d);
}

export function formatTime(d: Date): string {
  return new Intl.DateTimeFormat('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit' }).format(d);
}
