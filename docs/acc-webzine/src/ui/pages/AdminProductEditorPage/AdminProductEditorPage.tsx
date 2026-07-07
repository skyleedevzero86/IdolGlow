import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import MarkdownEditorField from '../../components/MarkdownEditorField/MarkdownEditorField';
import {
  createAdminProduct,
  fetchAdminOptionsPage,
  fetchAdminProduct,
  fetchProductTourAttractions,
  type AdminOptionPage,
  type AdminProductDetail,
  type AdminOptionSummary,
  type ProductTourAttractionItem,
  type ProductTourAttractionResult,
  updateAdminProduct,
} from '../../../shared/data/adminBookingApi';
import {
  composeTourBaseYm,
  formatTourSpotLabel,
  getDefaultTourBaseYmSeoul,
  parseTourBaseYm,
  SEOUL_DISTRICT_SIGNGU_OPTIONS,
  TOUR_AREA_OPTIONS,
} from '../../../shared/data/seoulTourDistrictOptions';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from '../AdminBookingManagement/AdminBookingManagement.module.css';

type ProductFormState = {
  readonly name: string;
  readonly description: string;
  /** 상품 기본가(원) — 옵션(추가 아이템) 금액과 별도 */
  readonly basePrice: string;
  readonly tagText: string;
  readonly optionIds: readonly number[];
  readonly slotStartDate: string;
  readonly slotEndDate: string;
  readonly slotStartTime: string;
  readonly slotEndTime: string;
};

const EMPTY_FORM: ProductFormState = {
  name: '',
  description: '',
  basePrice: '0',
  tagText: '',
  optionIds: [],
  slotStartDate: '',
  slotEndDate: '',
  slotStartTime: '09:00',
  slotEndTime: '16:00',
};

function toTagNames(tagText: string): string[] {
  return Array.from(
    new Set(
      tagText
        .split(',')
        .map(item => item.trim())
        .filter(Boolean)
    )
  );
}

const OPTION_PICKER_PAGE_SIZE = 10;

function buildFormFromDetail(detail: AdminProductDetail): ProductFormState {
  return {
    name: detail.name,
    description: detail.description,
    basePrice: String(detail.basePrice ?? 0),
    tagText: detail.tagNames.join(', '),
    optionIds: detail.options.map(option => option.id),
    slotStartDate: detail.slotStartDate ?? '',
    slotEndDate: detail.slotEndDate ?? '',
    slotStartTime: detail.slotStartTime ? detail.slotStartTime.slice(0, 5) : '09:00',
    slotEndTime: detail.slotEndTime ? detail.slotEndTime.slice(0, 5) : '16:00',
  };
}

function toOptionsById(
  list: readonly { id: number; name: string; description: string; price: number; location: string }[],
): Record<number, AdminOptionSummary> {
  const record: Record<number, AdminOptionSummary> = {};
  for (const option of list) {
    record[option.id] = {
      id: option.id,
      name: option.name,
      description: option.description,
      price: option.price,
      location: option.location,
    };
  }
  return record;
}

function formatWon(n: number): string {
  return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(n);
}

/** 숫자 PK만 수정 모드. 경로가 `.../products/new`로만 잡혀 `productId === 'new'`인 경우 등은 신규로 본다. */
function parseNumericProductId(raw: string | undefined): number | null {
  if (raw == null) {
    return null;
  }
  const trimmed = raw.trim();
  if (trimmed === '' || trimmed === 'new' || !/^\d+$/.test(trimmed)) {
    return null;
  }
  const n = Number(trimmed);
  return Number.isSafeInteger(n) && n > 0 ? n : null;
}

export function AdminProductEditorPage() {
  const { productId: productIdParam } = useParams<{ productId: string }>();
  const numericProductId = useMemo(() => parseNumericProductId(productIdParam), [productIdParam]);
  const isEdit = numericProductId != null;
  const navigate = useNavigate();
  const { accessToken, authReady, user } = useAuth();
  const [form, setForm] = useState<ProductFormState>(EMPTY_FORM);
  /** 상품에 붙은 옵션 메타(이름·가격·장소 표시용). 수천 건 전체를 로드하지 않음. */
  const [optionsById, setOptionsById] = useState<Record<number, AdminOptionSummary>>({});
  const [optionPickerOpen, setOptionPickerOpen] = useState(false);
  const [optionPickerQInput, setOptionPickerQInput] = useState('');
  const [optionPickerQ, setOptionPickerQ] = useState('');
  const [optionPickerPage, setOptionPickerPage] = useState(0);
  const [optionPickerData, setOptionPickerData] = useState<AdminOptionPage | null>(null);
  const [optionPickerLoading, setOptionPickerLoading] = useState(false);
  const [optionPickerError, setOptionPickerError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
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
    if (!authReady || user?.role !== 'ADMIN') {
      return;
    }

    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        if (numericProductId != null) {
          const detail = await fetchAdminProduct(accessToken, numericProductId);
          setForm(buildFormFromDetail(detail));
          setOptionsById(toOptionsById(detail.options));
          setPickedAttractions(detail.tourAttractionPicks ?? []);
        } else {
          setForm(EMPTY_FORM);
          setOptionsById({});
          setPickedAttractions([]);
        }
      } catch (loadError) {
        setForm(EMPTY_FORM);
        setOptionsById({});
        setPickedAttractions([]);
        setError(loadError instanceof Error ? loadError.message : '상품 정보를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [accessToken, authReady, numericProductId, user?.role]);

  useEffect(() => {
    if (!optionPickerOpen || !authReady || user?.role !== 'ADMIN') {
      return;
    }
    const loadOptions = async () => {
      setOptionPickerLoading(true);
      setOptionPickerError(null);
      try {
        const page = await fetchAdminOptionsPage(accessToken, {
          q: optionPickerQ,
          page: optionPickerPage,
          size: OPTION_PICKER_PAGE_SIZE,
        });
        setOptionPickerData(page);
      } catch (fetchError) {
        setOptionPickerData(null);
        setOptionPickerError(
          fetchError instanceof Error ? fetchError.message : '옵션 목록을 불러오지 못했습니다.',
        );
      } finally {
        setOptionPickerLoading(false);
      }
    };
    void loadOptions();
  }, [accessToken, authReady, optionPickerOpen, optionPickerPage, optionPickerQ, user?.role]);

  useEffect(() => {
    if (!optionPickerOpen) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOptionPickerOpen(false);
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [optionPickerOpen]);

  const openOptionPicker = useCallback(() => {
    setOptionPickerQInput('');
    setOptionPickerQ('');
    setOptionPickerPage(0);
    setOptionPickerData(null);
    setOptionPickerError(null);
    setOptionPickerOpen(true);
  }, []);

  const applyOptionSearch = useCallback(() => {
    setOptionPickerQ(optionPickerQInput.trim());
    setOptionPickerPage(0);
  }, [optionPickerQInput]);

  const addOptionFromPicker = useCallback((option: AdminOptionSummary) => {
    setOptionsById(previous => ({ ...previous, [option.id]: option }));
    setForm(previous => ({
      ...previous,
      optionIds: Array.from(new Set([...previous.optionIds, option.id])),
    }));
  }, []);

  const removeOptionFromProduct = useCallback((optionId: number) => {
    setForm(previous => ({
      ...previous,
      optionIds: previous.optionIds.filter(id => id !== optionId),
    }));
  }, []);

  const handleTourSearch = useCallback(async () => {
    if (!authReady || user?.role !== 'ADMIN' || !accessToken) {
      return;
    }
    const targetProductId = numericProductId ?? 0;
    const baseYm = composeTourBaseYm(tourYear, tourMonth);
    setAttractionLoading(true);
    setAttractionError(null);
    try {
      const response = await fetchProductTourAttractions(
        accessToken,
        targetProductId,
        tourResultLimit,
        tourAreaCode,
        tourSignguCode,
        baseYm,
      );
      setAttractionData(response);
    } catch (loadError) {
      setAttractionData(null);
      setAttractionError(
        loadError instanceof Error ? loadError.message : '주변 관광지 추천 정보를 불러오지 못했습니다.',
      );
    } finally {
      setAttractionLoading(false);
    }
  }, [
    accessToken,
    authReady,
    numericProductId,
    tourAreaCode,
    tourMonth,
    tourResultLimit,
    tourSignguCode,
    tourYear,
    user?.role,
  ]);

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

  const handleSubmit = async () => {
    if (!form.name.trim() || !form.description.trim()) {
      setError('상품명과 설명을 입력해 주세요.');
      return;
    }

    const basePriceN = Number(form.basePrice);
    if (Number.isNaN(basePriceN) || basePriceN < 0) {
      setError('상품 금액은 0 이상 숫자로 입력해 주세요.');
      return;
    }

    if (!form.slotStartTime || !form.slotEndTime) {
      setError('운영 시작/종료 시간을 입력해 주세요.');
      return;
    }

    const [startHour, startMinute] = form.slotStartTime.split(':').map(Number);
    const [endHour, endMinute] = form.slotEndTime.split(':').map(Number);
    const startTotalMinutes = startHour * 60 + startMinute;
    const endTotalMinutes = endHour * 60 + endMinute;

    if (
      Number.isNaN(startTotalMinutes) ||
      Number.isNaN(endTotalMinutes)
    ) {
      setError('운영 시간을 HH:mm 형식으로 입력해 주세요.');
      return;
    }

    if (startTotalMinutes >= endTotalMinutes) {
      setError('운영 시작 시간은 종료 시간보다 빨라야 합니다.');
      return;
    }

    const validOptionIds = Array.from(new Set(form.optionIds));

    setSaving(true);
    setError(null);
    try {
      const payload = {
        name: form.name,
        description: form.description,
        basePrice: basePriceN,
        slotStartDate: form.slotStartDate || null,
        slotEndDate: form.slotEndDate || null,
        slotStartHour: startHour,
        slotEndHour: endHour,
        slotStartTime: form.slotStartTime,
        slotEndTime: form.slotEndTime,
        optionIds: validOptionIds,
        tagNames: toTagNames(form.tagText),
        tourAttractionPicks: [...pickedAttractions],
      };

      if (numericProductId != null) {
        await updateAdminProduct(accessToken, numericProductId, payload);
        const refreshed = await fetchAdminProduct(accessToken, numericProductId);
        setForm(buildFormFromDetail(refreshed));
        setOptionsById(previous => {
          const next = { ...previous };
          for (const option of refreshed.options) {
            next[option.id] = {
              id: option.id,
              name: option.name,
              description: option.description,
              price: option.price,
              location: option.location,
            };
          }
          return next;
        });
        setPickedAttractions(refreshed.tourAttractionPicks ?? []);
        setError('저장되었습니다. 선택한 관광지는 상품에 연결되어 다시 불러올 때까지 유지됩니다.');
      } else {
        const created = await createAdminProduct(accessToken, payload);
        navigate(`/admin/products/${created.id}/edit`);
        return;
      }
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : '상품을 저장하지 못했습니다.');
    } finally {
      setSaving(false);
    }
  };

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.loading}>관리자 권한을 확인하는 중입니다.</div>
      </main>
    );
  }

  if (user?.role !== 'ADMIN') {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.denied}>관리자만 상품 편집 화면을 사용할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <>
    <AdminMarketingShell
      currentPath="/admin/products"
      title={isEdit ? '상품 수정' : '상품 등록'}
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
      }}
      statusText={error ?? (loading ? '상품 정보를 불러오는 중입니다.' : null)}
      stats={[]}
    >
      <section className={styles.section}>
        <section className={styles.panel}>
          <div className={styles.panelBody}>
            <div className={styles.formGrid}>
              <label className={`${styles.field} ${styles.fullWidth}`}>
                <span className={styles.label}>상품가 (기본가, 원)</span>
                <input
                  className={styles.input}
                  type="number"
                  min={0}
                  step="1"
                  inputMode="numeric"
                  value={form.basePrice}
                  onChange={event =>
                    setForm(previous => ({ ...previous, basePrice: event.target.value }))
                  }
                  placeholder="예: 35000 (옵션 추가가와 별도)"
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>상품명</span>
                <input
                  className={styles.input}
                  value={form.name}
                  onChange={event => setForm(previous => ({ ...previous, name: event.target.value }))}
                  placeholder="상품명을 입력해 주세요."
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>예약 시작일</span>
                <input
                  className={styles.dateInput}
                  type="date"
                  value={form.slotStartDate}
                  onChange={event =>
                    setForm(previous => ({ ...previous, slotStartDate: event.target.value }))
                  }
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>예약 종료일</span>
                <input
                  className={styles.dateInput}
                  type="date"
                  value={form.slotEndDate}
                  onChange={event =>
                    setForm(previous => ({ ...previous, slotEndDate: event.target.value }))
                  }
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>시작 시간</span>
                <input
                  className={styles.dateInput}
                  type="time"
                  step={60}
                  value={form.slotStartTime}
                  onChange={event =>
                    setForm(previous => ({
                      ...previous,
                      slotStartTime: event.target.value,
                    }))
                  }
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>종료 시간</span>
                <input
                  className={styles.dateInput}
                  type="time"
                  step={60}
                  value={form.slotEndTime}
                  onChange={event =>
                    setForm(previous => ({
                      ...previous,
                      slotEndTime: event.target.value,
                    }))
                  }
                />
              </label>

              <div className={`${styles.field} ${styles.fullWidth}`}>
                <span className={styles.label}>추가 아이템 (옵션)</span>
                <div className={styles.optionAddToolbar}>
                  <button type="button" className={styles.secondaryButton} onClick={openOptionPicker}>
                    옵션 검색·추가
                  </button>
                </div>
                <div className={styles.checkboxGrid}>
                  {form.optionIds.map(id => {
                    const option = optionsById[id];
                    const optionToggleId = `admin-product-option-${id}`;
                    if (!option) {
                      return (
                        <div key={id} className={styles.checkboxCard}>
                          <p className={styles.optionUnknown}>
                            옵션 #{id} (메타 없음) —{' '}
                            <button
                              type="button"
                              className={styles.linkishButton}
                              onClick={() => removeOptionFromProduct(id)}
                            >
                              연결 해제
                            </button>
                          </p>
                        </div>
                      );
                    }
                    return (
                      <div key={option.id} className={styles.checkboxCard}>
                        <label className={styles.checkboxCardToggle} htmlFor={optionToggleId}>
                          <input
                            id={optionToggleId}
                            type="checkbox"
                            checked
                            onChange={event => {
                              if (!event.target.checked) {
                                removeOptionFromProduct(option.id);
                              }
                            }}
                          />
                        </label>
                        <div className={`${styles.checkboxMeta} ${styles.optionItemMeta}`}>
                          <span className={styles.checkboxTitle}>
                            {option.name}{' '}
                            <span className={styles.optionPriceInline}>({formatWon(option.price)})</span>
                          </span>
                          <span className={styles.checkboxDescription}>{option.location}</span>
                        </div>
                        <div className={styles.optionItemActions}>
                          <button
                            type="button"
                            className={styles.ghostButton}
                            onClick={() => navigate(`/admin/options/${option.id}/edit`)}
                          >
                            옵션 편집
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

            </div>
          </div>
        </section>

        <section className={styles.panel}>
          <div className={styles.panelBody}>
            <MarkdownEditorField
              label="상품 설명"
              value={form.description}
              onChange={value => setForm(previous => ({ ...previous, description: value }))}
              placeholder="상품 설명을 마크다운으로 작성해 주세요."
              tagsValue={form.tagText}
              onTagsChange={value => setForm(previous => ({ ...previous, tagText: value }))}
              tagsPlaceholder="태그를 입력하세요 (예: 뷰티 웨딩 메이크업)"
            />
          </div>
        </section>

        <section className={styles.panel}>
          <div className={styles.panelBody}>
            <h3 className={styles.detailTitle}>상품 기준 주변/연계 관광지 추천</h3>
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
                        {String(month).padStart(2, '0')}월
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
                  {attractionLoading ? '조회 중...' : '추천 조회'}
                </button>
              </div>
              {attractionLoading && <p className={styles.helper}>추천 관광지 정보를 불러오는 중입니다.</p>}
              {!attractionLoading && attractionError && <p className={styles.error}>{attractionError}</p>}
              {!attractionLoading && !attractionError && !attractionData && (
                <p className={styles.tourSearchPrompt}>조건을 선택한 뒤 추천 조회를 눌러 주세요.</p>
              )}
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
                    <p className={styles.empty}>추천 결과가 없습니다. 기준연월·구역을 바꿔 보시거나 잠시 후 다시 시도해 주세요.</p>
                  ) : (
                    <ul className={styles.tourAddressList}>
                      {attractionData.attractions.map(item => {
                        const selected = pickedCodeSet.has(item.attractionCode);
                        return (
                          <li key={item.attractionCode}>
                            <button
                              type="button"
                              className={`${styles.tourAddressButton} ${selected ? styles.tourAddressButtonActive : ''}`}
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

        <section className={styles.panel}>
          <div className={styles.panelBody}>
            <div className={`${styles.buttonRow} ${styles.buttonRowAlignEnd}`}>
              <button
                type="button"
                className={styles.primaryButton}
                onClick={() => void handleSubmit()}
                disabled={saving || loading}
              >
                {saving ? '저장 중...' : isEdit ? '수정 저장' : '등록 저장'}
              </button>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => navigate('/admin/products')}
              >
                목록 이동
              </button>
            </div>
          </div>
        </section>
      </section>
    </AdminMarketingShell>
    {optionPickerOpen ? (
      <div
        className={styles.modalBackdrop}
        role="presentation"
        onClick={() => setOptionPickerOpen(false)}
      >
        <div
          className={`${styles.modalPanel} ${styles.optionPickerPanel}`}
          role="dialog"
          aria-modal="true"
          aria-labelledby="admin-option-picker-title"
          onClick={event => event.stopPropagation()}
        >
          <h3 id="admin-option-picker-title" className={styles.modalTitle}>
            옵션 검색·추가
          </h3>
          <p className={styles.modalSubtitle}>
            키워드는 옵션 <strong>이름·장소·설명</strong>을 함께 봅니다. 10개씩 페이지로 넘깁니다.
          </p>
          <div className={styles.optionPickerSearchRow}>
            <input
              className={styles.input}
              value={optionPickerQInput}
              onChange={event => setOptionPickerQInput(event.target.value)}
              onKeyDown={event => {
                if (event.key === 'Enter') {
                  applyOptionSearch();
                }
              }}
              placeholder="검색어 (이름, 장소, 설명)"
            />
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={applyOptionSearch}
            >
              검색
            </button>
          </div>
          {optionPickerLoading ? <p className={styles.pickerMuted}>불러오는 중…</p> : null}
          {optionPickerError ? <p className={styles.pickerError}>{optionPickerError}</p> : null}
          {optionPickerData && optionPickerData.content.length === 0 && !optionPickerLoading ? (
            <p className={styles.empty}>검색 결과가 없습니다. 검색어를 바꿔 보세요.</p>
          ) : null}
          {optionPickerData && optionPickerData.content.length > 0 ? (
            <div className={styles.optionPickerTableScroll}>
              <table className={styles.optionPickerTable}>
                <thead>
                  <tr>
                    <th>이름</th>
                    <th>추가 금액</th>
                    <th>장소</th>
                    <th>추가</th>
                  </tr>
                </thead>
                <tbody>
                  {optionPickerData.content.map(o => {
                    const already = form.optionIds.includes(o.id);
                    return (
                      <tr key={o.id}>
                        <td className={styles.tdName}>{o.name}</td>
                        <td className={styles.tdNum}>{formatWon(o.price)}</td>
                        <td className={styles.tdLocation}>{o.location}</td>
                        <td>
                          {already ? (
                            <span className={styles.pillTag}>이미 연결</span>
                          ) : (
                            <button
                              type="button"
                              className={styles.pickerAddButton}
                              onClick={() => addOptionFromPicker(o)}
                            >
                              추가
                            </button>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : null}
          {optionPickerData && optionPickerData.totalElements > 0 ? (
            <div className={styles.optionPickerPager}>
              <button
                type="button"
                className={styles.secondaryButton}
                disabled={optionPickerPage <= 0 || optionPickerLoading}
                onClick={() => setOptionPickerPage(p => Math.max(0, p - 1))}
              >
                이전
              </button>
              <span className={styles.pagerInfo}>
                {optionPickerData.number + 1} / {optionPickerData.totalPages} (전체 {optionPickerData.totalElements}건)
              </span>
              <button
                type="button"
                className={styles.secondaryButton}
                disabled={optionPickerLoading || optionPickerPage + 1 >= optionPickerData.totalPages}
                onClick={() => setOptionPickerPage(p => p + 1)}
              >
                다음
              </button>
            </div>
          ) : null}
          <div className={styles.modalFooter}>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={() => setOptionPickerOpen(false)}
            >
              닫기
            </button>
          </div>
        </div>
      </div>
    ) : null}
    </>
  );
}

export default AdminProductEditorPage;
