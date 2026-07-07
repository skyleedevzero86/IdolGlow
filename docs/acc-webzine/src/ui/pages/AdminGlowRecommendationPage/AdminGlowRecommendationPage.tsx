import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { useAuth } from '../../../auth/AuthContext';
import {
  fetchAdminProduct,
  fetchAdminProducts,
  fetchProductTourAttractions,
  type AdminProductSummary,
  type ProductTourAttractionItem,
  type ProductTourAttractionResult,
} from '../../../shared/data/adminBookingApi';
import { clearAdminSurveyPlaces, fetchAdminSurvey, upsertAdminSurvey, type SurveyConceptType } from '../../../shared/data/surveyAdminApi';
import { USER_SURVEY_CONCEPT_OPTIONS } from '../../../shared/data/userSurveyApi';
import {
  composeTourBaseYm,
  formatTourSpotLabel,
  SEOUL_DISTRICT_SIGNGU_OPTIONS,
  TOUR_AREA_OPTIONS,
} from '../../../shared/data/seoulTourDistrictOptions';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from './AdminGlowRecommendationPage.module.css';

const ISO_YMD = /^\d{4}-\d{2}-\d{2}$/;
const TIME_SLOT_OPTIONS = Array.from({ length: 31 }, (_, index) => {
  const totalMinutes = (9 * 60) + (index * 30);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
});

function sanitizeVisitDateTyping(raw: string): string {
  const digits = raw.replace(/\D/g, '').slice(0, 8);
  if (digits.length <= 4) return digits;
  if (digits.length <= 6) return `${digits.slice(0, 4)}-${digits.slice(4)}`;
  return `${digits.slice(0, 4)}-${digits.slice(4, 6)}-${digits.slice(6, 8)}`;
}

function isValidIsoYmd(value: string): boolean {
  if (!ISO_YMD.test(value)) return false;
  const [y, mo, d] = value.split('-').map(Number);
  const dt = new Date(y, mo - 1, d);
  return dt.getFullYear() === y && dt.getMonth() === mo - 1 && dt.getDate() === d;
}

type GlowPlaceRow = { readonly id: string; readonly label: string };

function dedupeLabels(labels: readonly string[]): string[] {
  const seen = new Set<string>();
  const out: string[] = [];
  for (const raw of labels) {
    const label = raw.trim();
    if (!label || seen.has(label)) continue;
    seen.add(label);
    out.push(label);
  }
  return out;
}

function dedupeAttractions(items: readonly ProductTourAttractionItem[]): ProductTourAttractionItem[] {
  const seen = new Set<string>();
  const out: ProductTourAttractionItem[] = [];
  for (const item of items) {
    const key = `${item.attractionCode}|${formatTourSpotLabel(item).trim()}`;
    if (seen.has(key)) continue;
    seen.add(key);
    out.push(item);
  }
  return out;
}

function resolveBaseYmOrNull(year: number | null, month: number | null): string | null {
  if (year == null || month == null) return null;
  return composeTourBaseYm(year, month);
}

export function AdminGlowRecommendationPage() {
  const { accessToken, authReady, user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const [surveyId, setSurveyId] = useState<number | null>(null);
  const [concept, setConcept] = useState<SurveyConceptType | ''>('');
  const [idolName, setIdolName] = useState('');
  const [visitStartDate, setVisitStartDate] = useState('');
  const [visitEndDate, setVisitEndDate] = useState('');
  const [visitStartTime, setVisitStartTime] = useState('09:00');
  const [visitEndTime, setVisitEndTime] = useState('24:00');
  const [placeRows, setPlaceRows] = useState<GlowPlaceRow[]>([]);
  const [presetRows, setPresetRows] = useState<GlowPlaceRow[]>([]);

  const [products, setProducts] = useState<readonly AdminProductSummary[]>([]);
  const [referenceProductId, setReferenceProductId] = useState<number | null>(null);
  const [tourAreaCode, setTourAreaCode] = useState(TOUR_AREA_OPTIONS[0]?.areaCode ?? 11);
  const [tourSignguCode, setTourSignguCode] = useState<number | null>(null);
  const [tourYear, setTourYear] = useState<number | null>(null);
  const [tourMonth, setTourMonth] = useState<number | null>(null);
  const [attractionData, setAttractionData] = useState<ProductTourAttractionResult | null>(null);
  const [attractionLoading, setAttractionLoading] = useState(false);
  const [attractionError, setAttractionError] = useState<string | null>(null);
  const [productTourPicks, setProductTourPicks] = useState<readonly ProductTourAttractionItem[]>([]);
  const [showOnlyRegisteredPicks, setShowOnlyRegisteredPicks] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  const placeRowIdSeq = useRef(0);
  const nextPlaceRowId = useCallback(() => {
    placeRowIdSeq.current += 1;
    return `glow-place-${placeRowIdSeq.current}`;
  }, []);

  /** 설문 장소(placeRows)는 DB에서 최초 1회만 채움. 이후 토큰 갱신 등으로 load가 돌아도 덮어쓰지 않음(제거·편집 유지). */
  const surveyPlacesHydratedKey = useRef<string | null>(null);
  /** 설문 기본 필드(concept/idol/date)는 DB에서 최초 1회만 채움. 이후 사용자 편집값을 유지. */
  const surveyFormHydratedKey = useRef<string | null>(null);

  const tourYearOptions = useMemo(() => {
    const current = new Date().getFullYear();
    const years: number[] = [];
    for (let year = current + 1; year >= 2018; year -= 1) {
      years.push(year);
    }
    return years;
  }, []);

  useEffect(() => {
    if (!authReady || !accessToken || user?.role !== 'ADMIN') return;
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);
      setMessage(null);
      try {
        const [survey, productList] = await Promise.all([
          fetchAdminSurvey(accessToken),
          fetchAdminProducts(accessToken),
        ]);
        if (cancelled) return;

        setProducts(productList);
        setReferenceProductId(prev =>
          prev != null && productList.some(product => product.id === prev) ? prev : null,
        );

        if (!survey) {
          setSurveyId(null);
          setConcept('');
          setIdolName('');
          setVisitStartDate('');
          setVisitEndDate('');
          setVisitStartTime('09:00');
          setVisitEndTime('24:00');
          setPlaceRows([]);
          setPresetRows([]);
          surveyPlacesHydratedKey.current = null;
          surveyFormHydratedKey.current = null;
          return;
        }

        setSurveyId(survey.id);

        const hydrateKey = `${user?.id ?? ''}|${survey.id}`;
        if (surveyFormHydratedKey.current !== hydrateKey) {
          surveyFormHydratedKey.current = hydrateKey;
          setConcept(survey.concept);
          setIdolName(survey.idolName);
          setVisitStartDate(survey.visitStartDate);
          setVisitEndDate(survey.visitEndDate);
          setVisitStartTime(survey.visitStartTime ?? '09:00');
          setVisitEndTime(survey.visitEndTime ?? '24:00');
        }
        if (surveyPlacesHydratedKey.current !== hydrateKey) {
          surveyPlacesHydratedKey.current = hydrateKey;
          const hydratedRows = dedupeLabels(survey.places).map(label => ({ id: nextPlaceRowId(), label }));
          setPlaceRows(hydratedRows);
          setPresetRows(hydratedRows);
        }
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : '취향·여행 정보를 불러오지 못했습니다.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    void load();
    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, user?.role, user?.id, nextPlaceRowId]);

  useEffect(() => {
    if (!authReady || !accessToken || user?.role !== 'ADMIN' || !referenceProductId) {
      setProductTourPicks([]);
      setShowOnlyRegisteredPicks(false);
      return;
    }
    let cancelled = false;
    void (async () => {
      try {
        const detail = await fetchAdminProduct(accessToken, referenceProductId);
        if (cancelled) return;
        const picks = detail.tourAttractionPicks ?? [];
        setProductTourPicks(picks);
        setShowOnlyRegisteredPicks(picks.length > 0);
      } catch {
        if (!cancelled) {
          setProductTourPicks([]);
          setShowOnlyRegisteredPicks(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, user?.role, referenceProductId]);

  const hasFixedProductPicks = productTourPicks.length > 0;
  const isAllSelectionMode =
    referenceProductId == null &&
    tourSignguCode == null &&
    tourYear == null &&
    tourMonth == null;

  const registeredPickCodes = useMemo(
    () => new Set(productTourPicks.map(p => p.attractionCode).filter(Boolean)),
    [productTourPicks],
  );

  const displayedAttractions = useMemo(() => {
    if (!attractionData) return [];
    const list = [...attractionData.attractions];
    const labelOf = (item: ProductTourAttractionItem) => formatTourSpotLabel(item).trim();
    const isRegistered = (item: ProductTourAttractionItem) =>
      registeredPickCodes.has(item.attractionCode) ||
      productTourPicks.some(p => labelOf(p) === labelOf(item));

    if (showOnlyRegisteredPicks) {
      return list.filter(isRegistered);
    }
    return list.sort((a, b) => {
      const pa = isRegistered(a) ? 0 : 1;
      const pb = isRegistered(b) ? 0 : 1;
      if (pa !== pb) return pa - pb;
      return a.rank - b.rank;
    });
  }, [attractionData, productTourPicks, registeredPickCodes, showOnlyRegisteredPicks]);

  const displayedAttractionLabelSet = useMemo(
    () => new Set(displayedAttractions.map(item => formatTourSpotLabel(item).trim()).filter(Boolean)),
    [displayedAttractions],
  );
  const presetLabelSet = useMemo(
    () => new Set(presetRows.map(row => row.label.trim()).filter(Boolean)),
    [presetRows],
  );
  const visibleAddressAttractions = useMemo(
    () => displayedAttractions.filter(item => presetLabelSet.has(formatTourSpotLabel(item).trim())),
    [displayedAttractions, presetLabelSet],
  );
  const shouldShowPresetRows = !attractionData || visibleAddressAttractions.length === 0;

  const visiblePickedRows = useMemo(() => {
    if (!hasSearched) {
      return [];
    }
    const hasAnyCondition =
      referenceProductId != null ||
      tourSignguCode != null ||
      tourYear != null ||
      tourMonth != null;
    if (hasAnyCondition) {
      return placeRows.filter(row => displayedAttractionLabelSet.has(row.label.trim()));
    }
    return placeRows;
  }, [displayedAttractionLabelSet, hasSearched, placeRows, referenceProductId, tourMonth, tourSignguCode, tourYear]);

  const handleTourSearch = async () => {
    if (!authReady || user?.role !== 'ADMIN' || !accessToken) return;
    setHasSearched(true);
    const baseYm = resolveBaseYmOrNull(tourYear, tourMonth);

    if (!referenceProductId) {
      setAttractionError(null);
      setAttractionLoading(true);
      try {
        const details = await Promise.all(products.map(product => fetchAdminProduct(accessToken, product.id)));
        const picksByProduct = details.map(detail => ({
          productId: detail.id,
          picks: detail.tourAttractionPicks ?? [],
        })).filter(entry => entry.picks.length > 0);

        let picks: ProductTourAttractionItem[];
        const hasConditionFilter = tourSignguCode != null || baseYm != null;
        if (!hasConditionFilter) {
          picks = dedupeAttractions(picksByProduct.flatMap(entry => entry.picks));
        } else {
          const filtered = await Promise.all(
            picksByProduct.map(async entry => {
              const fromApi = await fetchProductTourAttractions(
                accessToken,
                entry.productId,
                1000,
                tourAreaCode,
                tourSignguCode ?? 11530,
                baseYm ?? undefined,
              );
              const allowedCodes = new Set(fromApi.attractions.map(item => item.attractionCode));
              return entry.picks.filter(item => allowedCodes.has(item.attractionCode));
            }),
          );
          picks = dedupeAttractions(filtered.flat());
        }

        setAttractionData({
          productId: 0,
          productName: '전체',
          district: '',
          areaCode: tourAreaCode,
          signguCode: tourSignguCode ?? 0,
          baseYm: baseYm ?? '',
          attractions: picks,
        });
      } catch (e) {
        setAttractionData(null);
        setAttractionError(e instanceof Error ? e.message : '상품 등록 연계 장소를 불러오지 못했습니다.');
      } finally {
        setAttractionLoading(false);
      }
      return;
    }
    if (hasFixedProductPicks) {
      setAttractionError(null);
      setAttractionLoading(true);
      setAttractionData({
        productId: referenceProductId,
        productName: products.find(p => p.id === referenceProductId)?.name ?? '',
        district: '',
        areaCode: tourAreaCode,
        signguCode: tourSignguCode ?? 0,
        baseYm: baseYm ?? '',
        attractions: [],
      });
      if (tourSignguCode == null && baseYm == null) {
        setAttractionData(previous => previous ? ({ ...previous, attractions: [...productTourPicks] }) : previous);
        setAttractionLoading(false);
        return;
      }
      try {
        const filtered = await fetchProductTourAttractions(
          accessToken,
          referenceProductId,
          1000,
          tourAreaCode,
          tourSignguCode ?? 11530,
          baseYm ?? undefined,
        );
        const allowedCodes = new Set(filtered.attractions.map(item => item.attractionCode));
        setAttractionData(previous =>
          previous
            ? { ...previous, attractions: productTourPicks.filter(item => allowedCodes.has(item.attractionCode)) }
            : previous,
        );
      } catch (e) {
        setAttractionData(null);
        setAttractionError(e instanceof Error ? e.message : '추천 관광지 조회에 실패했습니다.');
      } finally {
        setAttractionLoading(false);
      }
      return;
    }
    setAttractionLoading(true);
    setAttractionError(null);
    try {
      const response = await fetchProductTourAttractions(
        accessToken,
        referenceProductId,
        1000,
        tourAreaCode,
        tourSignguCode ?? 11530,
        baseYm ?? undefined,
      );
      setAttractionData(response);
    } catch (e) {
      setAttractionData(null);
      setAttractionError(e instanceof Error ? e.message : '추천 관광지 조회에 실패했습니다.');
    } finally {
      setAttractionLoading(false);
    }
  };

  const togglePickAttraction = (item: ProductTourAttractionItem) => {
    const label = formatTourSpotLabel(item).trim();
    if (!label) return;
    setPlaceRows(previous => {
      const idx = previous.findIndex(row => row.label.trim() === label);
      if (idx >= 0) {
        return previous.filter((_, i) => i !== idx);
      }
      return [...previous, { id: nextPlaceRowId(), label }];
    });
  };

  const applyProductRegisteredPicksToPlaces = () => {
    const labels = dedupeLabels(productTourPicks.map(p => formatTourSpotLabel(p)));
    const nextRows = labels.map(label => ({ id: nextPlaceRowId(), label }));
    setPlaceRows(nextRows);
    setPresetRows(nextRows);
  };

  const addProductPickToPlaces = (pick: ProductTourAttractionItem) => {
    const label = formatTourSpotLabel(pick).trim();
    if (!label) return;
    setPlaceRows(previous => {
      if (previous.some(row => row.label.trim() === label)) return previous;
      const next = [...previous, { id: nextPlaceRowId(), label }];
      setPresetRows(next);
      return next;
    });
  };

  const cancelPresetRowByLabel = (label: string) => {
    setPresetRows(previous => {
      const index = previous.findIndex(row => row.label.trim() === label.trim());
      if (index < 0) return previous;
      return previous.filter((_, i) => i !== index);
    });
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!accessToken) return;

    const places = dedupeLabels(presetRows.map(p => p.label));
    if (!concept) return setError('선호 컨셉을 선택해 주세요.');
    if (!idolName.trim()) return setError('아이돌(또는 그룹) 이름을 입력해 주세요.');
    const start = visitStartDate.trim();
    const end = visitEndDate.trim();
    if (!start || !end) return setError('방문 시작일·종료일을 입력해 주세요.');
    if (!isValidIsoYmd(start) || !isValidIsoYmd(end)) {
      return setError('방문 시작일·종료일은 YYYY-MM-DD 형식(예: 2026-04-24)으로 입력해 주세요.');
    }
    if (end < start) return setError('방문 종료일은 시작일 이후여야 합니다.');

    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      if (places.length === 0) {
        await clearAdminSurveyPlaces(accessToken);
        setPlaceRows([]);
        setPresetRows([]);
        setMessage('선택한 장소를 모두 비웠습니다.');
        return;
      }
      const res = await upsertAdminSurvey(accessToken, {
        concept: concept as SurveyConceptType,
        idolName: idolName.trim(),
        visitStartDate: start,
        visitEndDate: end,
        visitStartTime,
        visitEndTime,
        places,
      });
      setSurveyId(res.id);
      surveyPlacesHydratedKey.current = `${user?.id ?? ''}|${res.id}`;
      setMessage('취향·여행 정보를 저장했습니다.');
    } catch (e) {
      setError(e instanceof Error ? e.message : '저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  if (!authReady) return <main className={shellStyles.page}><div className={shellStyles.loading}>관리자 권한을 확인하는 중입니다.</div></main>;
  if (!accessToken || user?.role !== 'ADMIN') return <main className={shellStyles.page}><div className={shellStyles.denied}>관리자만 이 화면을 사용할 수 있습니다.</div></main>;

  return (
    <AdminMarketingShell
      currentPath="/admin/glow-recommendation"
      title="Glow추천"
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
      }}
      statusText={error ? error : loading ? '취향·여행 정보를 불러오는 중입니다.' : null}
      stats={[]}
    >
      <div className={styles.container}>
        <div className={styles.sheet}>
          <form className={styles.form} onSubmit={handleSubmit}>
            <section className={styles.panel}>
              <div className={styles.panelHeader}><h2 className={styles.panelTitle}>취향·여행</h2></div>

              <div className={styles.formBody}>
                <label className={`${styles.field} ${styles.fullWidth}`}>
                <span className={styles.label}>선호 컨셉</span>
                <select className={styles.select} value={concept} onChange={e => setConcept((e.target.value || '') as SurveyConceptType | '')}>
                  <option value="">전체</option>
                  {USER_SURVEY_CONCEPT_OPTIONS.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
                </select>
                </label>

                <label className={`${styles.field} ${styles.fullWidth}`}>
                <span className={styles.label}>아이돌 / 그룹</span>
                <input className={styles.input} value={idolName} onChange={e => setIdolName(e.target.value)} />
                </label>

                <div className={styles.gridTwo}>
                <label className={styles.field}>
                  <span className={styles.label}>방문 시작일</span>
                  <input
                    className={styles.input}
                    type="date"
                    value={visitStartDate}
                    onChange={e => setVisitStartDate(e.target.value)}
                  />
                </label>
                <label className={styles.field}>
                  <span className={styles.label}>방문 종료일</span>
                  <input
                    className={styles.input}
                    type="date"
                    value={visitEndDate}
                    onChange={e => setVisitEndDate(e.target.value)}
                  />
                </label>
                </div>
                <div className={styles.gridTwo}>
                  <label className={styles.field}>
                    <span className={styles.label}>방문 시작 시간</span>
                    <div className={styles.timeButtonGroup} role="group" aria-label="방문 시작 시간">
                      {TIME_SLOT_OPTIONS.map(time => (
                        <button
                          key={`start-${time}`}
                          type="button"
                          className={`${styles.timeButton} ${visitStartTime === time ? styles.timeButtonActive : ''}`}
                          onClick={() => setVisitStartTime(time)}
                        >
                          {time}
                        </button>
                      ))}
                    </div>
                  </label>
                  <label className={styles.field}>
                    <span className={styles.label}>방문 종료 시간</span>
                    <div className={styles.timeButtonGroup} role="group" aria-label="방문 종료 시간">
                      {TIME_SLOT_OPTIONS.map(time => (
                        <button
                          key={`end-${time}`}
                          type="button"
                          className={`${styles.timeButton} ${visitEndTime === time ? styles.timeButtonActive : ''}`}
                          onClick={() => setVisitEndTime(time)}
                        >
                          {time}
                        </button>
                      ))}
                    </div>
                  </label>
                </div>
                <div className={styles.glowTourSection}>
                <h3 className={styles.glowTourTitle}>취향 선택</h3>
                <div className={styles.glowTourFilterGrid}>
                  <div className={styles.glowTourFilterField}>
                    <select
                      className={styles.select}
                      aria-label="기준 상품"
                      value={referenceProductId ?? ''}
                      onChange={e => {
                        const nextValue = e.target.value.trim();
                        const nextProductId = nextValue ? Number(nextValue) : null;
                        setReferenceProductId(nextProductId);
                        if (nextProductId == null) {
                          setTourSignguCode(null);
                          setTourYear(null);
                          setTourMonth(null);
                        } else {
                          setTourSignguCode(prev => prev ?? 11530);
                          setTourYear(prev => prev ?? new Date().getFullYear());
                          setTourMonth(prev => prev ?? 1);
                        }
                        setAttractionError(null);
                      }}
                    >
                      <option value="">전체</option>
                      {products.map(product => <option key={product.id} value={product.id}>{product.name}</option>)}
                    </select>
                  </div>
                  <div className={styles.glowTourFilterField}>
                    <select className={styles.select} aria-label="광역 지역" value={tourAreaCode} onChange={e => {
                      setTourAreaCode(Number(e.target.value));
                      setAttractionError(null);
                    }}>
                      {TOUR_AREA_OPTIONS.map(option => <option key={option.areaCode} value={option.areaCode}>{option.label}</option>)}
                    </select>
                  </div>
                  <div className={styles.glowTourFilterField}>
                    <select className={styles.select} aria-label="지역구" value={tourSignguCode ?? ''} onChange={e => {
                      setTourSignguCode(e.target.value ? Number(e.target.value) : null);
                      setAttractionError(null);
                    }}>
                      <option value="">전체</option>
                      {SEOUL_DISTRICT_SIGNGU_OPTIONS.map(option => <option key={option.signguCode} value={option.signguCode}>{option.label}</option>)}
                    </select>
                  </div>
                  <div className={styles.glowTourFilterField}>
                    <select className={styles.select} aria-label="기준 연도" value={tourYear ?? ''} onChange={e => {
                      setTourYear(e.target.value ? Number(e.target.value) : null);
                      setAttractionError(null);
                    }}>
                      <option value="">전체</option>
                      {tourYearOptions.map(year => <option key={year} value={year}>{year}년</option>)}
                    </select>
                  </div>
                  <div className={styles.glowTourFilterField}>
                    <select className={styles.select} aria-label="기준 월" value={tourMonth ?? ''} onChange={e => {
                      setTourMonth(e.target.value ? Number(e.target.value) : null);
                      setAttractionError(null);
                    }}>
                      <option value="">전체</option>
                      {Array.from({ length: 12 }, (_, index) => index + 1).map(month => <option key={month} value={month}>{String(month).padStart(2, '0')}월</option>)}
                    </select>
                  </div>
                  <button type="button" className={styles.glowTourPrimaryButton} onClick={() => void handleTourSearch()} disabled={attractionLoading}>
                    {attractionLoading ? '조회 중...' : '추천 조회'}
                  </button>
                </div>

                {productTourPicks.length > 0 ? (
                  <div className={styles.glowTourRegisteredPanel}>
                    <div className={styles.glowTourRegisteredHeader}>
                      <span className={styles.glowTourRegisteredTitle}>이 상품에 등록된 연계 장소 {productTourPicks.length}곳</span>
                      <button type="button" className={styles.glowTourGhostButton} onClick={() => applyProductRegisteredPicksToPlaces()}>
                        설문 장소로 덮어쓰기
                      </button>
                    </div>
                    <ul className={styles.glowTourRegisteredList}>
                      {productTourPicks.map(pick => {
                        const label = formatTourSpotLabel(pick).trim();
                        return (
                          <li key={pick.attractionCode || label} className={styles.glowTourRegisteredRow}>
                            <span className={styles.glowTourRegisteredLabel}>{label}</span>
                            <button type="button" className={styles.glowTourGhostButton} onClick={() => addProductPickToPlaces(pick)}>추가</button>
                          </li>
                        );
                      })}
                    </ul>
                  </div>
                ) : (
                  <p className={styles.glowTourInlineNote}>선택한 상품에 등록된 연계 관광지가 없습니다. 상품 편집 화면에서 먼저 등록해 주세요.</p>
                )}
                {attractionLoading && <p className={styles.glowTourInlineNote}>추천 관광지 정보를 불러오는 중입니다.</p>}
                {!attractionLoading && attractionError && <p className={styles.glowTourErrorInline}>{attractionError}</p>}
                {!attractionLoading && !attractionError && !attractionData && (
                  <p className={styles.glowTourSearchPrompt}>조건을 선택한 뒤 추천 조회를 눌러 주세요.</p>
                )}

                {visiblePickedRows.length > 0 ? (
                  <div className={styles.glowTourPickedPanel}>
                    <div className={styles.glowTourPickedHeader}>
                      <span className={styles.glowTourPickedTitle}>선택한 장소 {visiblePickedRows.length}곳</span>
                    </div>
                    <ul className={styles.glowTourPickedList}>
                      {visiblePickedRows.map(row => (
                        <li key={row.id} className={styles.glowTourPickedRow}>
                          <span className={styles.glowTourPickedLabel}>{row.label}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                ) : null}

                {!attractionLoading && !attractionError && attractionData ? (
                  <>
                    {!hasFixedProductPicks && attractionData && attractionData.attractions.length > 0 && productTourPicks.length > 0 ? (
                      <label className={styles.glowTourFilterToggle}>
                        <input
                          type="checkbox"
                          checked={showOnlyRegisteredPicks}
                          onChange={e => setShowOnlyRegisteredPicks(e.target.checked)}
                        />
                        <span>상품에 등록한 연계 장소만 목록에 표시</span>
                      </label>
                    ) : null}
                    {visibleAddressAttractions.length === 0 ? (
                      <p className={styles.glowTourInlineNote}>
                        {!hasFixedProductPicks && showOnlyRegisteredPicks
                          ? '등록된 연계 장소가 현재 검색 조건 결과에 없습니다. 지역·년월을 바꾸거나 «등록 장소만»을 끄고 다시 조회해 보세요.'
                          : '추천 결과가 없습니다. 조건을 바꿔 다시 시도해 주세요.'}
                      </p>
                    ) : (
                      <ul className={styles.glowTourAddressList}>
                        {visibleAddressAttractions.map(item => {
                          const label = formatTourSpotLabel(item).trim();
                          const selected = presetLabelSet.has(label);
                          return (
                            <li key={item.attractionCode}>
                              <button
                                type="button"
                                className={`${styles.glowTourAddressButton} ${selected ? styles.glowTourAddressButtonActive : ''}`}
                                onClick={() => cancelPresetRowByLabel(label)}
                              >
                                {label}
                              </button>
                            </li>
                          );
                        })}
                      </ul>
                    )}
                  </>
                ) : null}

                {shouldShowPresetRows && presetRows.length > 0 ? (
                  <ul className={styles.glowTourAddressList}>
                    {presetRows.map(row => (
                      <li key={`preset-${row.id}`}>
                        <button
                          type="button"
                          className={`${styles.glowTourAddressButton} ${styles.glowTourAddressButtonActive}`}
                          onClick={() => cancelPresetRowByLabel(row.label)}
                        >
                          {row.label}
                        </button>
                      </li>
                    ))}
                  </ul>
                ) : null}
                </div>
              </div>
            </section>

            {message ? <p className={styles.message}>{message}</p> : null}
            <div className={styles.actionBar}><button type="submit" className={styles.submitButton} disabled={saving || loading}>{saving ? '저장 중…' : '취향·여행 저장'}</button></div>
          </form>
        </div>
      </div>
    </AdminMarketingShell>
  );
}

export default AdminGlowRecommendationPage;
