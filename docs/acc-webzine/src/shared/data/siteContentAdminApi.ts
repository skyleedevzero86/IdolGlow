import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

interface ErrorBody {
  readonly message?: string;
}

const API_UNAVAILABLE_COOLDOWN_MS = 15000;
let apiUnavailableUntil = 0;

export interface AdminSiteContentImageUploadResponse {
  readonly url: string;
  readonly objectKey: string;
  readonly contentType: string;
  readonly size: number;
}

function buildSiteContentAssetUrl(objectKey: string): string {
  return `${getApiBaseUrl()}/site-content/assets?objectKey=${encodeURIComponent(objectKey)}`;
}

export function resolveSiteContentImageUrl(
  imagePath: string | null | undefined,
  objectKey?: string | null
): string | null {
  const directObjectKey = objectKey?.trim();
  if (directObjectKey) {
    return buildSiteContentAssetUrl(directObjectKey);
  }

  const raw = imagePath?.trim();
  if (!raw) {
    return null;
  }

  const normalizedApiBase = getApiBaseUrl().replace(/\/$/, '');
  if (raw.startsWith(`${normalizedApiBase}/site-content/assets`)) {
    return raw;
  }

  try {
    const parsed = new URL(raw);
    const fromWebzinePath = parsed.pathname.split('/webzine/')[1];
    if (fromWebzinePath) {
      return buildSiteContentAssetUrl(`webzine/${fromWebzinePath.replace(/^\/+/, '')}`);
    }

    const fromUploadPath = parsed.pathname.split('/uploads/webzine/')[1];
    if (fromUploadPath) {
      return buildSiteContentAssetUrl(`webzine/${fromUploadPath.replace(/^\/+/, '')}`);
    }
  } catch {
    if (raw.startsWith('/uploads/webzine/')) {
      return buildSiteContentAssetUrl(`webzine/${raw.replace('/uploads/webzine/', '')}`);
    }
  }

  return raw;
}

const withBaseHeaders = (accessToken: string, headers?: HeadersInit): HeadersInit => ({
  Authorization: `Bearer ${accessToken}`,
  'Accept-Language': acceptLanguageHeader(),
  ...headers,
});

function buildBackendUnavailableMessage(): string {
  return `백엔드 서버에 연결할 수 없습니다. API 주소(${getApiBaseUrl()})가 맞고 서버가 실행 중인지 확인해 주세요.`;
}

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ErrorBody;
    if (body.message?.trim()) {
      if (
        body.message.includes('No static resource') ||
        body.message.includes('요청한 API 경로를 찾지 못했습니다')
      ) {
        return '업로드 API를 찾지 못했습니다. 백엔드를 재시작하고 최신 코드가 반영됐는지 확인해 주세요.';
      }
      return body.message;
    }
  } catch {
    // ignore parse failure
  }

  return response.status >= 500
    ? '서버 처리 중 오류가 발생했습니다.'
    : '요청을 처리하지 못했습니다.';
}

async function requestJson<T>(input: RequestInfo | URL, init: RequestInit): Promise<T> {
  const response = await performRequest(input, init);

  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }

  return (await response.json()) as T;
}

async function requestVoid(input: RequestInfo | URL, init: RequestInit): Promise<void> {
  const response = await performRequest(input, init);

  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
}

async function performRequest(input: RequestInfo | URL, init: RequestInit): Promise<Response> {
  if (Date.now() < apiUnavailableUntil) {
    throw new Error(buildBackendUnavailableMessage());
  }

  try {
    const response = await fetch(input, { ...init, credentials: 'include' });
    apiUnavailableUntil = 0;
    return response;
  } catch {
    apiUnavailableUntil = Date.now() + API_UNAVAILABLE_COOLDOWN_MS;
    throw new Error(buildBackendUnavailableMessage());
  }
}

export interface AdminBannerItem {
  readonly bannerId: string;
  readonly domainId: string | null;
  readonly bannerName: string | null;
  readonly linkUrl: string | null;
  readonly imagePath: string | null;
  readonly imageFileName: string | null;
  readonly description: string | null;
  readonly sortOrder: number;
  readonly activeYn: string | null;
  readonly createdBy: string | null;
  readonly createdAt: string | null;
  readonly domainName: string | null;
}

export interface AdminBannerPageResponse {
  readonly items: readonly AdminBannerItem[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}

export interface BannerAdminInput {
  readonly bannerName: string;
  readonly linkUrl: string;
  readonly imagePath: string;
  readonly imageFileName: string;
  readonly description: string;
  readonly sortOrder: number;
  readonly activeYn: string;
  readonly createdBy: string;
}

export interface AdminPopupItem {
  readonly popupId: string;
  readonly domainId: string | null;
  readonly title: string | null;
  readonly fileUrl: string | null;
  readonly linkTarget: string | null;
  readonly imagePath: string | null;
  readonly imageFileName: string | null;
  readonly noticeStartDate: string | null;
  readonly noticeEndDate: string | null;
  readonly stopViewYn: string | null;
  readonly noticeYn: string | null;
  readonly createdBy: string | null;
  readonly createdAt: string | null;
  readonly updatedBy: string | null;
  readonly updatedAt: string | null;
  readonly domainName: string | null;
}

export interface AdminPopupPageResponse {
  readonly items: readonly AdminPopupItem[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}

export interface PopupAdminInput {
  readonly title: string;
  readonly fileUrl: string;
  readonly linkTarget: string;
  readonly imagePath: string;
  readonly imageFileName: string;
  readonly noticeStartDate: string;
  readonly noticeEndDate: string;
  readonly stopViewYn: string;
  readonly noticeYn: string;
  readonly createdBy: string;
  readonly updatedBy: string;
}

export interface AdminMainImageItem {
  readonly imageId: string;
  readonly domainId: string | null;
  readonly imageName: string | null;
  readonly imagePath: string | null;
  readonly imageFileName: string | null;
  readonly description: string | null;
  readonly activeYn: string | null;
  readonly createdBy: string | null;
  readonly createdAt: string | null;
  readonly domainName: string | null;
}

export interface AdminMainImagePageResponse {
  readonly items: readonly AdminMainImageItem[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}

export interface MainImageAdminInput {
  readonly imageName: string;
  readonly imagePath: string;
  readonly imageFileName: string;
  readonly description: string;
  readonly activeYn: string;
  readonly createdBy: string;
}

export interface AdminSiteContentListParams {
  readonly page?: number;
  readonly size?: number;
  readonly searchType?: string;
  readonly keyword?: string;
}

const DEFAULT_PAGE_SIZE = 6;

const buildListQueryString = (params: AdminSiteContentListParams = {}): string => {
  const search = new URLSearchParams();
  search.set('page', String(params.page ?? 1));
  search.set('size', String(params.size ?? DEFAULT_PAGE_SIZE));

  const searchType = params.searchType?.trim();
  const keyword = params.keyword?.trim();

  if (searchType) {
    search.set('searchType', searchType);
  }
  if (keyword) {
    search.set('keyword', keyword);
  }

  return search.toString();
};

async function uploadImage(
  accessToken: string,
  path: string,
  file: File
): Promise<AdminSiteContentImageUploadResponse> {
  const formData = new FormData();
  formData.append('file', file);

  return requestJson<AdminSiteContentImageUploadResponse>(`${getApiBaseUrl()}${path}`, {
    method: 'POST',
    headers: withBaseHeaders(accessToken),
    body: formData,
  });
}

export async function fetchAdminBanners(
  accessToken: string,
  params: AdminSiteContentListParams = {}
): Promise<AdminBannerPageResponse> {
  return requestJson<AdminBannerPageResponse>(
    `${getApiBaseUrl()}/admin/bnr?${buildListQueryString(params)}`,
    {
      method: 'GET',
      headers: withBaseHeaders(accessToken),
      cache: 'no-store',
    }
  );
}

export async function uploadAdminBannerImage(
  accessToken: string,
  file: File
): Promise<AdminSiteContentImageUploadResponse> {
  return uploadImage(accessToken, '/admin/bnr/uploads/images', file);
}

export async function createAdminBanner(
  accessToken: string,
  payload: BannerAdminInput
): Promise<AdminBannerItem> {
  return requestJson<AdminBannerItem>(`${getApiBaseUrl()}/admin/bnr`, {
    method: 'POST',
    headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(payload),
  });
}

export async function updateAdminBanner(
  accessToken: string,
  bannerId: string,
  payload: BannerAdminInput
): Promise<AdminBannerItem> {
  return requestJson<AdminBannerItem>(`${getApiBaseUrl()}/admin/bnr/${bannerId}`, {
    method: 'PUT',
    headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(payload),
  });
}

export async function deleteAdminBanner(accessToken: string, bannerId: string): Promise<void> {
  await requestVoid(`${getApiBaseUrl()}/admin/bnr/${bannerId}`, {
    method: 'DELETE',
    headers: withBaseHeaders(accessToken),
  });
}

export async function fetchAdminPopups(
  accessToken: string,
  params: AdminSiteContentListParams = {}
): Promise<AdminPopupPageResponse> {
  return requestJson<AdminPopupPageResponse>(
    `${getApiBaseUrl()}/admin/pup?${buildListQueryString(params)}`,
    {
      method: 'GET',
      headers: withBaseHeaders(accessToken),
      cache: 'no-store',
    }
  );
}

export async function uploadAdminPopupImage(
  accessToken: string,
  file: File
): Promise<AdminSiteContentImageUploadResponse> {
  return uploadImage(accessToken, '/admin/pup/uploads/images', file);
}

export async function createAdminPopup(
  accessToken: string,
  payload: PopupAdminInput
): Promise<AdminPopupItem> {
  return requestJson<AdminPopupItem>(`${getApiBaseUrl()}/admin/pup`, {
    method: 'POST',
    headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(payload),
  });
}

export async function updateAdminPopup(
  accessToken: string,
  popupId: string,
  payload: PopupAdminInput
): Promise<AdminPopupItem> {
  return requestJson<AdminPopupItem>(`${getApiBaseUrl()}/admin/pup/${popupId}`, {
    method: 'PUT',
    headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(payload),
  });
}

export async function deleteAdminPopup(accessToken: string, popupId: string): Promise<void> {
  await requestVoid(`${getApiBaseUrl()}/admin/pup/${popupId}`, {
    method: 'DELETE',
    headers: withBaseHeaders(accessToken),
  });
}

export async function fetchAdminMainImages(
  accessToken: string,
  params: AdminSiteContentListParams = {}
): Promise<AdminMainImagePageResponse> {
  return requestJson<AdminMainImagePageResponse>(
    `${getApiBaseUrl()}/admin/mim?${buildListQueryString(params)}`,
    {
      method: 'GET',
      headers: withBaseHeaders(accessToken),
      cache: 'no-store',
    }
  );
}

export async function uploadAdminMainImage(
  accessToken: string,
  file: File
): Promise<AdminSiteContentImageUploadResponse> {
  return uploadImage(accessToken, '/admin/mim/uploads/images', file);
}

export async function createAdminMainImage(
  accessToken: string,
  payload: MainImageAdminInput
): Promise<AdminMainImageItem> {
  return requestJson<AdminMainImageItem>(`${getApiBaseUrl()}/admin/mim`, {
    method: 'POST',
    headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(payload),
  });
}

export async function updateAdminMainImage(
  accessToken: string,
  imageId: string,
  payload: MainImageAdminInput
): Promise<AdminMainImageItem> {
  return requestJson<AdminMainImageItem>(`${getApiBaseUrl()}/admin/mim/${imageId}`, {
    method: 'PUT',
    headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(payload),
  });
}

export async function deleteAdminMainImage(accessToken: string, imageId: string): Promise<void> {
  await requestVoid(`${getApiBaseUrl()}/admin/mim/${imageId}`, {
    method: 'DELETE',
    headers: withBaseHeaders(accessToken),
  });
}
