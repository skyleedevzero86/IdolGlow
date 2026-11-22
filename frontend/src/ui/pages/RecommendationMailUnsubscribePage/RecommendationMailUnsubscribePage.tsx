import { useCallback, useEffect, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  type RecommendationMailUnsubscribePreview,
  confirmRecommendationMailUnsubscribe,
  previewRecommendationMailUnsubscribe,
} from "../../../shared/data/recommendationMailApi";
import styles from "./RecommendationMailUnsubscribePage.module.css";

type PageState =
  | { readonly kind: "loading" }
  | { readonly kind: "missing" }
  | { readonly kind: "error"; readonly message: string }
  | {
      readonly kind: "ready";
      readonly preview: RecommendationMailUnsubscribePreview;
    }
  | { readonly kind: "done"; readonly message: string };

const AUTO_CLOSE_DELAY_MS = 3000;
const CLOSE_CHECK_DELAY_MS = 500;

function isClosableState(state: PageState): boolean {
  if (state.kind === "done" || state.kind === "missing") return true;
  if (state.kind !== "ready") return false;
  return !state.preview.valid || state.preview.alreadyUnsubscribed;
}

export function RecommendationMailUnsubscribePage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token")?.trim() ?? "";
  const [state, setState] = useState<PageState>({ kind: "loading" });
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [closeBlocked, setCloseBlocked] = useState(false);
  const closeCheckTimer = useRef<number | null>(null);
  const closable = isClosableState(state);

  const tryCloseWindow = useCallback(() => {
    window.close();
    if (closeCheckTimer.current !== null) {
      window.clearTimeout(closeCheckTimer.current);
    }
    closeCheckTimer.current = window.setTimeout(() => {
      closeCheckTimer.current = null;
      if (!window.closed) setCloseBlocked(true);
    }, CLOSE_CHECK_DELAY_MS);
  }, []);

  useEffect(() => {
    if (!closable) return;
    const timer = window.setTimeout(tryCloseWindow, AUTO_CLOSE_DELAY_MS);
    return () => window.clearTimeout(timer);
  }, [closable, tryCloseWindow]);

  useEffect(
    () => () => {
      if (closeCheckTimer.current !== null) {
        window.clearTimeout(closeCheckTimer.current);
      }
    },
    [],
  );

  useEffect(() => {
    if (!token) {
      setState({ kind: "missing" });
      return;
    }
    let cancelled = false;
    setState({ kind: "loading" });
    previewRecommendationMailUnsubscribe(token)
      .then((preview) => {
        if (!cancelled) setState({ kind: "ready", preview });
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setState({
            kind: "error",
            message:
              e instanceof Error ? e.message : "링크를 확인하지 못했습니다.",
          });
        }
      });
    return () => {
      cancelled = true;
    };
  }, [token]);

  const handleConfirm = async () => {
    setSubmitting(true);
    setSubmitError(null);
    try {
      const result = await confirmRecommendationMailUnsubscribe(token);
      setState({ kind: "done", message: result.message });
    } catch (e) {
      setSubmitError(
        e instanceof Error ? e.message : "수신 거부를 처리하지 못했습니다.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className={styles.root}>
      <section className={styles.sheet}>
        <header className={styles.header}>
          <p className={styles.kicker}>상품 추천 메일</p>
          <h1 className={styles.title}>메일 수신 거부</h1>
        </header>

        {state.kind === "loading" ? (
          <p className={styles.body}>링크를 확인하는 중입니다…</p>
        ) : null}

        {state.kind === "missing" ? (
          <p className={styles.body}>
            수신 거부 링크가 올바르지 않아요. 메일에 있는 링크를 다시 눌러
            주세요.
          </p>
        ) : null}

        {state.kind === "error" ? (
          <p className={styles.error}>{state.message}</p>
        ) : null}

        {state.kind === "ready" && !state.preview.valid ? (
          <p className={styles.body}>
            만료되었거나 올바르지 않은 링크예요. 가장 최근에 받은 메일의
            링크를 눌러 주시거나, 로그인 후 마이페이지에서 수신 설정을 바꿔
            주세요.
          </p>
        ) : null}

        {state.kind === "ready" &&
        state.preview.valid &&
        state.preview.alreadyUnsubscribed ? (
          <>
            {state.preview.maskedEmail ? (
              <dl className={styles.infoList}>
                <div className={styles.infoRow}>
                  <dt>받는 이메일</dt>
                  <dd>{state.preview.maskedEmail}</dd>
                </div>
              </dl>
            ) : null}
            <p className={styles.done}>
              이미 상품 추천 메일 수신을 거부한 상태예요. 더 이상 메일을 보내지
              않아요.
            </p>
          </>
        ) : null}

        {state.kind === "ready" &&
        state.preview.valid &&
        !state.preview.alreadyUnsubscribed ? (
          <>
            <dl className={styles.infoList}>
              <div className={styles.infoRow}>
                <dt>받는 이메일</dt>
                <dd>{state.preview.maskedEmail ?? "-"}</dd>
              </div>
              <div className={styles.infoRow}>
                <dt>대상</dt>
                <dd>상품 추천 메일 (광고성 정보)</dd>
              </div>
            </dl>
            <p className={styles.body}>
              아래 버튼을 누르면 상품 추천 메일을 더 이상 보내지 않아요.
              소식지 구독 등 다른 메일 설정은 바뀌지 않아요.
            </p>
            {submitError ? (
              <p className={styles.error}>{submitError}</p>
            ) : null}
            <div className={styles.actions}>
              <button
                type="button"
                className={styles.primaryButton}
                onClick={() => void handleConfirm()}
                disabled={submitting}
              >
                {submitting ? "처리 중…" : "수신 거부"}
              </button>
            </div>
          </>
        ) : null}

        {state.kind === "done" ? (
          <p className={styles.done}>{state.message}</p>
        ) : null}

        {closable ? (
          <div className={styles.footer}>
            <output className={styles.closeNotice} aria-live="polite">
              {closeBlocked
                ? "브라우저에서 이 탭을 닫아 주세요."
                : "잠시 후 이 창이 닫힙니다."}
            </output>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={tryCloseWindow}
            >
              창 닫기
            </button>
          </div>
        ) : null}
      </section>
    </main>
  );
}

export default RecommendationMailUnsubscribePage;
