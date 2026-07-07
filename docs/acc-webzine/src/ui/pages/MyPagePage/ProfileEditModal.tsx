import { useCallback, useEffect, useId, useState } from 'react';
import {
  changePassword,
  updateProfile,
  uploadProfileImage,
  type UserLoginInfo,
} from '../../../auth/authApi';
import { NICKNAME_PATTERN } from '../../../lib/nicknameValidation';
import styles from './ProfileEditModal.module.css';

type ProfileEditModalProps = {
  readonly open: boolean;
  readonly onClose: () => void;
  readonly accessToken: string;
  readonly user: UserLoginInfo;
  /** 저장 후 서버가 준 최신 사용자(있으면 헤더·마이페이지 아바타에 즉시 반영) */
  readonly onApplied: (latest?: UserLoginInfo) => Promise<void>;
};

/** SNS 연동 계정: 닉네임·이미지(파일)만. 이메일 가입: 비밀번호·URL·파일·닉네임 */
export function ProfileEditModal({
  open,
  onClose,
  accessToken,
  user,
  onApplied,
}: ProfileEditModalProps) {
  const titleId = useId();
  const nickId = useId();
  const urlId = useId();
  const fileId = useId();
  const curPwId = useId();
  const newPwId = useId();
  const newPw2Id = useId();
  const [nickname, setNickname] = useState(user.nickname);
  const [profileImageUrl, setProfileImageUrl] = useState(user.picture ?? '');
  const [pickedFile, setPickedFile] = useState<File | null>(null);
  const [filePreviewUrl, setFilePreviewUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [previewBroken, setPreviewBroken] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');

  const oauthOnly = user.oauthLinked === true;
  const hasPw = user.hasPassword ?? !user.oauthLinked;
  const showPasswordChange = hasPw && !oauthOnly;
  const showImageUrlField = !oauthOnly;

  useEffect(() => {
    if (!open) return;
    setNickname(user.nickname);
    setProfileImageUrl(user.picture ?? '');
    setPickedFile(null);
    setFilePreviewUrl(null);
    setError(null);
    setSaving(false);
    setPreviewBroken(false);
    setCurrentPassword('');
    setNewPassword('');
    setNewPasswordConfirm('');
  }, [open, user.nickname, user.picture]);

  useEffect(() => {
    if (!pickedFile) {
      setFilePreviewUrl(null);
      return;
    }
    const url = URL.createObjectURL(pickedFile);
    setFilePreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [pickedFile]);

  const previewSrc = filePreviewUrl ?? (profileImageUrl.trim() || undefined);

  useEffect(() => {
    setPreviewBroken(false);
  }, [previewSrc]);

  const handleBackdropClick = useCallback(() => {
    if (!saving) onClose();
  }, [onClose, saving]);

  const handleResetLinkedImage = useCallback(async () => {
    setError(null);
    setSaving(true);
    try {
      const next = await updateProfile(accessToken, { profileImageUrl: '' });
      if (!next) {
        setError('이미지 초기화에 실패했습니다.');
        return;
      }
      setProfileImageUrl(next.picture ?? '');
      setPickedFile(null);
      await onApplied(next);
    } finally {
      setSaving(false);
    }
  }, [accessToken, onApplied]);

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      setError(null);
      const nick = nickname.trim();
      if (!NICKNAME_PATTERN.test(nick)) {
        setError('닉네임은 한글·영문·숫자 2~10자입니다.');
        return;
      }

      const pwTrim = currentPassword.trim();
      const nwTrim = newPassword.trim();
      const nw2Trim = newPasswordConfirm.trim();
      const anyPw = pwTrim.length > 0 || nwTrim.length > 0 || nw2Trim.length > 0;
      if (showPasswordChange && anyPw) {
        if (!pwTrim || !nwTrim || !nw2Trim) {
          setError('비밀번호를 바꿀 경우 현재·새·확인을 모두 입력해 주세요.');
          return;
        }
        if (nwTrim !== nw2Trim) {
          setError('새 비밀번호와 확인이 일치하지 않습니다.');
          return;
        }
        if (nwTrim.length < 8 || nwTrim.length > 72) {
          setError('새 비밀번호는 8~72자여야 합니다.');
          return;
        }
        if (!/[a-zA-Z]/.test(nwTrim) || !/[0-9]/.test(nwTrim)) {
          setError('새 비밀번호는 영문과 숫자를 각각 1자 이상 포함해야 합니다.');
          return;
        }
      }

      const url = profileImageUrl.trim();
      if (showImageUrlField && !pickedFile && url.length > 0) {
        try {
          const u = new URL(url);
          if (u.protocol !== 'http:' && u.protocol !== 'https:') {
            setError('이미지 주소는 http 또는 https 로 시작해야 합니다.');
            return;
          }
        } catch {
          setError('올바른 이미지 URL을 입력해 주세요.');
          return;
        }
      }

      setSaving(true);
      try {
        if (showPasswordChange && anyPw) {
          const cp = await changePassword(accessToken, {
            currentPassword: pwTrim,
            newPassword: nwTrim,
          });
          if (!cp.ok) {
            setError(cp.message);
            return;
          }
        }

        let latest: UserLoginInfo | undefined;
        if (pickedFile) {
          const up = await uploadProfileImage(accessToken, pickedFile);
          if (!up.ok) {
            setError(up.message);
            return;
          }
          latest = up.user;
          if (nick !== user.nickname) {
            const nextNick = await updateProfile(accessToken, { nickname: nick });
            if (!nextNick) {
              setError('닉네임 저장에 실패했습니다. 다시 시도해 주세요.');
              return;
            }
            latest = nextNick;
          }
        } else {
          const next = await updateProfile(accessToken, {
            nickname: nick,
            ...(showImageUrlField ? { profileImageUrl: url } : {}),
          });
          if (!next) {
            setError('저장에 실패했습니다. 다시 시도해 주세요.');
            return;
          }
          latest = next;
        }
        await onApplied(latest);
        onClose();
      } finally {
        setSaving(false);
      }
    },
    [
      accessToken,
      nickname,
      onApplied,
      onClose,
      pickedFile,
      profileImageUrl,
      showImageUrlField,
      showPasswordChange,
      currentPassword,
      newPassword,
      newPasswordConfirm,
      user.nickname,
    ]
  );

  if (!open) return null;

  return (
    <div
      className={styles.backdrop}
      role="presentation"
      onClick={handleBackdropClick}
    >
      <div
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        onClick={ev => ev.stopPropagation()}
      >
        <button type="button" className={styles.dismiss} onClick={onClose} aria-label="닫기">
          ×
        </button>
        <h2 id={titleId} className={styles.title}>
          프로필 수정
        </h2>
        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.previewWrap}>
            {previewSrc && !previewBroken ? (
              <img
                key={`${open}-${previewSrc}`}
                src={previewSrc}
                alt=""
                className={styles.previewImg}
                referrerPolicy="no-referrer"
                decoding="async"
                onError={() => setPreviewBroken(true)}
              />
            ) : (
              <div className={styles.previewPh} aria-hidden>
                <span className={styles.previewPhLabel}>
                  {previewBroken && previewSrc
                    ? '미리보기 불가'
                    : '이미지 없음'}
                </span>
              </div>
            )}
          </div>
          <div className={styles.field}>
            <label htmlFor={fileId} className={styles.label}>
              프로필 사진 업로드
            </label>
            <input
              id={fileId}
              type="file"
              accept="image/jpeg,image/png,image/webp"
              className={styles.input}
              onChange={e => setPickedFile(e.target.files?.[0] ?? null)}
            />
            <p className={styles.hint}>
              JPEG / PNG / WebP, 최대 5MB. 업로드 시 서버(MinIO 등)에 저장되며 아래 URL 입력보다 우선합니다.
            </p>
          </div>
          {oauthOnly ? (
            <div className={styles.field}>
              <button
                type="button"
                className={styles.secondaryBtn}
                onClick={() => void handleResetLinkedImage()}
                disabled={saving}
              >
                연동(SNS) 프로필 이미지로 되돌리기
              </button>
              <p className={styles.hint}>저장된 커스텀 이미지를 지우고 연동 계정 사진을 씁니다.</p>
            </div>
          ) : null}
          {showImageUrlField ? (
            <div className={styles.field}>
              <label htmlFor={urlId} className={styles.label}>
                프로필 이미지 URL
              </label>
              <input
                id={urlId}
                type="url"
                className={styles.input}
                value={profileImageUrl}
                onChange={e => setProfileImageUrl(e.target.value)}
                placeholder="https://…"
                autoComplete="off"
              />
              <p className={styles.hint}>
                파일을 올리지 않을 때만 사용합니다. 비우고 저장하면 커스텀 이미지가 제거됩니다.
              </p>
              <button
                type="button"
                className={styles.secondaryBtn}
                onClick={() => {
                  setProfileImageUrl('');
                  setPickedFile(null);
                }}
              >
                이미지 URL 비우기
              </button>
            </div>
          ) : null}
          {showPasswordChange ? (
            <div className={styles.fieldGroup}>
              <p className={styles.sectionLabel}>비밀번호 변경</p>
              <div className={styles.field}>
                <label htmlFor={curPwId} className={styles.label}>
                  현재 비밀번호
                </label>
                <input
                  id={curPwId}
                  type="password"
                  className={styles.input}
                  value={currentPassword}
                  onChange={e => setCurrentPassword(e.target.value)}
                  autoComplete="current-password"
                />
              </div>
              <div className={styles.field}>
                <label htmlFor={newPwId} className={styles.label}>
                  새 비밀번호
                </label>
                <input
                  id={newPwId}
                  type="password"
                  className={styles.input}
                  value={newPassword}
                  onChange={e => setNewPassword(e.target.value)}
                  autoComplete="new-password"
                />
              </div>
              <div className={styles.field}>
                <label htmlFor={newPw2Id} className={styles.label}>
                  새 비밀번호 확인
                </label>
                <input
                  id={newPw2Id}
                  type="password"
                  className={styles.input}
                  value={newPasswordConfirm}
                  onChange={e => setNewPasswordConfirm(e.target.value)}
                  autoComplete="new-password"
                />
              </div>
              <p className={styles.hint}>8자 이상, 영문·숫자 각 1자 이상. 바꾸지 않으면 비워 두세요.</p>
            </div>
          ) : null}
          <div className={styles.field}>
            <label htmlFor={nickId} className={styles.label}>
              닉네임
            </label>
            <input
              id={nickId}
              type="text"
              className={styles.input}
              value={nickname}
              onChange={e => setNickname(e.target.value)}
              maxLength={10}
              autoComplete="nickname"
            />
            <p className={styles.hint}>한글·영문·숫자 2~10자 (표시 이름)</p>
          </div>
          {error ? (
            <p className={styles.error} role="alert">
              {error}
            </p>
          ) : null}
          <div className={styles.actions}>
            <button type="button" className={styles.secondaryBtn} onClick={onClose} disabled={saving}>
              취소
            </button>
            <button type="submit" className={styles.primaryBtn} disabled={saving}>
              {saving ? '저장 중…' : '저장'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
