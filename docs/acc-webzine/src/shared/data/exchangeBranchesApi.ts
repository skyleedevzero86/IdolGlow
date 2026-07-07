import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

export type ExchangeBranchItem = {
  readonly branchId: number;
  readonly name: string;
  readonly rate: number;
  readonly currency: string;
  readonly lat: number;
  readonly lng: number;
  readonly airportHub: boolean;
  readonly durationMinutesFromAirport: number | null;
};

export type CreateExchangeAlertBody = {
  readonly fromCurrency: string;
  readonly toCurrency: string;
  readonly targetRate: number;
};

const CURRENCY_BASE_ALIASES: Record<string, string> = {
  // 한국수출입은행 API는 중국 위안을 CNH로 주는 경우가 있어 UI의 CNY와 같은 기준 통화로 본다.
  CNH: 'CNY',
};

function authHeaders(accessToken: string): Record<string, string> {
  return {
    Authorization: `Bearer ${accessToken}`,
    'Accept-Language': acceptLanguageHeader(),
  };
}

function jsonAuthHeaders(accessToken: string): Record<string, string> {
  return { ...authHeaders(accessToken), 'Content-Type': 'application/json' };
}

/**
 * 한국수출입은행 고시 단위 예: `JPY(100)`, `USD`, `KRW`.
 * 괄호 앞 한글/잡문자만 있을 때 잘못된 값이 나오지 않도록 3자리 ISO 코드만 인정합니다.
 */
export function currencyBaseCode(curUnit: string): string {
  const raw = curUnit.trim();
  if (!raw) return '';
  const compact = raw.toUpperCase().replace(/\s+/g, '');
  const normalize = (code: string): string => CURRENCY_BASE_ALIASES[code] ?? code;
  const head = /^([A-Z]{3})(?:\(\d+\))?$/.exec(compact);
  if (head) return normalize(head[1]);
  const inParen = /\(([A-Z]{3})\)/.exec(raw.toUpperCase());
  if (inParen) return normalize(inParen[1]);
  const word = /\b([A-Z]{3})\b/.exec(raw.toUpperCase());
  return word ? normalize(word[1]) : '';
}

export async function fetchExchangeBranches(
  accessToken: string,
  currency: string,
): Promise<readonly ExchangeBranchItem[]> {
  const params = new URLSearchParams({ currency });
  const base = getApiBaseUrl();
  const response = await fetch(`${base}/mypage/exchange/branches?${params.toString()}`, {
    credentials: 'include',
    cache: 'no-store',
    headers: authHeaders(accessToken),
  });
  if (!response.ok) {
    const text = response.statusText || '요청 실패';
    if (response.status === 401 || response.status === 403) {
      throw new Error('로그인이 필요합니다.');
    }
    if (response.status === 404) {
      return [];
    }
    throw new Error(`환전소 목록을 불러오지 못했습니다. (${response.status} ${text})`);
  }
  return (await response.json()) as ExchangeBranchItem[];
}

export async function postExchangeAlert(
  accessToken: string,
  body: CreateExchangeAlertBody,
): Promise<{ readonly id: number }> {
  const base = getApiBaseUrl();
  const response = await fetch(`${base}/mypage/exchange-alerts`, {
    method: 'POST',
    credentials: 'include',
    cache: 'no-store',
    headers: jsonAuthHeaders(accessToken),
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    const text = response.statusText || '요청 실패';
    if (response.status === 401 || response.status === 403) {
      throw new Error('로그인이 필요합니다.');
    }
    throw new Error(`알림 저장에 실패했습니다. (${response.status} ${text})`);
  }
  return (await response.json()) as { readonly id: number };
}
