import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  fetchPublicProductDetail,
  fetchPublicProductTourAttractions,
  type AdminProductDetail,
  type ProductTourAttractionItem,
  type ProductTourAttractionResult,
} from "../../../shared/data/adminBookingApi";
import {
  composeTourBaseYm,
  formatTourSpotLabel,
  getDefaultTourBaseYmSeoul,
  parseTourBaseYm,
  SEOUL_DISTRICT_SIGNGU_OPTIONS,
  TOUR_AREA_OPTIONS,
} from "../../../shared/data/seoulTourDistrictOptions";
import styles from "./ProductPublicDetailPage.module.css";

export function ProductPublicDetailPage() {
  const { id } = useParams<{ id: string }>();
  const productId = useMemo(() => Number(id), [id]);
  const [product, setProduct] = useState<AdminProductDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [attractionData, setAttractionData] = useState<ProductTourAttractionResult | null>(null);
  const [attractionLoading, setAttractionLoading] = useState(false);
  const [attractionError, setAttractionError] = useState<string | null>(null);

  const defaultTourYm = useMemo(() => parseTourBaseYm(getDefaultTourBaseYmSeoul()), []);
  const [tourAreaCode, setTourAreaCode] = useState(TOUR_AREA_OPTIONS[0]?.areaCode ?? 11);
  const [tourSignguCode, setTourSignguCode] = useState(11530);
  const [tourYear, setTourYear] = useState(defaultTourYm?.year ?? new Date().getFullYear());
  const [tourMonth, setTourMonth] = useState(defaultTourYm?.month ?? 1);
  const [tourResultLimit, setTourResultLimit] = useState(200);
  const [pickedAttractions, setPickedAttractions] = useState<readonly ProductTourAttractionItem[]>([]);

  const tourYearOptions = useMemo(() => {
    const current = new Date().getFullYear();
    const years: number[] = [];
    for (let year = current + 1; year >= 2018; year -= 1) {
      years.push(year);
    }
    return years;
  }, []);

  useEffect(() => {
    if (!Number.isFinite(productId) || productId <= 0) {
      setError("상품 ID가 올바르지 않습니다.");
      setLoading(false);
      return;
    }

    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const productResponse = await fetchPublicProductDetail(productId);
        setProduct(productResponse);
      } catch (loadError) {
        setProduct(null);
        setError(
          loadError instanceof Error
            ? loadError.message
            : "상품 상세 정보를 불러오지 못했습니다.",
        );
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [productId]);

  const handleTourSearch = useCallback(async () => {
    if (!Number.isFinite(productId) || productId <= 0) {
      return;
    }
    const baseYm = composeTourBaseYm(tourYear, tourMonth);
    setAttractionLoading(true);
    setAttractionError(null);
    try {
      const response = await fetchPublicProductTourAttractions(
        productId,
        tourResultLimit,
        tourAreaCode,
        tourSignguCode,
        baseYm,
      );
      setAttractionData(response);
    } catch (loadError) {
      setAttractionData(null);
      setAttractionError(
        loadError instanceof Error
          ? loadError.message
          : "주변 관광지 추천 정보를 불러오지 못했습니다.",
      );
    } finally {
      setAttractionLoading(false);
    }
  }, [productId, tourAreaCode, tourMonth, tourResultLimit, tourSignguCode, tourYear]);

  const pickedCodeSet = useMemo(
    () => new Set(pickedAttractions.map(item => item.attractionCode)),
    [pickedAttractions],
  );

  const togglePickAttraction = useCallback((item: ProductTourAttractionItem) => {
    setPickedAttractions(previous => {
      const exists = previous.some(entry => entry.attractionCode === item.attractionCode);
      if (exists) {
        return previous.filter(entry => entry.attractionCode !== item.attractionCode);
      }
      return [...previous, item];
    });
  }, []);

  const removePickedAttraction = useCallback((attractionCode: string) => {
    setPickedAttractions(previous => previous.filter(entry => entry.attractionCode !== attractionCode));
  }, []);

  const clearPickedAttractions = useCallback(() => {
    setPickedAttractions([]);
  }, []);

  return (
    <main className={styles.page}>
      <section className={styles.panel}>
        <div className={styles.panelBody}>
          <div className={styles.metaRow}>
            <Link to="/my-payments">결제 내역으로</Link>
          </div>
          {loading ? <p className={styles.helper}>상품 정보를 불러오는 중입니다.</p> : null}
          {error ? <p className={styles.error}>{error}</p> : null}
          {!loading && !error && product ? (
            <>
              <h1 className={styles.title}>{product.name}</h1>
              <p className={styles.description}>{product.description}</p>
              <div className={styles.metaRow}>
                <span className={styles.chip}>
                  최저가 {new Intl.NumberFormat("ko-KR").format(product.minPrice)}원
                </span>
                <span className={styles.chip}>
                  총합 {new Intl.NumberFormat("ko-KR").format(product.totalPrice)}원
                </span>
                <span className={styles.chip}>
                  태그 {product.tagNames.length > 0 ? product.tagNames.join(", ") : "없음"}
                </span>
              </div>
            </>
          ) : null}
        </div>
      </section>

      {!loading && !error && product ? (
        <section className={styles.panel}>
          <div className={styles.panelBody}>
            <h2 className={styles.title}>상품 기준 주변/연계 관광지 추천</h2>
            <div className={styles.tourFilterTopSpacer} aria-hidden />
            <div className={styles.tourFilterGrid}>
              <div className={styles.tourFilterField}>
                <select
                  className={styles.select}
                  aria-label="광역 지역"
                  value={tourAreaCode}
                  onChange={event => setTourAreaCode(Number(event.target.value))}
                >
                  {TOUR_AREA_OPTIONS.map(option => (
                    <option key={option.areaCode} value={option.areaCode}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className={styles.tourFilterField}>
                <select
                  className={styles.select}
                  aria-label="지역구"
                  value={tourSignguCode}
                  onChange={event => setTourSignguCode(Number(event.target.value))}
                >
                  {SEOUL_DISTRICT_SIGNGU_OPTIONS.map(option => (
                    <option key={option.signguCode} value={option.signguCode}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className={styles.tourFilterField}>
                <select
                  className={styles.select}
                  aria-label="기준 연도"
                  value={tourYear}
                  onChange={event => setTourYear(Number(event.target.value))}
                >
                  {tourYearOptions.map(year => (
                    <option key={year} value={year}>
                      {year}년
                    </option>
                  ))}
                </select>
              </div>
              <div className={styles.tourFilterField}>
                <select
                  className={styles.select}
                  aria-label="기준 월"
                  value={tourMonth}
                  onChange={event => setTourMonth(Number(event.target.value))}
                >
                  {Array.from({ length: 12 }, (_, index) => index + 1).map(month => (
                    <option key={month} value={month}>
                      {String(month).padStart(2, "0")}월
                    </option>
                  ))}
                </select>
              </div>
              <div className={styles.tourFilterField}>
                <select
                  className={styles.select}
                  aria-label="최대 건수"
                  value={tourResultLimit}
                  onChange={event => setTourResultLimit(Number(event.target.value))}
                >
                  {[50, 100, 200, 500, 1000].map(limit => (
                    <option key={limit} value={limit}>
                      {limit}건
                    </option>
                  ))}
                </select>
              </div>
              <button
                type="button"
                className={styles.primaryButton}
                onClick={() => void handleTourSearch()}
                disabled={attractionLoading}
              >
                {attractionLoading ? "조회 중..." : "추천 조회"}
              </button>
            </div>
            {attractionLoading ? <p className={styles.helper}>추천 관광지 정보를 불러오는 중입니다.</p> : null}
            {!attractionLoading && attractionError ? <p className={styles.error}>{attractionError}</p> : null}
            {!attractionLoading && !attractionError && !attractionData ? (
              <p className={styles.tourSearchPrompt}>조건을 선택한 뒤 추천 조회를 눌러 주세요.</p>
            ) : null}
            {pickedAttractions.length > 0 ? (
              <div className={styles.tourPickedPanel}>
                <div className={styles.tourPickedHeader}>
                  <span className={styles.tourPickedTitle}>선택한 장소 {pickedAttractions.length}곳</span>
                  <button type="button" className={styles.ghostButton} onClick={() => clearPickedAttractions()}>
                    선택 비우기
                  </button>
                </div>
                <ul className={styles.tourPickedList}>
                  {pickedAttractions.map(item => (
                    <li key={item.attractionCode} className={styles.tourPickedRow}>
                      <span className={styles.tourPickedLabel}>{formatTourSpotLabel(item)}</span>
                      <button
                        type="button"
                        className={styles.tourPickedRemove}
                        onClick={() => removePickedAttraction(item.attractionCode)}
                      >
                        제거
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}
            {!attractionLoading && !attractionError && attractionData ? (
              <>
                {attractionData.attractions.length === 0 ? (
                  <p className={styles.empty}>
                    추천 결과가 없습니다. 기준연월·구역을 바꿔 보시거나 잠시 후 다시 시도해 주세요.
                  </p>
                ) : (
                  <ul className={styles.tourAddressList}>
                    {attractionData.attractions.map(item => {
                      const selected = pickedCodeSet.has(item.attractionCode);
                      return (
                        <li key={item.attractionCode}>
                          <button
                            type="button"
                            className={selected ? styles.tourAddressButtonActive : styles.tourAddressButton}
                            onClick={() => togglePickAttraction(item)}
                          >
                            {formatTourSpotLabel(item)}
                          </button>
                        </li>
                      );
                    })}
                  </ul>
                )}
              </>
            ) : null}
          </div>
        </section>
      ) : null}
    </main>
  );
}

export default ProductPublicDetailPage;
