import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import {
  type PersonalizedRecommendationItem,
  fetchPersonalizedRecommendations,
} from "../../../shared/data/productRecommendationApi";
import {
  type SurveyRecommendationResponse,
  type SurveyUsedSignal,
  type SurveyUsedSignalSource,
  type SurveyUsedSignalType,
  fetchMyLatestSurveyRecommendation,
} from "../../../shared/data/surveyFormUserApi";
import styles from "./MySurveyPage.module.css";

const PERSONALIZED_SIZE = 6;
const PERSONALIZED_REASON_LINES = 3;
const LLM_POLL_INTERVAL_MS = 3000;
const LLM_POLL_LIMIT = 5;

const SIGNAL_TYPE_LABEL: Record<SurveyUsedSignalType, string> = {
  CONCEPT: "컨셉",
  IDOL: "아이돌",
  PLACE: "장소",
  TAG: "태그",
  BEHAVIOR_TAG: "찜·리뷰 태그",
  VISIT_PERIOD: "방문 기간",
};

const SIGNAL_SOURCE_LABEL: Record<SurveyUsedSignalSource, string> = {
  FORM: "설문 응답",
  USER_SURVEY: "취향·여행 정보",
  ACTIVITY: "찜·리뷰 활동",
};

function formatPrice(value: number): string {
  if (!Number.isFinite(value) || value <= 0) {
    return "가격 문의";
  }
  return `${Math.round(value).toLocaleString("ko-KR")}원~`;
}

export function RecommendationProductCard({
  item,
  maxReasons,
}: {
  readonly item: PersonalizedRecommendationItem;
  readonly maxReasons: number;
}) {
  const reasons = item.reasons.slice(0, maxReasons);
  return (
    <li className={styles.recoCard}>
      <Link to={`/products/${item.id}`} className={styles.recoLink}>
        <span className={styles.recoThumb}>
          {item.thumbnailUrl ? (
            <img src={item.thumbnailUrl} alt="" loading="lazy" />
          ) : null}
        </span>
        <span className={styles.recoBody}>
          <span className={styles.recoName}>{item.name}</span>
          <span className={styles.recoPrice}>{formatPrice(item.minPrice)}</span>
          {reasons.length > 0 ? (
            <ul className={styles.recoReasons}>
              {reasons.map((reason) => (
                <li key={`${reason.code}-${reason.label}`}>{reason.label}</li>
              ))}
            </ul>
          ) : null}
        </span>
      </Link>
    </li>
  );
}

function UsedSignalChips({
  signals,
}: { readonly signals: readonly SurveyUsedSignal[] }) {
  if (signals.length === 0) {
    return null;
  }
  return (
    <div className={styles.signalBlock}>
      <p className={styles.signalHeading}>반영한 응답</p>
      <ul className={styles.signalChips}>
        {signals.map((signal) => (
          <li
            key={`${signal.source}-${signal.type}`}
            className={styles.signalChip}
          >
            <strong>{SIGNAL_TYPE_LABEL[signal.type]}</strong>
            <span>{signal.values.join(", ")}</span>
            <em>{SIGNAL_SOURCE_LABEL[signal.source]}</em>
          </li>
        ))}
      </ul>
    </div>
  );
}

export function PersonalizedRecommendationSection({
  accessToken,
  hasUserSurvey,
  reloadKey,
  title = "내 설문 기반 추천",
  emptyMessage = "설문을 저장하면 추천을 볼 수 있어요.",
  className,
}: {
  readonly accessToken: string | null;
  readonly hasUserSurvey: boolean;
  readonly reloadKey: number;
  readonly title?: string;
  readonly emptyMessage?: string;
  readonly className?: string;
}) {
  const [items, setItems] = useState<readonly PersonalizedRecommendationItem[]>(
    [],
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const enabled = Boolean(accessToken) && hasUserSurvey;

  useEffect(() => {
    void reloadKey;
    if (!enabled || !accessToken) {
      setItems([]);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    fetchPersonalizedRecommendations(accessToken, PERSONALIZED_SIZE)
      .then((next) => {
        if (!cancelled) setItems(next);
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setError(
            e instanceof Error ? e.message : "추천 상품을 불러오지 못했습니다.",
          );
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, enabled, reloadKey]);

  const popularFallback =
    items.length > 0 && items.every((item) => item.fallback === "POPULAR");

  return (
    <section className={className ? `${styles.recoSection} ${className}` : styles.recoSection}>
      <div className={styles.recoHeader}>
        <h2 className={styles.sectionHeading}>{title}</h2>
        <p className={styles.recoHint}>
          예약은 상품 상세 화면에서 직접 진행해 주세요.
        </p>
      </div>
      {!enabled ? (
        <p className={styles.recoEmpty}>{emptyMessage}</p>
      ) : loading ? (
        <p className={styles.recoEmpty}>추천 상품을 불러오는 중…</p>
      ) : error ? (
        <p className={styles.error}>{error}</p>
      ) : items.length === 0 ? (
        <p className={styles.recoEmpty}>지금 보여 드릴 상품이 없어요.</p>
      ) : (
        <>
          {popularFallback ? (
            <p className={styles.recoNotice}>
              설문과 맞는 상품이 없어 인기 상품을 보여 드려요
            </p>
          ) : null}
          <ul className={styles.recoGrid}>
            {items.map((item) => (
              <RecommendationProductCard
                key={item.id}
                item={item}
                maxReasons={PERSONALIZED_REASON_LINES}
              />
            ))}
          </ul>
        </>
      )}
    </section>
  );
}

export function FormRecommendationSection({
  accessToken,
  reloadKey,
}: {
  readonly accessToken: string;
  readonly reloadKey: number;
}) {
  const [result, setResult] = useState<SurveyRecommendationResponse | null>(
    null,
  );
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const pollCount = useRef(0);

  const load = useCallback(
    async (silent: boolean) => {
      if (!silent) {
        setLoading(true);
        setError(null);
      }
      try {
        const next = await fetchMyLatestSurveyRecommendation(accessToken, true);
        setResult(next);
      } catch (e) {
        if (!silent) {
          setError(
            e instanceof Error ? e.message : "설문 추천을 불러오지 못했습니다.",
          );
        }
      } finally {
        if (!silent) setLoading(false);
      }
    },
    [accessToken],
  );

  useEffect(() => {
    void reloadKey;
    pollCount.current = 0;
    void load(false);
  }, [load, reloadKey]);

  useEffect(() => {
    if (!result?.llmPending || pollCount.current >= LLM_POLL_LIMIT) {
      return;
    }
    const timer = window.setTimeout(() => {
      pollCount.current += 1;
      void load(true);
    }, LLM_POLL_INTERVAL_MS);
    return () => window.clearTimeout(timer);
  }, [load, result]);

  if (loading) {
    return (
      <section className={styles.recoSection}>
        <p className={styles.recoEmpty}>설문 추천 결과를 불러오는 중…</p>
      </section>
    );
  }

  if (error) {
    return (
      <section className={styles.recoSection}>
        <p className={styles.error}>{error}</p>
        <button
          type="button"
          className={styles.secondaryButton}
          onClick={() => void load(false)}
        >
          다시 불러오기
        </button>
      </section>
    );
  }

  if (!result) {
    return null;
  }

  const popularFallback =
    result.recommendedProducts.length > 0 &&
    result.recommendedProducts.every((item) => item.fallback === "POPULAR");

  return (
    <section className={styles.recoSection}>
      <div className={styles.recoHeader}>
        <p className={styles.boardKicker}>설문 추천 결과</p>
        <h2 className={styles.resultTitle}>{result.title}</h2>
        {result.subtitle ? (
          <p className={styles.resultSub}>{result.subtitle}</p>
        ) : null}
      </div>
      {result.narrative ? (
        <p className={styles.resultNarrative}>{result.narrative}</p>
      ) : null}
      {result.llmPending ? (
        <p className={styles.recoHint}>추천 문장을 다듬는 중이에요.</p>
      ) : null}
      <UsedSignalChips signals={result.usedSignals} />

      <h3 className={styles.recoSubheading}>추천 상품</h3>
      {result.recommendedProducts.length === 0 ? (
        <p className={styles.recoEmpty}>지금 보여 드릴 상품이 없어요.</p>
      ) : (
        <>
          {popularFallback ? (
            <p className={styles.recoNotice}>
              설문과 맞는 상품이 없어 인기 상품을 보여 드려요
            </p>
          ) : null}
          <ul className={styles.recoGrid}>
            {result.recommendedProducts.map((item) => (
              <RecommendationProductCard
                key={item.id}
                item={item}
                maxReasons={PERSONALIZED_REASON_LINES}
              />
            ))}
          </ul>
        </>
      )}

      {result.attractions.length > 0 ? (
        <>
          <h3 className={styles.recoSubheading}>함께 둘러볼 관광지</h3>
          <ol className={styles.attractionList}>
            {result.attractions.map((attraction) => (
              <li key={attraction.attractionCode}>
                <strong>{attraction.name}</strong>
                {attraction.signguName ? (
                  <span className={styles.attractionMeta}>
                    {attraction.signguName}
                  </span>
                ) : null}
                {attraction.reason ? (
                  <p className={styles.attractionReason}>{attraction.reason}</p>
                ) : null}
              </li>
            ))}
          </ol>
        </>
      ) : null}
    </section>
  );
}
