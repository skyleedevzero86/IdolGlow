import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import {
  fetchFestivalEventsByDate,
  fetchLclsCodes,
  fetchLDongCodes,
  searchFestivalByKeyword,
  type FestivalEventItem,
  type TourCodeItem,
} from "../../../shared/data/festivalInfoApi";
import shellStyles from "../AdminMarketingPage/AdminMarketingPage.module.css";
import { AdminMarketingShell } from "../AdminMarketingPage/AdminMarketingShell";
import styles from "./EventInfoPage.module.css";

const PAGE_SIZE = 8;

type EventSearchFilters = {
  readonly keyword: string;
  readonly region: string;
  readonly district: string;
  readonly lcls1: string;
  readonly lcls2: string;
  readonly lcls3: string;
};

function emptySearchFilters(): EventSearchFilters {
  return {
    keyword: "",
    region: "",
    district: "",
    lcls1: "",
    lcls2: "",
    lcls3: "",
  };
}

function hasSearchFilters(filters: EventSearchFilters): boolean {
  return Boolean(
    filters.keyword.trim() ||
      filters.region ||
      filters.district ||
      filters.lcls1 ||
      filters.lcls2 ||
      filters.lcls3,
  );
}

export function eventInfoDetailPath(item: Pick<FestivalEventItem, "source" | "contentId">): string {
  return `/event-info/${encodeURIComponent(item.source)}/${encodeURIComponent(item.contentId)}`;
}

function makeTourCodeItem(code: string, name: string): TourCodeItem {
  return {
    code,
    name,
    rnum: null,
    lDongRegnCd: code,
    lDongRegnNm: name,
    lDongSignguCd: null,
    lDongSignguNm: null,
    lclsSystm1Cd: null,
    lclsSystm1Nm: null,
    lclsSystm2Cd: null,
    lclsSystm2Nm: null,
    lclsSystm3Cd: null,
    lclsSystm3Nm: null,
  };
}

const FALLBACK_REGION_OPTIONS: readonly TourCodeItem[] = [
  makeTourCodeItem("11", "서울특별시"),
  makeTourCodeItem("26", "부산광역시"),
  makeTourCodeItem("27", "대구광역시"),
  makeTourCodeItem("28", "인천광역시"),
  makeTourCodeItem("29", "광주광역시"),
  makeTourCodeItem("30", "대전광역시"),
  makeTourCodeItem("31", "울산광역시"),
  makeTourCodeItem("36", "세종특별자치시"),
  makeTourCodeItem("41", "경기도"),
  makeTourCodeItem("43", "충청북도"),
  makeTourCodeItem("44", "충청남도"),
  makeTourCodeItem("46", "전라남도"),
  makeTourCodeItem("47", "경상북도"),
  makeTourCodeItem("48", "경상남도"),
  makeTourCodeItem("50", "제주특별자치도"),
  makeTourCodeItem("51", "강원특별자치도"),
  makeTourCodeItem("52", "전북특별자치도"),
];

const REGION_LABEL_BY_CODE = new Map(
  FALLBACK_REGION_OPTIONS.map(option => [(option.lDongRegnCd ?? option.code ?? "").trim(), option.lDongRegnNm ?? option.name ?? ""]),
);

function regionCodeOf(item: TourCodeItem): string {
  return (item.lDongRegnCd ?? item.code ?? "").trim();
}

function regionLabelOf(item: TourCodeItem): string {
  const code = regionCodeOf(item);
  return REGION_LABEL_BY_CODE.get(code) || (item.lDongRegnNm ?? item.name ?? code).trim() || code;
}

function codeKeyOf(item: TourCodeItem): string {
  return [
    item.code,
    item.lDongRegnCd,
    item.lDongSignguCd,
    item.lclsSystm1Cd,
    item.lclsSystm2Cd,
    item.lclsSystm3Cd,
  ].join("|");
}

function mergeCodeItems(previous: readonly TourCodeItem[], next: readonly TourCodeItem[]): readonly TourCodeItem[] {
  return Array.from(new Map([...previous, ...next].map(item => [codeKeyOf(item), item])).values());
}

function mergeRegionOptions(apiRows: readonly TourCodeItem[]): readonly TourCodeItem[] {
  const map = new Map<string, TourCodeItem>();
  for (const item of FALLBACK_REGION_OPTIONS) {
    map.set(regionCodeOf(item), item);
  }
  for (const item of apiRows) {
    const code = regionCodeOf(item);
    if (code && !map.has(code)) map.set(code, item);
  }
  return Array.from(map.values());
}

const WEEKDAY = ["일", "월", "화", "수", "목", "금", "토"];
const WEEKDAY_FULL = ["일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일"];

function dateToInput(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function toInputDate(yyyymmdd: string): string {
  if (!/^\d{8}$/.test(yyyymmdd)) return "";
  return `${yyyymmdd.slice(0, 4)}-${yyyymmdd.slice(4, 6)}-${yyyymmdd.slice(6, 8)}`;
}

function toApiDate(yyyyMmDd: string): string {
  return yyyyMmDd.replace(/-/g, "");
}

function formatPeriod(start: string | null, end: string | null): string {
  if (!start && !end) return "일정 정보 없음";
  const s = start && /^\d{8}$/.test(start) ? toInputDate(start) : start ?? "-";
  const e = end && /^\d{8}$/.test(end) ? toInputDate(end) : end ?? "-";
  return `${s} ~ ${e}`;
}

function mapQuery(event: FestivalEventItem): string | null {
  if (event.mapX != null && event.mapY != null) {
    return `${event.mapY},${event.mapX}`;
  }
  if (event.address) {
    return event.address;
  }
  return null;
}

function buildDateRail(centerDate: string): readonly string[] {
  const base = new Date(`${centerDate}T00:00:00`);
  return Array.from({ length: 10 }, (_, idx) => {
    const d = new Date(base);
    d.setDate(base.getDate() + idx - 1);
    return dateToInput(d);
  });
}

function CalendarGlyph() {
  return (
    <svg className={styles.calendarGlyph} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        fill="currentColor"
        d="M16.6 2a.6.6 0 1 0-1.2 0v1.4H8.6V2a.6.6 0 0 0-1.2 0v1.4H5A2.606 2.606 0 0 0 2.4 6v14c0 1.432 1.169 2.6 2.6 2.6h14c1.432 0 2.6-1.168 2.6-2.6V6c0-1.431-1.168-2.6-2.6-2.6h-2.4zm3.8 8.6H3.6V20c0 .769.632 1.4 1.4 1.4h14c.769 0 1.4-.631 1.4-1.4z"
        clipRule="evenodd"
        fillRule="evenodd"
      />
    </svg>
  );
}

type MonthCell = { readonly key: string; readonly day: number | null };

function buildMonthGrid(year: number, month1to12: number): readonly MonthCell[] {
  const first = new Date(year, month1to12 - 1, 1);
  const startPad = first.getDay();
  const daysInMonth = new Date(year, month1to12, 0).getDate();
  const cells: MonthCell[] = [];
  for (let i = 0; i < startPad; i += 1) {
    cells.push({ key: `p-${i}`, day: null });
  }
  for (let d = 1; d <= daysInMonth; d += 1) {
    cells.push({ key: `d-${d}`, day: d });
  }
  while (cells.length % 7 !== 0) {
    cells.push({ key: `t-${cells.length}`, day: null });
  }
  while (cells.length < 42) {
    cells.push({ key: `e-${cells.length}`, day: null });
  }
  return cells;
}

type MonthCalendarProps = {
  readonly year: number;
  readonly month: number;
  readonly selectedDateStr: string;
  readonly onPick: (yyyyMmDd: string) => void;
};

function MonthCalendar({ year, month, selectedDateStr, onPick }: MonthCalendarProps) {
  const cells = useMemo(() => buildMonthGrid(year, month), [year, month]);
  const selected = new Date(`${selectedDateStr}T12:00:00`);
  const isSelectedDay = (day: number | null) => {
    if (day == null) return false;
    const probe = new Date(year, month - 1, day);
    return (
      probe.getFullYear() === selected.getFullYear() &&
      probe.getMonth() === selected.getMonth() &&
      probe.getDate() === selected.getDate()
    );
  };

  return (
    <div className={styles.monthBlock}>
      <div className={styles.monthTitle}>
        {year}년 {month}월
      </div>
      <div className={styles.monthWeekdays}>
        {WEEKDAY.map(d => (
          <span key={d} className={styles.monthWeekday}>
            {d}
          </span>
        ))}
      </div>
      <div className={styles.monthGrid}>
        {cells.map(cell => {
          if (cell.day == null) {
            return <div key={cell.key} className={`${styles.monthCell} ${styles.monthCellEmpty}`} />;
          }
          const active = isSelectedDay(cell.day);
          const dt = new Date(year, month - 1, cell.day);
          const isSun = dt.getDay() === 0;
          return (
            <button
              key={cell.key}
              type="button"
              className={`${styles.monthCell} ${active ? styles.monthCellActive : ""} ${isSun ? styles.monthCellSun : ""}`}
              onClick={() => onPick(dateToInput(dt))}
            >
              {cell.day}
            </button>
          );
        })}
      </div>
    </div>
  );
}

export function EventInfoPage() {
  const { accessToken, authReady } = useAuth();
  const navigate = useNavigate();
  const [date, setDate] = useState(dateToInput(new Date()));
  const [keyword, setKeyword] = useState("");
  const [selectedRegion, setSelectedRegion] = useState("");
  const [selectedDistrict, setSelectedDistrict] = useState("");
  const [selectedLcls1, setSelectedLcls1] = useState("");
  const [selectedLcls2, setSelectedLcls2] = useState("");
  const [selectedLcls3, setSelectedLcls3] = useState("");
  const [appliedFilters, setAppliedFilters] = useState<EventSearchFilters>(() => emptySearchFilters());
  const [searchRevision, setSearchRevision] = useState(0);
  const [items, setItems] = useState<readonly FestivalEventItem[]>([]);
  const [pageNo, setPageNo] = useState(1);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [regions, setRegions] = useState<readonly TourCodeItem[]>([]);
  const [districts, setDistricts] = useState<readonly TourCodeItem[]>([]);
  const [districtLoading, setDistrictLoading] = useState(false);
  const [lclsAll, setLclsAll] = useState<readonly TourCodeItem[]>([]);
  const [calendarOpen, setCalendarOpen] = useState(false);
  const [advancedSearchOpen, setAdvancedSearchOpen] = useState(true);
  const calendarWrapRef = useRef<HTMLDivElement | null>(null);

  const requestDate = useMemo(() => toApiDate(date), [date]);
  const dateRail = useMemo(() => buildDateRail(date), [date]);
  const tourSearchMode = useMemo(() => hasSearchFilters(appliedFilters), [appliedFilters]);
  const queryKey = useMemo(
    () =>
      tourSearchMode
        ? `search|${searchRevision}|${appliedFilters.keyword.trim()}|${appliedFilters.region}|${appliedFilters.district}|${appliedFilters.lcls1}|${appliedFilters.lcls2}|${appliedFilters.lcls3}`
        : `date|${requestDate}`,
    [
      tourSearchMode,
      requestDate,
      searchRevision,
      appliedFilters,
    ],
  );

  useEffect(() => {
    setPageNo(1);
  }, [queryKey]);

  useEffect(() => {
    if (!authReady) return;
    if (!accessToken) {
      setError("로그인이 필요합니다.");
      setItems([]);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    void (async () => {
      try {
        const rows = tourSearchMode
          ? await searchFestivalByKeyword(accessToken, appliedFilters.keyword.trim(), {
              lDongRegnCd: appliedFilters.region || undefined,
              lDongSignguCd: appliedFilters.district || undefined,
              lclsSystm1: appliedFilters.lcls1 || undefined,
              lclsSystm2: appliedFilters.lcls2 || undefined,
              lclsSystm3: appliedFilters.lcls3 || undefined,
              pageNo,
              numOfRows: PAGE_SIZE,
            })
          : await fetchFestivalEventsByDate(accessToken, requestDate, {
              pageNo,
              numOfRows: PAGE_SIZE,
            });
        if (cancelled) return;
        setItems(prev => (pageNo === 1 ? rows : [...prev, ...rows]));
        setHasMore(rows.length >= PAGE_SIZE);
      } catch (e) {
        if (cancelled) return;
        if (pageNo === 1) setItems([]);
        setError(e instanceof Error ? e.message : "행사 정보를 불러오지 못했습니다.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [
    authReady,
    accessToken,
    tourSearchMode,
    requestDate,
    pageNo,
    appliedFilters,
  ]);

  useEffect(() => {
    if (!authReady || !accessToken) return;
    let cancelled = false;
    void (async () => {
      try {
        const [regionResult, lclsResult] = await Promise.allSettled([
          fetchLDongCodes(accessToken, undefined, "Y"),
          fetchLclsCodes(accessToken, { lclsSystmListYn: "Y" }),
        ]);
        if (cancelled) return;
        const regionRows = regionResult.status === "fulfilled" ? regionResult.value : [];
        const lclsRows = lclsResult.status === "fulfilled" ? lclsResult.value : [];
        setRegions(regionRows.filter(r => (r.lDongRegnCd ?? r.code ?? "").trim().length > 0));
        setLclsAll(lclsRows.filter(r => (r.lclsSystm1Cd ?? "").length > 0));
        setDistricts([]);
      } catch {
        if (!cancelled) {
          setRegions([]);
          setDistricts([]);
          setLclsAll([]);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [authReady, accessToken]);

  useEffect(() => {
    if (!authReady || !accessToken) return;
    if (!selectedRegion) {
      setDistricts([]);
      setSelectedDistrict("");
      setDistrictLoading(false);
      return;
    }
    let cancelled = false;
    setDistrictLoading(true);
    void (async () => {
      try {
        const districtRows = await fetchLDongCodes(accessToken, selectedRegion, "N");
        if (cancelled) return;
        const normalizedDistricts = districtRows.filter(r => (r.lDongSignguCd ?? r.code ?? "").trim().length > 0);
        const districtUnique = Array.from(
          new Map(
            normalizedDistricts.map(row => [
              `${row.lDongRegnCd ?? selectedRegion}-${row.lDongSignguCd ?? row.code ?? ""}`,
              row,
            ]),
          ).values(),
        );
        setDistricts(districtUnique);
        setSelectedDistrict(prev =>
          prev && districtUnique.some(d => ((d.lDongSignguCd ?? d.code ?? "").trim()) === prev) ? prev : "",
        );
      } catch {
        if (!cancelled) {
          setDistricts([]);
          setSelectedDistrict("");
        }
      } finally {
        if (!cancelled) setDistrictLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [authReady, accessToken, selectedRegion]);

  useEffect(() => {
    if (!authReady || !accessToken || !selectedLcls1) return;
    let cancelled = false;
    void (async () => {
      try {
        const rows = await fetchLclsCodes(accessToken, {
          lclsSystm1: selectedLcls1,
          lclsSystmListYn: "Y",
        });
        if (!cancelled) setLclsAll(prev => mergeCodeItems(prev, rows));
      } catch {
        // Keep already loaded category options.
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [authReady, accessToken, selectedLcls1]);

  useEffect(() => {
    if (!authReady || !accessToken || !selectedLcls1 || !selectedLcls2) return;
    let cancelled = false;
    void (async () => {
      try {
        const rows = await fetchLclsCodes(accessToken, {
          lclsSystm1: selectedLcls1,
          lclsSystm2: selectedLcls2,
          lclsSystmListYn: "Y",
        });
        if (!cancelled) setLclsAll(prev => mergeCodeItems(prev, rows));
      } catch {
        // Keep already loaded category options.
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [authReady, accessToken, selectedLcls1, selectedLcls2]);

  const regionOptions = useMemo(() => mergeRegionOptions(regions), [regions]);
  const lcls1Options = useMemo(
    () =>
      Array.from(new Map(lclsAll.map(r => [r.lclsSystm1Cd, r])).values()).filter(
        r => r.lclsSystm1Cd && r.lclsSystm1Nm,
      ),
    [lclsAll],
  );
  const lcls2Options = useMemo(
    () => {
      if (!selectedLcls1) return [];
      return (
      Array.from(
        new Map(
          lclsAll
            .filter(r => r.lclsSystm1Cd === selectedLcls1)
            .map(r => [r.lclsSystm2Cd, r]),
        ).values(),
      ).filter(
        r => r.lclsSystm2Cd && r.lclsSystm2Nm,
      )
      );
    },
    [lclsAll, selectedLcls1],
  );
  const lcls3Options = useMemo(
    () => {
      if (!selectedLcls1 || !selectedLcls2) return [];
      return (
      Array.from(
        new Map(
          lclsAll
            .filter(
              r =>
                r.lclsSystm1Cd === selectedLcls1 &&
                r.lclsSystm2Cd === selectedLcls2,
            )
            .map(r => [r.lclsSystm3Cd, r]),
        ).values(),
      ).filter(
        r => r.lclsSystm3Cd && r.lclsSystm3Nm,
      )
      );
    },
    [lclsAll, selectedLcls1, selectedLcls2],
  );
  const districtOptions = useMemo(() => {
    if (!selectedRegion) return [];
    return districts.filter(d => {
      const parent = (d.lDongRegnCd ?? "").trim();
      if (parent === selectedRegion) return true;
      if (!parent && (d.code ?? "").startsWith(selectedRegion)) return true;
      return false;
    });
  }, [districts, selectedRegion]);
  const selectedDate = useMemo(() => new Date(`${date}T00:00:00`), [date]);

  const openEventDetail = (item: FestivalEventItem, hash = "") => {
    navigate(`${eventInfoDetailPath(item)}${hash}`, { state: { item } });
  };

  const submitSearch = () => {
    setAppliedFilters({
      keyword: keyword.trim(),
      region: selectedRegion,
      district: selectedDistrict,
      lcls1: selectedLcls1,
      lcls2: selectedLcls2,
      lcls3: selectedLcls3,
    });
    setSearchRevision(v => v + 1);
  };

  const resetSearch = () => {
    setKeyword("");
    setSelectedRegion("");
    setSelectedDistrict("");
    setSelectedLcls1("");
    setSelectedLcls2("");
    setSelectedLcls3("");
    setAppliedFilters(emptySearchFilters());
    setSearchRevision(v => v + 1);
  };

  const calendarMonths = useMemo(() => {
    return [0, 1, 2, 3].map(offset => {
      const d = new Date(selectedDate.getFullYear(), selectedDate.getMonth() + offset, 1);
      return { year: d.getFullYear(), month: d.getMonth() + 1 };
    });
  }, [selectedDate]);

  useEffect(() => {
    if (!calendarOpen) return;
    const onDocMouseDown = (e: MouseEvent) => {
      const el = calendarWrapRef.current;
      if (!el || !(e.target instanceof Node) || el.contains(e.target)) return;
      setCalendarOpen(false);
    };
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        e.preventDefault();
        setCalendarOpen(false);
      }
    };
    document.addEventListener("mousedown", onDocMouseDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onDocMouseDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [calendarOpen]);

  return (
    <AdminMarketingShell
      currentPath="/event-info"
      title="Glow 행사정보"
      description="날짜별 행사와 키워드·지역·분류 상세검색"
      stats={[]}
    >
      <section className={`${shellStyles.panel} ${styles.panel}`}>
        <div className={shellStyles.panelBody}>
          <div className={styles.dayRailWrap}>
            <button
              type="button"
              className={styles.dayArrow}
              onClick={() => {
                const d = new Date(`${date}T00:00:00`);
                d.setDate(d.getDate() - 7);
                setDate(dateToInput(d));
              }}
              aria-label="이전 날짜"
            >
              ‹
            </button>
            <div className={styles.dayRail}>
              {dateRail.map(day => {
                const dt = new Date(`${day}T00:00:00`);
                const isActive = day === date;
                const isSun = dt.getDay() === 0;
                return (
                  <button
                    key={day}
                    type="button"
                    className={`${isActive ? styles.dayBtnActive : styles.dayBtn} ${isSun && !isActive ? styles.dayBtnSun : ""}`}
                    onClick={() => setDate(day)}
                  >
                    <span>{WEEKDAY[dt.getDay()]}</span>
                    <strong>{dt.getDate()}</strong>
                  </button>
                );
              })}
            </div>
            <button
              type="button"
              className={styles.dayArrow}
              onClick={() => {
                const d = new Date(`${date}T00:00:00`);
                d.setDate(d.getDate() + 7);
                setDate(dateToInput(d));
              }}
              aria-label="다음 날짜"
            >
              ›
            </button>
            <div className={styles.calendarField} ref={calendarWrapRef}>
              <button
                type="button"
                className={styles.calendarTrigger}
                aria-label="달력에서 날짜 선택"
                aria-expanded={calendarOpen}
                aria-haspopup="dialog"
                onClick={() => setCalendarOpen(v => !v)}
              >
                <CalendarGlyph />
              </button>
              {calendarOpen ? (
                <div className={styles.calendarPopover} role="dialog" aria-label="날짜 선택 달력">
                  <div className={styles.monthRow}>
                    {calendarMonths.map(({ year: y, month: m }) => (
                      <MonthCalendar
                        key={`${y}-${m}`}
                        year={y}
                        month={m}
                        selectedDateStr={date}
                        onPick={next => {
                          setDate(next);
                          setCalendarOpen(false);
                        }}
                      />
                    ))}
                  </div>
                </div>
              ) : null}
            </div>
          </div>

          <details
            className={styles.filterBox}
            open={advancedSearchOpen}
            onToggle={e => setAdvancedSearchOpen((e.currentTarget as HTMLDetailsElement).open)}
          >
            <summary>상세검색</summary>
            <div className={styles.filterGrid}>
              <input className={styles.searchInput} placeholder="키워드 (선택)" value={keyword} onChange={e => setKeyword(e.target.value)} />
              <select
                className={styles.searchSelect}
                value={selectedRegion}
                onChange={e => {
                  setSelectedRegion(e.target.value);
                  setSelectedDistrict("");
                }}
              >
                <option value="">지역 전체</option>
                {regionOptions.map(r => {
                  const v = (r.lDongRegnCd ?? r.code ?? "").trim();
                  const label = regionLabelOf(r);
                  return (
                    <option key={`${v}-${label}`} value={v}>
                      {label}
                    </option>
                  );
                })}
              </select>
              <select
                className={styles.searchSelect}
                value={selectedDistrict}
                onChange={e => setSelectedDistrict(e.target.value)}
                disabled={!selectedRegion || districtLoading}
              >
                <option value="">시군구 전체</option>
                {districtOptions.map(d => {
                  const v = (d.lDongSignguCd ?? d.code ?? "").trim();
                  const label = (d.lDongSignguNm ?? d.name ?? v).trim() || v;
                  return (
                    <option key={`${d.lDongRegnCd ?? ""}-${v}-${label}`} value={v}>
                      {label}
                    </option>
                  );
                })}
              </select>
              <select
                className={styles.searchSelect}
                value={selectedLcls1}
                onChange={e => {
                  setSelectedLcls1(e.target.value);
                  setSelectedLcls2("");
                  setSelectedLcls3("");
                }}
              >
                <option value="">대분류</option>
                {lcls1Options.map(c => (
                  <option key={c.lclsSystm1Cd} value={c.lclsSystm1Cd ?? ""}>
                    {c.lclsSystm1Nm}
                  </option>
                ))}
              </select>
              <select
                className={styles.searchSelect}
                value={selectedLcls2}
                onChange={e => {
                  setSelectedLcls2(e.target.value);
                  setSelectedLcls3("");
                }}
                disabled={!selectedLcls1}
              >
                <option value="">중분류</option>
                {lcls2Options.map(c => (
                  <option key={c.lclsSystm2Cd} value={c.lclsSystm2Cd ?? ""}>
                    {c.lclsSystm2Nm}
                  </option>
                ))}
              </select>
              <select className={styles.searchSelect} value={selectedLcls3} onChange={e => setSelectedLcls3(e.target.value)} disabled={!selectedLcls2}>
                <option value="">소분류</option>
                {lcls3Options.map(c => (
                  <option key={c.lclsSystm3Cd} value={c.lclsSystm3Cd ?? ""}>
                    {c.lclsSystm3Nm}
                  </option>
                ))}
              </select>
            </div>
            <div className={styles.filterActions}>
              <button
                type="button"
                className={styles.filterResetBtn}
                aria-label="검색"
                onClick={submitSearch}
              >
                조건 초기화
              </button>
              {tourSearchMode ? <span className={styles.filterModeBadge}>검색 모드</span> : null}
              {tourSearchMode ? (
                <button type="button" className={styles.filterModeBadge} onClick={resetSearch}>
                  초기화
                </button>
              ) : null}
            </div>
          </details>

          <p className={styles.currentDate}>
            {date} ({WEEKDAY_FULL[selectedDate.getDay()]})
            {tourSearchMode ? <span className={styles.searchModeNote}> · 상단 날짜와 무관한 검색 결과입니다</span> : null}
          </p>
          {error ? <p className={styles.error}>{error}</p> : null}
          {loading && pageNo === 1 ? <p className={styles.status}>행사 정보를 불러오는 중…</p> : null}

          <ul className={styles.cardGrid}>
            {items.map(item => {
              const image = item.thumbnailImageUrl || item.imageUrl;
              const detailPath = eventInfoDetailPath(item);
              const hasMapTarget = Boolean(mapQuery(item));
              return (
                <li key={`${item.source}-${item.contentId}`} className={styles.eventCard}>
                  <button type="button" className={styles.cardButton} onClick={() => openEventDetail(item)}>
                    <div className={styles.cardText}>
                      <h3>{item.title}</h3>
                      <p>{formatPeriod(item.eventStartDate, item.eventEndDate)}</p>
                      <p className={styles.place}>📍 {item.address || "장소 정보 없음"}</p>
                    </div>
                    <div className={styles.cardThumb}>
                      {image ? <img src={image} alt={item.title} loading="lazy" decoding="async" /> : <div className={styles.thumbFallback}>NO IMAGE</div>}
                    </div>
                  </button>
                  <div className={styles.cardLinks}>
                    <button type="button" onClick={() => navigate(detailPath, { state: { item } })}>
                      상세페이지
                    </button>
                    {hasMapTarget ? (
                      <button type="button" onClick={() => openEventDetail(item, "#store-info")} aria-label={`${item.title} 지도 보기`}>
                        지도
                      </button>
                    ) : null}
                  </div>
                </li>
              );
            })}
          </ul>

          {!loading && items.length === 0 ? (
            <p className={styles.status}>
              {tourSearchMode ? "조건에 맞는 행사가 없습니다. 키워드나 지역을 바꿔 보세요." : "선택한 날짜에 진행 중인 행사가 없습니다."}
            </p>
          ) : null}
          {hasMore ? (
            <div className={styles.moreWrap}>
              <button type="button" className={styles.moreButton} onClick={() => setPageNo(v => v + 1)} disabled={loading}>
                {loading ? "불러오는 중…" : "더 보기"}
              </button>
            </div>
          ) : null}
        </div>
      </section>

    </AdminMarketingShell>
  );
}

export default EventInfoPage;
