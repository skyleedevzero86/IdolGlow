import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

const SUBWAY_API_PREFIX = '/mypage/subway';

export type SubwayLine = {
  readonly id: string;
  readonly name: string;
  readonly colorHex: string;
};

export type SubwayStation = {
  readonly stationCd: string;
  readonly name: string;
  readonly frCode: string;
  readonly lineId: string;
  readonly lineName: string;
};

export type SubwayStationRef = {
  readonly lineId: string;
  readonly lineName: string;
  readonly stationCd: string;
  readonly name: string;
  readonly frCode: string;
};

export type SubwaySummary = {
  readonly title: string;
  readonly bullets: readonly string[];
  readonly learnMoreLabel: string;
  readonly learnMoreUrl: string | null;
};

export type SubwayNearby = {
  readonly radiusMeters: number;
  readonly count: number;
  readonly label: string;
};

export type SubwayPage = {
  readonly line: SubwayLine;
  readonly station: SubwayStation;
  readonly prevStation: SubwayStationRef;
  readonly nextStation: SubwayStationRef;
  readonly summary: SubwaySummary;
  readonly nearby: SubwayNearby;
};

function headers(accessToken: string): Record<string, string> {
  return {
    Authorization: `Bearer ${accessToken}`,
    'Accept-Language': acceptLanguageHeader(),
  };
}

function subwayRequestError(res: Response, fallbackMessage: string): Error {
  const base = getApiBaseUrl();
  const path = `${base}${SUBWAY_API_PREFIX}`;
  if (res.status === 404) {
    return new Error(
      `지하철 API를 찾을 수 없습니다(404). 백엔드를 최신 코드로 빌드·재시작했는지 확인하세요. (${path}/… )`,
    );
  }
  if (res.status === 401 || res.status === 403) {
    return new Error('로그인이 필요하거나 세션이 만료되었습니다. 다시 로그인해 주세요.');
  }
  return new Error(res.statusText || fallbackMessage);
}

export async function fetchSubwayLines(accessToken: string): Promise<readonly SubwayLine[]> {
  const base = getApiBaseUrl();
  const res = await fetch(`${base}${SUBWAY_API_PREFIX}/lines`, {
    credentials: 'include',
    cache: 'no-store',
    headers: headers(accessToken),
  });
  if (!res.ok) {
    throw subwayRequestError(res, '노선 목록을 불러오지 못했습니다.');
  }
  return (await res.json()) as SubwayLine[];
}

export async function fetchSubwayStations(accessToken: string, lineId: string): Promise<readonly SubwayStation[]> {
  const base = getApiBaseUrl();
  const res = await fetch(`${base}${SUBWAY_API_PREFIX}/lines/${encodeURIComponent(lineId)}/stations`, {
    credentials: 'include',
    cache: 'no-store',
    headers: headers(accessToken),
  });
  if (!res.ok) {
    throw subwayRequestError(res, '역 목록을 불러오지 못했습니다.');
  }
  return (await res.json()) as SubwayStation[];
}

export async function searchSubwayStations(accessToken: string, q: string): Promise<readonly SubwayStationRef[]> {
  const base = getApiBaseUrl();
  const params = new URLSearchParams({ q });
  const res = await fetch(`${base}${SUBWAY_API_PREFIX}/stations/search?${params}`, {
    credentials: 'include',
    cache: 'no-store',
    headers: headers(accessToken),
  });
  if (!res.ok) {
    throw subwayRequestError(res, '검색에 실패했습니다.');
  }
  return (await res.json()) as SubwayStationRef[];
}

export async function fetchSubwayStationPage(
  accessToken: string,
  lineId: string,
  stationCd: string,
): Promise<SubwayPage> {
  const base = getApiBaseUrl();
  const res = await fetch(
    `${base}${SUBWAY_API_PREFIX}/lines/${encodeURIComponent(lineId)}/stations/${encodeURIComponent(stationCd)}/page`,
    {
      credentials: 'include',
      cache: 'no-store',
      headers: headers(accessToken),
    },
  );
  if (!res.ok) {
    throw subwayRequestError(res, '역 정보를 불러오지 못했습니다.');
  }
  return (await res.json()) as SubwayPage;
}
