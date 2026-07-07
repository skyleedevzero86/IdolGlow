import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

export type GlowWeatherRegionSummary = {
  readonly id: string;
  readonly name: string;
  readonly areaLabel: string;
};

export const GLOW_WEATHER_REGION_OPTIONS: readonly GlowWeatherRegionSummary[] = [
  { id: 'seoul', name: '서울', areaLabel: '서울·경기' },
  { id: 'incheon', name: '인천', areaLabel: '서울·경기' },
  { id: 'suwon', name: '수원', areaLabel: '서울·경기' },
  { id: 'chuncheon', name: '춘천', areaLabel: '강원영서' },
  { id: 'gangneung', name: '강릉', areaLabel: '강원영동' },
  { id: 'daejeon', name: '대전', areaLabel: '대전·세종·충남' },
  { id: 'cheongju', name: '청주', areaLabel: '충청북도' },
  { id: 'jeonju', name: '전주', areaLabel: '전북자치도' },
  { id: 'gwangju', name: '광주', areaLabel: '광주·전남' },
  { id: 'daegu', name: '대구', areaLabel: '대구·경북' },
  { id: 'busan', name: '부산', areaLabel: '부산·울산·경남' },
  { id: 'ulsan', name: '울산', areaLabel: '부산·울산·경남' },
  { id: 'jeju', name: '제주', areaLabel: '제주도' },
  { id: 'seogwipo', name: '서귀포', areaLabel: '제주도' },
];

export type GlowWeatherSelectedRegion = {
  readonly id: string;
  readonly name: string;
  readonly areaLabel: string;
  readonly latitude: number;
  readonly longitude: number;
};

export type GlowWeatherCurrent = {
  readonly regionName: string;
  readonly observedAt: string;
  readonly temperatureC: number | null;
  readonly humidity: number | null;
  readonly skyLabel: string;
  readonly precipitationLabel: string;
  readonly windDirectionDegrees: number | null;
  readonly windDirectionLabel: string;
  readonly windSpeedMps: number | null;
};

export type GlowWeatherMonthlySummary = {
  readonly monthLabel: string;
  readonly averageTemperatureC: number | null;
  readonly rainyDays: number;
  readonly basedOn: string;
};

export type GlowWeatherForecastDay = {
  readonly regionName: string;
  readonly date: string;
  readonly dateLabel: string;
  readonly dayLabel: string;
  readonly summary: string;
  readonly icon: 'sun' | 'partly' | 'cloud' | 'rain' | 'snow' | string;
  readonly minTempC: number | null;
  readonly maxTempC: number | null;
  readonly precipitationChance: number | null;
  readonly windDirectionDegrees: number | null;
  readonly windDirectionLabel: string | null;
  readonly windSpeedMps: number | null;
  readonly source: string;
};

export type GlowWeatherRecommendation = {
  readonly id: string;
  readonly icon: 'sun' | 'rain' | 'shirt' | string;
  readonly tone: 'sunny' | 'mint' | 'rain' | 'teal' | 'sky' | 'blue' | string;
  readonly title: string;
  readonly subtitle: string;
  readonly description: string;
};

export type GlowWeatherWindPoint = {
  readonly label: string;
  readonly degrees: number;
};

export type GlowWeatherWindGuide = {
  readonly directionDegrees: number | null;
  readonly directionLabel: string;
  readonly speedMps: number | null;
  readonly message: string;
  readonly referencePoints: readonly GlowWeatherWindPoint[];
  readonly windFromClimateStatistics?: boolean;
  readonly climateStatisticsMonth?: number | null;
};

export type GlowWeatherDashboardResponse = {
  readonly selectedRegionId: string;
  readonly regions: readonly GlowWeatherRegionSummary[];
  readonly region: GlowWeatherSelectedRegion;
  readonly current: GlowWeatherCurrent;
  readonly monthlySummary: GlowWeatherMonthlySummary;
  readonly outlookSummary: string;
  readonly forecast: readonly GlowWeatherForecastDay[];
  readonly recommendations: readonly GlowWeatherRecommendation[];
  readonly windGuide: GlowWeatherWindGuide;
  readonly forecastFromApi?: boolean;
  readonly currentFromApi?: boolean;
  readonly generatedAt: string;
};

function withAuthHeaders(accessToken: string): Record<string, string> {
  return {
    Authorization: `Bearer ${accessToken}`,
    'Accept-Language': acceptLanguageHeader(),
  };
}

export async function fetchGlowWeatherDashboard(
  accessToken: string,
  regionId?: string,
): Promise<GlowWeatherDashboardResponse> {
  const params = new URLSearchParams();
  if (regionId) params.set('regionId', regionId);
  const query = params.toString();
  const base = getApiBaseUrl();
  const response = await fetch(`${base}/mypage/glow-weather/dashboard${query ? `?${query}` : ''}`, {
    credentials: 'include',
    cache: 'no-store',
    headers: withAuthHeaders(accessToken),
  });

  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      throw new Error('Glow 날씨는 로그인 후 이용할 수 있어요.');
    }
    throw new Error(`Glow 날씨 정보를 불러오지 못했어요. (${response.status})`);
  }

  return (await response.json()) as GlowWeatherDashboardResponse;
}
