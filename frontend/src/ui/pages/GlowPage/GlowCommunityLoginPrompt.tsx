import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import styles from "./GlowCommunity.module.css";

type GlowCommunityLoginPromptProps = {
  readonly message: string;
  readonly onClose: () => void;
  readonly redirectTo?: string;
};

export function GlowCommunityLoginPrompt({
  message,
  onClose,
  redirectTo,
}: GlowCommunityLoginPromptProps) {
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [onClose]);

  const goLogin = () => {
    const redirect = encodeURIComponent(
      redirectTo ?? `${location.pathname}${location.search}${location.hash}`,
    );
    navigate(`/?login=1&redirect=${redirect}`);
  };

  return (
    <div className={styles.overlay} role="presentation" onMouseDown={onClose}>
      <section
        className={styles.promptDialog}
        // biome-ignore lint/a11y/useSemanticElements: custom overlay modal shared with existing GLOW popups
        role="dialog"
        aria-modal="true"
        aria-labelledby="community-login-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className={styles.dialogHeader}>
          <h2 id="community-login-title">로그인이 필요합니다</h2>
          <button type="button" aria-label="닫기" onClick={onClose}>
            ×
          </button>
        </div>
        <p className={styles.promptText}>{message}</p>
        <div className={styles.dialogActions}>
          <button
            type="button"
            className={styles.ghostButton}
            onClick={onClose}
          >
            취소
          </button>
          <button
            type="button"
            className={styles.primaryButton}
            onClick={goLogin}
          >
            로그인
          </button>
        </div>
      </section>
    </div>
  );
}

export default GlowCommunityLoginPrompt;
