import { useEffect, useMemo, useState, type CSSProperties } from "react";
import { useAuth } from "../../../auth/AuthContext";
import {
  fetchAirportCrowdCriteria,
  fetchArrivalsCongestion,
  fetchDepartureCongestion,
  fetchParkingCongestion,
  fetchPassengerForecast,
  type AirportCrowdCriteriaItem,
  type AirportCrowdLevel,
  type ArrivalCongestionItem,
  type DepartureCongestionItem,
  type ParkingCongestionItem,
  type PassengerForecastBundle,
  type PassengerForecastItem,
} from "../../../shared/data/airportCrowdApi";
import { AdminMarketingShell } from "../AdminMarketingPage/AdminMarketingShell";
import styles from "./AirportCrowdPage.module.css";

type CrowdLevel = AirportCrowdLevel;
type ZoneType = "departure" | "arrival" | "parking";
type DayType = "today" | "tomorrow";
type TerminalType = "T1" | "T2";

type GateStatus = {
  readonly id: string;
  readonly name: string;
  readonly area: string;
  readonly metricValue: number;
  readonly metricLabel: string;
  readonly level: CrowdLevel;
  readonly tip: string;
};

type HourStatus = {
  readonly hour: string;
  readonly value: number;
  readonly count: number;
  readonly level: CrowdLevel;
};

const CROWD_META: Record<CrowdLevel, { readonly label: string; readonly en: string; readonly color: string }> = {
  smooth: { label: "정상", en: "Smooth", color: "#0ac84c" },
  moderate: { label: "다소 붐빔", en: "Moderate", color: "#fdd83f" },
  busy: { label: "붐빔", en: "Busy", color: "#ff8a00" },
  heavy: { label: "매우 혼잡", en: "Very busy", color: "#f24548" },
  unknown: { label: "확인중", en: "Checking", color: "#d1d5db" },
};

const TERMINAL_ID: Record<TerminalType, string> = {
  T1: "P01",
  T2: "P03",
};

const GATE_AREA: Record<TerminalType, Record<string, string>> = {
  T1: {
    "1": "A/B",
    "2": "C",
    "3": "D",
    "4": "E",
    "5": "F",
    "6": "G",
  },
  T2: {
    "1": "A/B/C/D",
    "2": "E/F/G/H",
  },
};

const FALLBACK_GATES: Record<TerminalType, Record<ZoneType, readonly GateStatus[]>> = {
  T1: {
    departure: [
      { id: "1", name: "출국장 1", area: "A/B", metricValue: 12, metricLabel: "12분 대기", level: "smooth", tip: "서편보다 빠르게 이동할 수 있어요." },
      { id: "2", name: "출국장 2", area: "C", metricValue: 24, metricLabel: "24분 대기", level: "moderate", tip: "일반적인 대기 수준입니다." },
      { id: "3", name: "출국장 3", area: "D", metricValue: 34, metricLabel: "34분 대기", level: "busy", tip: "탑승 시간이 임박했다면 다른 출국장을 확인하세요." },
      { id: "4", name: "출국장 4", area: "E", metricValue: 46, metricLabel: "46분 대기", level: "heavy", tip: "가능하면 출국장 1 또는 2로 이동하세요." },
      { id: "5", name: "출국장 5", area: "F", metricValue: 28, metricLabel: "28분 대기", level: "moderate", tip: "가족 여행객이 몰리는 시간대입니다." },
      { id: "6", name: "출국장 6", area: "G", metricValue: 0, metricLabel: "확인중", level: "unknown", tip: "운영 여부를 현장에서 확인하세요." },
    ],
    arrival: [
      { id: "A", name: "입국장 A", area: "동편", metricValue: 180, metricLabel: "180명", level: "moderate", tip: "수하물 수취 후 이동 동선이 짧아요." },
      { id: "B", name: "입국장 B", area: "동편", metricValue: 438, metricLabel: "438명", level: "busy", tip: "도착편이 겹치는 시간대입니다." },
      { id: "C", name: "입국심사 C", area: "중앙", metricValue: 621, metricLabel: "621명", level: "heavy", tip: "외국인 심사 대기 시간이 길 수 있어요." },
      { id: "D", name: "입국심사 D", area: "중앙", metricValue: 242, metricLabel: "242명", level: "moderate", tip: "비교적 안정적인 흐름입니다." },
    ],
    parking: [
      { id: "P1", name: "장기 P1", area: "장기", metricValue: 61, metricLabel: "61%", level: "moderate", tip: "여유 구역이 아직 남아 있어요." },
      { id: "P2", name: "장기 P2", area: "장기", metricValue: 42, metricLabel: "42%", level: "smooth", tip: "현재 가장 여유로운 구역입니다." },
      { id: "P3", name: "장기 P3", area: "장기", metricValue: 82, metricLabel: "82%", level: "busy", tip: "진입 전 대체 주차장을 확인하세요." },
      { id: "단기", name: "단기주차장", area: "단기", metricValue: 92, metricLabel: "92%", level: "heavy", tip: "단기주차장 이용객이 많습니다." },
    ],
  },
  T2: {
    departure: [
      { id: "1", name: "출국장 1", area: "A/B/C/D", metricValue: 22, metricLabel: "22분 대기", level: "moderate", tip: "오전 시간대는 안정적입니다." },
      { id: "2", name: "출국장 2", area: "E/F/G/H", metricValue: 37, metricLabel: "37분 대기", level: "busy", tip: "동편보다 대기열이 길 수 있어요." },
    ],
    arrival: [
      { id: "1", name: "입국장 1", area: "동편", metricValue: 166, metricLabel: "166명", level: "smooth", tip: "현재 입국 흐름이 좋아요." },
      { id: "2", name: "입국장 2", area: "서편", metricValue: 394, metricLabel: "394명", level: "moderate", tip: "도착편 집중 시간에 주의하세요." },
    ],
    parking: [
      { id: "단기", name: "단기주차장", area: "단기", metricValue: 70, metricLabel: "70%", level: "moderate", tip: "아직 주차 여유가 있습니다." },
      { id: "장기", name: "장기주차장", area: "장기", metricValue: 84, metricLabel: "84%", level: "busy", tip: "장기주차장 진입 전 셔틀 시간을 확인하세요." },
    ],
  },
};

const FALLBACK_FORECAST: Record<DayType, readonly HourStatus[]> = {
  today: [
    { hour: "09", value: 34, count: 820, level: "smooth" },
    { hour: "10", value: 46, count: 1220, level: "moderate" },
    { hour: "11", value: 62, count: 2860, level: "busy" },
    { hour: "12", value: 74, count: 3420, level: "busy" },
    { hour: "13", value: 88, count: 4620, level: "heavy" },
    { hour: "14", value: 81, count: 4110, level: "heavy" },
    { hour: "15", value: 68, count: 3100, level: "busy" },
    { hour: "16", value: 48, count: 1860, level: "moderate" },
  ],
  tomorrow: [
    { hour: "09", value: 28, count: 710, level: "smooth" },
    { hour: "10", value: 39, count: 1040, level: "moderate" },
    { hour: "11", value: 51, count: 1710, level: "moderate" },
    { hour: "12", value: 66, count: 2780, level: "busy" },
    { hour: "13", value: 72, count: 3240, level: "busy" },
    { hour: "14", value: 59, count: 2210, level: "moderate" },
    { hour: "15", value: 44, count: 1510, level: "moderate" },
    { hour: "16", value: 31, count: 940, level: "smooth" },
  ],
};

const zoneLabels: Record<ZoneType, string> = {
  departure: "출발 구역",
  arrival: "입국 구역",
  parking: "주차장",
};

const dayLabels: Record<DayType, string> = {
  today: "오늘",
  tomorrow: "내일",
};

const levelOrder: Record<CrowdLevel, number> = {
  unknown: 0,
  smooth: 1,
  moderate: 2,
  busy: 3,
  heavy: 4,
};

function normalizeLevel(level: string | null | undefined): CrowdLevel {
  if (level === "smooth" || level === "moderate" || level === "busy" || level === "heavy") return level;
  return "unknown";
}

function crowdLevelByValue(value: number): CrowdLevel {
  if (value < 1000) return "smooth";
  if (value < 2500) return "moderate";
  if (value < 4000) return "busy";
  return "heavy";
}

function worstLevel(gates: readonly GateStatus[]): CrowdLevel {
  return gates.reduce<CrowdLevel>((worst, gate) => (levelOrder[gate.level] > levelOrder[worst] ? gate.level : worst), "unknown");
}

function bestGate(gates: readonly GateStatus[]): GateStatus | null {
  const active = gates.filter(gate => gate.level !== "unknown");
  if (active.length === 0) return null;
  return [...active].sort((a, b) => a.metricValue - b.metricValue)[0];
}

function parseDepartureGateNumber(gateId: string): string {
  return /DG(\d+)/i.exec(gateId)?.[1] ?? /\d+/.exec(gateId)?.[0] ?? gateId;
}

function toDepartureGates(rows: readonly DepartureCongestionItem[], terminal: TerminalType): readonly GateStatus[] {
  if (rows.length === 0) return FALLBACK_GATES[terminal].departure;

  const grouped = new Map<string, DepartureCongestionItem[]>();
  for (const row of rows) {
    const id = parseDepartureGateNumber(row.gateId);
    grouped.set(id, [...(grouped.get(id) ?? []), row]);
  }

  return Array.from(grouped.entries())
    .sort(([a], [b]) => a.localeCompare(b, "ko", { numeric: true }))
    .map(([id, items]) => {
      const waitTime = Math.max(...items.map(item => item.waitTimeMinutes ?? 0));
      const waitLength = Math.max(...items.map(item => item.waitLength ?? 0));
      const value = waitTime || waitLength;
      const level = items.reduce<CrowdLevel>((worst, item) => {
        const current = normalizeLevel(item.level);
        return levelOrder[current] > levelOrder[worst] ? current : worst;
      }, "unknown");
      return {
        id,
        name: `출국장 ${id}`,
        area: GATE_AREA[terminal][id] ?? items.map(item => item.gateId).join(", "),
        metricValue: value,
        metricLabel: waitTime ? `${waitTime}분 대기` : waitLength ? `${waitLength}m 대기열` : "확인중",
        level,
        tip: level === "heavy" ? "가능하면 다른 출국장을 확인하세요." : "현재 공공데이터 기준으로 산정한 상태입니다.",
      };
    });
}

function toArrivalGates(rows: readonly ArrivalCongestionItem[], terminal: TerminalType): readonly GateStatus[] {
  if (rows.length === 0) return FALLBACK_GATES[terminal].arrival;

  const grouped = new Map<string, ArrivalCongestionItem[]>();
  for (const row of rows) {
    const id = row.entryGate || row.gateNumber || row.airport || "입국";
    grouped.set(id, [...(grouped.get(id) ?? []), row]);
  }

  return Array.from(grouped.entries())
    .sort(([a], [b]) => a.localeCompare(b, "ko", { numeric: true }))
    .map(([id, items]) => {
      const total = items.reduce((sum, item) => sum + item.totalFlow, 0);
      const korean = items.reduce((sum, item) => sum + (item.korean ?? 0), 0);
      const foreigner = items.reduce((sum, item) => sum + (item.foreigner ?? 0), 0);
      const level = items.reduce<CrowdLevel>((worst, item) => {
        const current = normalizeLevel(item.level);
        return levelOrder[current] > levelOrder[worst] ? current : worst;
      }, "unknown");
      const airports = Array.from(new Set(items.map(item => item.airport).filter(Boolean))).join(", ");
      return {
        id,
        name: `입국장 ${id}`,
        area: airports || `${items.length}개 도착편`,
        metricValue: total,
        metricLabel: `${total.toLocaleString()}명`,
        level,
        tip: `내국인 ${korean.toLocaleString()}명 · 외국인 ${foreigner.toLocaleString()}명`,
      };
    });
}

function toParkingGates(rows: readonly ParkingCongestionItem[], terminal: TerminalType): readonly GateStatus[] {
  if (rows.length === 0) return FALLBACK_GATES[terminal].parking;

  return rows.map(row => {
    const occupancy = row.occupancyRate ?? 0;
    return {
      id: row.floor.replace(/^T[12]/i, "").trim() || row.floor,
      name: row.floor,
      area: row.terminal ?? terminal,
      metricValue: occupancy || row.parking || 0,
      metricLabel: row.occupancyRate != null
        ? `${row.occupancyRate}%`
        : row.parking != null && row.parkingArea != null
          ? `${row.parking.toLocaleString()}/${row.parkingArea.toLocaleString()}대`
          : "확인중",
      level: normalizeLevel(row.level),
      tip: row.available != null ? `여유 ${row.available.toLocaleString()}면` : "주차 가능 대수를 확인 중입니다.",
    };
  });
}

function forecastMetric(item: PassengerForecastItem, terminal: TerminalType, zone: ZoneType): number {
  if (zone === "arrival") {
    return terminal === "T1" ? item.terminal1ArrivalTotal ?? 0 : item.terminal2ArrivalTotal ?? 0;
  }
  if (terminal === "T1") return item.terminal1DepartureTotal ?? 0;
  return item.terminal2DepartureTotal ?? 0;
}

function toHourLabel(slot: string): string {
  const digits = slot.match(/\d{1,2}/)?.[0];
  return digits ? digits.padStart(2, "0") : slot;
}

function toForecastBars(
  bundle: PassengerForecastBundle | null,
  day: DayType,
  terminal: TerminalType,
  zone: ZoneType,
): readonly HourStatus[] {
  const rows = bundle?.[day] ?? [];
  if (rows.length === 0) return FALLBACK_FORECAST[day];
  const raw = rows.map(item => ({
    hour: toHourLabel(item.timeSlot),
    count: forecastMetric(item, terminal, zone),
  }));
  const max = Math.max(...raw.map(item => item.count), 1);
  return raw.map(item => ({
    hour: item.hour,
    count: item.count,
    value: Math.max(6, Math.round((item.count / max) * 100)),
    level: crowdLevelByValue(item.count),
  }));
}

function latestOf(...values: Array<string | null | undefined>): string | null {
  const sorted = values.filter(Boolean).sort();
  return sorted.length > 0 ? sorted[sorted.length - 1] ?? null : null;
}

function AirportTerminalMap({ gates, terminal, zone }: { readonly gates: readonly GateStatus[]; readonly terminal: TerminalType; readonly zone: ZoneType }) {
  return (
    <div className={styles.terminalMap} aria-label={`${terminal} ${zoneLabels[zone]} 혼잡도 지도`}>
      <div className={styles.mapTerminalLabel}>
        {terminal} {zoneLabels[zone]}
      </div>
      <div className={styles.mapSpine} />
      <div className={styles.mapGateGrid}>
        {gates.map(gate => (
          <div key={gate.id} className={styles.mapGate} style={{ "--level-color": CROWD_META[gate.level].color } as CSSProperties}>
            <span className={styles.mapGateNumber}>{gate.id}</span>
            <span className={styles.mapGateName}>{gate.area}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

export function AirportCrowdPage() {
  const { accessToken, authReady } = useAuth();
  const [zone, setZone] = useState<ZoneType>("departure");
  const [day, setDay] = useState<DayType>("today");
  const [terminal, setTerminal] = useState<TerminalType>("T1");
  const [departureRows, setDepartureRows] = useState<readonly DepartureCongestionItem[]>([]);
  const [arrivalRows, setArrivalRows] = useState<readonly ArrivalCongestionItem[]>([]);
  const [parkingRows, setParkingRows] = useState<readonly ParkingCongestionItem[]>([]);
  const [forecastBundle, setForecastBundle] = useState<PassengerForecastBundle | null>(null);
  const [criteriaRows, setCriteriaRows] = useState<readonly AirportCrowdCriteriaItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdatedAt, setLastUpdatedAt] = useState<string | null>(null);

  useEffect(() => {
    if (!authReady) return;
    if (!accessToken) {
      setError("로그인이 필요합니다.");
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    void (async () => {
      try {
        const selectedDate = day === "today" ? 0 : 1;
        const forecastPromise = fetchPassengerForecast(accessToken, selectedDate);

        if (zone === "departure") {
          const [forecast, departures] = await Promise.all([
            forecastPromise,
            fetchDepartureCongestion(accessToken, { terminalId: TERMINAL_ID[terminal] }),
          ]);
          if (cancelled) return;
          setForecastBundle(forecast);
          setDepartureRows(departures);
          setArrivalRows([]);
          setParkingRows([]);
          setLastUpdatedAt(latestOf(...departures.map(item => item.occurredAt)));
        } else if (zone === "arrival") {
          const [forecast, arrivals] = await Promise.all([
            forecastPromise,
            fetchArrivalsCongestion(accessToken, { terminal }),
          ]);
          if (cancelled) return;
          setForecastBundle(forecast);
          setArrivalRows(arrivals);
          setDepartureRows([]);
          setParkingRows([]);
          setLastUpdatedAt(latestOf(...arrivals.map(item => item.estimatedTime ?? item.scheduleTime)));
        } else {
          const [forecast, parking] = await Promise.all([
            forecastPromise,
            fetchParkingCongestion(accessToken, { terminal }),
          ]);
          if (cancelled) return;
          setForecastBundle(forecast);
          setParkingRows(parking);
          setDepartureRows([]);
          setArrivalRows([]);
          setLastUpdatedAt(latestOf(...parking.map(item => item.observedAt)));
        }
      } catch (e) {
        if (cancelled) return;
        setError(e instanceof Error ? e.message : "공항 혼잡 정보를 불러오지 못했습니다.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, day, terminal, zone]);

  useEffect(() => {
    if (!authReady || !accessToken) {
      setCriteriaRows([]);
      return;
    }

    let cancelled = false;

    void fetchAirportCrowdCriteria(accessToken, zone)
      .then(rows => {
        if (!cancelled) setCriteriaRows(rows);
      })
      .catch(() => {
        if (!cancelled) setCriteriaRows([]);
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, zone]);

  const gates = useMemo(() => {
    if (zone === "departure") return toDepartureGates(departureRows, terminal);
    if (zone === "arrival") return toArrivalGates(arrivalRows, terminal);
    return toParkingGates(parkingRows, terminal);
  }, [arrivalRows, departureRows, parkingRows, terminal, zone]);

  const forecast = useMemo(() => toForecastBars(forecastBundle, day, terminal, zone), [day, forecastBundle, terminal, zone]);
  const peakLevel = worstLevel(gates);
  const recommendedGate = bestGate(gates);
  const peakMeta = CROWD_META[peakLevel];
  const hasLiveRows =
    (zone === "departure" && departureRows.length > 0) ||
    (zone === "arrival" && arrivalRows.length > 0) ||
    (zone === "parking" && parkingRows.length > 0);
  const isFallbackState = !loading && !error && !hasLiveRows;

  const metricSummary = useMemo(() => {
    if (zone === "departure") {
      const maxWait = Math.max(...gates.map(gate => gate.metricValue), 0);
      return maxWait ? `최대 ${maxWait}분` : "확인중";
    }
    if (zone === "parking") {
      const active = gates.filter(gate => gate.metricValue > 0);
      if (active.length === 0) return "확인중";
      const avg = Math.round(active.reduce((sum, gate) => sum + gate.metricValue, 0) / active.length);
      return `평균 ${avg}%`;
    }
    const total = gates.reduce((sum, gate) => sum + gate.metricValue, 0);
    return `${total.toLocaleString()}명`;
  }, [gates, zone]);

  const actionMessage = useMemo(() => {
    if (zone === "parking") {
      return recommendedGate
        ? `${recommendedGate.name}이 가장 여유로워요. 렌터카나 픽업 차량은 진입 전 주차 구역을 먼저 확인하세요.`
        : "주차장 운영 상태를 확인 중입니다.";
    }
    if (zone === "arrival") {
      return "입국장은 항공편 도착이 겹치는 시간에 빠르게 붐빌 수 있어요. 픽업 예정이라면 20~30분 여유를 두세요.";
    }
    return recommendedGate
      ? `지금은 ${recommendedGate.name} 이용을 추천해요. 출국까지 2시간 이내라면 바로 보안검색대로 이동하세요.`
      : "출국장 운영 상태를 확인 중입니다.";
  }, [recommendedGate, zone]);

  return (
    <AdminMarketingShell
      currentPath="/airport-crowd"
      title="Glow 공항인파"
      description=""
      classNames={{
        main: styles.shellMain,
        toolbarCard: styles.shellToolbarCard,
        statCard: styles.shellStatCard,
      }}
      stats={[
        { label: "현재 상태", value: peakMeta.label },
        { label: zone === "parking" ? "주차 점유" : "혼잡 수치", value: metricSummary },
        { label: "추천 구역", value: recommendedGate?.name ?? "확인 중" },
      ]}
    >
      <section className={styles.airportPage} aria-label="공항 혼잡 상태">
        <div className={styles.heroPanel}>
          <div className={styles.heroCopy}>
            <span className={styles.eyebrow}>Airport Crowd Board</span>
            <h2>공항 혼잡 상태 확인해 보세요</h2>
            <p>시간대별 인천공항 혼잡 정보를 확인하고, 지금 가장 덜 붐비는 구역을 빠르게 찾아보세요.</p>
          </div>
          <div className={styles.heroStatus} style={{ "--level-color": peakMeta.color } as CSSProperties}>
            <span className={styles.statusDot} />
            <strong>{peakMeta.label}</strong>
            <span>{peakMeta.en}</span>
          </div>
        </div>

        <div className={styles.filterBar} aria-label="공항 혼잡 필터">
          <label>
            <span>구역</span>
            <select value={zone} onChange={e => setZone(e.target.value as ZoneType)}>
              <option value="departure">출발 구역</option>
              <option value="arrival">입국 구역</option>
              <option value="parking">주차장</option>
            </select>
          </label>
          <label>
            <span>날짜</span>
            <select value={day} onChange={e => setDay(e.target.value as DayType)}>
              <option value="today">오늘</option>
              <option value="tomorrow">내일</option>
            </select>
          </label>
          <div className={styles.terminalTabs} role="tablist" aria-label="터미널 선택">
            {(["T1", "T2"] as const).map(item => (
              <button
                key={item}
                type="button"
                className={item === terminal ? styles.terminalTabActive : styles.terminalTab}
                onClick={() => setTerminal(item)}
                role="tab"
                aria-selected={item === terminal}
              >
                여객 터미널 {item.slice(1)}
              </button>
            ))}
          </div>
        </div>

        {loading ? (
          <p className={styles.dataState} aria-live="polite">
            공항 혼잡 정보를 불러오는 중입니다.
          </p>
        ) : null}
        {isFallbackState ? (
          <p className={styles.dataState} aria-live="polite">
            API 응답이 비어 있어 예시 데이터를 표시 중입니다.
          </p>
        ) : null}
        {error ? <p className={styles.errorText}>{error}</p> : null}

        <div className={styles.dashboardGrid}>
          <section className={styles.mainCard}>
            <div className={styles.cardHeader}>
              <div>
                <span className={styles.sectionKicker}>Current</span>
                <h3>
                  {terminal} {zoneLabels[zone]} 혼잡도
                </h3>
              </div>
              <span className={styles.updatedAt}>{lastUpdatedAt ?? "실시간"} 기준</span>
            </div>
            <AirportTerminalMap gates={gates} terminal={terminal} zone={zone} />
            <div className={styles.legend}>
              {(["smooth", "moderate", "busy", "heavy", "unknown"] as const).map(level => (
                <span key={level}>
                  <i style={{ backgroundColor: CROWD_META[level].color }} />
                  {CROWD_META[level].label}
                </span>
              ))}
            </div>
          </section>
        </div>

        <div className={styles.contentGrid}>
          <section className={styles.gatePanel}>
            <div className={styles.cardHeader}>
              <div>
                <span className={styles.sectionKicker}>Gate Status</span>
                <h3>구역별 상태</h3>
              </div>
            </div>
            <div className={styles.gateList}>
              {gates.map(gate => (
                <article key={`${gate.name}-${gate.id}`} className={styles.gateRow} style={{ "--level-color": CROWD_META[gate.level].color } as CSSProperties}>
                  <div>
                    <strong>{gate.name}</strong>
                    <span>{gate.area} · {gate.tip}</span>
                  </div>
                  <div className={styles.gateMeter}>
                    <span style={{ width: `${Math.min(100, Math.max(8, gate.metricValue))}%` }} />
                  </div>
                  <div className={styles.gateValue}>
                    <strong>{gate.metricLabel}</strong>
                    <span>{CROWD_META[gate.level].label}</span>
                  </div>
                </article>
              ))}
            </div>
          </section>
          <aside className={styles.actionCard} aria-label="여행자 행동 가이드">
            <span className={styles.sectionKicker}>Traveler Guide</span>
            <h3>지금 이렇게 움직이면 좋아요</h3>
            <p>{actionMessage}</p>
            <div className={styles.guideBox}>
              <strong>For visitors</strong>
              <span>Arrive 3 hours before departure when the airport is busy.</span>
            </div>
          </aside>
        </div>

        <section className={styles.forecastPanel}>
          <div className={styles.cardHeader}>
            <div>
              <span className={styles.sectionKicker}>Forecast</span>
              <h3>{dayLabels[day]} 시간대별 예상 혼잡</h3>
            </div>
          </div>
          <div className={styles.forecastBars}>
            {forecast.map(item => (
              <div key={`${item.hour}-${item.count}`} className={styles.forecastItem}>
                <div className={styles.forecastBar}>
                  <span style={{ height: `${item.value}%`, backgroundColor: CROWD_META[item.level].color }} />
                </div>
                <strong>{item.hour}시</strong>
                <em>{item.count.toLocaleString()}명</em>
              </div>
            ))}
          </div>
        </section>

        <details className={styles.criteriaCard}>
          <summary>혼잡 기준</summary>
          {criteriaRows.length > 0 ? (
            <div className={styles.criteriaList}>
              {criteriaRows.map(item => (
                <article
                  key={item.level}
                  className={styles.criteriaItem}
                  style={{ "--criteria-color": item.color } as CSSProperties}
                >
                  <strong>{item.title}</strong>
                  <span>{item.description}</span>
                </article>
              ))}
            </div>
          ) : (
            <span className={styles.criteriaEmpty}>혼잡 기준을 불러오는 중입니다.</span>
          )}
        </details>

        <a className={styles.serviceBanner} href="/exchange-rate">
          <div>
            <strong>공항에서 바로 필요한 여행 도구도 함께 확인하세요</strong>
            <span>환율, 교통, 행사 정보를 Glow에서 한 번에 볼 수 있어요.</span>
          </div>
          <span className={styles.busIcon} aria-hidden="true">
            <img draggable={false} src="/airport-traffic-info/bus.svg" alt="" />
          </span>
        </a>
      </section>
    </AdminMarketingShell>
  );
}

export default AirportCrowdPage;
