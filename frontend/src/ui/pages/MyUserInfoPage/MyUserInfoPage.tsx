import {
  useCallback,
  useEffect,
  useId,
  useMemo,
  useState,
  type ChangeEvent,
  type FormEvent,
} from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import {
  changePassword,
  checkSignupEmail,
  checkSignupNickname,
  getOAuthLoginUrl,
  requestSignupEmailVerification,
  updateProfile,
  uploadProfileImage,
  type UserLoginInfo,
} from "../../../auth/authApi";
import { NICKNAME_PATTERN } from "../../../lib/nicknameValidation";
import { registerIdolGlowSubscription } from "../../../shared/data/subscriptionApi";
import shellStyles from "../AdminMarketingPage/AdminMarketingPage.module.css";
import { AdminMarketingShell } from "../AdminMarketingPage/AdminMarketingShell";
import plainStyles from "../MyPagePage/MyPagePlain.module.css";
import styles from "./MyUserInfoPage.module.css";

type TabKey = "profile" | "sns";
type AccountProviderId = "GOOGLE" | "NAVER" | "KAKAO" | "IDOLGLOW";

const MAX_PROFILE_IMAGE_BYTES = 5 * 1024 * 1024;

const PROVIDER_LABELS: Record<AccountProviderId, string> = {
  GOOGLE: "구글",
  NAVER: "네이버",
  KAKAO: "카카오",
  IDOLGLOW: "Idolglow",
};

const profileInitial = (user: UserLoginInfo): string => {
  const source = user.nickname?.trim() || user.email?.trim() || "I";
  return source.slice(0, 1).toUpperCase();
};

const normalizeProviderId = (value?: string | null): AccountProviderId | null => {
  const normalized = value?.trim().toUpperCase();
  if (!normalized) return null;
  if (normalized.includes("GOOGLE")) return "GOOGLE";
  if (normalized.includes("NAVER")) return "NAVER";
  if (normalized.includes("KAKAO")) return "KAKAO";
  if (
    normalized.includes("IDOLGLOW") ||
    normalized.includes("IDOL_GLOW") ||
    normalized.includes("LOCAL") ||
    normalized.includes("PASSWORD")
  ) {
    return "IDOLGLOW";
  }
  return null;
};

const getProviderIdsFromUser = (user: UserLoginInfo): AccountProviderId[] => {
  const rawValues = [
    ...(user.oauthProviders ?? []),
    ...(user.linkedProviders ?? []),
    user.authProvider,
    user.provider,
    user.loginProvider,
    user.signupProvider,
  ];
  const ids = rawValues
    .map((value) => normalizeProviderId(value))
    .filter((value): value is AccountProviderId => value != null);
  const hasOauthProvider = ids.some((id) => id !== "IDOLGLOW");
  if (user.oauthLinked && !hasOauthProvider) {
    ids.push("GOOGLE");
  }
  return Array.from(new Set(ids));
};

const hasIdolglowPassword = (user: UserLoginInfo): boolean =>
  user.hasPassword ?? !user.oauthLinked;

const isSnsOnlyAccount = (user: UserLoginInfo): boolean =>
  user.oauthLinked && !hasIdolglowPassword(user);

const accountTypeLabel = (user: UserLoginInfo): string => {
  const providerIds = getProviderIdsFromUser(user).filter((id) => id !== "IDOLGLOW");
  const snsLabel = providerIds.map((id) => PROVIDER_LABELS[id]).join(", ");
  const hasPassword = hasIdolglowPassword(user);
  if (hasPassword && providerIds.length > 0) return `Idolglow + ${snsLabel}`;
  if (providerIds.length > 0) return `${snsLabel} 연동 계정`;
  return "Idolglow 일반가입";
};

const validateImageFile = (file: File): string | null => {
  if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
    return "JPEG, PNG, WebP 이미지만 업로드할 수 있습니다.";
  }
  if (file.size > MAX_PROFILE_IMAGE_BYTES) {
    return "프로필 이미지는 5MB 이하만 업로드할 수 있습니다.";
  }
  return null;
};

function ProfilePreview({
  src,
  user,
}: {
  readonly src?: string;
  readonly user: UserLoginInfo;
}) {
  const [broken, setBroken] = useState(false);

  useEffect(() => {
    setBroken(false);
  }, [src]);

  return (
    <div className={styles.previewWrap}>
      {src && !broken ? (
        <img
          key={src}
          src={src}
          alt=""
          className={styles.previewImg}
          referrerPolicy="no-referrer"
          decoding="async"
          onError={() => setBroken(true)}
        />
      ) : (
        <div className={styles.previewFallback} aria-hidden>
          {profileInitial(user)}
        </div>
      )}
    </div>
  );
}

function SnsProfileForm({
  accessToken,
  user,
  onApplied,
}: {
  readonly accessToken: string;
  readonly user: UserLoginInfo;
  readonly onApplied: (latest?: UserLoginInfo) => Promise<void>;
}) {
  const nickId = useId();
  const fileId = useId();
  const [nickname, setNickname] = useState(user.nickname);
  const [pickedFile, setPickedFile] = useState<File | null>(null);
  const [filePreviewUrl, setFilePreviewUrl] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setNickname(user.nickname);
    setPickedFile(null);
    setMessage(null);
    setError(null);
  }, [user.nickname, user.picture]);

  useEffect(() => {
    if (!pickedFile) {
      setFilePreviewUrl(null);
      return;
    }
    const url = URL.createObjectURL(pickedFile);
    setFilePreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [pickedFile]);

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const next = event.target.files?.[0] ?? null;
    setError(null);
    setMessage(null);
    if (!next) {
      setPickedFile(null);
      return;
    }
    const validationError = validateImageFile(next);
    if (validationError) {
      event.target.value = "";
      setError(validationError);
      setPickedFile(null);
      return;
    }
    setPickedFile(next);
  };

  const handleResetLinkedImage = useCallback(async () => {
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const latest = await updateProfile(accessToken, { profileImageUrl: "" });
      if (!latest) {
        setError("연동 프로필 이미지로 되돌리지 못했습니다.");
        return;
      }
      setPickedFile(null);
      await onApplied(latest);
      setMessage("연동 계정의 프로필 이미지로 되돌렸습니다.");
    } finally {
      setSaving(false);
    }
  }, [accessToken, onApplied]);

  const handleSubmit = useCallback(
    async (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      setError(null);
      setMessage(null);

      const nick = nickname.trim();
      if (!NICKNAME_PATTERN.test(nick)) {
        setError("닉네임은 한글·영문·숫자 2~10자만 사용할 수 있습니다.");
        return;
      }

      setSaving(true);
      try {
        let latest: UserLoginInfo | undefined;
        if (pickedFile) {
          const uploaded = await uploadProfileImage(accessToken, pickedFile);
          if (!uploaded.ok) {
            setError(uploaded.message);
            return;
          }
          latest = uploaded.user;
        }

        if (nick !== user.nickname) {
          const updated = await updateProfile(accessToken, { nickname: nick });
          if (!updated) {
            setError("닉네임 저장에 실패했습니다. 다시 시도해 주세요.");
            return;
          }
          latest = updated;
        }

        if (!latest) {
          const updated = await updateProfile(accessToken, { nickname: nick });
          if (!updated) {
            setError("저장에 실패했습니다. 다시 시도해 주세요.");
            return;
          }
          latest = updated;
        }

        setPickedFile(null);
        await onApplied(latest);
        setMessage("개인정보를 저장했습니다.");
      } finally {
        setSaving(false);
      }
    },
    [accessToken, nickname, onApplied, pickedFile, user.nickname],
  );

  const previewSrc = filePreviewUrl ?? user.picture ?? undefined;

  return (
    <form className={styles.formShell} onSubmit={handleSubmit} noValidate>
      <ProfilePreview src={previewSrc} user={user} />

      <div className={styles.field}>
        <label htmlFor={fileId} className={styles.label}>
          프로필 사진 업로드
        </label>
        <input
          id={fileId}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          className={styles.fileInput}
          onChange={handleFileChange}
        />
        <p className={styles.hint}>
          JPEG / PNG / WebP, 최대 5MB. 업로드 시 서버에 저장되며 아래 URL
          입력보다 우선합니다.
        </p>
      </div>

      <div className={styles.field}>
        <button
          type="button"
          className={styles.wideSecondaryButton}
          onClick={() => void handleResetLinkedImage()}
          disabled={saving}
        >
          연동(SNS) 프로필 이미지로 되돌리기
        </button>
        <p className={styles.hint}>
          저장된 커스텀 이미지를 지우고 연동 계정 사진을 씁니다.
        </p>
      </div>

      <div className={styles.field}>
        <label htmlFor={nickId} className={styles.label}>
          닉네임
        </label>
        <input
          id={nickId}
          type="text"
          className={styles.input}
          value={nickname}
          onChange={(event) => setNickname(event.target.value)}
          maxLength={10}
          autoComplete="nickname"
        />
        <p className={styles.hint}>한글·영문·숫자 2~10자 (표시 이름)</p>
      </div>

      <FormFeedback message={message} error={error} />

      <div className={styles.actions}>
        <Link to="/mypage" className={styles.secondaryButton}>
          취소
        </Link>
        <button type="submit" className={styles.primaryButton} disabled={saving}>
          {saving ? "저장 중" : "저장"}
        </button>
      </div>
    </form>
  );
}

function PasswordProfileForm({
  accessToken,
  user,
  onApplied,
}: {
  readonly accessToken: string;
  readonly user: UserLoginInfo;
  readonly onApplied: (latest?: UserLoginInfo) => Promise<void>;
}) {
  const emailId = useId();
  const nickId = useId();
  const fileId = useId();
  const currentPwId = useId();
  const newPwId = useId();
  const confirmPwId = useId();
  const subscribeId = useId();
  const [email, setEmail] = useState(user.email);
  const [nickname, setNickname] = useState(user.nickname);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");
  const [subscribeToUpdates, setSubscribeToUpdates] = useState(false);
  const [pickedFile, setPickedFile] = useState<File | null>(null);
  const [filePreviewUrl, setFilePreviewUrl] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [emailMessage, setEmailMessage] = useState<string | null>(null);
  const [nicknameMessage, setNicknameMessage] = useState<string | null>(null);

  useEffect(() => {
    setEmail(user.email);
    setNickname(user.nickname);
    setCurrentPassword("");
    setNewPassword("");
    setNewPasswordConfirm("");
    setSubscribeToUpdates(false);
    setPickedFile(null);
    setMessage(null);
    setError(null);
    setEmailMessage(null);
    setNicknameMessage(null);
  }, [user.email, user.nickname, user.picture]);

  useEffect(() => {
    if (!pickedFile) {
      setFilePreviewUrl(null);
      return;
    }
    const url = URL.createObjectURL(pickedFile);
    setFilePreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [pickedFile]);

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const next = event.target.files?.[0] ?? null;
    setError(null);
    setMessage(null);
    if (!next) {
      setPickedFile(null);
      return;
    }
    const validationError = validateImageFile(next);
    if (validationError) {
      event.target.value = "";
      setError(validationError);
      setPickedFile(null);
      return;
    }
    setPickedFile(next);
  };

  const handleEmailDupCheck = useCallback(async () => {
    const target = email.trim().toLowerCase();
    setEmailMessage(null);
    if (!target) {
      setEmailMessage("이메일을 입력해 주세요.");
      return;
    }
    if (target === user.email.trim().toLowerCase()) {
      setEmailMessage("현재 사용 중인 이메일입니다.");
      return;
    }
    const checked = await checkSignupEmail(target);
    if (!checked) {
      setEmailMessage("이메일 중복확인 중 오류가 발생했습니다.");
      return;
    }
    if (checked.available) {
      setEmailMessage("사용 가능한 이메일입니다.");
      return;
    }
    if (checked.code === "INVALID_FORMAT") {
      setEmailMessage("올바른 이메일 형식을 입력해 주세요.");
      return;
    }
    setEmailMessage("이미 가입된 이메일입니다.");
  }, [email, user.email]);

  const handleEmailVerification = useCallback(async () => {
    const target = email.trim().toLowerCase();
    setEmailMessage(null);
    if (!target) {
      setEmailMessage("이메일을 입력해 주세요.");
      return;
    }
    if (target === user.email.trim().toLowerCase()) {
      setEmailMessage("현재 이메일은 추가 인증이 필요하지 않습니다.");
      return;
    }
    const checked = await checkSignupEmail(target);
    if (!checked?.available) {
      setEmailMessage("사용 가능한 이메일인지 먼저 확인해 주세요.");
      return;
    }
    const result = await requestSignupEmailVerification(target);
    if (!result?.sent) {
      setEmailMessage("인증 메일 발송에 실패했습니다.");
      return;
    }
    setEmailMessage("인증 메일을 발송했습니다. 메일 링크를 확인해 주세요.");
  }, [email, user.email]);

  const handleNicknameDupCheck = useCallback(async () => {
    const nick = nickname.trim();
    setNicknameMessage(null);
    if (!nick) {
      setNicknameMessage("별명을 입력해 주세요.");
      return;
    }
    if (nick === user.nickname) {
      setNicknameMessage("현재 사용 중인 별명입니다.");
      return;
    }
    const checked = await checkSignupNickname(nick);
    if (!checked) {
      setNicknameMessage("별명 중복확인 중 오류가 발생했습니다.");
      return;
    }
    if (checked.available) {
      setNicknameMessage("사용 가능한 별명입니다.");
      return;
    }
    if (checked.code === "INVALID_FORMAT") {
      setNicknameMessage("별명은 한글·영문·숫자 2~10자만 사용할 수 있습니다.");
      return;
    }
    setNicknameMessage("이미 사용 중인 별명입니다.");
  }, [nickname, user.nickname]);

  const handleSubmit = useCallback(
    async (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      setError(null);
      setMessage(null);

      const nick = nickname.trim();
      if (!NICKNAME_PATTERN.test(nick)) {
        setError("별명은 한글·영문·숫자 2~10자만 사용할 수 있습니다.");
        return;
      }

      const currentPw = currentPassword.trim();
      const nextPw = newPassword.trim();
      const nextPwConfirm = newPasswordConfirm.trim();
      const shouldChangePassword =
        currentPw.length > 0 || nextPw.length > 0 || nextPwConfirm.length > 0;

      if (shouldChangePassword) {
        if (!currentPw || !nextPw || !nextPwConfirm) {
          setError("비밀번호를 변경하려면 현재 비밀번호와 새 비밀번호를 모두 입력해 주세요.");
          return;
        }
        if (nextPw !== nextPwConfirm) {
          setError("새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
          return;
        }
        if (nextPw.length < 8 || nextPw.length > 72) {
          setError("새 비밀번호는 8~72자여야 합니다.");
          return;
        }
        if (!/[a-zA-Z]/.test(nextPw) || !/[0-9]/.test(nextPw)) {
          setError("새 비밀번호는 영문과 숫자를 각각 1자 이상 포함해야 합니다.");
          return;
        }
      }

      setSaving(true);
      try {
        if (shouldChangePassword) {
          const changed = await changePassword(accessToken, {
            currentPassword: currentPw,
            newPassword: nextPw,
          });
          if (!changed.ok) {
            setError(changed.message);
            return;
          }
        }

        let latest: UserLoginInfo | undefined;
        if (pickedFile) {
          const uploaded = await uploadProfileImage(accessToken, pickedFile);
          if (!uploaded.ok) {
            setError(uploaded.message);
            return;
          }
          latest = uploaded.user;
        }

        if (nick !== user.nickname) {
          const updated = await updateProfile(accessToken, { nickname: nick });
          if (!updated) {
            setError("별명 저장에 실패했습니다. 다시 시도해 주세요.");
            return;
          }
          latest = updated;
        }

        if (subscribeToUpdates) {
          await registerIdolGlowSubscription({
            email: user.email,
            agreedToPrivacy: true,
            subscribeNewsletters: true,
            subscribeIssues: true,
          });
        }

        if (latest) {
          await onApplied(latest);
        } else {
          await onApplied();
        }

        setPickedFile(null);
        setCurrentPassword("");
        setNewPassword("");
        setNewPasswordConfirm("");
        setMessage("회원 정보를 저장했습니다.");
        if (email.trim().toLowerCase() !== user.email.trim().toLowerCase()) {
          setEmailMessage("이메일 변경은 인증 완료 후 별도 반영됩니다.");
        }
      } catch (saveError) {
        setError(saveError instanceof Error ? saveError.message : "저장에 실패했습니다.");
      } finally {
        setSaving(false);
      }
    },
    [
      accessToken,
      currentPassword,
      email,
      newPassword,
      newPasswordConfirm,
      nickname,
      onApplied,
      pickedFile,
      subscribeToUpdates,
      user.email,
      user.nickname,
    ],
  );

  const previewSrc = filePreviewUrl ?? user.picture ?? undefined;

  return (
    <form className={`${styles.formShell} ${styles.joinLikeForm}`} onSubmit={handleSubmit} noValidate>
      <ProfilePreview src={previewSrc} user={user} />

      <div className={styles.field}>
        <label htmlFor={emailId} className={styles.visuallyHidden}>
          이메일
        </label>
        <div className={styles.inputAttach}>
          <input
            id={emailId}
            type="email"
            className={styles.attachInput}
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="이메일 주소 입력"
            autoComplete="email"
          />
          <div className={styles.attachButtons}>
            <button type="button" className={styles.attachButton} onClick={() => void handleEmailDupCheck()}>
              중복확인
            </button>
            <button type="button" className={styles.attachButton} onClick={() => void handleEmailVerification()}>
              인증메일
            </button>
          </div>
        </div>
        {emailMessage ? <p className={styles.inlineMessage}>{emailMessage}</p> : null}
      </div>

      <div className={styles.field}>
        <label htmlFor={nickId} className={styles.visuallyHidden}>
          별명
        </label>
        <div className={styles.inputAttach}>
          <input
            id={nickId}
            type="text"
            className={styles.attachInput}
            value={nickname}
            onChange={(event) => setNickname(event.target.value)}
            placeholder="별명 입력 (2~10자 한글·영문·숫자)"
            maxLength={10}
            autoComplete="nickname"
          />
          <div className={styles.inputGroupAppendCompact}>
            <button type="button" className={styles.attachButton} onClick={() => void handleNicknameDupCheck()}>
              중복확인
            </button>
          </div>
        </div>
        {nicknameMessage ? <p className={styles.inlineMessage}>{nicknameMessage}</p> : null}
      </div>

      <div className={styles.field}>
        <label htmlFor={fileId} className={styles.visuallyHidden}>
          프로필 사진
        </label>
        <input
          id={fileId}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          className={styles.joinFileInput}
          onChange={handleFileChange}
        />
        <p className={styles.hint}>선택: JPEG / PNG / WebP, 5MB 이하</p>
      </div>

      <div className={styles.passwordGrid}>
        <label htmlFor={currentPwId} className={styles.visuallyHidden}>
          현재 비밀번호
        </label>
        <input
          id={currentPwId}
          type="password"
          className={`${styles.input} ${styles.passwordFull}`}
          value={currentPassword}
          onChange={(event) => setCurrentPassword(event.target.value)}
          placeholder="현재 비밀번호"
          autoComplete="current-password"
        />
        <label htmlFor={newPwId} className={styles.visuallyHidden}>
          새 비밀번호
        </label>
        <input
          id={newPwId}
          type="password"
          className={styles.input}
          value={newPassword}
          onChange={(event) => setNewPassword(event.target.value)}
          placeholder="8자 이상, 영문·숫자 포함"
          autoComplete="new-password"
          maxLength={72}
        />
        <label htmlFor={confirmPwId} className={styles.visuallyHidden}>
          비밀번호 확인
        </label>
        <input
          id={confirmPwId}
          type="password"
          className={styles.input}
          value={newPasswordConfirm}
          onChange={(event) => setNewPasswordConfirm(event.target.value)}
          placeholder="비밀번호 확인"
          autoComplete="new-password"
          maxLength={72}
        />
      </div>

      <div className={styles.checkField}>
        <input
          id={subscribeId}
          type="checkbox"
          checked={subscribeToUpdates}
          onChange={(event) => setSubscribeToUpdates(event.target.checked)}
        />
        <label htmlFor={subscribeId}>웹진 Idol Glow 구독하기</label>
      </div>
      <p className={styles.hint}>문화예술 콘텐츠를 이메일로 받아보세요!</p>

      <FormFeedback message={message} error={error} />

      <div className={styles.actions}>
        <Link to="/mypage" className={styles.secondaryButton}>
          취소
        </Link>
        <button type="submit" className={styles.primaryButton} disabled={saving}>
          {saving ? "저장 중" : "저장"}
        </button>
      </div>
    </form>
  );
}

function FormFeedback({
  message,
  error,
}: {
  readonly message: string | null;
  readonly error: string | null;
}) {
  if (!message && !error) return null;
  return (
    <p className={error ? styles.errorMessage : styles.successMessage} role={error ? "alert" : "status"}>
      {error ?? message}
    </p>
  );
}

function SnsConnectionsTab({ user }: { readonly user: UserLoginInfo }) {
  const providerIds = getProviderIdsFromUser(user);
  const linkedOauthProviderIds = providerIds.filter((id) => id !== "IDOLGLOW");
  const linkedOauthSet = new Set(linkedOauthProviderIds);
  const idolglowConnected = hasIdolglowPassword(user) || providerIds.includes("IDOLGLOW");

  const oauthRows: ReadonlyArray<{
    readonly id: Exclude<AccountProviderId, "IDOLGLOW">;
    readonly href: string;
  }> = [
    { id: "GOOGLE", href: getOAuthLoginUrl("google") },
    { id: "NAVER", href: getOAuthLoginUrl("naver") },
    { id: "KAKAO", href: getOAuthLoginUrl("kakao") },
  ];

  return (
    <section className={styles.connectionPanel} aria-labelledby="sns-connect-heading">
      <h2 id="sns-connect-heading" className={styles.visuallyHidden}>
        SNS 연동
      </h2>
      <ul className={styles.connectionList}>
        {oauthRows.map((row) => {
          const connected = linkedOauthSet.has(row.id);
          return (
            <li key={row.id} className={styles.connectionRow}>
              <div className={styles.connectionMeta}>
                <span className={styles.providerName}>{PROVIDER_LABELS[row.id]}</span>
                <span className={styles.providerStatus}>{connected ? user.email : "연결되지 않음"}</span>
              </div>
              {connected ? (
                <button type="button" className={styles.connectButtonDisabled} disabled>
                  연결됨
                </button>
              ) : (
                <a href={row.href} className={styles.connectButton}>
                  연결해
                </a>
              )}
            </li>
          );
        })}

        <li className={styles.connectionRow}>
          <div className={styles.connectionMeta}>
            <span className={styles.providerName}>{PROVIDER_LABELS.IDOLGLOW}</span>
            <span className={styles.providerStatus}>
              {idolglowConnected ? user.email : "연결되지 않음"}
            </span>
          </div>
          {idolglowConnected ? (
            <button type="button" className={styles.connectButtonDisabled} disabled>
              연결됨
            </button>
          ) : (
            <Link
              to="/?login=1&join=1&redirect=%2Fmypage%2FuserInfo%3Ftab%3Dsns"
              className={styles.connectButton}
            >
              연결해
            </Link>
          )}
        </li>
      </ul>
    </section>
  );
}

export function MyUserInfoPage() {
  const { accessToken, authReady, user, applyLoginUser, refreshUser } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab: TabKey = searchParams.get("tab") === "sns" ? "sns" : "profile";

  const linkedProviderIds = useMemo(
    () => (user ? getProviderIdsFromUser(user).filter((id) => id !== "IDOLGLOW") : []),
    [user],
  );

  const applyLatestUser = useCallback(
    async (latest?: UserLoginInfo) => {
      if (latest) {
        applyLoginUser(latest);
      }
      await refreshUser();
    },
    [applyLoginUser, refreshUser],
  );

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.loading}>개인정보를 불러오는 중입니다.</div>
      </main>
    );
  }

  if (!user || !accessToken) {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.denied}>로그인이 필요합니다.</div>
      </main>
    );
  }

  const isSnsOnly = isSnsOnlyAccount(user);
  const tabButton = (tab: TabKey, label: string) => (
    <button
      type="button"
      className={[styles.tabButton, activeTab === tab ? styles.tabButtonActive : ""]
        .filter(Boolean)
        .join(" ")}
      onClick={() => {
        if (tab === "profile") {
          setSearchParams({});
        } else {
          setSearchParams({ tab });
        }
      }}
      aria-selected={activeTab === tab}
      role="tab"
    >
      {label}
    </button>
  );

  return (
    <AdminMarketingShell
      currentPath="/mypage/userInfo"
      title="개인정보 변경"
      description={`${user.nickname || user.email}님의 계정 정보를 관리합니다.`}
      statusText={`${accountTypeLabel(user)} · ${user.email}`}
      classNames={{
        toolbarCard: plainStyles.flatToolbar,
      }}
      stats={[
        { label: "가입 구분", value: accountTypeLabel(user) },
        { label: "SNS 연동", value: linkedProviderIds.length > 0 ? `${linkedProviderIds.length}개` : "없음" },
        { label: "Idolglow", value: hasIdolglowPassword(user) ? "연결됨" : "미연결" },
      ]}
    >
      <section className={`${shellStyles.panel} ${plainStyles.flatPanel} ${styles.panel}`}>
        <div className={styles.tabList} role="tablist" aria-label="개인정보 설정 메뉴">
          {tabButton("profile", "개인정보 변경")}
          {tabButton("sns", "SNS 연동")}
        </div>

        <div className={styles.tabPanel} role="tabpanel">
          {activeTab === "profile" ? (
            isSnsOnly ? (
              <SnsProfileForm accessToken={accessToken} user={user} onApplied={applyLatestUser} />
            ) : (
              <PasswordProfileForm accessToken={accessToken} user={user} onApplied={applyLatestUser} />
            )
          ) : (
            <SnsConnectionsTab user={user} />
          )}
        </div>
      </section>
    </AdminMarketingShell>
  );
}

export default MyUserInfoPage;
