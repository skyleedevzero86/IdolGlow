import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

export interface AdminNewsletterSummary {
  readonly id: number;
  readonly slug: string;
  readonly title: string;
  readonly categoryLabel: string;
  readonly publishedAt: string;
  readonly imageUrl: string;
  readonly tags: readonly string[];
  readonly summary: string;
}

export interface AdminNewsletterPageResponse {
  readonly newsletters: readonly AdminNewsletterSummary[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
  readonly hasNext: boolean;
}

export interface AdminNewsletterDetail {
  readonly id: number;
  readonly slug: string;
  readonly title: string;
  readonly categoryLabel: string;
  readonly publishedAt: string;
  readonly imageUrl: string;
  readonly tags: readonly string[];
  readonly summary: string;
  readonly paragraphs: readonly string[];
}

export interface NewsletterAdminInput {
  readonly title: string;
  readonly categoryLabel: string;
  readonly publishedAt: string;
  readonly imageUrl: string;
  readonly tags: readonly string[];
  readonly summary: string;
  readonly paragraphs: readonly string[];
}

export interface AdminNewsletterImageUploadResponse {
  readonly url: string;
  readonly objectKey: string;
  readonly contentType: string;
  readonly size: number;
}

interface ErrorBody {
  readonly message?: string;
}

const withBaseHeaders = (accessToken: string, headers?: HeadersInit): HeadersInit => ({
  Authorization: `Bearer ${accessToken}`,
  'Accept-Language': acceptLanguageHeader(),
  ...headers,
});

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

export async function fetchAdminNewsletterPage(
  accessToken: string,
  page: number,
  size: number
): Promise<AdminNewsletterPageResponse> {
  return requestJson<AdminNewsletterPageResponse>(
    `${getApiBaseUrl()}/admin/newsletters?page=${page}&size=${size}`,
    {
      method: 'GET',
      headers: withBaseHeaders(accessToken),
      cache: 'no-store',
    }
  );
}

export async function fetchAdminNewsletter(
  accessToken: string,
  newsletterSlug: string
): Promise<AdminNewsletterDetail> {
  return requestJson<AdminNewsletterDetail>(
    `${getApiBaseUrl()}/admin/newsletters/${newsletterSlug}`,
    {
      method: 'GET',
      headers: withBaseHeaders(accessToken),
      cache: 'no-store',
    }
  );
}

export async function createAdminNewsletter(
  accessToken: string,
  payload: NewsletterAdminInput
): Promise<AdminNewsletterDetail> {
  return requestJson<AdminNewsletterDetail>(`${getApiBaseUrl()}/admin/newsletters`, {
    method: 'POST',
    headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(payload),
  });
}

export async function updateAdminNewsletter(
  accessToken: string,
  newsletterSlug: string,
  payload: NewsletterAdminInput
): Promise<AdminNewsletterDetail> {
  return requestJson<AdminNewsletterDetail>(
    `${getApiBaseUrl()}/admin/newsletters/${newsletterSlug}`,
    {
      method: 'PUT',
      headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
      body: JSON.stringify(payload),
    }
  );
}

export async function deleteAdminNewsletter(
  accessToken: string,
  newsletterSlug: string
): Promise<void> {
  await requestVoid(`${getApiBaseUrl()}/admin/newsletters/${newsletterSlug}`, {
    method: 'DELETE',
    headers: withBaseHeaders(accessToken),
  });
}

export async function uploadAdminNewsletterImage(
  accessToken: string,
  file: File,
  folder?: string
): Promise<AdminNewsletterImageUploadResponse> {
  const formData = new FormData();
  formData.append('file', file);
  if (folder?.trim()) {
    formData.append('folder', folder.trim());
  }

  return requestJson<AdminNewsletterImageUploadResponse>(
    `${getApiBaseUrl()}/admin/newsletters/uploads/images`,
    {
      method: 'POST',
      headers: withBaseHeaders(accessToken),
      body: formData,
    }
  );
}
