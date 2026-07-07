import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

export type ExchangeRateItem = {
  readonly curUnit: string;
  readonly curNm: string;
  readonly ttb: string;
  readonly tts: string;
  readonly dealBasR: string;
  readonly bkpr: string;
};

function withAuthHeaders(accessToken: string): Record<string, string> {
  return {
    Authorization: `Bearer ${accessToken}`,
    'Accept-Language': acceptLanguageHeader(),
  };
}

export async function fetchDailyExchangeRates(
  accessToken: string,
  searchDate?: string,
): Promise<readonly ExchangeRateItem[]> {
  const params = new URLSearchParams();
  if (searchDate) params.set('searchDate', searchDate);
  const q = params.toString();
  const base = getApiBaseUrl();
  const response = await fetch(`${base}/mypage/exchange-rates/daily${q ? `?${q}` : ''}`, {
    credentials: 'include',
    cache: 'no-store',
    headers: withAuthHeaders(accessToken),
  });
  if (!response.ok) {
    const text = response.statusText || '요청 실패';
    if (response.status === 404) {
      throw new Error(
        `환율 API를 찾을 수 없습니다(404). 백엔드를 재시작했는지 확인하세요. (${base}/mypage/exchange-rates/daily)`,
      );
    }
    if (response.status === 401 || response.status === 403) {
      throw new Error('로그인이 필요하거나 권한이 없습니다. 다시 로그인한 뒤 시도하세요.');
    }
    throw new Error(`환율 정보를 불러오지 못했습니다. (${response.status} ${text})`);
  }
  return (await response.json()) as ExchangeRateItem[];
}
