import { getApiBaseUrl } from "../../auth/authConfig";
import { acceptLanguageHeader } from "../../ui/i18n/uiLangStorage";

export type FestivalEventItem = {
  readonly contentId: string;
  readonly title: string;
  readonly address: string | null;
  readonly eventStartDate: string | null;
  readonly eventEndDate: string | null;
  readonly thumbnailImageUrl: string | null;
  readonly imageUrl: string | null;
  readonly mapX: number | null;
  readonly mapY: number | null;
  readonly phone: string | null;
  readonly detailUrl: string | null;
  readonly category: string | null;
  readonly synopsis: string | null;
  readonly source: string;
  readonly cast: string | null;
  readonly runningTime: string | null;
  readonly age: string | null;
  readonly bookingPlaces: string | null;
  readonly introImageUrls: readonly string[];
};

export type FestivalCommonDetail = {
  readonly contentId: string;
  readonly contentTypeId: string | null;
  readonly title: string | null;
  readonly homepage: string | null;
  readonly overview: string | null;
  readonly address: string | null;
  readonly addressDetail: string | null;
  readonly mapX: number | null;
  readonly mapY: number | null;
  readonly tel: string | null;
  readonly firstImage: string | null;
  readonly firstImage2: string | null;
};

export type FestivalImageItem = {
  readonly contentId: string;
  readonly imageName: string | null;
  readonly originImageUrl: string | null;
  readonly smallImageUrl: string | null;
  readonly copyrightType: string | null;
  readonly serialNum: string | null;
};

export type TourCodeItem = {
  readonly code: string | null;
  readonly name: string | null;
  readonly rnum: number | null;
  readonly lDongRegnCd: string | null;
  readonly lDongRegnNm: string | null;
  readonly lDongSignguCd: string | null;
  readonly lDongSignguNm: string | null;
  readonly lclsSystm1Cd: string | null;
  readonly lclsSystm1Nm: string | null;
  readonly lclsSystm2Cd: string | null;
  readonly lclsSystm2Nm: string | null;
  readonly lclsSystm3Cd: string | null;
  readonly lclsSystm3Nm: string | null;
};

export type KopisAreaStatItem = {
  readonly area: string;
  readonly fcltycnt: number | null;
  readonly prfplccnt: number | null;
  readonly seatcnt: number | null;
  readonly prfcnt: number | null;
  readonly prfprocnt: number | null;
  readonly prfdtcnt: number | null;
  readonly nmrs: number | null;
  readonly nmrcancl: number | null;
  readonly totnmrs: number | null;
  readonly amount: number | null;
};

export type SpecialDayItem = {
  readonly dateName: string;
  readonly locDate: string;
  readonly dateKind: string | null;
  readonly isHoliday: string | null;
  readonly seq: number | null;
  readonly source: string;
};

function authHeaders(accessToken: string): Record<string, string> {
  return {
    Authorization: `Bearer ${accessToken}`,
    "Accept-Language": acceptLanguageHeader(),
  };
}

const EVENT_INFO_API_PREFIX = "/mypage/event-info";

function eventInfoUrl(path: string): string {
  return `${getApiBaseUrl()}${EVENT_INFO_API_PREFIX}${path}`;
}

function proxiedEventInfoUrl(path: string): string {
  return `${EVENT_INFO_API_PREFIX}${path}`;
}

function isNetworkFetchError(cause: unknown): boolean {
  if (!(cause instanceof Error)) return false;
  return cause instanceof TypeError || /failed to fetch|network/i.test(cause.message);
}

async function fetchEventInfo(path: string, init: RequestInit): Promise<Response> {
  try {
    return await fetch(eventInfoUrl(path), init);
  } catch (cause) {
    if (!isNetworkFetchError(cause)) throw cause;
    return fetch(proxiedEventInfoUrl(path), init);
  }
}

export async function fetchFestivalEventsByDate(
  accessToken: string,
  date: string,
  options?: {
    pageNo?: number;
    numOfRows?: number;
  },
): Promise<readonly FestivalEventItem[]> {
  const params = new URLSearchParams({
    date,
    pageNo: String(options?.pageNo ?? 1),
    numOfRows: String(options?.numOfRows ?? 30),
  });
  const response = await fetchEventInfo(
    `/festivals?${params.toString()}`,
    {
      method: "GET",
      credentials: "include",
      cache: "no-store",
      headers: authHeaders(accessToken),
    },
  );
  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      throw new Error("로그인이 필요합니다.");
    }
    const text = response.statusText || "요청 실패";
    throw new Error(`행사 정보를 불러오지 못했습니다. (${response.status} ${text})`);
  }
  return (await response.json()) as FestivalEventItem[];
}

export async function fetchFestivalCommonDetail(
  accessToken: string,
  contentId: string,
): Promise<FestivalCommonDetail | null> {
  const response = await fetch(
    `${getApiBaseUrl()}/mypage/event-info/festivals/${encodeURIComponent(contentId)}/common`,
    {
      method: "GET",
      credentials: "include",
      cache: "no-store",
      headers: authHeaders(accessToken),
    },
  );
  if (!response.ok) {
    if (response.status === 404) return null;
    throw new Error(`상세 정보를 불러오지 못했습니다. (${response.status})`);
  }
  return (await response.json()) as FestivalCommonDetail | null;
}

/** 목록 contentId 형식 `culture-{seq}` → 문화캘린더 detail2 프록시 */
export async function fetchCultureCalendarDetail(
  accessToken: string,
  contentId: string,
): Promise<FestivalCommonDetail | null> {
  const seq = /^culture-(\d+)$/.exec(contentId.trim())?.[1];
  if (!seq) return null;
  const params = new URLSearchParams({ seq });
  const response = await fetch(`${getApiBaseUrl()}/mypage/event-info/culture-calendar/detail?${params.toString()}`, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
    headers: authHeaders(accessToken),
  });
  if (!response.ok) {
    if (response.status === 404) return null;
    throw new Error(`문화캘린더 상세를 불러오지 못했습니다. (${response.status})`);
  }
  return (await response.json()) as FestivalCommonDetail | null;
}

/** 문화정보 realm2 목록 프록시. numOfrows 최대 1000, sortStdr: 1=등록일, 2=공연명, 3=지역. */
export async function fetchCultureRealm2Events(
  accessToken: string,
  options: {
    readonly from: string;
    readonly to: string;
    readonly serviceTp: "A" | "B" | "C";
    readonly realmCode?: string;
    readonly sortStdr?: 1 | 2 | 3;
    readonly pageNo?: number;
    readonly numOfrows?: number;
  },
): Promise<readonly FestivalEventItem[]> {
  const params = new URLSearchParams({
    from: options.from,
    to: options.to,
    serviceTp: options.serviceTp,
    sortStdr: String(options.sortStdr ?? 1),
    pageNo: String(options.pageNo ?? 1),
    numOfrows: String(Math.min(1000, Math.max(1, options.numOfrows ?? 1000))),
  });
  if (options.realmCode?.trim()) params.set("realmCode", options.realmCode.trim());
  const response = await fetch(
    `${getApiBaseUrl()}/mypage/event-info/culture-calendar/realm2?${params.toString()}`,
    {
      method: "GET",
      credentials: "include",
      cache: "no-store",
      headers: authHeaders(accessToken),
    },
  );
  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      throw new Error("로그인이 필요합니다.");
    }
    throw new Error(`문화 realm2 목록을 불러오지 못했습니다. (${response.status})`);
  }
  return (await response.json()) as FestivalEventItem[];
}

/** 문화정보 area2 목록 프록시. numOfrows 최대 1000, sortStdr: 1=등록일, 2=공연명, 3=지역. */
export async function fetchCultureArea2Events(
  accessToken: string,
  options: {
    readonly from: string;
    readonly to: string;
    readonly serviceTp: "A" | "B" | "C";
    readonly sortStdr?: 1 | 2 | 3;
    readonly pageNo?: number;
    readonly numOfrows?: number;
  },
): Promise<readonly FestivalEventItem[]> {
  const params = new URLSearchParams({
    from: options.from,
    to: options.to,
    serviceTp: options.serviceTp,
    sortStdr: String(options.sortStdr ?? 1),
    pageNo: String(options.pageNo ?? 1),
    numOfrows: String(Math.min(1000, Math.max(1, options.numOfrows ?? 1000))),
  });
  const response = await fetch(
    `${getApiBaseUrl()}/mypage/event-info/culture-calendar/area2?${params.toString()}`,
    {
      method: "GET",
      credentials: "include",
      cache: "no-store",
      headers: authHeaders(accessToken),
    },
  );
  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      throw new Error("로그인이 필요합니다.");
    }
    throw new Error(`문화 area2 목록을 불러오지 못했습니다. (${response.status})`);
  }
  return (await response.json()) as FestivalEventItem[];
}

/** 문화캘린더 지역 필터용 시·도 목록(정적, `/culture-calendar/areas`). */
export async function fetchCultureSidoReference(accessToken: string): Promise<readonly TourCodeItem[]> {
  const response = await fetch(`${getApiBaseUrl()}/mypage/event-info/culture-calendar/areas`, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
    headers: authHeaders(accessToken),
  });
  if (!response.ok) {
    throw new Error(`시·도 참고 목록을 불러오지 못했습니다. (${response.status})`);
  }
  return (await response.json()) as TourCodeItem[];
}

/** realm2 조회용 분야 코드(A000=연극 등) 정적 참고. */
export async function fetchCultureRealmCodeReference(accessToken: string): Promise<readonly TourCodeItem[]> {
  const response = await fetch(`${getApiBaseUrl()}/mypage/event-info/culture-calendar/realm-code-reference`, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
    headers: authHeaders(accessToken),
  });
  if (!response.ok) {
    throw new Error(`분야 코드 참고를 불러오지 못했습니다. (${response.status})`);
  }
  return (await response.json()) as TourCodeItem[];
}

export async function fetchFestivalImages(
  accessToken: string,
  contentId: string,
  imageYn: "Y" | "N" = "Y",
): Promise<readonly FestivalImageItem[]> {
  const params = new URLSearchParams({ imageYn });
  const response = await fetch(
    `${getApiBaseUrl()}/mypage/event-info/festivals/${encodeURIComponent(contentId)}/images?${params.toString()}`,
    {
      method: "GET",
      credentials: "include",
      cache: "no-store",
      headers: authHeaders(accessToken),
    },
  );
  if (!response.ok) {
    throw new Error(`이미지 정보를 불러오지 못했습니다. (${response.status})`);
  }
  return (await response.json()) as FestivalImageItem[];
}

/** 키워드는 비울 수 있음 — 지역·분류만 있으면 백엔드가 넓은 기본 키워드로 조회합니다. */
export async function searchFestivalByKeyword(
  accessToken: string,
  keyword: string,
  options?: {
    lDongRegnCd?: string;
    lDongSignguCd?: string;
    lclsSystm1?: string;
    lclsSystm2?: string;
    lclsSystm3?: string;
    pageNo?: number;
    numOfRows?: number;
  },
): Promise<readonly FestivalEventItem[]> {
  const params = new URLSearchParams({
    keyword,
    pageNo: String(options?.pageNo ?? 1),
    numOfRows: String(options?.numOfRows ?? 30),
  });
  if (options?.lDongRegnCd) params.set("lDongRegnCd", options.lDongRegnCd);
  if (options?.lDongSignguCd) params.set("lDongSignguCd", options.lDongSignguCd);
  if (options?.lclsSystm1) params.set("lclsSystm1", options.lclsSystm1);
  if (options?.lclsSystm2) params.set("lclsSystm2", options.lclsSystm2);
  if (options?.lclsSystm3) params.set("lclsSystm3", options.lclsSystm3);
  const response = await fetch(`${getApiBaseUrl()}/mypage/event-info/search-keyword?${params.toString()}`, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
    headers: authHeaders(accessToken),
  });
  if (!response.ok) {
    throw new Error(`키워드 검색에 실패했습니다. (${response.status})`);
  }
  return (await response.json()) as FestivalEventItem[];
}

export async function fetchLDongCodes(
  accessToken: string,
  lDongRegnCd?: string,
  lDongListYn: "Y" | "N" = "N",
): Promise<readonly TourCodeItem[]> {
  const params = new URLSearchParams({ lDongListYn });
  if (lDongRegnCd) params.set("lDongRegnCd", lDongRegnCd);
  const response = await fetch(`${getApiBaseUrl()}/mypage/event-info/ldong-codes?${params.toString()}`, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
    headers: authHeaders(accessToken),
  });
  if (!response.ok) {
    throw new Error(`법정동 코드 조회에 실패했습니다. (${response.status})`);
  }
  return (await response.json()) as TourCodeItem[];
}

export async function fetchLclsCodes(
  accessToken: string,
  options?: {
    lclsSystm1?: string;
    lclsSystm2?: string;
    lclsSystm3?: string;
    lclsSystmListYn?: "Y" | "N";
  },
): Promise<readonly TourCodeItem[]> {
  const params = new URLSearchParams({
    lclsSystmListYn: options?.lclsSystmListYn ?? "N",
  });
  if (options?.lclsSystm1) params.set("lclsSystm1", options.lclsSystm1);
  if (options?.lclsSystm2) params.set("lclsSystm2", options.lclsSystm2);
  if (options?.lclsSystm3) params.set("lclsSystm3", options.lclsSystm3);
  const response = await fetch(`${getApiBaseUrl()}/mypage/event-info/lcls-codes?${params.toString()}`, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
    headers: authHeaders(accessToken),
  });
  if (!response.ok) {
    throw new Error(`분류체계 코드 조회에 실패했습니다. (${response.status})`);
  }
  return (await response.json()) as TourCodeItem[];
}

export async function fetchKopisAreaStats(
  accessToken: string,
  stDate: string,
  edDate: string,
): Promise<readonly KopisAreaStatItem[]> {
  const params = new URLSearchParams({ stDate, edDate });
  const response = await fetch(`${getApiBaseUrl()}/mypage/event-info/kopis/area-stats?${params.toString()}`, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
    headers: authHeaders(accessToken),
  });
  if (!response.ok) {
    throw new Error(`KOPIS 지역 통계를 불러오지 못했습니다. (${response.status})`);
  }
  return (await response.json()) as KopisAreaStatItem[];
}

export async function fetchKopisPerformanceDetail(
  accessToken: string,
  contentId: string,
): Promise<FestivalEventItem | null> {
  const response = await fetch(
    `${getApiBaseUrl()}/mypage/event-info/kopis/performances/${encodeURIComponent(contentId)}`,
    {
      method: "GET",
      credentials: "include",
      cache: "no-store",
      headers: authHeaders(accessToken),
    },
  );
  if (!response.ok) {
    if (response.status === 404) return null;
    throw new Error(`KOPIS 공연 상세를 불러오지 못했습니다. (${response.status})`);
  }
  return (await response.json()) as FestivalEventItem | null;
}

export async function fetchSjwPerformanceDetail(
  accessToken: string,
  performIdx: string,
): Promise<FestivalEventItem | null> {
  const response = await fetch(
    `${getApiBaseUrl()}/mypage/event-info/sjw-perform/${encodeURIComponent(performIdx)}`,
    {
      method: "GET",
      credentials: "include",
      cache: "no-store",
      headers: authHeaders(accessToken),
    },
  );
  if (!response.ok) {
    if (response.status === 404) return null;
    throw new Error(`세종문화회관 공연 상세를 불러오지 못했습니다. (${response.status})`);
  }
  return (await response.json()) as FestivalEventItem | null;
}

async function fetchSpecialDaysByType(
  accessToken: string,
  endpoint: "holidays" | "rest-days" | "anniversaries",
  solYear: string,
  solMonth: string,
): Promise<readonly SpecialDayItem[]> {
  const params = new URLSearchParams({ solYear, solMonth });
  const response = await fetch(
    `${getApiBaseUrl()}/mypage/event-info/special-days/${endpoint}?${params.toString()}`,
    {
      method: "GET",
      credentials: "include",
      cache: "no-store",
      headers: authHeaders(accessToken),
    },
  );
  if (!response.ok) {
    throw new Error(`특일 정보를 불러오지 못했습니다. (${response.status})`);
  }
  return (await response.json()) as SpecialDayItem[];
}

export async function fetchSpecialHolidayInfo(
  accessToken: string,
  solYear: string,
  solMonth: string,
): Promise<readonly SpecialDayItem[]> {
  return fetchSpecialDaysByType(accessToken, "holidays", solYear, solMonth);
}

export async function fetchSpecialRestDayInfo(
  accessToken: string,
  solYear: string,
  solMonth: string,
): Promise<readonly SpecialDayItem[]> {
  return fetchSpecialDaysByType(accessToken, "rest-days", solYear, solMonth);
}

export async function fetchSpecialAnniversaryInfo(
  accessToken: string,
  solYear: string,
  solMonth: string,
): Promise<readonly SpecialDayItem[]> {
  return fetchSpecialDaysByType(accessToken, "anniversaries", solYear, solMonth);
}
