import { getApiBaseUrl } from '../../auth/authConfig';

const SITE_HOME_CONTENT_CACHE_KEY = 'idolglow:site-home-content';

function buildSiteContentAssetUrl(objectKey: string): string {
  return `${getApiBaseUrl()}/site-content/assets?objectKey=${encodeURIComponent(objectKey)}`;
}

export function resolveSiteContentImageUrl(imagePath: string | null | undefined): string | null {
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

export interface HomeHeroSlide {
  readonly imageId: string;
  readonly title: string;
  readonly subtitle: string | null;
  readonly imageUrl: string;
  readonly linkUrl: string;
  readonly categoryLabel: string;
}

export interface HomeBannerCard {
  readonly bannerId: string;
  readonly title: string;
  readonly description: string | null;
  readonly imageUrl: string;
  readonly linkUrl: string;
}

export interface HomePopup {
  readonly popupId: string;
  readonly title: string;
  readonly imageUrl: string | null;
  readonly linkUrl: string | null;
  readonly linkTarget: string;
  readonly noticeStartDate: string | null;
  readonly noticeEndDate: string | null;
  readonly stopViewYn: string | null;
}

export interface SiteHomeContentResponse {
  readonly heroSlides: readonly HomeHeroSlide[];
  readonly banners: readonly HomeBannerCard[];
  readonly popups: readonly HomePopup[];
}

function isSiteHomeContentResponse(value: unknown): value is SiteHomeContentResponse {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const target = value as Record<string, unknown>;
  return (
    Array.isArray(target.heroSlides) &&
    Array.isArray(target.banners) &&
    Array.isArray(target.popups)
  );
}

export function readCachedSiteHomeContent(): SiteHomeContentResponse | null {
  if (typeof window === 'undefined') {
    return null;
  }

  try {
    const raw = window.localStorage.getItem(SITE_HOME_CONTENT_CACHE_KEY);
    if (!raw) {
      return null;
    }

    const parsed = JSON.parse(raw) as unknown;
    return isSiteHomeContentResponse(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

function writeCachedSiteHomeContent(content: SiteHomeContentResponse) {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.setItem(SITE_HOME_CONTENT_CACHE_KEY, JSON.stringify(content));
}

export async function fetchSiteHomeContent(): Promise<SiteHomeContentResponse> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/site-content/home`, {
      method: 'GET',
      credentials: 'include',
      cache: 'no-store',
    });
  } catch {
    throw new Error('홈 콘텐츠 서버에 연결할 수 없습니다. 백엔드 실행 상태를 확인해 주세요.');
  }

  if (!response.ok) {
    if (response.status === 500) {
      throw new Error(
        '홈 API가 서버 오류(500)를 반환했습니다. 백엔드 콘솔 로그를 보고, PostgreSQL/MySQL에 Flyway V2(통합 마이그레이션)가 적용됐는지 확인해 주세요.',
      );
    }
    throw new Error('홈 화면 콘텐츠를 불러오지 못했습니다.');
  }

  const content = (await response.json()) as SiteHomeContentResponse;
  writeCachedSiteHomeContent(content);
  return content;
}
