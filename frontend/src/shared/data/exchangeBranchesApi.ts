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
