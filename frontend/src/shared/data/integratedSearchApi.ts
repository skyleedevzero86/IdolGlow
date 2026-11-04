import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';
import { mockArticles } from './mockArticles';
import { hasNoticeBoardMarker } from './mbrdBoardMarkers';
import { fetchSiteHomeContent } from './siteContentApi';

export type IntegratedSearchHit = {
  readonly id: string;
  readonly title: string;
  readonly snippet: string | null;
  readonly pathLabel: string;
  readonly href: string;
  readonly sourceLabel: string | null;
  readonly dateLabel: string | null;
};

export type IntegratedSearchSection = {
  readonly id: 'menu' | 'article' | 'notice' | 'event' | 'product' | 'content';
  readonly title: string;
  readonly totalCount: number;
  readonly items: readonly IntegratedSearchHit[];
  readonly moreHref: string | null;
  readonly emptyActionLabel: string | null;
  readonly emptyActionHref: string | null;
};

export type IntegratedSearchResult = {
  readonly keyword: string;
  readonly totalCount: number;
  readonly sections: readonly IntegratedSearchSection[];
};

type MbrdSummary = {
  readonly documentId: string;
  readonly title: string;
  readonly author: string;
  readonly introduction: string | null;
  readonly tags: readonly string[];
  readonly status: 'published' | 'draft';
  readonly updatedAt: string;
};

type MbrdPageResponse = {
  readonly content: readonly MbrdSummary[];
  readonly totalElements: number;
};

type ProductBrowseItem = {
  readonly id: number;
  readonly name: string;
  readonly description: string;
  readonly tagNames?: readonly string[];
  readonly location?: string | null;
};

type ProductBrowseResponse = {
  readonly items: readonly ProductBrowseItem[];
};

const PREVIEW_SIZE = 5;

const MENU_ENTRIES: ReadonlyArray<{ readonly pathLabel: string; readonly href: string; readonly keywords: string }> = [
  { pathLabel: 'IDOL GLOW > GLOW', href: '/glow', keywords: 'glow 추천' },
  { pathLabel: 'IDOL GLOW > WISH', href: '/wish', keywords: 'wish 위시' },
  { pathLabel: 'IDOL GLOW > 이벤트', href: '/events', keywords: '이벤트 event' },
  { pathLabel: 'IDOL GLOW > 아카이브', href: '/archive', keywords: '아카이브 archive' },
  { pathLabel: 'IDOL GLOW > 공지', href: '/notices', keywords: '공지 공지사항 notice' },
  { pathLabel: 'IDOL GLOW > 아티클', href: '/articles', keywords: '아티클 기사 article' },
  { pathLabel: 'IDOL GLOW > Glow 알림', href: '/glow-alerts', keywords: '알림 glow alert' },
  { pathLabel: '마이페이지 > 내 정보', href: '/mypage/userInfo', keywords: '마이페이지 내정보 프로필' },
  { pathLabel: '마이페이지 > 설문', href: '/my-survey', keywords: '설문 survey' },
  { pathLabel: '마이페이지 > 환율', href: '/exchange-rate', keywords: '환율 exchange' },
  { pathLabel: '마이페이지 > 지하철', href: '/subway', keywords: '지하철 subway' },
  { pathLabel: '마이페이지 > 행사정보', href: '/event-info', keywords: '행사정보 festival' },
  { pathLabel: '마이페이지 > 날씨', href: '/glow-weather', keywords: '날씨 weather' },
  { pathLabel: '마이페이지 > 지도', href: '/glow_map', keywords: '지도 map' },
];

function includesKeyword(haystack: string, keyword: string): boolean {
  return haystack.toLowerCase().includes(keyword.toLowerCase());
}

function formatDate(value: string | null | undefined): string | null {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value.slice(0, 10);
  }
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

async function fetchMbrdDocuments(keyword: string, size: number): Promise<readonly MbrdSummary[]> {
  const query = new URLSearchParams({
    page: '0',
    size: String(size),
    query: keyword,
    status: 'published',
  });
  const response = await fetch(`${getApiBaseUrl()}/api/mbrd/editor/documents?${query.toString()}`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: { 'Accept-Language': acceptLanguageHeader() },
  });
  if (!response.ok) {
    return [];
  }
  const body = (await response.json()) as MbrdPageResponse;
  return body.content.filter(item => item.status === 'published');
}

async function fetchProductsByKeyword(keyword: string, size: number): Promise<readonly ProductBrowseItem[]> {
  const query = new URLSearchParams({
    size: String(size),
    keyword,
  });
  const response = await fetch(`${getApiBaseUrl()}/products?${query.toString()}`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: { 'Accept-Language': acceptLanguageHeader() },
  });
  if (!response.ok) {
    return [];
  }
  const body = (await response.json()) as ProductBrowseResponse;
  return body.items ?? [];
}

function searchMenus(keyword: string): IntegratedSearchHit[] {
  return MENU_ENTRIES.filter(entry =>
    includesKeyword(`${entry.pathLabel} ${entry.keywords}`, keyword),
  ).map(entry => ({
    id: entry.href,
    title: entry.pathLabel,
    snippet: null,
    pathLabel: entry.pathLabel,
    href: entry.href,
    sourceLabel: null,
    dateLabel: null,
  }));
}

function searchArticles(keyword: string): IntegratedSearchHit[] {
  return mockArticles
    .filter(article =>
      includesKeyword(
        `${article.title} ${article.excerpt} ${article.category} ${article.tags.join(' ')}`,
        keyword,
      ),
    )
    .map(article => ({
      id: article.id,
      title: article.title,
      snippet: article.excerpt,
      pathLabel: `아티클 > ${article.category}`,
      href: `/articles/${article.id}`,
      sourceLabel: article.authorName,
      dateLabel: article.publishDate,
    }));
}

async function searchContents(keyword: string): Promise<IntegratedSearchHit[]> {
  try {
    const home = await fetchSiteHomeContent();
    const slides = home.heroSlides
      .filter(item => includesKeyword(`${item.title} ${item.subtitle ?? ''} ${item.categoryLabel}`, keyword))
      .map(item => ({
        id: `slide-${item.imageId}`,
        title: item.title,
        snippet: item.subtitle,
        pathLabel: `홈 > ${item.categoryLabel || '히어로'}`,
        href: item.linkUrl || '/',
        sourceLabel: null,
        dateLabel: null,
      }));
    const banners = home.banners
      .filter(item => includesKeyword(`${item.title} ${item.description ?? ''}`, keyword))
      .map(item => ({
        id: `banner-${item.bannerId}`,
        title: item.title,
        snippet: item.description,
        pathLabel: '홈 > 배너',
        href: item.linkUrl || '/',
        sourceLabel: null,
        dateLabel: null,
      }));
    return [...slides, ...banners];
  } catch {
    return [];
  }
}

export async function fetchIntegratedSearch(keywordRaw: string): Promise<IntegratedSearchResult> {
  const keyword = keywordRaw.trim();
  if (!keyword) {
    return { keyword: '', totalCount: 0, sections: [] };
  }

  const [mbrdDocs, products, contents] = await Promise.all([
    fetchMbrdDocuments(keyword, 50),
    fetchProductsByKeyword(keyword, 20),
    searchContents(keyword),
  ]);

  const menus = searchMenus(keyword);
  const articles = searchArticles(keyword);
  const notices = mbrdDocs
    .filter(item => hasNoticeBoardMarker(item.tags))
    .map(item => ({
      id: item.documentId,
      title: item.title,
      snippet: item.introduction,
      pathLabel: '공지',
      href: `/notices/${item.documentId}`,
      sourceLabel: item.author || '공지',
      dateLabel: formatDate(item.updatedAt),
    }));
  const events = mbrdDocs
    .filter(item => !hasNoticeBoardMarker(item.tags))
    .map(item => ({
      id: item.documentId,
      title: item.title,
      snippet: item.introduction,
      pathLabel: '이벤트',
      href: `/events/${item.documentId}`,
      sourceLabel: item.author || '이벤트',
      dateLabel: formatDate(item.updatedAt),
    }));
  const productHits = products.map(item => ({
    id: String(item.id),
    title: item.name,
    snippet: item.description,
    pathLabel: item.location?.trim() || '상품',
    href: `/products/${item.id}`,
    sourceLabel: '상품',
    dateLabel: null,
  }));

  const sections: IntegratedSearchSection[] = [
    {
      id: 'menu',
      title: '메뉴',
      totalCount: menus.length,
      items: menus.slice(0, 50),
      moreHref: menus.length > PREVIEW_SIZE ? `/search?q=${encodeURIComponent(keyword)}&section=menu` : null,
      emptyActionLabel: null,
      emptyActionHref: null,
    },
    {
      id: 'article',
      title: '아티클',
      totalCount: articles.length,
      items: articles.slice(0, 50),
      moreHref: `/articles?q=${encodeURIComponent(keyword)}`,
      emptyActionLabel: null,
      emptyActionHref: null,
    },
    {
      id: 'notice',
      title: '공지',
      totalCount: notices.length,
      items: notices.slice(0, 50),
      moreHref: notices.length > 0 ? `/notices` : null,
      emptyActionLabel: notices.length === 0 ? '공지를 검색하시려면 클릭하세요' : null,
      emptyActionHref: notices.length === 0 ? '/notices' : null,
    },
    {
      id: 'event',
      title: '이벤트',
      totalCount: events.length,
      items: events.slice(0, 50),
      moreHref: `/events`,
      emptyActionLabel: null,
      emptyActionHref: null,
    },
    {
      id: 'product',
      title: '상품',
      totalCount: productHits.length,
      items: productHits.slice(0, 50),
      moreHref: productHits.length > 0 ? `/glow` : null,
      emptyActionLabel: null,
      emptyActionHref: null,
    },
    {
      id: 'content',
      title: '콘텐츠',
      totalCount: contents.length,
      items: contents.slice(0, 50),
      moreHref: contents.length > 0 ? `/` : null,
      emptyActionLabel: null,
      emptyActionHref: null,
    },
  ];

  const totalCount = sections.reduce((sum, section) => sum + section.totalCount, 0);
  return { keyword, totalCount, sections };
}

export function highlightKeyword(text: string, keyword: string): string {
  if (!text || !keyword.trim()) {
    return text;
  }
  const escaped = keyword.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return text.replace(new RegExp(`(${escaped})`, 'gi'), '<mark>$1</mark>');
}
