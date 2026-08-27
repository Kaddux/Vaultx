import { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import {
  api,
  getAccessToken,
  setTokens,
  clearTokens,
  UserResponse,
  WalletResponse,
} from '../api';

interface RegisterDto {
  username: string;
  email: string;
  password: string;
  fullName?: string;
}

interface AuthContextValue {
  user: UserResponse | null;
  wallet: WalletResponse | null;
  loading: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (dto: RegisterDto) => Promise<void>;
  logout: () => void;
  refresh: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const loadProfile = useCallback(async () => {
    const [u, w] = await Promise.all([api.users.getMe(), api.wallet.get()]);
    setUser(u);
    setWallet(w);
  }, []);

  useEffect(() => {
    if (!getAccessToken()) {
      setLoading(false);
      return;
    }
    loadProfile()
      .catch(() => {
        clearTokens();
        setUser(null);
        setWallet(null);
      })
      .finally(() => setLoading(false));
  }, [loadProfile]);

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await api.auth.login(email, password);
      setTokens(res.accessToken, res.refreshToken);
      await loadProfile();
    },
    [loadProfile]
  );

  const register = useCallback(
    async (dto: RegisterDto) => {
      const res = await api.auth.register(dto);
      setTokens(res.accessToken, res.refreshToken);
      await loadProfile();
    },
    [loadProfile]
  );

  const logout = useCallback(() => {
    clearTokens();
    setUser(null);
    setWallet(null);
  }, []);

  const refresh = useCallback(async () => {
    await loadProfile();
  }, [loadProfile]);

  return (
    <AuthContext.Provider
      value={{
        user,
        wallet,
        loading,
        isAuthenticated: !!user,
        login,
        register,
        logout,
        refresh,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}