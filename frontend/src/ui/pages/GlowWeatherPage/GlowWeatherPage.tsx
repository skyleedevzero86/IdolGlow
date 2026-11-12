import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../../auth/AuthContext';
import {
  fetchGlowWeatherDashboard,
  GLOW_WEATHER_REGION_OPTIONS,
  type GlowWeatherDashboardResponse,
  type GlowWeatherRecommendation,
  type GlowWeatherWindPoint,
} from '../../../shared/data/glowWeatherApi';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import styles from './GlowWeatherPage.module.css';

type TemperatureUnit = 'C' | 'F';

const toneClassName: Record<string, string> = {
  sunny: styles.recommendCardSunny,
  mint: styles.recommendCardMint,
  rain: styles.recommendCardRain,
  teal: styles.recommendCardTeal,
  sky: styles.recommendCardSky,
  blue: styles.recommendCardBlue,
};

function toFahrenheit(value: number): number {
  return (value * 9) / 5 + 32;
}

function formatTemperature(value: number | null, unit: TemperatureUnit): string {
  if (value == null || Number.isNaN(value)) return '-';
  const target = unit === 'F' ? toFahrenheit(value) : value;
  return `${Number(target.toFixed(1)).toLocaleString()}°${unit}`;
}

function WeatherIcon({ icon }: { readonly icon: string }) {
  if (icon === 'rain') {
    return (
      <svg viewBox="0 0 24 24" className={styles.weatherIcon} aria-hidden="true">
        <path d="M7 17a4 4 0 1 1 .8-7.92A5 5 0 0 1 17 8a4 4 0 1 1 0 8H7Z" fill="currentColor" opacity="0.3" />
        <path d="M9 19l-1 2M13 19l-1 2M17 19l-1 2" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      </svg>
    );
  }
  if (icon === 'snow') {
    return (
      <svg viewBox="0 0 24 24" className={styles.weatherIcon} aria-hidden="true">
        <path d="M12 3v18M5.6 6.7l12.8 10.6M18.4 6.7L5.6 17.3M4 12h16" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      </svg>
    );
  }
  if (icon === 'cloud') {
    return (
      <svg viewBox="0 0 24 24" className={styles.weatherIcon} aria-hidden="true">
        <path d="M7.5 18a4.5 4.5 0 1 1 .7-8.95A5.5 5.5 0 0 1 18.5 10a4 4 0 1 1 0 8H7.5Z" fill="currentColor" opacity="0.35" />
      </svg>
    );
  }
  if (icon === 'partly') {
    return (
      <svg viewBox="0 0 24 24" className={styles.weatherIcon} aria-hidden="true">
        <circle cx="9" cy="9" r="4" fill="currentColor" opacity="0.9" />
        <path d="M9 2.5v2.2M9 13.3v2.2M2.5 9h2.2M13.3 9h2.2M4.5 4.5l1.6 1.6M11.9 11.9l1.6 1.6M13.5 4.5l-1.6 1.6M6.1 11.9l-1.6 1.6" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
        <path d="M10.5 18a4 4 0 1 1 .64-7.95A5.2 5.2 0 0 1 19.5 11a3.6 3.6 0 1 1 0 7h-9Z" fill="currentColor" opacity="0.24" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 24 24" className={styles.weatherIcon} aria-hidden="true">
      <circle cx="12" cy="12" r="4" fill="currentColor" />
      <path d="M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3M5.4 5.4l2.1 2.1M16.5 16.5l2.1 2.1M18.6 5.4l-2.1 2.1M7.5 16.5l-2.1 2.1" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

function RecommendationIcon({ icon }: { readonly icon: string }) {
  if (icon === 'rain') {
    return <span className={styles.recommendEmoji} aria-hidden="true">☔</span>;
  }
  if (icon === 'shirt') {
    return <span className={styles.recommendEmoji} aria-hidden="true">👕</span>;
  }
  return <span className={styles.recommendEmoji} aria-hidden="true">☀</span>;
}

function WindCompass({ points, degrees, direction }: { readonly points: readonly GlowWeatherWindPoint[]; readonly degrees: number | null; readonly direction: string }) {
  const uniquePoints = points.filter((point, index) => index === points.findIndex(candidate => candidate.label === point.label));
  return (
    <div className={styles.compassWrap}>
      <div className={styles.compass}>
        <div className={styles.compassRing} />
        <div
          className={styles.compassNeedle}
          style={{ transform: `translate(-50%, -100%) rotate(${degrees ?? 0}deg)` }}
          aria-hidden="true"
        />
        <div className={styles.compassCenter}>
          <strong>{direction}</strong>
          <span>{degrees != null ? `${degrees}°` : '-'}</span>
        </div>
        {uniquePoints.map(point => (
          <span
            key={`${point.label}-${point.degrees}`}
            className={styles.compassPoint}
            style={{ transform: `translate(-50%, -50%) rotate(${point.degrees}deg) translateY(-118px) rotate(-${point.degrees}deg)` }}
          >
            <em>{point.degrees}</em>
            <strong>{point.label}</strong>
          </span>
        ))}
      </div>
    </div>
  );
}

function ForecastBanner() {
  return (
    <div className={styles.forecastBanner}>
      <div className={styles.forecastBannerContent}>
        <strong>이번 주 바깥 일정, 조금 더 편하게 챙겨보세요</strong>
        <p>강수와 기온 흐름만 먼저 보면 외출 준비가 훨씬 가벼워져요.</p>
        <span className={styles.forecastBannerChip}>지금 시도해 보세요</span>
      </div>
      <div className={styles.forecastBannerMarks} aria-hidden="true">
        <span>☀</span>
        <span>☁</span>
        <span>☂</span>
      </div>
    </div>
  );
}

export const GlowWeatherPage = () => {
  const { accessToken, authReady } = useAuth();
  const [regionId, setRegionId] = useState<string>('seoul');
  const [unit, setUnit] = useState<TemperatureUnit>('C');
  const [expandedForecast, setExpandedForecast] = useState(false);
  const [expandedRecommendationId, setExpandedRecommendationId] = useState<string | null>(null);
  const [data, setData] = useState<GlowWeatherDashboardResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!authReady || !accessToken) return;
    let cancelled = false;
    setLoading(true);
    setError(null);
    setData(null);

    void fetchGlowWeatherDashboard(accessToken, regionId)
      .then(response => {
        if (cancelled) return;
        setData(response);
        setRegionId(response.selectedRegionId);
      })
      .catch(cause => {
        if (cancelled) return;
        setError(cause instanceof Error ? cause.message : 'Glow 날씨 정보를 불러오지 못했어요.');
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, regionId]);

  const visibleForecast = useMemo(() => {
    const all = data?.forecast ?? [];
    return expandedForecast ? all : all.slice(0, 5);
  }, [data?.forecast, expandedForecast]);

  const stats = data
    ? [
        { label: '현재 기온', value: formatTemperature(data.current.temperatureC, unit) },
        { label: '하늘 상태', value: data.current.skyLabel },
        { label: '풍향', value: `${data.current.windDirectionLabel}${data.current.windSpeedMps != null ? ` / ${data.current.windSpeedMps.toFixed(1)}m/s` : ''}` },
      ]
    : [];

  const toggleRecommendation = (item: GlowWeatherRecommendation) => {
    setExpandedRecommendationId(current => (current === item.id ? null : item.id));
  };

  return (
    <AdminMarketingShell
      currentPath="/glow-weather"
      title="Glow 날씨"
      description="지역별 날씨 흐름과 바깥 일정 감각을 한 화면에서 볼 수 있어요."
      stats={stats}
      classNames={{ main: styles.shellMain, toolbarCard: styles.toolbarCard, statCard: styles.statCard }}
    >
      <section className={styles.pageSection}>
        <div className={styles.controlCard}>
          <label className={styles.selectField}>
            <span className={styles.srOnly}>지역 선택</span>
            <select value={regionId} onChange={event => setRegionId(event.target.value)}>
              {GLOW_WEATHER_REGION_OPTIONS.map(item => (
                <option key={item.id} value={item.id}>
                  {item.name}
                </option>
              ))}
            </select>
          </label>
        </div>

        <section className={styles.summaryCard} aria-label="월간 요약">
          <div className={styles.summaryHeader}>
            <h2>{data?.monthlySummary.monthLabel ?? '-'}</h2>
            <span>{data?.monthlySummary.basedOn ?? ''}</span>
          </div>
          <ul className={styles.summaryList}>
            <li>
              <span>{data?.region.name ?? '서울'}의 평균 기온은</span>
              <strong>{formatTemperature(data?.monthlySummary.averageTemperatureC ?? null, unit)}</strong>
            </li>
            <li>
              <span>한 달 내 비 오는 일수</span>
              <strong>{data?.monthlySummary.rainyDays ?? 0}일</strong>
            </li>
          </ul>
        </section>

        <section className={styles.forecastSection} aria-label="10일 예보">
          <div className={styles.forecastHeader}>
            <div>
              <h2>향후 10일간의 일기예보</h2>
              <p>{data?.current.regionName ?? '서울'} · {data?.current.observedAt ?? ''}</p>
            </div>
            <label className={styles.unitToggle}>
              <input
                type="checkbox"
                checked={unit === 'F'}
                onChange={event => setUnit(event.target.checked ? 'F' : 'C')}
              />
              <span>화씨에서 보기</span>
            </label>
          </div>

          {loading ? <p className={styles.infoText}>날씨 정보를 불러오는 중이에요.</p> : null}
          {error ? <p className={styles.errorText}>{error}</p> : null}

          <div className={styles.forecastList}>
            {visibleForecast.map((item, index) => (
              <div key={`${item.dateLabel}-${item.summary}`} className={styles.forecastRow}>
                <div className={styles.forecastDate}>
                  <strong>{item.dateLabel}</strong>
                  <span>{item.dayLabel}</span>
                </div>
                <div className={styles.forecastMeta}>
                  <span>{item.summary}</span>
                  <small>{item.precipitationChance != null ? `강수 ${item.precipitationChance}%` : '강수 정보 없음'}</small>
                </div>
                <div className={styles.forecastWeather}>
                  <WeatherIcon icon={item.icon} />
                  <strong>{formatTemperature(item.minTempC, unit)} / {formatTemperature(item.maxTempC, unit)}</strong>
                </div>
                {!expandedForecast && index === 4 ? <ForecastBanner /> : null}
              </div>
            ))}

            {expandedForecast ? <ForecastBanner /> : null}
          </div>

          <button
            type="button"
            className={styles.expandButton}
            onClick={() => setExpandedForecast(current => !current)}
          >
            {expandedForecast ? '닫습니다' : '더 읽기'}
          </button>
        </section>

        <section className={styles.recommendSection} aria-label="추천 카드">
          <h2>이제 어떻게 계획을 세울 수 있죠?</h2>
          <div className={styles.recommendList}>
            {(data?.recommendations ?? []).map(item => {
              const expanded = expandedRecommendationId === item.id;
              return (
                <article
                  key={item.id}
                  className={[styles.recommendCard, toneClassName[item.tone] ?? styles.recommendCardBlue].join(' ')}
                >
                  <div className={styles.recommendHeading}>
                    <RecommendationIcon icon={item.icon} />
                    <div>
                      <strong>{item.title}</strong>
                      <p>{item.subtitle}</p>
                    </div>
                  </div>
                  {expanded ? <p className={styles.recommendDetail}>{item.description}</p> : null}
                  <button type="button" className={styles.recommendButton} onClick={() => toggleRecommendation(item)}>
                    {expanded ? '접기' : '더 읽기'}
                  </button>
                </article>
              );
            })}
          </div>
        </section>

        <section className={styles.windSection} aria-label="풍향 가이드">
          <div className={styles.windCopy}>
            <h2>풍향</h2>
            <p>{data?.windGuide.message ?? '풍향 데이터를 준비 중이에요.'}</p>
            {data?.windGuide.windFromClimateStatistics && data.windGuide.climateStatisticsMonth != null ? (
              <p className={styles.windClimateCaption}>
                {data.windGuide.climateStatisticsMonth}월 기준 기후통계(최다풍향·평균풍속)
              </p>
            ) : null}
            <dl className={styles.windFacts}>
              <div>
                <dt>{data?.windGuide.windFromClimateStatistics ? '최다풍향(통계)' : '현재 풍향'}</dt>
                <dd>{data?.windGuide.directionLabel ?? '-'}</dd>
              </div>
              <div>
                <dt>{data?.windGuide.windFromClimateStatistics ? '참고 각도' : '각도'}</dt>
                <dd>{data?.windGuide.directionDegrees != null ? `${data.windGuide.directionDegrees}°` : '-'}</dd>
              </div>
              <div>
                <dt>{data?.windGuide.windFromClimateStatistics ? '평균 풍속' : '풍속'}</dt>
                <dd>{data?.windGuide.speedMps != null ? `${data.windGuide.speedMps.toFixed(1)}m/s` : '-'}</dd>
              </div>
            </dl>
          </div>
          <WindCompass
            points={data?.windGuide.referencePoints ?? []}
            degrees={data?.windGuide.directionDegrees ?? null}
            direction={data?.windGuide.directionLabel ?? '-'}
          />
        </section>
      </section>
    </AdminMarketingShell>
  );
};

export default GlowWeatherPage;
