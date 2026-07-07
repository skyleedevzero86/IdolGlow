import { getApiBaseUrl } from "../../auth/authConfig";
import { acceptLanguageHeader } from "../../ui/i18n/uiLangStorage";

export type AirportCrowdLevel = "smooth" | "moderate" | "busy" | "heavy" | "unknown";

export type AirportCrowdZone = "departure" | "arrival" | "parking";

export type AirportCrowdCriteriaItem = {
  readonly level: AirportCrowdLevel;
  readonly levelLabel: string;
  readonly title: string;
  readonly description: string;
  readonly color: string;
};

export type DepartureCongestionItem = {
  readonly gateId: string;
  readonly terminalId: string;
  readonly waitTimeMinutes: number | null;
  readonly waitLength: number | null;
  readonly occurredAt: string | null;
  readonly operatingTime: string | null;
  readonly level: AirportCrowdLevel;
  readonly levelLabel: string;
};

export type PassengerForecastItem = {
  readonly date: string | null;
  readonly timeSlot: string;
  readonly terminal1DepartureTotal: number | null;
  readonly terminal2DepartureTotal: number | null;
  readonly terminal1ArrivalTotal: number | null;
  readonly terminal2ArrivalTotal: number | null;
  readonly totalDeparture: number;
  readonly level: AirportCrowdLevel;
  readonly levelLabel: string;
};

export type PassengerForecastBundle = {
  readonly today: readonly PassengerForecastItem[];
  readonly tomorrow: readonly PassengerForecastItem[];
};

export type ArrivalCongestionItem = {
  readonly terminal: string;
  readonly airport: string | null;
  readonly entryGate: string | null;
  readonly gateNumber: string | null;
  readonly flightId: string | null;
  readonly korean: number | null;
  readonly foreigner: number | null;
  readonly totalFlow: number;
  readonly scheduleTime: string | null;
  readonly estimatedTime: string | null;
  readonly level: AirportCrowdLevel;
  readonly levelLabel: string;
};

export type ParkingCongestionItem = {
  readonly terminal: string | null;
  readonly floor: string;
  readonly parking: number | null;
  readonly parkingArea: number | null;
  readonly available: number | null;
  readonly occupancyRate: number | null;
  readonly observedAt: string | null;
  readonly level: AirportCrowdLevel;
  readonly levelLabel: string;
};

function authHeaders(accessToken: string): Record<string, string> {
  return {
    Authorization: `Bearer ${accessToken}`,
    "Accept-Language": acceptLanguageHeader(),
  };
}

async function fetchJson<T>(accessToken: string, path: string, params?: URLSearchParams): Promise<T> {
  const query = params?.toString();
  const response = await fetch(`${getApiBaseUrl()}${path}${query ? `?${query}` : ""}`, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
    headers: authHeaders(accessToken),
  });

  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      throw new Error("로그인이 필요합니다.");
    }
    const text = response.statusText || "요청 실패";
    throw new Error(`공항 혼잡 정보를 불러오지 못했습니다. (${response.status} ${text})`);
  }

  return (await response.json()) as T;
}

export function fetchDepartureCongestion(
  accessToken: string,
  options?: {
    readonly terminalId?: string;
    readonly gateId?: string;
  },
): Promise<readonly DepartureCongestionItem[]> {
  const params = new URLSearchParams();
  if (options?.terminalId) params.set("terminalId", options.terminalId);
  if (options?.gateId) params.set("gateId", options.gateId);
  return fetchJson<readonly DepartureCongestionItem[]>(accessToken, "/mypage/airport-crowd/departure-congestion", params);
}

export function fetchPassengerForecast(
  accessToken: string,
  selectDate?: 0 | 1,
): Promise<PassengerForecastBundle> {
  const params = new URLSearchParams();
  if (selectDate != null) params.set("selectdate", String(selectDate));
  return fetchJson<PassengerForecastBundle>(accessToken, "/mypage/airport-crowd/passenger-forecast", params);
}

export function fetchArrivalsCongestion(
  accessToken: string,
  options?: {
    readonly terminal?: "T1" | "T2";
    readonly airport?: string;
  },
): Promise<readonly ArrivalCongestionItem[]> {
  const params = new URLSearchParams();
  if (options?.terminal) params.set("terno", options.terminal);
  if (options?.airport) params.set("airport", options.airport);
  return fetchJson<readonly ArrivalCongestionItem[]>(accessToken, "/mypage/airport-crowd/arrivals-congestion", params);
}

export function fetchParkingCongestion(
  accessToken: string,
  options?: {
    readonly terminal?: "T1" | "T2";
  },
): Promise<readonly ParkingCongestionItem[]> {
  const params = new URLSearchParams();
  if (options?.terminal) params.set("terno", options.terminal);
  return fetchJson<readonly ParkingCongestionItem[]>(accessToken, "/mypage/airport-crowd/parking-congestion", params);
}

export function fetchAirportCrowdCriteria(
  accessToken: string,
  zone?: AirportCrowdZone,
): Promise<readonly AirportCrowdCriteriaItem[]> {
  const params = new URLSearchParams();
  if (zone) params.set("zone", zone);
  return fetchJson<readonly AirportCrowdCriteriaItem[]>(accessToken, "/mypage/airport-crowd/criteria", params);
}
