import { useCallback, useEffect, useId, useState } from "react";
import { Link } from "react-router-dom";
import { formatIdolGlowDate } from "../../../shared/format/idolGlowDateTime";
import {
  type RecommendationMailConsentSource,
  type RecommendationMailPreferredBasis,
  type RecommendationMailSetting,
  fetchRecommendationMailSetting,
  subscribeRecommendationMail,
  unsubscribeRecommendationMail,
} from "../../../shared/data/recommendationMailApi";
import styles from "./RecommendationMailPanel.module.css";

type RecommendationMailPanelProps = {
  readonly accessToken: string;
  readonly consentSource: RecommendationMailConsentSource;
  readonly showBasisOptions: boolean;
  readonly reloadKey?: number;
};

const BASIS_OPTIONS: ReadonlyArray<{
  readonly value: RecommendationMailPreferredBasis;
  readonly label: string;
}> = [
  { value: "AUTO", label: "내 설문 취향 (설문이 없으면 인기순)" },
  { value: "POPULAR", label: "인기순만" },
];

const expectedBasisLabel = (setting: RecommendationMailSetting): string =>
  setting.expectedBasis === "PERSONALIZED"
    ? "내 설문 취향에 맞춘 추천 상품"
    : "인기순 상품";

export function RecommendationMailPanel({
  accessToken,
  consentSource,
  showBasisOptions,
  reloadKey = 0,
}: RecommendationMailPanelProps) {
  const consentId = useId();
  const basisName = useId();
  const [setting, setSetting] = useState<RecommendationMailSetting | null>(
    null,
  );
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [agreed, setAgreed] = useState(false);
  const [basis, setBasis] = useState<RecommendationMailPreferredBasis>("AUTO");

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const next = await fetchRecommendationMailSetting(accessToken);
      setSetting(next);
      setBasis(next.preferredBasis);
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "추천 메일 설정을 불러오지 못했습니다.",
      );
    } finally {
      setLoading(false);
    }
  }, [accessToken]);

  useEffect(() => {
    void reloadKey;
    void load();
  }, [load, reloadKey]);

  const handleSubscribe = async () => {
    if (!agreed) {
      setError("광고성 정보 수신에 동의해야 추천 메일을 받을 수 있어요.");
      return;
    }
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      const next = await subscribeRecommendationMail(accessToken, {
        preferredBasis: basis,
        consentSource,
      });
      setSetting(next);
      setBasis(next.preferredBasis);
      setAgreed(false);
      setMessage("추천 메일 받기를 신청했습니다.");
    } catch (e) {
      setError(e instanceof Error ? e.message : "신청하지 못했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const handleBasisChange = async (next: RecommendationMailPreferredBasis) => {
    setBasis(next);
    if (!setting?.active) {
      return;
    }
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      const saved = await subscribeRecommendationMail(accessToken, {
        preferredBasis: next,
        consentSource,
      });
      setSetting(saved);
      setBasis(saved.preferredBasis);
      setMessage("추천 기준을 바꿨습니다.");
    } catch (e) {
      setBasis(setting.preferredBasis);
      setError(e instanceof Error ? e.message : "추천 기준을 바꾸지 못했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const handleUnsubscribe = async () => {
    if (!window.confirm("상품 추천 메일 수신을 거부할까요?")) {
      return;
    }
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      const next = await unsubscribeRecommendationMail(accessToken);
      setSetting(next);
      setMessage(
        "수신 거부를 처리했습니다. 이제 상품 추천 메일을 보내지 않아요.",
      );
    } catch (e) {
      setError(e instanceof Error ? e.message : "해지하지 못했습니다.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className={styles.panel} aria-labelledby={`${consentId}-title`}>
      <div className={styles.header}>
        <h2 id={`${consentId}-title`} className={styles.title}>
          취향 추천 메일 받기
        </h2>
        {setting ? (
          <span
            className={`${styles.status} ${setting.active ? styles.statusOn : ""}`}
          >
            {setting.active ? "구독 중" : "받지 않음"}
          </span>
        ) : null}
      </div>

      {loading ? (
        <p className={styles.muted}>추천 메일 설정을 불러오는 중…</p>
      ) : !setting ? (
        <>
          {error ? <p className={styles.error}>{error}</p> : null}
          <button
            type="button"
            className={styles.secondaryButton}
            onClick={() => void load()}
          >
            다시 불러오기
          </button>
        </>
      ) : (
        <>
          <dl className={styles.infoList}>
            <div className={styles.infoRow}>
              <dt>받는 이메일</dt>
              <dd>{setting.maskedEmail ?? "등록된 이메일이 없어요"}</dd>
            </div>
            <div className={styles.infoRow}>
              <dt>받는 주기</dt>
              <dd>정해진 주기 (밤 9시~아침 8시에는 보내지 않아요)</dd>
            </div>
            <div className={styles.infoRow}>
              <dt>보내는 내용</dt>
              <dd>{expectedBasisLabel(setting)}</dd>
            </div>
            {setting.active && setting.consentedAt ? (
              <div className={styles.infoRow}>
                <dt>동의일</dt>
                <dd>{formatIdolGlowDate(setting.consentedAt)}</dd>
              </div>
            ) : null}
          </dl>

          {showBasisOptions ? (
            <fieldset className={styles.basisGroup} disabled={busy}>
              <legend className={styles.legend}>추천 기준</legend>
              {BASIS_OPTIONS.map((option) => (
                <label key={option.value} className={styles.choiceRow}>
                  <input
                    type="radio"
                    name={basisName}
                    value={option.value}
                    checked={basis === option.value}
                    onChange={() => void handleBasisChange(option.value)}
                  />
                  <span>{option.label}</span>
                </label>
              ))}
              {!setting.hasUserSurvey ? (
                <p className={styles.hint}>
                  설문을 저장하면 취향에 맞춰 보내 드려요.{" "}
                  <Link to="/my-survey" className={styles.inlineLink}>
                    설문하러 가기
                  </Link>
                </p>
              ) : null}
            </fieldset>
          ) : null}

          {setting.active ? (
            <div className={styles.actions}>
              <span className={styles.muted}>
                구독 중 · 언제든 해지할 수 있어요.
              </span>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => void handleUnsubscribe()}
                disabled={busy}
              >
                {busy ? "처리 중…" : "해지"}
              </button>
            </div>
          ) : (
            <div className={styles.consentBox}>
              <label htmlFor={consentId} className={styles.consentRow}>
                <input
                  id={consentId}
                  type="checkbox"
                  checked={agreed}
                  onChange={(event) => setAgreed(event.target.checked)}
                />
                <span>
                  상품 추천 등 광고성 정보를 이메일로 받겠습니다{" "}
                  <em className={styles.optional}>(선택)</em>
                </span>
              </label>
              <p className={styles.hint}>
                개인정보 수집·이용 동의, 소식지 구독과는 따로 받는 동의예요.
                동의하지 않아도 서비스 이용에는 영향이 없어요.
              </p>
              <button
                type="button"
                className={styles.primaryButton}
                onClick={() => void handleSubscribe()}
                disabled={busy || !agreed || !setting.maskedEmail}
              >
                {busy ? "신청 중…" : "추천 메일 받기"}
              </button>
            </div>
          )}

          {error ? <p className={styles.error}>{error}</p> : null}
          {message ? <p className={styles.message}>{message}</p> : null}
        </>
      )}
    </section>
  );
}

export default RecommendationMailPanel;
