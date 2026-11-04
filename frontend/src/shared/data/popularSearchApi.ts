import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

export interface PopularSearchKeyword {
  readonly keyword: string;
  readonly searchCount: number;
}

export async function fetchPopularSearchKeywords(limit = 6): Promise<readonly PopularSearchKeyword[]> {
  const response = await fetch(`${getApiBaseUrl()}/search-keywords/popular?limit=${limit}`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: {
      'Accept-Language': acceptLanguageHeader(),
    },
  });
  if (!response.ok) {
    throw new Error(`자주 찾는 검색어를 불러오지 못했습니다. (${response.status})`);
  }
  return (await response.json()) as readonly PopularSearchKeyword[];
}

export async function recordSearchKeyword(keyword: string): Promise<void> {
  const normalized = keyword.trim();
  if (!normalized) {
    return;
  }
  try {
    await fetch(`${getApiBaseUrl()}/search-keywords`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        'Accept-Language': acceptLanguageHeader(),
      },
      body: JSON.stringify({ keyword: normalized }),
    });
  } catch {
  }
}
