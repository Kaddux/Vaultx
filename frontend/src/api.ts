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

export interface SellerInfo {
  handle: string;
  displayName: string;
  avatarInitial: string;
  rating: number;
  reviewCount: number;
  memberSince: string;
  location: string;
  verified: boolean;
}

export interface Auction {
  id: string;
  lotNumber: string;
  title: string;
  subtitle?: string;
  description: string;            // rich HTML-ish description text
  lotDescription: string[];       // paragraphs
  specs: { label: string; value: string }[];  // bullet specs
  seller: string;
  sellerInfo: SellerInfo;
  category: string;
  subcategory?: string;
  startingPrice: number;
  reservePrice?: number;
  bidIncrement: number;
  currentBid: number;
  totalBids: number;
  status: 'ACTIVE' | 'PENDING' | 'SOLD' | 'UNSOLD';
  endsAt: Date;
  imageColor: string;             // placeholder gradient bg color
  imageAccent: string;            // icon to show in placeholder
  imageCount: number;             // number of carousel images
  reserveMet: boolean;
  views: number;
  watchers: number;
  payout?: 'RELEASED' | 'PENDING' | null;
}

export interface Bid {
  id: string;
  username: string;
  maskedUsername: string;
  isCurrentUser: boolean;
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
const storedUser = localStorage.getItem('vaultx_user');
export const MOCK_USER: AuthResponse = storedUser ? JSON.parse(storedUser) : {
  id: 'usr_8402',
  username: 'alex_vault',
  email: 'alex@vaultx.io',
  fullName: 'Alex Morgan',
  role: 'SELLER',
  balance: 12_480.00,
  reservedBalance: 3_200.00,
  kycStatus: 'VERIFIED',
};
if (!storedUser) {
  localStorage.setItem('vaultx_user', JSON.stringify(MOCK_USER));
}

// ─── Mock Auctions ───────────────────────────────────────────────────────────
const now = new Date();
const in90s = new Date(now.getTime() + 90 * 1000);
const in5m  = new Date(now.getTime() + 5 * 60 * 1000);
const in2h  = new Date(now.getTime() + 2 * 60 * 60 * 1000);
const in1d  = new Date(now.getTime() + 24 * 60 * 60 * 1000);

const SELLER_HERITAGE: SellerInfo = {
  handle: 'heritage_horology',
  displayName: 'Heritage Horology Ltd.',
  avatarInitial: 'H',
  rating: 4.85,
  reviewCount: 342,
  memberSince: '2019',
  location: 'London, UK',
  verified: true,
};

const SELLER_SPORTS: SellerInfo = {
  handle: 'sports_memorabilia',
  displayName: 'Sports Memorabilia Co.',
  avatarInitial: 'S',
  rating: 4.9,
  reviewCount: 218,
  memberSince: '2018',
  location: 'New York, US',
  verified: true,
};

const SELLER_ARTHAUS: SellerInfo = {
  handle: 'arthaus_berlin',
  displayName: 'Arthaus Berlin GmbH',
  avatarInitial: 'A',
  rating: 4.7,
  reviewCount: 97,
  memberSince: '2021',
  location: 'Berlin, DE',
  verified: true,
};

const SELLER_GUITAR: SellerInfo = {
  handle: 'guitar_vault',
  displayName: 'Guitar Vault Inc.',
  avatarInitial: 'G',
  rating: 4.8,
  reviewCount: 156,
  memberSince: '2020',
  location: 'Nashville, US',
  verified: true,
};

const SELLER_TECH: SellerInfo = {
  handle: 'tech_resellers',
  displayName: 'Tech Resellers LLC',
  avatarInitial: 'T',
  rating: 4.6,
  reviewCount: 441,
  memberSince: '2017',
  location: 'San Francisco, US',
  verified: false,
};

const SELLER_MAISON: SellerInfo = {
  handle: 'maison_luxe',
  displayName: 'Maison Luxe Paris',
  avatarInitial: 'M',
  rating: 4.95,
  reviewCount: 503,
  memberSince: '2016',
  location: 'Paris, FR',
  verified: true,
};

const DEFAULT_AUCTIONS: Auction[] = [
  {
    id: 'auc_001',
    lotNumber: '8402',
    title: '1952 Topps Mickey Mantle Baseball Card',
    subtitle: 'PSA Grade 8 NM-MT — The Holy Grail of Baseball Cards',
    description: 'Iconic post-war baseball card in exceptional condition.',
    lotDescription: [
      'This 1952 Topps Mickey Mantle #311 is widely regarded as the most iconic and valuable post-war baseball card ever produced. This example has been graded PSA 8 NM-MT, placing it among the finest known specimens in the hobby.',
      'The card presents with brilliant original colors, sharp corners, and full gloss. The centering is outstanding at approximately 55/45 left-to-right and 55/45 top-to-bottom. There is no visible print defect, creasing, or surface wear visible to the naked eye.',
      'Mickey Mantle\'s 1952 Topps card is the cornerstone of any serious collection — this is a once-in-a-generation opportunity to acquire one at the NM-MT level.',
    ],
    specs: [
      { label: 'Grade', value: 'PSA 8 NM-MT' },
      { label: 'Year', value: '1952' },
      { label: 'Set', value: 'Topps #311' },
      { label: 'Player', value: 'Mickey Mantle' },
      { label: 'Team', value: 'New York Yankees' },
      { label: 'Authentication', value: 'PSA/DNA certified' },
    ],
    seller: 'sports_memorabilia',
    sellerInfo: SELLER_SPORTS,
    category: 'Sports',
    subcategory: 'Baseball Cards',
    startingPrice: 5000,
    reservePrice: 8000,
    bidIncrement: 100,
    currentBid: 9_400,
    totalBids: 47,
    status: 'ACTIVE',
    endsAt: in90s,
    imageColor: '#1a1f36',
    imageAccent: 'sports_baseball',
    imageCount: 4,
    reserveMet: true,
    views: 3_241,
    watchers: 198,
    payout: null,
  },
  {
    id: 'auc_002',
    lotNumber: '8403',
    title: 'Rolex Submariner Date 126610LN',
    subtitle: '2023 — Full Set with Box & Papers, Unworn',
    description: '2023 Rolex Submariner in perfect condition with full original box and papers.',
    lotDescription: [
      'Presented here is a 2023 Rolex Submariner Date reference 126610LN in unworn, pristine condition. This example comes complete with its original Rolex box, all accompanying papers, hang tags, and the green warranty card registered in 2023.',
      'The Submariner Date features the Oystersteel case and bracelet with Cerachrom bezel insert in black ceramic. The black dial features luminescent hour markers and Mercedes hands. Powered by Caliber 3235 with approximately 70-hour power reserve.',
      'With the current grey market premium on new Rolexes, this represents excellent value for a sealed, unworn example with all documentation intact.',
    ],
    specs: [
      { label: 'Reference', value: '126610LN' },
      { label: 'Year', value: '2023' },
      { label: 'Case Material', value: 'Oystersteel' },
      { label: 'Movement', value: 'Cal. 3235 (Automatic)' },
      { label: 'Water Resistance', value: '300m / 1,000ft' },
      { label: 'Condition', value: 'Unworn, full set' },
    ],
    seller: 'luxury_watch_co',
    sellerInfo: SELLER_HERITAGE,
    category: 'Watches',
    subcategory: 'Rolex',
    startingPrice: 12000,
    reservePrice: 14000,
    bidIncrement: 250,
    currentBid: 14_750,
    totalBids: 32,
    status: 'ACTIVE',
    endsAt: in5m,
    imageColor: '#0a3d2e',
    imageAccent: 'watch',
    imageCount: 6,
    reserveMet: true,
    views: 5_812,
    watchers: 421,
    payout: null,
  },
  {
    id: 'auc_003',
    lotNumber: '8404',
    title: 'Vintage Fender Stratocaster 1962',
    subtitle: '3-Tone Sunburst, All Original, OHSC',
    description: 'All original, sunburst finish, lightweight body. Incredible tone and playability.',
    lotDescription: [
      'This exceptional 1962 Fender Stratocaster is a superb example of the pre-CBS golden era. The guitar features its original 3-tone sunburst finish which has aged to a beautiful, transparent amber in the lighter areas while the burst retains its warm red-to-black transition.',
      'All electronics are completely original — original pickups, pots, selector switch, and output jack. The neck retains approximately 95% of its original frets. The original tuning machines are in excellent working order. This guitar has never been refinished or had any non-original parts installed.',
      'Sold with its original brown hardshell case (OHSC) and original strap button. A truly time-capsule Strat that plays and sounds exactly as it should.',
    ],
    specs: [
      { label: 'Year', value: '1962' },
      { label: 'Finish', value: '3-Tone Sunburst (original)' },
      { label: 'Body', value: 'Alder' },
      { label: 'Neck', value: 'Maple with slab rosewood' },
      { label: 'Pickups', value: 'All original single-coils' },
      { label: 'Case', value: 'Original hardshell case (OHSC)' },
    ],
    seller: 'guitar_vault',
    sellerInfo: SELLER_GUITAR,
    category: 'Music',
    subcategory: 'Vintage Guitars',
    startingPrice: 18000,
    reservePrice: 22000,
    bidIncrement: 500,
    currentBid: 19_500,
    totalBids: 14,
    status: 'ACTIVE',
    endsAt: in2h,
    imageColor: '#3d1a00',
    imageAccent: 'music_note',
    imageCount: 5,
    reserveMet: false,
    views: 1_904,
    watchers: 156,
    payout: null,
  },
  {
    id: 'auc_004',
    lotNumber: '8405',
    title: 'Abstract Oil Painting — "Chromatic Drift"',
    subtitle: 'Original Oil on Linen, 60×80cm, Signed & Authenticated',
    description: 'Original oil on linen, 60x80cm, signed and authenticated. Contemporary artist.',
    lotDescription: [
      '"Chromatic Drift" is an original oil painting on Belgian linen canvas, 60×80cm, executed in 2024 by Berlin-based contemporary artist Marlene Kessler. The work is part of her "Fluid Architectures" series exploring tension between geometric structure and organic dissolution.',
      'The painting is applied in multiple dense layers of oil, creating a relief-like texture with visible impasto technique. The palette transitions from deep cobalt and prussian blue through cadmium orange into raw umber — all archival-quality pigments guaranteed not to yellow.',
      'Comes with a certificate of authenticity signed by the artist, provenance documentation, and is ready to hang with gallery-style hanging hardware installed.',
    ],
    specs: [
      { label: 'Medium', value: 'Oil on Belgian linen' },
      { label: 'Dimensions', value: '60 × 80 cm (unframed)' },
      { label: 'Year', value: '2024' },
      { label: 'Artist', value: 'Marlene Kessler' },
      { label: 'Series', value: 'Fluid Architectures' },
      { label: 'Includes', value: 'CoA, provenance docs, hanging hardware' },
    ],
    seller: 'arthaus_berlin',
    sellerInfo: SELLER_ARTHAUS,
    category: 'Art',
    subcategory: 'Contemporary',
    startingPrice: 1500,
    reservePrice: 2000,
    bidIncrement: 50,
    currentBid: 2_200,
    totalBids: 28,
    status: 'ACTIVE',
    endsAt: in1d,
    imageColor: '#2d1a4a',
    imageAccent: 'palette',
    imageCount: 3,
    reserveMet: true,
    views: 987,
    watchers: 73,
    payout: null,
  },
  {
    id: 'auc_005',
    lotNumber: '8406',
    title: 'Apple Mac Pro M2 Ultra — Studio Config',
    subtitle: 'Sealed Box · 192GB RAM · 8TB SSD · Afterburner',
    description: 'Brand new in sealed box. 192GB RAM, 8TB SSD, Afterburner card.',
    lotDescription: [
      'Brand new in original sealed Apple packaging — this is the top-spec Mac Pro with M2 Ultra chip. This configuration includes 192GB of unified memory and 8TB of SSD storage, alongside the optional Afterburner accelerator card for ProRes/RAW video acceleration.',
      'Apple serial number and purchase receipt available for verification. The unit ships with all original accessories: 140W USB-C power adapter, Magic Mouse, Magic Keyboard with Touch ID and numeric keypad, and power cable for your region.',
    ],
    specs: [
      { label: 'Chip', value: 'Apple M2 Ultra (24-core CPU, 76-core GPU)' },
      { label: 'Memory', value: '192GB unified memory' },
      { label: 'Storage', value: '8TB SSD' },
      { label: 'Add-on', value: 'Afterburner accelerator card' },
      { label: 'Condition', value: 'Brand new, sealed' },
      { label: 'Warranty', value: 'Full Apple warranty remaining' },
    ],
    seller: 'tech_resellers',
    sellerInfo: SELLER_TECH,
    category: 'Technology',
    subcategory: 'Apple',
    startingPrice: 8000,
    reservePrice: 9500,
    bidIncrement: 200,
    currentBid: 8_400,
    totalBids: 11,
    status: 'ACTIVE',
    endsAt: in2h,
    imageColor: '#1c1c1e',
    imageAccent: 'computer',
    imageCount: 3,
    reserveMet: false,
    views: 2_108,
    watchers: 212,
    payout: null,
  },
  {
    id: 'auc_006',
    lotNumber: '8407',
    title: 'Hermès Birkin 30 — Togo Leather Gold',
    subtitle: 'Gold Hardware · Pristine Condition · Full Hermès Set',
    description: 'Pristine condition, authentic authentication papers, Hermès orange box included.',
    lotDescription: [
      'A pristine Hermès Birkin 30 in Togo leather with Gold hardware. Togo is the most sought-after Birkin leather for its scratch-resistant properties and the way it absorbs and retains the rich color. The Gold colorway is the most timeless and versatile of all Birkin colors.',
      'This bag is accompanied by its full Hermès set: the iconic orange box with Hermès ribbon, dustbag, rain cape, lock, two keys, and clochette. All hardware functions perfectly. The bag shows no signs of wear — this is a true investment-grade piece.',
    ],
    specs: [
      { label: 'Size', value: '30cm' },
      { label: 'Leather', value: 'Togo (Gold color)' },
      { label: 'Hardware', value: 'Gold-plated (GHW)' },
      { label: 'Condition', value: 'Pristine, never carried' },
      { label: 'Includes', value: 'Full Hermès set, box, dustbag, lock & keys' },
      { label: 'Authentication', value: 'Hermès receipt & heat stamp visible' },
    ],
    seller: 'maison_luxe',
    sellerInfo: SELLER_MAISON,
    category: 'Fashion',
    subcategory: 'Hermès',
    startingPrice: 22000,
    reservePrice: 28000,
    bidIncrement: 500,
    currentBid: 31_000,
    totalBids: 63,
    status: 'SOLD',
    endsAt: new Date(now.getTime() - 60000),
    imageColor: '#8b3a1a',
    imageAccent: 'shopping_bag',
    imageCount: 5,
    reserveMet: true,
    views: 8_904,
    watchers: 634,
    payout: 'RELEASED',
  },
];

function reviveDates(key: string, value: any) {
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/.test(value)) {
    return new Date(value);
  }
  return value;
}

const storedAuctions = localStorage.getItem('vaultx_auctions');
export const MOCK_AUCTIONS: Auction[] = storedAuctions ? JSON.parse(storedAuctions, reviveDates) : DEFAULT_AUCTIONS;
if (!storedAuctions) {
  localStorage.setItem('vaultx_auctions', JSON.stringify(MOCK_AUCTIONS));
}

const DEFAULT_BID_HISTORY: Bid[] = [
  { id: 'b1', username: 'jade_collector', maskedUsername: 'j***e', isCurrentUser: false, amount: 9400, timestamp: new Date(now.getTime() - 5000) },
  { id: 'b2', username: 'bidmaster99',    maskedUsername: 'b***9', isCurrentUser: false, amount: 9200, timestamp: new Date(now.getTime() - 18000) },
  { id: 'b3', username: 'rare_finds',     maskedUsername: 'r***s', isCurrentUser: false, amount: 9000, timestamp: new Date(now.getTime() - 45000) },
  { id: 'b4', username: 'alex_vault',     maskedUsername: 'a***t', isCurrentUser: true,  amount: 8800, timestamp: new Date(now.getTime() - 120000) },
  { id: 'b5', username: 'sports_fan42',   maskedUsername: 's***2', isCurrentUser: false, amount: 8500, timestamp: new Date(now.getTime() - 300000) },
  { id: 'b6', username: 'collector_pro',  maskedUsername: 'c***o', isCurrentUser: false, amount: 8200, timestamp: new Date(now.getTime() - 600000) },
  { id: 'b7', username: 'bidder_x',       maskedUsername: 'b***x', isCurrentUser: false, amount: 7900, timestamp: new Date(now.getTime() - 1200000) },
];

const storedBids = localStorage.getItem('vaultx_bids');
export const MOCK_BID_HISTORY: Bid[] = storedBids ? JSON.parse(storedBids, reviveDates) : DEFAULT_BID_HISTORY;
if (!storedBids) {
  localStorage.setItem('vaultx_bids', JSON.stringify(MOCK_BID_HISTORY));
}

const DEFAULT_TRANSACTIONS: Transaction[] = [
  { id: 'txn_001', date: new Date(now.getTime() - 1 * 3600000),  type: 'DEPOSIT',         amount:  5000, status: 'COMPLETED', description: 'Bank transfer deposit' },
  { id: 'txn_002', date: new Date(now.getTime() - 3 * 3600000),  type: 'ESCROW_HOLD',     amount: -3200, status: 'COMPLETED', description: 'Bid placed on Rolex Submariner' },
  { id: 'txn_003', date: new Date(now.getTime() - 6 * 3600000),  type: 'ESCROW_RELEASE',  amount:  2500, status: 'COMPLETED', description: 'Outbid on Stratocaster auction' },
  { id: 'txn_004', date: new Date(now.getTime() - 24 * 3600000), type: 'REFUND',          amount:  1500, status: 'COMPLETED', description: 'Auction cancelled by seller' },
  { id: 'txn_005', date: new Date(now.getTime() - 48 * 3600000), type: 'DEPOSIT',         amount: 10000, status: 'PENDING',   description: 'Wire transfer pending verification' },
  { id: 'txn_006', date: new Date(now.getTime() - 72 * 3600000), type: 'ESCROW_HOLD',     amount:  -800, status: 'FAILED',    description: 'Insufficient funds' },
  { id: 'txn_007', date: new Date(now.getTime() - 96 * 3600000), type: 'DEPOSIT',         amount:  2000, status: 'COMPLETED', description: 'Card deposit' },
];

const storedTransactions = localStorage.getItem('vaultx_transactions');
export const MOCK_TRANSACTIONS: Transaction[] = storedTransactions ? JSON.parse(storedTransactions, reviveDates) : DEFAULT_TRANSACTIONS;
if (!storedTransactions) {
  localStorage.setItem('vaultx_transactions', JSON.stringify(MOCK_TRANSACTIONS));
}

export const MOCK_SELLER_AUCTIONS = MOCK_AUCTIONS.filter((a) => a.seller === 'sports_memorabilia' || a.sellerInfo.handle === 'heritage_horology').concat([
  {
    id: 'sel_001',
    lotNumber: '8410',
    title: 'Signed Michael Jordan Jersey 1996',
    subtitle: 'Bulls Championship Season, JSA Certified',
    description: '',
    lotDescription: [],
    specs: [],
    seller: 'alex_vault',
    sellerInfo: { handle: 'alex_vault', displayName: 'Alex Morgan', avatarInitial: 'A', rating: 4.85, reviewCount: 48, memberSince: '2022', location: 'Austin, US', verified: true },
    category: 'Sports',
    subcategory: 'Basketball',
    startingPrice: 3000,
    reservePrice: 5000,
    bidIncrement: 100,
    currentBid: 6_200,
    totalBids: 24,
    status: 'ACTIVE' as const,
    endsAt: in2h,
    imageColor: '#1D4ED8',
    imageAccent: 'sports_basketball',
    imageCount: 3,
    reserveMet: true,
    views: 892,
    watchers: 64,
    payout: null,
  },
  {
    id: 'sel_002',
    lotNumber: '8411',
    title: 'First Edition Pokémon Card Set',
    subtitle: 'Base Set 1st Edition, PSA Graded',
    description: '',
    lotDescription: [],
    specs: [],
    seller: 'alex_vault',
    sellerInfo: { handle: 'alex_vault', displayName: 'Alex Morgan', avatarInitial: 'A', rating: 4.85, reviewCount: 48, memberSince: '2022', location: 'Austin, US', verified: true },
    category: 'Collectibles',
    subcategory: 'Pokémon',
    startingPrice: 8000,
    reservePrice: 12000,
    bidIncrement: 250,
    currentBid: 9_500,
    totalBids: 18,
    status: 'ACTIVE' as const,
    endsAt: in1d,
    imageColor: '#D97706',
    imageAccent: 'catching_pokemon',
    imageCount: 4,
    reserveMet: false,
    views: 1_340,
    watchers: 112,
    payout: null,
  },
]);

// ─── Helpers ─────────────────────────────────────────────────────────────────
export function formatCurrency(n: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 2 }).format(n);
}

export function formatDate(d: Date): string {
  return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(d);
}

export function formatTime(d: Date): string {
  return new Intl.DateTimeFormat('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }).format(d);
}

export function formatBidTime(d: Date): string {
  return new Intl.DateTimeFormat('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }).format(d);
}

export function saveState() {
  localStorage.setItem('vaultx_user', JSON.stringify(MOCK_USER));
  localStorage.setItem('vaultx_auctions', JSON.stringify(MOCK_AUCTIONS));
  localStorage.setItem('vaultx_bids', JSON.stringify(MOCK_BID_HISTORY));
  localStorage.setItem('vaultx_transactions', JSON.stringify(MOCK_TRANSACTIONS));
}
