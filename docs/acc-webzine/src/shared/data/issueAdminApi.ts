import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';
import { getApiBaseUrl } from '../../auth/authConfig';
import type { IssueCategoryKey } from './mockIssues';

export interface AdminIssueHeadline {
  readonly slug: string;
  readonly title: string;
  readonly category: IssueCategoryKey;
  readonly categoryLabel: string;
}

export interface AdminIssueSummary {
  readonly id: number;
  readonly slug: string;
  readonly volume: number;
  readonly issueDate: string;
  readonly issueYear: number;
  readonly issueMonth: number;
  readonly coverImageUrl: string;
  readonly teaser: string;
  readonly articleCount: number;
  readonly headlines: readonly AdminIssueHeadline[];
}

export interface AdminIssuePageResponse {
  readonly issues: readonly AdminIssueSummary[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
  readonly hasNext: boolean;
  readonly latestVolume: number;
  readonly totalArticleCount: number;
  readonly availableYears: readonly number[];
  readonly availableMonths: readonly number[];
  readonly availableVolumes: readonly number[];
}

export interface AdminIssueArticleCard {
  readonly id: number;
  readonly slug: string;
  readonly issueSlug: string;
  readonly volume: number;
  readonly issueDate: string;
  readonly title: string;
  readonly kicker: string;
  readonly summary: string;
  readonly cardImageUrl: string;
  readonly category: IssueCategoryKey;
  readonly categoryLabel: string;
  readonly formatLabel: string;
  readonly tags: readonly string[];
  readonly authorName: string;
}

export interface AdminIssueVolume {
  readonly id: number;
  readonly slug: string;
  readonly volume: number;
  readonly issueDate: string;
  readonly issueYear: number;
  readonly issueMonth: number;
  readonly coverImageUrl: string;
  readonly teaser: string;
  readonly articleCount: number;
  readonly articles: readonly AdminIssueArticleCard[];
}

export interface AdminIssueArticleSection {
  readonly id: number;
  readonly heading: string | null;
  readonly body: string;
  readonly paragraphs: readonly string[];
  readonly note: string | null;
}

export interface AdminIssueRelatedContent {
  readonly id: number;
  readonly slug: string;
  readonly title: string;
  readonly category: IssueCategoryKey;
  readonly categoryLabel: string;
  readonly imageUrl: string;
}

export interface AdminIssueArticle {
  readonly id: number;
  readonly slug: string;
  readonly issueSlug: string;
  readonly volume: number;
  readonly issueDate: string;
  readonly title: string;
  readonly kicker: string;
  readonly summary: string;
  readonly heroImageUrl: string;
  readonly cardImageUrl: string;
  readonly galleryImageUrls: readonly string[];
  readonly category: IssueCategoryKey;
  readonly categoryLabel: string;
  readonly formatLabel: string;
  readonly tags: readonly string[];
  readonly authorName: string;
  readonly authorEmail: string;
  readonly creditLine: string;
  readonly highlightQuote: string | null;
  readonly sections: readonly AdminIssueArticleSection[];
  readonly relatedContents: readonly AdminIssueRelatedContent[];
}

export interface IssueAdminSectionInput {
  readonly heading: string;
  readonly body: string;
  readonly note: string;
}

export interface IssueAdminArticleInput {
  readonly title: string;
  readonly kicker: string;
  readonly summary: string;
  readonly category: IssueCategoryKey;
  readonly formatLabel: string;
  readonly heroImageUrl: string;
  readonly cardImageUrl: string;
  readonly galleryImageUrls: readonly string[];
  readonly tags: readonly string[];
  readonly authorName: string;
  readonly authorEmail: string;
  readonly creditLine: string;
  readonly highlightQuote: string;
  readonly sections: readonly IssueAdminSectionInput[];
}

export interface IssueAdminIssueInput {
  readonly volume: number;
  readonly issueDate: string;
  readonly coverImageUrl: string;
  readonly teaser: string;
}

export interface AdminIssueImageUploadResponse {
  readonly url: string;
  readonly objectKey: string;
  readonly contentType: string;
  readonly size: number;
}

export interface FetchAdminIssuesParams {
  readonly page?: number;
  readonly size?: number;
  readonly year?: number | null;
  readonly month?: number | null;
  readonly volume?: number | null;
}

interface ErrorBody {
  readonly message?: string;
}

const withBaseHeaders = (accessToken: string, headers?: HeadersInit): HeadersInit => ({
  Authorization: `Bearer ${accessToken}`,
  'Accept-Language': acceptLanguageHeader(),
  ...headers,
});

const toQueryString = (params: FetchAdminIssuesParams): string => {
  const search = new URLSearchParams();

  if (params.page) search.set('page', String(params.page));
  if (params.size) search.set('size', String(params.size));
  if (params.year) search.set('year', String(params.year));
  if (params.month) search.set('month', String(params.month));
  if (params.volume) search.set('volume', String(params.volume));

  const query = search.toString();
  return query ? `?${query}` : '';
};

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ErrorBody;
    if (body.message?.trim()) {
      return body.message;
    }
  } catch {
    // ignore parse failure
  }

  return response.status >= 500
    ? '서버 요청 처리 중 오류가 발생했습니다.'
    : '요청을 처리하지 못했습니다.';
}

async function requestJson<T>(input: RequestInfo | URL, init: RequestInit): Promise<T> {
  const response = await fetch(input, { ...init, credentials: 'include' });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }

  return (await response.json()) as T;
}

async function requestVoid(input: RequestInfo | URL, init: RequestInit): Promise<void> {
  const response = await fetch(input, { ...init, credentials: 'include' });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
}

export async function fetchAdminIssuePage(
  accessToken: string,
  params: FetchAdminIssuesParams = {}
): Promise<AdminIssuePageResponse> {
  return requestJson<AdminIssuePageResponse>(
    `${getApiBaseUrl()}/admin/issues${toQueryString(params)}`,
    {
      method: 'GET',
      headers: withBaseHeaders(accessToken),
      cache: 'no-store',
    }
  );
}

export async function fetchAdminIssueVolume(
  accessToken: string,
  issueSlug: string
): Promise<AdminIssueVolume> {
  return requestJson<AdminIssueVolume>(`${getApiBaseUrl()}/admin/issues/${issueSlug}`, {
    method: 'GET',
    headers: withBaseHeaders(accessToken),
    cache: 'no-store',
  });
}

export async function fetchAdminIssueArticle(
  accessToken: string,
  issueSlug: string,
  articleSlug: string
): Promise<AdminIssueArticle> {
  return requestJson<AdminIssueArticle>(
    `${getApiBaseUrl()}/admin/issues/${issueSlug}/articles/${articleSlug}`,
    {
      method: 'GET',
      headers: withBaseHeaders(accessToken),
      cache: 'no-store',
    }
  );
}

export async function createAdminIssue(
  accessToken: string,
  payload: IssueAdminIssueInput
): Promise<AdminIssueVolume> {
  return requestJson<AdminIssueVolume>(`${getApiBaseUrl()}/admin/issues`, {
    method: 'POST',
    headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(payload),
  });
}

export async function updateAdminIssue(
  accessToken: string,
  issueSlug: string,
  payload: IssueAdminIssueInput
): Promise<AdminIssueVolume> {
  return requestJson<AdminIssueVolume>(`${getApiBaseUrl()}/admin/issues/${issueSlug}`, {
    method: 'PUT',
    headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(payload),
  });
}

export async function deleteAdminIssue(accessToken: string, issueSlug: string): Promise<void> {
  await requestVoid(`${getApiBaseUrl()}/admin/issues/${issueSlug}`, {
    method: 'DELETE',
    headers: withBaseHeaders(accessToken),
  });
}

export async function createAdminIssueArticle(
  accessToken: string,
  issueSlug: string,
  payload: IssueAdminArticleInput
): Promise<AdminIssueArticle> {
  return requestJson<AdminIssueArticle>(`${getApiBaseUrl()}/admin/issues/${issueSlug}/articles`, {
    method: 'POST',
    headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(payload),
  });
}

export async function updateAdminIssueArticle(
  accessToken: string,
  issueSlug: string,
  articleSlug: string,
  payload: IssueAdminArticleInput
): Promise<AdminIssueArticle> {
  return requestJson<AdminIssueArticle>(
    `${getApiBaseUrl()}/admin/issues/${issueSlug}/articles/${articleSlug}`,
    {
      method: 'PUT',
      headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
      body: JSON.stringify(payload),
    }
  );
}

export async function deleteAdminIssueArticle(
  accessToken: string,
  issueSlug: string,
  articleSlug: string
): Promise<void> {
  await requestVoid(`${getApiBaseUrl()}/admin/issues/${issueSlug}/articles/${articleSlug}`, {
    method: 'DELETE',
    headers: withBaseHeaders(accessToken),
  });
}

export async function uploadAdminIssueImage(
  accessToken: string,
  file: File,
  folder?: string
): Promise<AdminIssueImageUploadResponse> {
  const formData = new FormData();
  formData.append('file', file);
  if (folder?.trim()) {
    formData.append('folder', folder.trim());
  }

  return requestJson<AdminIssueImageUploadResponse>(`${getApiBaseUrl()}/admin/issues/uploads/images`, {
    method: 'POST',
    headers: withBaseHeaders(accessToken),
    body: formData,
  });
}
