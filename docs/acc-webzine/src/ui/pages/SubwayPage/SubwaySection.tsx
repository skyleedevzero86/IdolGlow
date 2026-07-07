import { useCallback, useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../../auth/AuthContext';
import {
  fetchSubwayLines,
  fetchSubwayStationPage,
  fetchSubwayStations,
  searchSubwayStations,
  type SubwayLine,
  type SubwayPage,
  type SubwayStation,
  type SubwayStationRef,
} from '../../../shared/data/subwayApi';
import styles from './SubwayPage.module.css';

const DEFAULT_LINE = '2';
const DEFAULT_STATION = '0222';

export function SubwaySection() {
  const { accessToken, authReady } = useAuth();
  const [lines, setLines] = useState<readonly SubwayLine[]>([]);
  const [stations, setStations] = useState<readonly SubwayStation[]>([]);
  const [lineId, setLineId] = useState(DEFAULT_LINE);
  const [stationCd, setStationCd] = useState(DEFAULT_STATION);
  const [page, setPage] = useState<SubwayPage | null>(null);
  const [searchText, setSearchText] = useState('');
  const [suggest, setSuggest] = useState<readonly SubwayStationRef[]>([]);
  const [suggestOpen, setSuggestOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loadingPage, setLoadingPage] = useState(false);

  const token = accessToken ?? '';

  useEffect(() => {
    if (!authReady || !token) {
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const ls = await fetchSubwayLines(token);
        if (cancelled) return;
        setLines(ls);
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : '노선을 불러오지 못했습니다.');
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [authReady, token]);

  useEffect(() => {
    if (!authReady || !token) {
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const list = await fetchSubwayStations(token, lineId);
        if (cancelled) return;
        setStations(list);
        if (list.length > 0) {
          setStationCd(prev => (list.some(s => s.stationCd === prev) ? prev : list[0].stationCd));
        }
      } catch (e) {
        if (!cancelled) {
          setStations([]);
          setError(e instanceof Error ? e.message : '역 목록을 불러오지 못했습니다.');
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [authReady, token, lineId]);

  const stationsBelongToLine = useMemo(
    () => stations.length > 0 && stations.every(s => s.lineId === lineId),
    [stations, lineId],
  );

  useEffect(() => {
    if (!authReady || !token || stations.length === 0 || !stationsBelongToLine) {
      setPage(null);
      return;
    }
    let cancelled = false;
    (async () => {
      setLoadingPage(true);
      setError(null);
      try {
        const p = await fetchSubwayStationPage(token, lineId, stationCd);
        if (!cancelled) {
          setPage(p);
        }
      } catch (e) {
        if (!cancelled) {
          setPage(null);
          setError(e instanceof Error ? e.message : '역 정보를 불러오지 못했습니다.');
        }
      } finally {
        if (!cancelled) {
          setLoadingPage(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [authReady, token, lineId, stationCd, stations.length, stationsBelongToLine]);

  useEffect(() => {
    const q = searchText.trim();
    if (!authReady || !token || q.length < 1) {
      setSuggest([]);
      return;
    }
    const t = window.setTimeout(() => {
      void (async () => {
        try {
          const hits = await searchSubwayStations(token, q);
          setSuggest(hits);
        } catch {
          setSuggest([]);
        }
      })();
    }, 280);
    return () => window.clearTimeout(t);
  }, [authReady, token, searchText]);

  const onPickSuggest = useCallback((hit: SubwayStationRef) => {
    setLineId(hit.lineId);
    setStationCd(hit.stationCd);
    setSearchText('');
    setSuggest([]);
    setSuggestOpen(false);
  }, []);

  const goToStationRef = useCallback((ref: SubwayStationRef) => {
    setLineId(prev => (ref.lineId !== prev ? ref.lineId : prev));
    setStationCd(ref.stationCd);
  }, []);

  const lineMeta = useMemo(() => lines.find(l => l.id === lineId), [lines, lineId]);

  /** Avoid showing the previous station's rail/AI while a new station page is loading. */
  const displayPage = useMemo(() => {
    if (!page) {
      return null;
    }
    if (page.line.id !== lineId || page.station.stationCd !== stationCd) {
      return null;
    }
    return page;
  }, [page, lineId, stationCd]);

  const railColor = displayPage?.line.colorHex ?? lineMeta?.colorHex ?? '#00A84D';

  const nearbySentence = useMemo(() => {
    if (!displayPage) return '';
    const { nearby, station } = displayPage;
    const stationLabel = `${station.name}역`;
    if (nearby.count <= 0) {
      return `${stationLabel} 주변 ${nearby.radiusMeters.toLocaleString()}m 이내 ${nearby.label} 거점 정보를 준비 중입니다.`;
    }
    return `${stationLabel} 주변 ${nearby.radiusMeters.toLocaleString()}m 이내에 ${nearby.count.toLocaleString()}개의 ${nearby.label} 거점이 있습니다.`;
  }, [displayPage]);

  if (!authReady) {
    return <p className={styles.error}>로딩 중…</p>;
  }
  if (!token) {
    return <p className={styles.error}>로그인 후 이용할 수 있습니다.</p>;
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.toolbar}>
        <div className={styles.field}>
          <span className={styles.fieldLabel}>노선</span>
          <select
            className={styles.select}
            value={lineId}
            onChange={e => setLineId(e.target.value)}
            aria-label="노선 선택"
          >
            {lines.map(l => (
              <option key={l.id} value={l.id}>
                {l.name}
              </option>
            ))}
          </select>
        </div>
        <div className={styles.field}>
          <span className={styles.fieldLabel}>역</span>
          <select
            className={styles.select}
            value={stationCd}
            onChange={e => setStationCd(e.target.value)}
            aria-label="역 선택"
            disabled={stations.length === 0}
          >
            {stations.map(s => (
              <option key={s.stationCd} value={s.stationCd}>
                {s.name}
              </option>
            ))}
          </select>
        </div>
        <div className={`${styles.field} ${styles.searchWrap}`}>
          <span className={styles.fieldLabel}>검색</span>
          <span className={styles.searchIcon} aria-hidden>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path
                d="M10.5 18a7.5 7.5 0 1 1 0-15 7.5 7.5 0 0 1 0 15Zm0-2a5.5 5.5 0 1 0 0-11 5.5 5.5 0 0 0 0 11Z"
                fill="currentColor"
              />
              <path d="M20 20 15.2 15.2" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </span>
          <input
            className={styles.searchInput}
            placeholder="역 이름으로 검색"
            value={searchText}
            onChange={e => {
              setSearchText(e.target.value);
              setSuggestOpen(true);
            }}
            onFocus={() => setSuggestOpen(true)}
            onBlur={() => window.setTimeout(() => setSuggestOpen(false), 180)}
            aria-label="역 이름으로 검색"
            autoComplete="off"
          />
          {suggestOpen && suggest.length > 0 ? (
            <ul className={styles.suggest} role="listbox">
              {suggest.map(hit => (
                <li key={`${hit.lineId}-${hit.stationCd}`}>
                  <button
                    type="button"
                    className={styles.suggestItem}
                    role="option"
                    onMouseDown={e => e.preventDefault()}
                    onClick={() => onPickSuggest(hit)}
                  >
                    {hit.name}
                    <span className={styles.suggestMeta}>
                      {hit.lineName} · 외부코드 {hit.frCode}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          ) : null}
        </div>
      </div>

      {error ? <p className={styles.error}>{error}</p> : null}

      {loadingPage && !displayPage ? <p className={styles.error}>역 정보를 불러오는 중…</p> : null}

      {displayPage ? (
        <>
          <div className={styles.rail} style={{ backgroundColor: railColor }}>
            <button
              type="button"
              className={`${styles.railSide} ${styles.railSidePrev} ${styles.railNavBtn}`}
              onClick={() => goToStationRef(displayPage.prevStation)}
              disabled={loadingPage}
              aria-label={`이전 역 ${displayPage.prevStation.name}으로 이동`}
            >
              <span className={styles.railArrow} aria-hidden>
                ‹
              </span>
              <span className={styles.railName}>{displayPage.prevStation.name}</span>
            </button>
            <div className={styles.railCenter}>
              <div className={styles.pill}>
                <p className={styles.pillTitle}>{displayPage.station.name}</p>
                <div className={styles.pillBadge}>
                  <span className={styles.pillBadgeLine} style={{ backgroundColor: railColor }}>
                    {displayPage.line.name}
                  </span>
                  <span className={styles.pillBadgeName}>{displayPage.station.name}</span>
                </div>
              </div>
            </div>
            <button
              type="button"
              className={`${styles.railSide} ${styles.railSideNext} ${styles.railNavBtn}`}
              onClick={() => goToStationRef(displayPage.nextStation)}
              disabled={loadingPage}
              aria-label={`다음 역 ${displayPage.nextStation.name}으로 이동`}
            >
              <span className={styles.railName}>{displayPage.nextStation.name}</span>
              <span className={styles.railArrow} aria-hidden>
                ›
              </span>
            </button>
          </div>

          <section className={styles.aiCard} aria-labelledby="subway-ai-summary">
            <div className={styles.aiHead}>
              <span className={styles.aiBadge}>AI 요약</span>
              <span id="subway-ai-summary" className={styles.aiTitle}>
                {displayPage.summary.title}
              </span>
            </div>
            <ul className={styles.aiList}>
              {displayPage.summary.bullets.map(t => (
                <li key={t}>{t}</li>
              ))}
            </ul>
            {displayPage.summary.learnMoreUrl ? (
              <a className={styles.aiLink} href={displayPage.summary.learnMoreUrl} target="_blank" rel="noreferrer">
                {displayPage.summary.learnMoreLabel} ›
              </a>
            ) : (
              <span className={styles.aiLink} style={{ cursor: 'default', textDecoration: 'none' }}>
                {displayPage.summary.learnMoreLabel} ›
              </span>
            )}
          </section>

          <div className={styles.bottomBar}>
            <span className={styles.bottomIcon} aria-hidden>
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
                <circle cx="7" cy="17" r="2.5" stroke="currentColor" strokeWidth="1.6" />
                <circle cx="17" cy="17" r="2.5" stroke="currentColor" strokeWidth="1.6" />
                <path
                  d="M4.5 17V9.5L8 6h5l2.5 3.5H19V17"
                  stroke="currentColor"
                  strokeWidth="1.6"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <path d="M8 6V4h3v2" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
              </svg>
            </span>
            <p className={styles.bottomText}>{nearbySentence}</p>
            <span className={styles.bottomArrow} aria-hidden>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M9 6l6 6-6 6" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
              </svg>
            </span>
          </div>
        </>
      ) : null}
    </div>
  );
}
