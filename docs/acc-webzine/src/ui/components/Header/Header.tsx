/**
 * Header — 다크 매거진형 (IDOL + GLOW, 우측 네비 + 검색/테마)
 */

import { useCallback, useEffect, useId, useRef, useState } from "react";
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import type { To } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import {
  changePassword,
  checkSignupEmail,
  checkSignupNickname,
  getGoogleLoginUrl,
  getOAuthLoginUrl,
  loginWithPassword,
  requestAccountIdReminder,
  requestSignupEmailVerification,
  requestTemporaryPassword,
  signupWithPassword,
  uploadProfileImage,
} from "../../../auth/authApi";
import { NICKNAME_PATTERN } from "../../../lib/nicknameValidation";
import {
  GLOW_ALERTS_CHANGED_EVENT,
  fetchGlowAlertUnreadCount,
} from "../../../shared/data/glowAlertsApi";
import { useTheme } from "../../hooks/useTheme";
import {
  UI_LANG_STORAGE_KEY,
  type UiLang,
  readUiLang,
  writeUiLang,
} from "../../i18n/uiLangStorage";
import { HeaderSearchPanel } from "../HeaderSearchPanel/HeaderSearchPanel";
import styles from "./Header.module.css";

type NavId = "glow" | "wish" | "events" | "archive";

type NavItemDef = {
  readonly id: NavId;
  readonly to: To;
  readonly label: string;
  readonly ariaLabel: string;
};

const NAV_ITEMS: ReadonlyArray<NavItemDef> = [
  { id: "glow", to: "/glow", label: "GLOW", ariaLabel: "GLOW 추천 페이지로 이동" },
  { id: "wish", to: "/wish", label: "WISH", ariaLabel: "WISH 페이지로 이동" },
  {
    id: "events",
    to: "/events",
    label: "이벤트",
    ariaLabel: "이벤트 페이지로 이동",
  },
  {
    id: "archive",
    to: "/archive",
    label: "아카이브",
    ariaLabel: "아카이브 페이지로 이동",
  },
];

function isNavItemActive(pathname: string, search: string, id: NavId): boolean {
  switch (id) {
    case "glow":
      return pathname === "/glow" || pathname === "/glow_map";
    case "wish":
      return pathname === "/wish";
    case "events":
      return pathname === "/events";
    case "archive":
      return pathname === "/archive";
    default:
      return false;
  }
}

const SearchIcon = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    aria-hidden="true"
  >
    <circle cx="11" cy="11" r="8" />
    <path d="M21 21l-4.35-4.35" />
  </svg>
);

const MenuIcon = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    aria-hidden="true"
  >
    <path d="M3 12h18M3 6h18M3 18h18" />
  </svg>
);

const CloseIcon = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    aria-hidden="true"
  >
    <path d="M18 6L6 18M6 6l12 12" />
  </svg>
);

const SunIcon = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    aria-hidden="true"
  >
    <circle cx="12" cy="12" r="5" />
    <line x1="12" y1="1" x2="12" y2="3" />
    <line x1="12" y1="21" x2="12" y2="23" />
    <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
    <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
    <line x1="1" y1="12" x2="3" y2="12" />
    <line x1="21" y1="12" x2="23" y2="12" />
    <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
    <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
  </svg>
);

const MoonIcon = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    aria-hidden="true"
  >
    <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
  </svg>
);

const BellIcon = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    aria-hidden="true"
  >
    <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
    <path d="M13.73 21a2 2 0 0 1-3.46 0" />
  </svg>
);

const GlobeIcon = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    aria-hidden="true"
  >
    <circle cx="12" cy="12" r="10" />
    <path d="M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
  </svg>
);

/** 소셜 로그인 모달용 — 원 안에 표시 (viewBox 24) */
const IconLoginNaver = () => (
  <svg className={styles.socialSvg} viewBox="0 0 24 24" aria-hidden="true">
    <path
      fill="currentColor"
      d="M7.2 6.5h4.1l3.1 4.7 3.1-4.7h2.3V17.5h-3.9v-5.2l-2.2 3.3h-.1l-2.2-3.3v5.2H7.2V6.5z"
    />
  </svg>
);

const IconLoginKakao = () => (
  <svg className={styles.socialSvg} viewBox="0 0 24 24" aria-hidden="true">
    <path
      fill="currentColor"
      d="M12 4.5c-3.9 0-7 2.5-7 5.6 0 2 1.3 3.8 3.2 4.8l-.7 2.6c-.1.4.3.7.6.5l3.1-2.1h.8c3.9 0 7-2.5 7-5.6S15.9 4.5 12 4.5z"
    />
  </svg>
);

const UserMenuIcon = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.75"
    aria-hidden="true"
  >
    <circle cx="12" cy="8" r="3.5" />
    <path strokeLinecap="round" d="M6.5 19.5v-.5a5.5 5.5 0 0111 0v.5" />
  </svg>
);

const IconLoginGoogle = () => (
  <svg className={styles.socialSvg} viewBox="0 0 24 24" aria-hidden="true">
    <path
      fill="#EA4335"
      d="M12 10.2v3.9h5.4c-.2 1.3-1 2.4-2.1 3.1l3.4 2.6c2-1.8 3.1-4.5 3.1-7.6 0-.7-.1-1.4-.2-2H12z"
    />
    <path
      fill="#34A853"
      d="M5.8 14.1l-.8.6-2.8 2.2C4.4 19.8 7.9 22 12 22c3 0 5.5-1 7.4-2.6l-3.4-2.6c-1 .7-2.2 1.1-4 1.1-3.1 0-5.7-2.1-6.6-4.9z"
    />
    <path
      fill="#FBBC05"
      d="M2.2 7.3C1.4 8.9 1 10.7 1 12.5s.4 3.6 1.2 5.2l3.7-2.9c-.4-1.1-.4-2.4 0-3.5L2.2 7.3z"
    />
    <path
      fill="#4285F4"
      d="M12 5.4c1.7 0 2.9.7 3.5 1.3l2.6-2.5C17.5 2.7 15 1.5 12 1.5 7.9 1.5 4.4 3.7 2.6 7.3l3.7 2.9c.9-2.8 3.5-4.8 6.7-4.8z"
    />
  </svg>
);

export const Header = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const loginTitleId = useId();
  const joinTitleId = useId();
  const pwdTitleId = useId();
  const findIdTitleId = useId();
  const forceChangeTitleId = useId();
  const recoveryMessageId = useId();
  const pwdEmailFieldId = useId();
  const loginEmailFieldId = useId();
  const loginPwFieldId = useId();
  const loginSaveFieldId = useId();
  const joinAgreementFieldId = useId();
  const joinEmailFieldId = useId();
  const joinNameFieldId = useId();
  const joinPwFieldId = useId();
  const joinPwConfirmFieldId = useId();
  const joinSubscribeFieldId = useId();
  const joinAvatarFieldId = useId();
  const loginCloseRef = useRef<HTMLButtonElement>(null);

  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [isLoginOpen, setIsLoginOpen] = useState(false);
  const [authModalView, setAuthModalView] = useState<
    "login" | "join" | "forgot-password" | "find-id" | "force-change"
  >("login");
  const [loginPanelExiting, setLoginPanelExiting] = useState(false);
  const [loginExitTarget, setLoginExitTarget] = useState<
    "join" | "forgot-password" | "find-id"
  >("join");
  const [loginEmail, setLoginEmail] = useState("");
  const [loginPassword, setLoginPassword] = useState("");
  const [saveLoginEmail, setSaveLoginEmail] = useState(false);
  const [joinAgreed, setJoinAgreed] = useState(false);
  const [joinEmail, setJoinEmail] = useState("");
  const [joinNickname, setJoinNickname] = useState("");
  const [joinPw, setJoinPw] = useState("");
  const [joinPwConfirm, setJoinPwConfirm] = useState("");
  const [joinSubscribe, setJoinSubscribe] = useState(false);
  const [joinAvatarFile, setJoinAvatarFile] = useState<File | null>(null);
  const [forgotEmail, setForgotEmail] = useState("");
  const [joinMessage, setJoinMessage] = useState("");
  const [loginMessage, setLoginMessage] = useState("");
  const [forgotMessage, setForgotMessage] = useState("");
  const [tempPasswordForChange, setTempPasswordForChange] = useState("");
  const [forcedAccessToken, setForcedAccessToken] = useState("");
  const [forcedNewPassword, setForcedNewPassword] = useState("");
  const [forcedNewPasswordConfirm, setForcedNewPasswordConfirm] = useState("");
  const [forcedPasswordMessage, setForcedPasswordMessage] = useState("");
  const [uiLang, setUiLang] = useState<UiLang>(() => readUiLang());
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);
  const {
    user,
    authReady,
    accessToken,
    logout,
    applyAccessToken,
    refreshUser,
    applyLoginUser,
  } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const [unreadAlertCount, setUnreadAlertCount] = useState(0);

  const closeMobileMenu = useCallback(() => {
    setIsMobileMenuOpen(false);
  }, []);

  const closeSearch = useCallback(() => {
    setIsSearchOpen(false);
  }, []);

  const toggleMobileMenu = useCallback(() => {
    setIsMobileMenuOpen((prev) => {
      if (!prev) setIsSearchOpen(false);
      return !prev;
    });
  }, []);

  const toggleSearch = useCallback(() => {
    setIsSearchOpen((prev) => {
      if (!prev) setIsMobileMenuOpen(false);
      return !prev;
    });
  }, []);

  useEffect(() => {
    let active = true;

    const loadUnreadCount = () => {
      void fetchGlowAlertUnreadCount(accessToken).then((count) => {
        if (active) {
          setUnreadAlertCount(count);
        }
      });
    };

    loadUnreadCount();
    const handleGlowAlertsChanged = (event: Event) => {
      const unreadCount = (event as CustomEvent<{ unreadCount?: number }>).detail?.unreadCount;
      if (typeof unreadCount === "number" && Number.isFinite(unreadCount)) {
        setUnreadAlertCount(unreadCount);
        return;
      }
      loadUnreadCount();
    };
    const timerId = window.setInterval(loadUnreadCount, 60_000);
    window.addEventListener(GLOW_ALERTS_CHANGED_EVENT, handleGlowAlertsChanged);

    return () => {
      active = false;
      window.clearInterval(timerId);
      window.removeEventListener(GLOW_ALERTS_CHANGED_EVENT, handleGlowAlertsChanged);
    };
  }, [accessToken, location.pathname]);

  useEffect(() => {
    if (!userMenuOpen) return;
    const onDocMouseDown = (e: MouseEvent) => {
      if (userMenuRef.current?.contains(e.target as Node)) return;
      setUserMenuOpen(false);
    };
    document.addEventListener("mousedown", onDocMouseDown);
    return () => document.removeEventListener("mousedown", onDocMouseDown);
  }, [userMenuOpen]);

  const closeLogin = useCallback(() => {
    setIsLoginOpen(false);
    setAuthModalView("login");
    setLoginPanelExiting(false);
    setLoginExitTarget("join");
    setForcedAccessToken("");
    setTempPasswordForChange("");
  }, []);

  const openLoginModal = useCallback(() => {
    setAuthModalView("login");
    setLoginPanelExiting(false);
    setLoginExitTarget("join");
    setIsLoginOpen(true);
  }, []);

  const openJoinModal = useCallback(() => {
    setAuthModalView("join");
    setLoginPanelExiting(false);
    setLoginExitTarget("join");
    setIsLoginOpen(true);
  }, []);

  const mypageGuestModalShownRef = useRef(false);

  useEffect(() => {
    if (!authReady) return;
    if (location.pathname !== "/mypage") {
      mypageGuestModalShownRef.current = false;
      return;
    }
    if (user) {
      mypageGuestModalShownRef.current = false;
      return;
    }
    if (mypageGuestModalShownRef.current) return;
    mypageGuestModalShownRef.current = true;
    openLoginModal();
  }, [authReady, user, location.pathname, openLoginModal]);

  useEffect(() => {
    const q = new URLSearchParams(location.search);
    const shouldOpenLogin = q.get("login") === "1";
    const shouldOpenJoin = q.get("join") === "1" || q.get("view") === "join";
    if (!shouldOpenLogin && !shouldOpenJoin) return;
    if (shouldOpenJoin) {
      openJoinModal();
    } else {
      openLoginModal();
    }
    q.delete("login");
    q.delete("join");
    q.delete("view");
    const s = q.toString();
    navigate(
      { pathname: location.pathname, search: s ? `?${s}` : "" },
      { replace: true },
    );
  }, [location.pathname, location.search, navigate, openJoinModal, openLoginModal]);

  const openJoinFromLogin = useCallback(() => {
    setLoginExitTarget("join");
    if (
      typeof window !== "undefined" &&
      window.matchMedia("(prefers-reduced-motion: reduce)").matches
    ) {
      setAuthModalView("join");
      return;
    }
    setLoginPanelExiting(true);
  }, []);

  const openForgotPasswordFromLogin = useCallback(() => {
    setForgotMessage("");
    setLoginExitTarget("forgot-password");
    if (
      typeof window !== "undefined" &&
      window.matchMedia("(prefers-reduced-motion: reduce)").matches
    ) {
      setAuthModalView("forgot-password");
      return;
    }
    setLoginPanelExiting(true);
  }, []);

  const openFindIdFromLogin = useCallback(() => {
    setForgotMessage("");
    setLoginExitTarget("find-id");
    if (
      typeof window !== "undefined" &&
      window.matchMedia("(prefers-reduced-motion: reduce)").matches
    ) {
      setAuthModalView("find-id");
      return;
    }
    setLoginPanelExiting(true);
  }, []);

  const onLoginExitAnimationEnd = useCallback(() => {
    if (!loginPanelExiting) return;
    setLoginPanelExiting(false);
    setAuthModalView(loginExitTarget);
  }, [loginPanelExiting, loginExitTarget]);

  useEffect(() => {
    if (!loginPanelExiting) return;
    const target = loginExitTarget;
    const t = window.setTimeout(() => {
      setLoginPanelExiting(false);
      setAuthModalView(target);
    }, 500);
    return () => window.clearTimeout(t);
  }, [loginPanelExiting, loginExitTarget]);

  useEffect(() => {
    if (!isSearchOpen) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") setIsSearchOpen(false);
    };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [isSearchOpen]);

  useEffect(() => {
    if (!isLoginOpen) return;
    document.documentElement.style.overflow = "hidden";
    if (authModalView === "login" && !loginPanelExiting) {
      loginCloseRef.current?.focus();
    }
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") closeLogin();
    };
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.documentElement.style.overflow = "";
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [isLoginOpen, authModalView, loginPanelExiting, closeLogin]);

  useEffect(() => {
    if (!isLoginOpen || authModalView !== "join") return;
    const t = window.setTimeout(() => loginCloseRef.current?.focus(), 80);
    return () => window.clearTimeout(t);
  }, [isLoginOpen, authModalView]);

  useEffect(() => {
    document.documentElement.lang = uiLang === "ko" ? "ko" : "en";
  }, [uiLang]);

  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key !== UI_LANG_STORAGE_KEY || e.newValue == null) return;
      if (e.newValue === "en" || e.newValue === "ko") setUiLang(e.newValue);
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  const toggleUiLang = useCallback(() => {
    setUiLang((prev) => {
      const next: UiLang = prev === "ko" ? "en" : "ko";
      writeUiLang(next);
      return next;
    });
  }, []);

  const resolvePostAuthPath = useCallback(() => {
    const q = new URLSearchParams(location.search);
    const redirect = q.get("redirect");
    if (redirect?.startsWith("/") && !redirect.startsWith("//")) {
      return redirect;
    }
    return "/mypage";
  }, [location.search]);

  const handleLoginSubmit = useCallback(
    (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      setLoginMessage("");
      void (async () => {
        const email = loginEmail.trim();
        const password = loginPassword;
        if (!email || !password) {
          setLoginMessage("이메일과 비밀번호를 입력해 주세요.");
          return;
        }
        const result = await loginWithPassword({ email, password });
        if (!result.ok) {
          setLoginMessage(result.message);
          return;
        }
        await applyAccessToken(result.accessToken);
        if (result.requirePasswordChange) {
          setTempPasswordForChange(password);
          setForcedAccessToken(result.accessToken);
          setForcedNewPassword("");
          setForcedNewPasswordConfirm("");
          setForcedPasswordMessage(
            "임시 비밀번호로 로그인했습니다. 새 비밀번호를 설정해 주세요.",
          );
          setAuthModalView("force-change");
          return;
        }
        setIsLoginOpen(false);
        setAuthModalView("login");
        setLoginEmail(saveLoginEmail ? email : "");
        setLoginPassword("");
        setLoginMessage("");
        navigate(resolvePostAuthPath(), { replace: true });
      })();
    },
    [
      loginEmail,
      loginPassword,
      saveLoginEmail,
      applyAccessToken,
      navigate,
      resolvePostAuthPath,
    ],
  );

  const handleJoinEmailDupCheck = useCallback(async () => {
    setJoinMessage("");
    const email = joinEmail.trim();
    if (!email) {
      setJoinMessage("이메일을 입력한 뒤 중복확인을 눌러 주세요.");
      return;
    }
    const res = await checkSignupEmail(email);
    if (!res) {
      setJoinMessage("중복확인 중 오류가 발생했습니다.");
      return;
    }
    if (res.available) {
      setJoinMessage("사용 가능한 이메일입니다.");
      return;
    }
    if (res.code === "TAKEN") {
      setJoinMessage("이미 가입된 이메일입니다.");
      return;
    }
    if (res.code === "INVALID_FORMAT") {
      setJoinMessage("올바른 이메일 형식을 입력해 주세요.");
      return;
    }
    setJoinMessage("이메일을 입력해 주세요.");
  }, [joinEmail]);

  const handleJoinEmailVerificationRequest = useCallback(async () => {
    setJoinMessage("");
    const email = joinEmail.trim();
    if (!email) {
      setJoinMessage("이메일을 입력해 주세요.");
      return;
    }
    const checked = await checkSignupEmail(email);
    if (!checked?.available) {
      setJoinMessage("이메일 중복확인을 먼저 완료해 주세요.");
      return;
    }
    const result = await requestSignupEmailVerification(email);
    if (!result) {
      setJoinMessage("인증 메일 발송에 실패했습니다.");
      return;
    }
    if (!result.sent) {
      setJoinMessage(
        "인증 메일을 발송할 수 없습니다. 이메일 상태를 다시 확인해 주세요.",
      );
      return;
    }
    setJoinMessage(
      "인증 메일을 발송했습니다. 5분 내 메일 링크를 눌러 인증을 완료해 주세요.",
    );
  }, [joinEmail]);

  const handleJoinNicknameDupCheck = useCallback(async () => {
    setJoinMessage("");
    const nick = joinNickname.trim();
    if (!nick) {
      setJoinMessage("별명을 입력한 뒤 중복확인을 눌러 주세요.");
      return;
    }
    const res = await checkSignupNickname(nick);
    if (!res) {
      setJoinMessage("중복확인 중 오류가 발생했습니다.");
      return;
    }
    if (res.available) {
      setJoinMessage("사용 가능한 별명입니다.");
      return;
    }
    if (res.code === "TAKEN") {
      setJoinMessage("등록된 별명입니다.");
      return;
    }
    if (res.code === "INVALID_FORMAT") {
      setJoinMessage(
        "별명은 2~10자의 한글·영문 또는 숫자만 사용할 수 있습니다.",
      );
      return;
    }
    setJoinMessage("별명을 입력해 주세요.");
  }, [joinNickname]);

  const handleJoinSubmit = useCallback(
    async (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      setJoinMessage("");
      if (!joinAgreed) {
        setJoinMessage("개인정보 수집 및 활용에 동의해 주세요.");
        return;
      }
      const email = joinEmail.trim();
      const nickname = joinNickname.trim();
      if (!email) {
        setJoinMessage("이메일을 입력해 주세요.");
        return;
      }
      if (!nickname) {
        setJoinMessage("별명을 입력해 주세요.");
        return;
      }
      if (!NICKNAME_PATTERN.test(nickname)) {
        setJoinMessage(
          "별명은 2~10자의 한글·영문 또는 숫자만 사용할 수 있습니다.",
        );
        return;
      }
      const pw = joinPw.trim();
      const pwConfirm = joinPwConfirm.trim();
      if (!pw) {
        setJoinMessage("비밀번호를 입력해 주세요.");
        return;
      }
      if (pw.length < 8 || pw.length > 72) {
        setJoinMessage(
          "비밀번호는 8자 이상이며 영문과 숫자를 각각 1자 이상 포함해야 합니다.",
        );
        return;
      }
      if (!/[a-zA-Z]/.test(pw) || !/[0-9]/.test(pw)) {
        setJoinMessage(
          "비밀번호는 8자 이상이며 영문과 숫자를 각각 1자 이상 포함해야 합니다.",
        );
        return;
      }
      if (pw !== pwConfirm) {
        setJoinMessage("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        return;
      }
      const result = await signupWithPassword({
        email,
        nickname,
        password: pw,
        subscribeToUpdates: joinSubscribe,
      });
      if (!result.ok) {
        setJoinMessage(result.message);
        return;
      }
      await applyAccessToken(result.accessToken);
      if (joinAvatarFile) {
        const uploaded = await uploadProfileImage(
          result.accessToken,
          joinAvatarFile,
        );
        if (!uploaded.ok) {
          setJoinMessage(uploaded.message);
          return;
        }
        applyLoginUser(uploaded.user);
        await refreshUser();
      }
      setIsLoginOpen(false);
      setAuthModalView("login");
      setJoinEmail("");
      setJoinNickname("");
      setJoinPw("");
      setJoinPwConfirm("");
      setJoinAgreed(false);
      setJoinSubscribe(false);
      setJoinAvatarFile(null);
      setJoinMessage("");
      navigate(resolvePostAuthPath(), { replace: true });
    },
    [
      joinAgreed,
      joinEmail,
      joinNickname,
      joinPw,
      joinPwConfirm,
      joinAvatarFile,
      applyAccessToken,
      applyLoginUser,
      refreshUser,
      navigate,
      resolvePostAuthPath,
    ],
  );

  const handlePwdSubmit = useCallback(
    (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      setForgotMessage("");
      void (async () => {
        const email = forgotEmail.trim();
        if (!email) {
          setForgotMessage("이메일을 입력해 주세요.");
          return;
        }
        const result = await requestTemporaryPassword(email);
        if (!result) {
          setForgotMessage("임시 비밀번호 발급 요청에 실패했습니다.");
          return;
        }
        setForgotMessage(result.message);
      })();
    },
    [forgotEmail],
  );

  const handleFindId = useCallback(() => {
    setForgotMessage("");
    void (async () => {
      const email = forgotEmail.trim();
      if (!email) {
        setForgotMessage("이메일을 입력해 주세요.");
        return;
      }
      const result = await requestAccountIdReminder(email);
      if (!result) {
        setForgotMessage("아이디 찾기 요청에 실패했습니다.");
        return;
      }
      setForgotMessage(result.message);
    })();
  }, [forgotEmail]);

  const handleForcedPasswordSubmit = useCallback(
    (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      setForcedPasswordMessage("");
      void (async () => {
        const next = forcedNewPassword.trim();
        const confirm = forcedNewPasswordConfirm.trim();
        if (!next || !confirm) {
          setForcedPasswordMessage("새 비밀번호와 확인 값을 입력해 주세요.");
          return;
        }
        if (next !== confirm) {
          setForcedPasswordMessage(
            "새 비밀번호와 비밀번호 확인이 일치하지 않습니다.",
          );
          return;
        }
        if (
          next.length < 8 ||
          next.length > 72 ||
          !/[a-zA-Z]/.test(next) ||
          !/[0-9]/.test(next)
        ) {
          setForcedPasswordMessage(
            "비밀번호는 8자 이상이며 영문과 숫자를 각각 1자 이상 포함해야 합니다.",
          );
          return;
        }
        if (!forcedAccessToken) {
          setForcedPasswordMessage(
            "세션이 만료되었습니다. 다시 로그인해 주세요.",
          );
          setAuthModalView("login");
          return;
        }
        const changed = await changePassword(forcedAccessToken, {
          currentPassword: tempPasswordForChange,
          newPassword: next,
        });
        if (!changed.ok) {
          setForcedPasswordMessage(changed.message);
          return;
        }
        setForcedPasswordMessage("");
        setIsLoginOpen(false);
        setAuthModalView("login");
        setLoginPassword("");
        navigate(resolvePostAuthPath(), { replace: true });
      })();
    },
    [
      forcedNewPassword,
      forcedNewPasswordConfirm,
      tempPasswordForChange,
      forcedAccessToken,
      navigate,
      resolvePostAuthPath,
    ],
  );

  const linkClass = (id: NavId) => {
    const active = isNavItemActive(location.pathname, location.search, id);
    return `${styles.navLink} ${active ? styles.navLinkActive : ""}`;
  };

  return (
    <>
      <header className={styles.header} role="banner">
        <div className={styles.headerTop}>
          <Link
            to="/"
            className={styles.logo}
            aria-label="IDOL GLOW 홈으로 이동"
          >
            <span className={styles.logoIdol}>Idol</span>
            <span className={styles.logoGlow}>Glow</span>
          </Link>

          <div className={styles.rightCluster}>
            <nav className={styles.nav} aria-label="메인 네비게이션">
              <ul className={styles.navList} role="menubar">
                {NAV_ITEMS.map((item) => (
                  <li key={item.id} className={styles.navItem}>
                    <NavLink
                      to={item.to}
                      end
                      className={() => linkClass(item.id)}
                      aria-label={item.ariaLabel}
                      role="menuitem"
                    >
                      {item.label}
                    </NavLink>
                  </li>
                ))}
              </ul>
            </nav>

            <div className={styles.headerMisc}>
              {authReady && user ? (
                <div className={styles.userMenuWrap} ref={userMenuRef}>
                  <button
                    type="button"
                    className={styles.userMenuTrigger}
                    aria-haspopup="menu"
                    aria-expanded={userMenuOpen}
                    onClick={() => setUserMenuOpen((v) => !v)}
                    aria-label="회원메뉴"
                  >
                    <UserMenuIcon />
                  </button>
                  {userMenuOpen ? (
                    <div className={styles.userMenuDropdown} role="menu">
                      <button
                        type="button"
                        role="menuitem"
                        className={styles.userMenuItem}
                        onClick={() => {
                          setUserMenuOpen(false);
                          void logout();
                        }}
                      >
                        로그아웃
                      </button>
                      <Link
                        to="/mypage"
                        role="menuitem"
                        className={styles.userMenuItem}
                        onClick={() => setUserMenuOpen(false)}
                      >
                        나의정보
                      </Link>
                    </div>
                  ) : null}
                </div>
              ) : (
                <button
                  type="button"
                  className={styles.loginWinona}
                  data-text="로그인"
                  onClick={openLoginModal}
                  aria-haspopup="dialog"
                  aria-expanded={isLoginOpen}
                  aria-controls="login-modal"
                >
                  <span>로그인</span>
                </button>
              )}

              <button
                type="button"
                className={styles.langButton}
                onClick={toggleUiLang}
                aria-label={
                  uiLang === "ko"
                    ? "한국어 사용 중. 영어로 전환"
                    : "English. 한국어로 전환"
                }
              >
                <span className={styles.langIcon} aria-hidden="true">
                  <GlobeIcon />
                </span>
                <span className={styles.langText} aria-hidden="true">
                  {uiLang === "ko" ? "KO" : "ENG"}
                </span>
              </button>

              <button
                type="button"
                className={styles.iconButton}
                aria-label={isSearchOpen ? "검색 닫기" : "검색 열기"}
                aria-expanded={isSearchOpen}
                aria-controls="header-search-panel"
                onClick={toggleSearch}
              >
                <SearchIcon />
              </button>

              <button
                type="button"
                className={styles.iconButton}
                aria-label={
                  theme === "dark" ? "라이트 모드로 전환" : "다크 모드로 전환"
                }
                onClick={toggleTheme}
              >
                {theme === "dark" ? <SunIcon /> : <MoonIcon />}
              </button>

              <button
                type="button"
                className={`${styles.iconButton} ${styles.alertButton}`}
                aria-label={
                  unreadAlertCount > 0
                    ? `Glow 알림으로 이동, 읽지 않은 알림 ${unreadAlertCount}개`
                    : "Glow 알림으로 이동"
                }
                onClick={() => navigate("/glow-alerts")}
              >
                <BellIcon />
                {unreadAlertCount > 0 ? (
                  <span className={styles.alertBadge} aria-hidden="true">
                    {unreadAlertCount > 99 ? "99+" : unreadAlertCount}
                  </span>
                ) : null}
              </button>

              <button
                type="button"
                className={`${styles.iconButton} ${styles.menuButton}`}
                onClick={toggleMobileMenu}
                aria-label={isMobileMenuOpen ? "메뉴 닫기" : "메뉴 열기"}
                aria-expanded={isMobileMenuOpen}
                aria-controls="mobile-nav"
              >
                {isMobileMenuOpen ? <CloseIcon /> : <MenuIcon />}
              </button>
            </div>
          </div>
        </div>

        {isSearchOpen && (
          <div id="header-search-panel" className={styles.searchStrip}>
            <HeaderSearchPanel onClose={closeSearch} />
          </div>
        )}

        <nav
          id="mobile-nav"
          className={`${styles.mobileNav} ${isMobileMenuOpen ? styles.mobileNavOpen : ""}`}
          aria-label="모바일 네비게이션"
          aria-hidden={!isMobileMenuOpen}
        >
          <ul className={styles.mobileNavList}>
            {NAV_ITEMS.map((item) => (
              <li key={item.id} className={styles.navItem}>
                <NavLink
                  to={item.to}
                  end
                  className={() => linkClass(item.id)}
                  onClick={closeMobileMenu}
                  aria-label={item.ariaLabel}
                >
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      </header>

      {isLoginOpen && (
        <div
          className={styles.modalBackdrop}
          role="presentation"
          onClick={closeLogin}
        >
          <div
            id="login-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby={
              authModalView === "join"
                ? joinTitleId
                : authModalView === "forgot-password"
                  ? pwdTitleId
                  : authModalView === "find-id"
                    ? findIdTitleId
                    : authModalView === "force-change"
                      ? forceChangeTitleId
                      : loginTitleId
            }
            className={`${styles.loginModal} ${authModalView === "join" ? styles.loginModalWide : ""}`}
            onClick={(e) => e.stopPropagation()}
          >
            <button
              ref={loginCloseRef}
              type="button"
              className={styles.loginModalDismiss}
              onClick={closeLogin}
              aria-label={
                authModalView === "join"
                  ? "회원가입 창 닫기"
                  : authModalView === "forgot-password"
                    ? "비밀번호 찾기 창 닫기"
                    : authModalView === "find-id"
                      ? "아이디 찾기 창 닫기"
                      : authModalView === "force-change"
                        ? "비밀번호 변경 창 닫기"
                        : "로그인 창 닫기"
              }
            >
              <CloseIcon />
            </button>

            {authModalView === "login" && (
              <form
                className={`${styles.loginForm} ${loginPanelExiting ? styles.loginFormExit : ""}`}
                onSubmit={handleLoginSubmit}
                onAnimationEnd={onLoginExitAnimationEnd}
                autoComplete="off"
                noValidate
              >
                <div className={styles.loginAgree} aria-hidden="true" />
                <div className={styles.loginInput}>
                  <h2 id={loginTitleId} className={styles.loginModalTitle}>
                    로그인
                  </h2>
                  <div className={styles.loginIdpw}>
                    <div className={styles.formGroup}>
                      <label
                        htmlFor={loginEmailFieldId}
                        className={styles.srOnly}
                      >
                        이메일
                      </label>
                      <input
                        id={loginEmailFieldId}
                        type="email"
                        className={styles.formControl}
                        value={loginEmail}
                        onChange={(e) => setLoginEmail(e.target.value)}
                        placeholder="이메일 주소 입력"
                        autoComplete="email"
                      />
                    </div>
                    <div className={styles.formGroup}>
                      <label htmlFor={loginPwFieldId} className={styles.srOnly}>
                        비밀번호
                      </label>
                      <input
                        id={loginPwFieldId}
                        type="password"
                        className={styles.formControl}
                        value={loginPassword}
                        onChange={(e) => setLoginPassword(e.target.value)}
                        placeholder="비밀번호 입력"
                        autoComplete="new-password"
                      />
                    </div>
                  </div>
                  <div className={styles.loginSave}>
                    <input
                      id={loginSaveFieldId}
                      type="checkbox"
                      className={styles.loginSaveCheckbox}
                      checked={saveLoginEmail}
                      onChange={(e) => setSaveLoginEmail(e.target.checked)}
                    />
                    <label
                      htmlFor={loginSaveFieldId}
                      className={styles.loginSaveLabel}
                    >
                      이메일주소 저장
                    </label>
                  </div>
                  <div className={styles.loginButton}>
                    <button type="submit" className={styles.btnLoginSubmit}>
                      로그인
                    </button>
                  </div>
                  <div
                    id="login_message"
                    className={styles.joinFeedback}
                    role="status"
                    aria-live="polite"
                  >
                    {loginMessage}
                  </div>
                  <div className={styles.socialLoginTitle}>
                    <span className={styles.socialLoginTitleText}>
                      소셜 계정으로 간편 로그인
                    </span>
                  </div>
                  <ul className={styles.socialLogin} id="wrap_social_login">
                    <li className={styles.loginNaver}>
                      <a
                        href={getOAuthLoginUrl("naver")}
                        className={styles.slNaver}
                        data-provider="naver"
                        aria-label="네이버로 로그인"
                      >
                        <span className={styles.wrapIcon}>
                          <IconLoginNaver />
                        </span>
                      </a>
                    </li>
                    <li className={styles.loginKakao}>
                      <a
                        href={getOAuthLoginUrl("kakao")}
                        className={styles.slKakao}
                        data-provider="kakao"
                        aria-label="카카오로 로그인"
                      >
                        <span className={styles.wrapIcon}>
                          <IconLoginKakao />
                        </span>
                      </a>
                    </li>
                    <li className={styles.loginGoogle}>
                      <a
                        href={getGoogleLoginUrl()}
                        className={styles.slGoogle}
                        data-provider="google"
                        aria-label="Google로 로그인"
                      >
                        <span className={styles.wrapIcon}>
                          <IconLoginGoogle />
                        </span>
                      </a>
                    </li>
                  </ul>
                  <div className={styles.loginJoin}>
                    <button
                      type="button"
                      className={styles.btnLoginJoin}
                      onClick={openJoinFromLogin}
                    >
                      간편 회원가입
                    </button>
                    <button
                      type="button"
                      className={styles.btnFindPwd}
                      onClick={openFindIdFromLogin}
                    >
                      아이디 찾기
                    </button>
                    <button
                      type="button"
                      className={styles.btnFindPwd}
                      onClick={openForgotPasswordFromLogin}
                    >
                      비밀번호 찾기
                    </button>
                  </div>
                </div>
              </form>
            )}

            {authModalView === "join" && (
              <div className={styles.joinModalBody}>
                <h3 id={joinTitleId} className={styles.srOnly}>
                  회원가입
                </h3>
                <form
                  className={styles.joinForm}
                  onSubmit={handleJoinSubmit}
                  autoComplete="off"
                  noValidate
                >
                  <div className={styles.joinAgree}>
                    <h5 className={styles.joinSectionTitle}>
                      개인정보 수집 및 활용 동의
                    </h5>
                    <div className={styles.joinAgreement}>
                      <div className={styles.agreePrivacy}>
                        <dl className={styles.agreeDl}>
                          <dt>1. 이용목적</dt>
                          <dd>&apos;Idol Glow&apos; 웹진 서비스 이용</dd>
                        </dl>
                        <dl className={styles.agreeDl}>
                          <dt>2. 수집항목</dt>
                          <dd>이메일</dd>
                        </dl>
                        <dl className={styles.agreeDl}>
                          <dt>3. 보유기간</dt>
                          <dd>수집한 이메일은 회원탈퇴 시까지 보유</dd>
                        </dl>
                        <dl className={styles.agreeDl}>
                          <dt>4. 동의여부</dt>
                          <dd>
                            개인 정보 수집 동의 후 웹진 서비스를 이용하실 수
                            있습니다.
                          </dd>
                        </dl>
                      </div>
                      <div className={styles.agreeConfirm}>
                        <input
                          id={joinAgreementFieldId}
                          type="checkbox"
                          className={styles.loginSaveCheckbox}
                          checked={joinAgreed}
                          onChange={(e) => setJoinAgreed(e.target.checked)}
                        />
                        <label
                          htmlFor={joinAgreementFieldId}
                          className={styles.loginSaveLabel}
                        >
                          개인정보 수집 및 활용에 동의합니다.
                        </label>
                      </div>
                    </div>
                  </div>
                  <div className={styles.joinInputSection}>
                    <h5 className={styles.joinSectionTitle}>간편 회원가입</h5>
                    <div className={styles.loginIdpw}>
                      <div className={styles.formGroup}>
                        <label
                          htmlFor={joinEmailFieldId}
                          className={styles.srOnly}
                        >
                          이메일
                        </label>
                        <div className={styles.inputGroup}>
                          <input
                            id={joinEmailFieldId}
                            type="email"
                            className={styles.formControl}
                            value={joinEmail}
                            onChange={(e) => setJoinEmail(e.target.value)}
                            placeholder="이메일 주소 입력"
                            autoComplete="email"
                          />
                          <div className={styles.inputGroupAppend}>
                            <button
                              type="button"
                              className={styles.btnDupCheck}
                              onClick={() => void handleJoinEmailDupCheck()}
                            >
                              중복확인
                            </button>
                            <button
                              type="button"
                              className={styles.btnDupCheck}
                              onClick={() =>
                                void handleJoinEmailVerificationRequest()
                              }
                            >
                              인증메일
                            </button>
                          </div>
                        </div>
                      </div>
                      <div className={styles.formGroup}>
                        <label
                          htmlFor={joinNameFieldId}
                          className={styles.srOnly}
                        >
                          별명
                        </label>
                        <div className={styles.inputGroup}>
                          <input
                            id={joinNameFieldId}
                            type="text"
                            className={styles.formControl}
                            value={joinNickname}
                            onChange={(e) => setJoinNickname(e.target.value)}
                            placeholder="별명 입력 (2~10자 한글·영문·숫자)"
                            maxLength={10}
                            autoComplete="nickname"
                          />
                          <div className={styles.inputGroupAppend}>
                            <button
                              type="button"
                              className={styles.btnDupCheck}
                              onClick={() => void handleJoinNicknameDupCheck()}
                            >
                              중복확인
                            </button>
                          </div>
                        </div>
                      </div>
                      <div className={styles.formGroup}>
                        <label
                          htmlFor={joinAvatarFieldId}
                          className={styles.srOnly}
                        >
                          프로필 사진
                        </label>
                        <input
                          id={joinAvatarFieldId}
                          type="file"
                          accept="image/jpeg,image/png,image/webp"
                          className={styles.formControl}
                          onChange={(e) =>
                            setJoinAvatarFile(e.target.files?.[0] ?? null)
                          }
                        />
                        <p className={styles.joinSubscribeNote}>
                          선택: JPEG / PNG / WebP, 5MB 이하
                        </p>
                      </div>
                      <div
                        className={`${styles.formGroup} ${styles.joinPwRow}`}
                      >
                        <label
                          htmlFor={joinPwFieldId}
                          className={styles.srOnly}
                        >
                          비밀번호
                        </label>
                        <input
                          id={joinPwFieldId}
                          type="password"
                          className={styles.formControl}
                          value={joinPw}
                          onChange={(e) => setJoinPw(e.target.value)}
                          placeholder="8자 이상, 영문·숫자 포함"
                          autoComplete="new-password"
                          maxLength={72}
                        />
                        <label
                          htmlFor={joinPwConfirmFieldId}
                          className={styles.srOnly}
                        >
                          비밀번호 확인
                        </label>
                        <input
                          id={joinPwConfirmFieldId}
                          type="password"
                          className={styles.formControl}
                          value={joinPwConfirm}
                          onChange={(e) => setJoinPwConfirm(e.target.value)}
                          placeholder="비밀번호 확인"
                          autoComplete="new-password"
                          maxLength={72}
                        />
                      </div>
                      <div className={styles.formGroup}>
                        <div className={styles.loginSave}>
                          <input
                            id={joinSubscribeFieldId}
                            type="checkbox"
                            className={styles.loginSaveCheckbox}
                            checked={joinSubscribe}
                            onChange={(e) => setJoinSubscribe(e.target.checked)}
                          />
                          <label
                            htmlFor={joinSubscribeFieldId}
                            className={styles.loginSaveLabel}
                          >
                            웹진 Idol Glow 구독하기
                          </label>
                        </div>
                        <p className={styles.joinSubscribeNote}>
                          문화예술 콘텐츠를 이메일로 받아보세요!
                        </p>
                      </div>
                      <div
                        id="join_message"
                        className={styles.joinFeedback}
                        role="status"
                        aria-live="polite"
                      >
                        {joinMessage}
                      </div>
                    </div>
                    <div className={styles.joinBtnWrap}>
                      <button type="submit" className={styles.btnJoinSubmit}>
                        회원가입
                      </button>
                    </div>
                  </div>
                </form>
              </div>
            )}

            {(authModalView === "forgot-password" ||
              authModalView === "find-id") && (
              <div className={`${styles.joinModalBody} ${styles.pwdModalBody}`}>
                <h3
                  id={
                    authModalView === "find-id" ? findIdTitleId : pwdTitleId
                  }
                  className={styles.srOnly}
                >
                  {authModalView === "find-id"
                    ? "아이디 찾기"
                    : "비밀번호 찾기"}
                </h3>
                <form
                  className={styles.pwdForm}
                  onSubmit={
                    authModalView === "find-id"
                      ? (e) => {
                          e.preventDefault();
                          void handleFindId();
                        }
                      : handlePwdSubmit
                  }
                  autoComplete="off"
                  noValidate
                >
                  <div className={styles.pwdInput}>
                    <h5 className={styles.pwdVisibleTitle}>
                      {authModalView === "find-id"
                        ? "아이디 찾기"
                        : "비밀번호 찾기"}
                    </h5>
                    <p className={styles.pwdRecoveryHint}>
                      {authModalView === "find-id"
                        ? "가입 시 사용한 이메일로 아이디 안내 메일을 보내 드립니다."
                        : "가입 시 사용한 이메일로 임시 비밀번호 발급 안내를 보내 드립니다."}
                    </p>
                    <div className={styles.pwdElement}>
                      <div className={styles.formGroup}>
                        <label
                          htmlFor={pwdEmailFieldId}
                          className={styles.srOnly}
                        >
                          이메일
                        </label>
                        <div className={styles.pwdInputGroup}>
                          <input
                            id={pwdEmailFieldId}
                            type="email"
                            className={`${styles.formControl} ${styles.pwdFormControl}`}
                            value={forgotEmail}
                            onChange={(e) => setForgotEmail(e.target.value)}
                            placeholder="이메일 주소 입력"
                            autoComplete="email"
                          />
                        </div>
                      </div>
                      <div
                        id={recoveryMessageId}
                        className={styles.joinFeedback}
                        role="status"
                        aria-live="polite"
                      >
                        {forgotMessage}
                      </div>
                    </div>
                    <div className={styles.pwdBtnWrap}>
                      <button
                        type="submit"
                        className={styles.btnPwdSubmit}
                        id="pwd_submit"
                      >
                        {authModalView === "find-id"
                          ? "아이디 찾기"
                          : "비밀번호 찾기"}
                      </button>
                    </div>
                    <div className={styles.pwdSwitchRow}>
                      <button
                        type="button"
                        className={styles.btnFindPwd}
                        onClick={() => {
                          setForgotMessage("");
                          setAuthModalView(
                            authModalView === "find-id"
                              ? "forgot-password"
                              : "find-id",
                          );
                        }}
                      >
                        {authModalView === "find-id"
                          ? "비밀번호 찾기"
                          : "아이디 찾기"}
                      </button>
                    </div>
                  </div>
                </form>
              </div>
            )}

            {authModalView === "force-change" && (
              <div className={`${styles.joinModalBody} ${styles.pwdModalBody}`}>
                <h3 id={forceChangeTitleId} className={styles.srOnly}>
                  비밀번호 변경
                </h3>
                <form
                  className={styles.pwdForm}
                  onSubmit={handleForcedPasswordSubmit}
                  autoComplete="off"
                  noValidate
                >
                  <div className={styles.pwdInput}>
                    <h5 className={styles.pwdVisibleTitle}>
                      임시 비밀번호 변경
                    </h5>
                    <div className={styles.pwdElement}>
                      <div className={styles.formGroup}>
                        <input
                          type="password"
                          className={`${styles.formControl} ${styles.pwdFormControl}`}
                          value={forcedNewPassword}
                          onChange={(e) => setForcedNewPassword(e.target.value)}
                          placeholder="새 비밀번호 입력 (8자 이상, 영문·숫자 포함)"
                          autoComplete="new-password"
                        />
                      </div>
                      <div className={styles.formGroup}>
                        <input
                          type="password"
                          className={`${styles.formControl} ${styles.pwdFormControl}`}
                          value={forcedNewPasswordConfirm}
                          onChange={(e) =>
                            setForcedNewPasswordConfirm(e.target.value)
                          }
                          placeholder="새 비밀번호 확인"
                          autoComplete="new-password"
                        />
                      </div>
                      <div
                        className={styles.joinFeedback}
                        role="status"
                        aria-live="polite"
                      >
                        {forcedPasswordMessage}
                      </div>
                    </div>
                    <div className={styles.pwdBtnWrap}>
                      <button type="submit" className={styles.btnPwdSubmit}>
                        비밀번호 변경
                      </button>
                    </div>
                  </div>
                </form>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
};

export default Header;
