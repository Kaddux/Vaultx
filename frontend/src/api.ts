// ──────────────────────────────────────────────────────────────────────────────
// api.ts — Real API client for Vaultx frontend (replaces the mock layer)
// Base URL: API Gateway via Vite proxy (/api → :8080) or VITE_API_BASE_URL
// ──────────────────────────────────────────────────────────────────────────────

// ═══ Backend DTO types ════════════════════════════════════════════════════════

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
}

export interface UserResponse {
  id: string;
  username: string;
  email: string;
  fullName: string;
  phone: string | null;
  kycStatus: string;
  userRating: number;
  role: string;
  createdAt: string;
}

export interface WalletResponse {
  id: string;
  userId: string;
  balance: number;
  reservedBalance: number;
  availableBalance: number;
  currency: string;
}

export interface AuctionResponse {
  id: string;
  title: string;
  description: string | null;
  sellerId: string;
  startingPrice: number;
  reservePrice: number | null;
  currentBid: number | null;
  bidIncrement: number;
  status: string;
  startTime: string;
  endTime: string;
  extendedAt: string | null;
  extensionPeriodSeconds: number;
  currency: string;
  createdAt: string;
  coverMediaUrl: string | null;
  bidCount?: number;
}

export interface BidResponse {
  id: string;
  auctionId: string;
  bidderId: string;
  amount: number;
  maxAutoBid: number | null;
  autoBid: boolean;
  status: string;
  currentHighestBid: number | null;
  currentWinner: boolean;
  createdAt: string;
}

export interface MyBidResponse {
  bidId: string;
  auctionId: string;
  auctionTitle: string;
  auctionStatus: string;
  currentBid: number | null;
  endTime: string;
  myBidAmount: number;
  myStatus: string;
  createdAt: string;
}

export interface WatchlistResponse {
  id: string;
  title: string;
  status: string;
  currentBid: number | null;
  endTime: string;
  sellerId: string;
  watchedAt: string;
}

export interface TransactionResponse {
  id: string;
  userId: string;
  auctionId: string | null;
  type: string;
  amount: number;
  currency: string;
  status: string;
  description: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface NotificationResponse {
  id: string;
  eventType: string;
  channel: string;
  title: string;
  message: string;
  status: string;
  createdAt: string;
  sentAt: string | null;
}

export interface PaymentStatus {
  auctionId: string;
  status: string;
  amount: number;
  buyerId: string;
  sellerId: string;
  createdAt?: string;
  walletDebited?: boolean;
  shortfall?: string | null;
}

export interface AuctionCreateRequest {
  title: string;
  description?: string;
  startingPrice: number;
  reservePrice?: number;
  bidIncrement: number;
  startTime: string;
  endTime: string;
  extensionPeriodSeconds?: number;
  currency?: string;
}

export interface AuctionMedia {
  id: string;
  auctionId: string;
  mediaType: 'IMAGE' | 'VIDEO';
  contentType: string;
  sizeBytes: number;
  url: string;
  cover: boolean;
  sortOrder: number;
  status: string;
  createdAt: string;
}

export interface MediaUploadRequest {
  contentType: string;
  fileName: string;
  fileSizeBytes: number;
}

export interface PresignResponse {
  mediaId: string;
  auctionId: string;
  mediaType: 'IMAGE' | 'VIDEO';
  objectKey: string;
  contentType: string;
  sizeBytes: number;
  uploadUrl: string;
  headers: Record<string, string>;
  expiresInSeconds: number;
}

// ═══ Token storage ════════════════════════════════════════════════════════════

const ACCESS_TOKEN_KEY = 'vaultx_access_token';
const REFRESH_TOKEN_KEY = 'vaultx_refresh_token';

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}
export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}
export function setTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}
export function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

// ═══ HTTP client ══════════════════════════════════════════════════════════════

const API_BASE = (import.meta as any).env?.VITE_API_BASE_URL ?? '';

export class ApiError extends Error {
  status: number;
  error: string;
  constructor(status: number, error: string, message: string) {
    super(message);
    this.status = status;
    this.error = error;
  }
}

let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) throw new ApiError(401, 'Unauthorized', 'No refresh token');
  const res = await fetch(`${API_BASE}/api/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });
  if (!res.ok) throw new ApiError(401, 'Unauthorized', 'Refresh failed');
  const data: AuthResponse = await res.json();
  setTokens(data.accessToken, data.refreshToken);
  return data.accessToken;
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (response.status === 204) return undefined as T;
  const text = await response.text();
  let body: any = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = text;
    }
  }
  if (!response.ok) {
    const error = body?.error ?? body?.message ?? 'Request Failed';
    const message =
      body?.message ??
      body?.error ??
      (typeof body === 'string' ? body : 'Something went wrong');
    throw new ApiError(response.status, error, message);
  }
  return body as T;
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options.headers as Record<string, string>) ?? {}),
  };
  const token = getAccessToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });

  if (response.status === 401 && token) {
    try {
      refreshPromise = refreshPromise ?? refreshAccessToken();
      const newToken = await refreshPromise;
      refreshPromise = null;
      headers.Authorization = `Bearer ${newToken}`;
      const retry = await fetch(`${API_BASE}${path}`, { ...options, headers });
      return handleResponse<T>(retry);
    } catch (err) {
      refreshPromise = null;
      clearTokens();
      throw err instanceof ApiError ? err : new ApiError(401, 'Unauthorized', 'Session expired');
    }
  }

  return handleResponse<T>(response);
}

function queryString(params?: Record<string, string | number | undefined>): string {
  if (!params) return '';
  const search = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') search.set(k, String(v));
  });
  const s = search.toString();
  return s ? `?${s}` : '';
}

// ═══ API functions ════════════════════════════════════════════════════════════

export const api = {
  auth: {
    login: (email: string, password: string) =>
      request<AuthResponse>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      }),
    register: (dto: { username: string; email: string; password: string; fullName?: string }) =>
      request<AuthResponse>('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(dto),
      }),
  },
  users: {
    getMe: () => request<UserResponse>('/api/users/me'),
    updateMe: (dto: { fullName?: string; phone?: string }) =>
      request<UserResponse>('/api/users/me', { method: 'PATCH', body: JSON.stringify(dto) }),
    deleteMe: () => request<void>('/api/users/me', { method: 'DELETE' }),
    submitKyc: (dto: {
      docType: string;
      fullName: string;
      address?: string;
      documentRef?: string;
      selfieRef?: string;
    }) =>
      request<UserResponse>('/api/users/me/kyc', {
        method: 'POST',
        body: JSON.stringify(dto),
      }),
  },
  wallet: {
    get: () => request<WalletResponse>('/api/wallet'),
    deposit: (amount: number, idempotencyKey: string) =>
      request<WalletResponse>('/api/wallet/deposit', {
        method: 'POST',
        body: JSON.stringify({ amount, idempotencyKey }),
      }),
  },
  auctions: {
    list: (params?: { status?: string; sellerId?: string }) =>
      request<AuctionResponse[]>(`/api/auctions${queryString(params)}`),
    getById: (id: string) => request<AuctionResponse>(`/api/auctions/${id}`),
    create: (dto: AuctionCreateRequest) =>
      request<AuctionResponse>('/api/auctions', { method: 'POST', body: JSON.stringify(dto) }),
    myBids: () => request<MyBidResponse[]>('/api/auctions/bids/mine'),
    listMedia: (auctionId: string) => request<AuctionMedia[]>(`/api/auctions/${auctionId}/media`),
    createUpload: (auctionId: string, dto: MediaUploadRequest) =>
      request<PresignResponse>(`/api/auctions/${auctionId}/media`, {
        method: 'POST',
        body: JSON.stringify(dto),
      }),
    completeUpload: (auctionId: string, mediaId: string) =>
      request<AuctionMedia>(`/api/auctions/${auctionId}/media/${mediaId}/complete`, {
        method: 'POST',
      }),
    setCover: (auctionId: string, mediaId: string) =>
      request<AuctionMedia>(`/api/auctions/${auctionId}/media/${mediaId}/cover`, {
        method: 'PUT',
      }),
    removeMedia: (auctionId: string, mediaId: string) =>
      request<void>(`/api/auctions/${auctionId}/media/${mediaId}`, { method: 'DELETE' }),
  },
  bids: {
    list: (auctionId: string) => request<BidResponse[]>(`/api/auctions/${auctionId}/bids`),
    place: (auctionId: string, dto: { amount: number; maxAutoBid?: number; idempotencyKey: string }) =>
      request<BidResponse>(`/api/auctions/${auctionId}/bids`, {
        method: 'POST',
        body: JSON.stringify(dto),
      }),
    mine: (auctionId: string) => request<BidResponse[]>(`/api/auctions/${auctionId}/bids/mine`),
  },
  payments: {
    getStatus: (auctionId: string) => request<PaymentStatus>(`/api/payments/${auctionId}`),
    createSession: (auctionId: string) => request<{ url: string }>(`/api/payments/${auctionId}/session`),
    confirm: (sessionId: string) =>
      request<{ status: string }>('/api/payments/confirm', {
        method: 'POST',
        body: JSON.stringify({ sessionId }),
      }),
    release: (auctionId: string) =>
      request<{ status: string; auctionId: string }>('/api/payments/release', {
        method: 'POST',
        body: JSON.stringify({ auctionId }),
      }),
    refund: (auctionId: string) =>
      request<{ status: string; auctionId: string }>('/api/payments/refund', {
        method: 'POST',
        body: JSON.stringify({ auctionId }),
      }),
  },
  transactions: {
    list: () => request<TransactionResponse[]>('/api/transactions'),
  },
  notifications: {
    list: (page = 0, size = 20) =>
      request<NotificationResponse[]>(`/api/notifications?page=${page}&size=${size}`),
    unreadCount: () => request<{ unread: number }>('/api/notifications/unread-count'),
    markRead: () => request<{ status: string }>('/api/notifications/read', { method: 'PUT' }),
    updatePreference: (eventType: string, dto: { channel: string; enabled: boolean }) =>
      request<{ status: string }>(`/api/notifications/preferences/${eventType}`, {
        method: 'PUT',
        body: JSON.stringify(dto),
      }),
  },
  watchlist: {
    list: () => request<WatchlistResponse[]>('/api/watchlist'),
    add: (auctionId: string) =>
      request<{ status: string; auctionId: string }>(`/api/auctions/${auctionId}/watchlist`, {
        method: 'POST',
      }),
    remove: (auctionId: string) =>
      request<void>(`/api/auctions/${auctionId}/watchlist`, { method: 'DELETE' }),
  },
  assistant: {
    chat: (message: string, conversationId?: string) =>
      request<{ reply: string; conversationId: string }>('/api/assistant/chat', {
        method: 'POST',
        body: JSON.stringify({ message, conversationId }),
      }),
    resetConversation: (conversationId: string) =>
      request<{ status: string }>(`/api/assistant/conversations/${conversationId}/reset`, {
        method: 'POST',
      }),
  },
};

// ═══ Presigned upload (direct to object storage) ═════════════════════════════

const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
const MAX_VIDEO_BYTES = 100 * 1024 * 1024;

export function mediaTypeOf(file: File): 'IMAGE' | 'VIDEO' {
  return file.type.startsWith('video/') ? 'VIDEO' : 'IMAGE';
}

export function mediaSizeLimit(type: 'IMAGE' | 'VIDEO'): number {
  return type === 'VIDEO' ? MAX_VIDEO_BYTES : MAX_IMAGE_BYTES;
}

/**
 * Performs the direct-to-storage PUT once a presigned URL has been issued.
 * No Authorization header is sent — the URL signature authorizes the request.
 */
export async function uploadToPresignedUrl(presign: PresignResponse, file: File): Promise<void> {
  const res = await fetch(presign.uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': file.type || presign.contentType },
    body: file,
  });
  if (!res.ok) {
    throw new ApiError(res.status, 'UploadFailed', `Failed to upload media (${res.status})`);
  }
}

// ═══ UI mapping helpers ═══════════════════════════════════════════════════════

/** Backend serializes LocalDateTime as a naive ISO string representing UTC; parse as UTC. */
export function toUtcDate(value: string): Date {
  return new Date(value.endsWith('Z') || value.includes('+') ? value : `${value}Z`);
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
  description: string;
  lotDescription: string[];
  specs: { label: string; value: string }[];
  seller: string;
  sellerId: string;
  sellerInfo: SellerInfo;
  category: string;
  subcategory?: string;
  startingPrice: number;
  reservePrice?: number;
  bidIncrement: number;
  currentBid: number | null;
  totalBids: number;
  status: 'ACTIVE' | 'PENDING' | 'AWAITING_PAYMENT' | 'SOLD' | 'UNSOLD';
  endsAt: Date;
  imageColor: string;
  imageAccent: string;
  imageCount: number;
  coverImageUrl?: string;
  media?: AuctionMedia[];
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
  type: 'DEPOSIT' | 'ESCROW_HOLD' | 'ESCROW_RELEASE' | 'REFUND' | 'WITHDRAWAL' | 'RESERVE' | 'RELEASE';
  amount: number;
  status: 'COMPLETED' | 'PENDING' | 'FAILED' | 'SUCCESS' | 'SUCCEEDED';
  description: string;
}

function hashId(input: string): number {
  let hash = 0;
  for (let i = 0; i < input.length; i++) {
    hash = (hash * 31 + input.charCodeAt(i)) | 0;
  }
  return Math.abs(hash);
}

function shortId(id: string): string {
  return id.length > 12 ? id.slice(0, 6) : id;
}

const PALETTE = ['#1a1f36', '#0a3d2e', '#3d1a00', '#2d1a4a', '#1c1c1e', '#8b3a1a', '#173a5e'];

export function mapAuction(a: AuctionResponse): Auction {
  const reserveMet =
    a.reservePrice != null && a.currentBid != null && a.currentBid >= a.reservePrice;
  const hash = hashId(a.id);
  return {
    id: a.id,
    lotNumber: String(1000 + (hash % 9000)),
    title: a.title,
    subtitle: a.description ? a.description.slice(0, 90) : undefined,
    description: a.description ?? '',
    lotDescription: a.description ? [a.description] : [],
    specs: [],
    seller: shortId(a.sellerId),
    sellerId: a.sellerId,
    sellerInfo: {
      handle: shortId(a.sellerId),
      displayName: `Seller ${shortId(a.sellerId)}`,
      avatarInitial: 'S',
      rating: 0,
      reviewCount: 0,
      memberSince: '',
      location: '',
      verified: false,
    },
    category: 'Collectibles',
    startingPrice: a.startingPrice,
    reservePrice: a.reservePrice ?? undefined,
    bidIncrement: a.bidIncrement,
    currentBid: a.currentBid,
    totalBids: a.bidCount ?? 0,
    status: a.status as Auction['status'],
    endsAt: toUtcDate(a.endTime),
    imageColor: PALETTE[hash % PALETTE.length],
    imageAccent: 'sell',
    imageCount: 1,
    coverImageUrl: a.coverMediaUrl ?? undefined,
    reserveMet,
    views: 0,
    watchers: 0,
    payout: null,
  };
}

export function mapBid(b: BidResponse, currentUserId?: string): Bid {
  const handle = b.bidderId;
  return {
    id: b.id,
    username: handle,
    maskedUsername: `${handle.slice(0, 3)}***${handle.slice(-3)}`,
    isCurrentUser: currentUserId != null && b.bidderId === currentUserId,
    amount: b.amount,
    timestamp: new Date(b.createdAt),
  };
}

export function mapTransaction(t: TransactionResponse): Transaction {
  let type: Transaction['type'] = 'DEPOSIT';
  switch (t.type) {
    case 'ESCROW_HOLD':
    case 'DEBIT':
    case 'PURCHASE':
      type = 'ESCROW_HOLD';
      break;
    case 'ESCROW_RELEASE':
    case 'CREDIT':
    case 'RELEASE':
      type = 'RELEASE';
      break;
    case 'ESCROW_REFUND':
    case 'REFUND':
      type = 'REFUND';
      break;
    case 'WITHDRAWAL':
      type = 'WITHDRAWAL';
      break;
    case 'RESERVE':
      type = 'RESERVE';
      break;
    default:
      type = 'DEPOSIT';
  }
  let status: Transaction['status'] = 'COMPLETED';
  switch (t.status) {
    case 'PENDING':
    case 'SUCCEEDED':
    case 'SUCCESS':
      status = 'COMPLETED';
      break;
    case 'FAILED':
      status = 'FAILED';
      break;
    default:
      status = 'COMPLETED';
  }
  return {
    id: t.id,
    date: new Date(t.createdAt),
    type,
    amount: t.amount,
    status,
    description: t.description ?? '',
  };
}

// ═══ Formatters ═══════════════════════════════════════════════════════════════

export function formatCurrency(n: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 2,
  }).format(n);
}

export function formatDate(d: Date): string {
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(d);
}

export function formatTime(d: Date): string {
  return new Intl.DateTimeFormat('en-US', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(d);
}

export function formatBidTime(d: Date): string {
  return new Intl.DateTimeFormat('en-US', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(d);
}