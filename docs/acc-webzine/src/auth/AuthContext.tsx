import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import {
  fetchLoginUser,
  getGoogleLoginUrl,
  logoutRequest,
  reissueAccessToken,
  type UserLoginInfo,
} from './authApi';
import {
  ADMIN_SESSION_WALL_MS,
  clearAdminSessionDeadline,
  readAdminSessionDeadlineMs,
  writeAdminSessionDeadlineMs,
} from './adminSessionWall';

type AuthContextValue = {
  readonly user: UserLoginInfo | null;
  readonly accessToken: string | null;
  /** 초기 세션 복원(reissue) 시도 완료 여부 */
  readonly authReady: boolean;
  /** 탭 기준 2시간 벽시계 세션 만료 시각(ms) */
  readonly adminSessionDeadlineMs: number | null;
  readonly startGoogleLogin: () => void;
  readonly logout: () => Promise<void>;
  readonly refreshUser: () => Promise<void>;
  /** PATCH/업로드 등 API가 준 최신 프로필로 즉시 동기화(프로필 사진 갱신 누락 방지) */
  readonly applyLoginUser: (u: UserLoginInfo) => void;
  /** 로컬 회원가입 직후 액세스 토큰 반영 */
  readonly applyAccessToken: (accessToken: string) => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);
const AUTH_CONTEXT_FALLBACK: AuthContextValue = {
  user: null,
  accessToken: null,
  authReady: false,
  adminSessionDeadlineMs: null,
  startGoogleLogin: () => {},
  logout: async () => {},
  refreshUser: async () => {},
  applyLoginUser: () => {},
  applyAccessToken: async () => {},
};

export function AuthProvider({ children }: { readonly children: ReactNode }) {
  const [user, setUser] = useState<UserLoginInfo | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [authReady, setAuthReady] = useState(false);
  const [adminSessionDeadlineMs, setAdminSessionDeadlineMs] = useState<number | null>(null);
  const accessTokenRef = useRef<string | null>(null);
  accessTokenRef.current = accessToken;

  const refreshUser = useCallback(async () => {
    const t = accessTokenRef.current;
    if (!t) {
      setUser(null);
      return;
    }
    const u = await fetchLoginUser(t);
    setUser(u);
  }, []);

  const applyLoginUser = useCallback((u: UserLoginInfo) => {
    setUser(u);
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const token = await reissueAccessToken();
        if (cancelled) return;
        if (token) {
          setAccessToken(token);
          const u = await fetchLoginUser(token);
          if (!cancelled) {
            setUser(u);
            if (!u) {
              setAccessToken(null);
            }
          }
        } else {
          if (!cancelled) setUser(null);
        }
      } finally {
        if (!cancelled) setAuthReady(true);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const startGoogleLogin = useCallback(() => {
    window.location.assign(getGoogleLoginUrl());
  }, []);

  const logout = useCallback(async () => {
    clearAdminSessionDeadline();
    await logoutRequest();
    setAccessToken(null);
    setUser(null);
    setAdminSessionDeadlineMs(null);
  }, []);

  const applyAccessToken = useCallback(async (token: string) => {
    clearAdminSessionDeadline();
    setAccessToken(token);
    const u = await fetchLoginUser(token);
    setUser(u);
    if (!u) {
      setAccessToken(null);
    }
  }, []);

  useEffect(() => {
    if (!accessToken || !user || user.role !== 'ADMIN') {
      setAdminSessionDeadlineMs(null);
      return;
    }
    const run = (): boolean => {
      const now = Date.now();
      let until = readAdminSessionDeadlineMs();
      if (until == null) {
        until = now + ADMIN_SESSION_WALL_MS;
        writeAdminSessionDeadlineMs(until);
      }
      if (now >= until) {
        clearAdminSessionDeadline();
        void logout();
        return false;
      }
      setAdminSessionDeadlineMs(until);
      return true;
    };
    if (!run()) {
      return;
    }
    const id = window.setInterval(() => {
      const now = Date.now();
      const until = readAdminSessionDeadlineMs();
      if (until != null && now >= until) {
        clearAdminSessionDeadline();
        void logout();
      }
    }, 5000);
    return () => window.clearInterval(id);
  }, [accessToken, user, logout]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      accessToken,
      authReady,
      adminSessionDeadlineMs,
      startGoogleLogin,
      logout,
      refreshUser,
      applyLoginUser,
      applyAccessToken,
    }),
    [
      user,
      accessToken,
      authReady,
      adminSessionDeadlineMs,
      startGoogleLogin,
      logout,
      refreshUser,
      applyLoginUser,
      applyAccessToken,
    ]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  return ctx ?? AUTH_CONTEXT_FALLBACK;
}
