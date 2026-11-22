import { type OffsetPage, apiRequest } from "./apiClient";

export type IdolglowHeadline = {
  readonly slug: string;
  readonly order: number;
  readonly title: string;
  readonly category: string;
  readonly categoryLabel: string;
};

export type IdolglowIssueSummary = {
  readonly slug: string;
  readonly volume: number;
  readonly issueDate: string;
  readonly issueYear: number;
  readonly issueMonth: number;
  readonly coverImageUrl: string;
  readonly teaser: string;
  readonly articleCount: number;
  readonly leadArticleSlug: string | null;
  readonly articles: readonly IdolglowHeadline[];
};

export type IdolglowArticleCard = {
  readonly slug: string;
  readonly order: number;
  readonly title: string;
  readonly kicker: string;
  readonly summary: string;
  readonly cardImageUrl: string;
  readonly category: string;
  readonly categoryLabel: string;
  readonly formatLabel: string;
};

export type IdolglowIssueDetail = {
  readonly slug: string;
  readonly volume: number;
  readonly issueDate: string;
  readonly issueYear: number;
  readonly issueMonth: number;
  readonly coverImageUrl: string;
  readonly teaser: string;
  readonly articleCount: number;
  readonly articles: readonly IdolglowArticleCard[];
};

export type IdolglowArticleSection = {
  readonly order: number;
  readonly heading: string | null;
  readonly paragraphs: readonly string[];
  readonly note: string | null;
};

export type IdolglowArticleDetail = {
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
  readonly category: string;
  readonly categoryLabel: string;
  readonly formatLabel: string;
  readonly tags: readonly string[];
  readonly authorName: string;
  readonly creditLine: string;
  readonly highlightQuote: string | null;
  readonly sections: readonly IdolglowArticleSection[];
  readonly previousArticle: IdolglowHeadline | null;
  readonly nextArticle: IdolglowHeadline | null;
  readonly relatedArticles: readonly IdolglowArticleCard[];
};

export type IdolglowVolumeOption = {
  readonly slug: string;
  readonly volume: number;
  readonly year: number;
  readonly month: number;
};

export type IdolglowIssueFilters = {
  readonly latestVolume: number | null;
  readonly latestIssueSlug: string | null;
  readonly years: readonly number[];
  readonly months: Readonly<Record<string, readonly number[]>>;
  readonly volumes: readonly IdolglowVolumeOption[];
};

export type IdolglowIssueQuery = {
  readonly page: number;
  readonly size: number;
  readonly year?: number | null;
  readonly month?: number | null;
  readonly volume?: number | null;
  readonly keyword?: string | null;
};

const BASE_PATH = "/api/idolglow/issues";

const segment = (value: string): string => encodeURIComponent(value);

export const idolglowIssuePath = (issueSlug: string): string =>
  `/glow?tab=trend&issue=${segment(issueSlug)}`;

export const idolglowArticlePath = (
  issueSlug: string,
  articleSlug: string,
): string =>
  `/glow?tab=trend&issue=${segment(issueSlug)}&article=${segment(articleSlug)}`;

export const idolglowArticleKey = (
  issueSlug: string,
  articleSlug: string,
): string => `${issueSlug}/${articleSlug}`;

export function fetchIdolglowIssues(
  query: IdolglowIssueQuery,
  signal?: AbortSignal,
): Promise<OffsetPage<IdolglowIssueSummary>> {
  return apiRequest<OffsetPage<IdolglowIssueSummary>>(BASE_PATH, {
    query: {
      page: query.page,
      size: query.size,
      year: query.year,
      month: query.month,
      volume: query.volume,
      keyword: query.keyword?.trim() || null,
    },
    signal,
  });
}

export function fetchIdolglowIssueFilters(
  signal?: AbortSignal,
): Promise<IdolglowIssueFilters> {
  return apiRequest<IdolglowIssueFilters>(`${BASE_PATH}/filters`, { signal });
}

export function fetchIdolglowIssue(
  issueSlug: string,
  signal?: AbortSignal,
): Promise<IdolglowIssueDetail> {
  return apiRequest<IdolglowIssueDetail>(`${BASE_PATH}/${segment(issueSlug)}`, {
    signal,
  });
}

export function fetchIdolglowArticle(
  issueSlug: string,
  articleSlug: string,
  signal?: AbortSignal,
): Promise<IdolglowArticleDetail> {
  return apiRequest<IdolglowArticleDetail>(
    `${BASE_PATH}/${segment(issueSlug)}/articles/${segment(articleSlug)}`,
    { signal },
  );
}

export type IdolglowArticleSort = "latest" | "popular" | "likes";

export type IdolglowRankedArticle = {
  readonly issueSlug: string;
  readonly volume: number;
  readonly issueDate: string;
  readonly articleSlug: string;
  readonly title: string;
  readonly summary: string;
  readonly cardImageUrl: string;
  readonly category: string;
  readonly categoryLabel: string;
  readonly tags: readonly string[];
  readonly likeTargetType: string;
  readonly likeTargetKey: string;
  readonly likeCount: number;
  readonly viewerCount: number;
  readonly liked: boolean;
};

export function fetchIdolglowArticleCards(
  sort: IdolglowArticleSort,
  page: number,
  size: number,
  accessToken?: string | null,
  signal?: AbortSignal,
): Promise<OffsetPage<IdolglowRankedArticle>> {
  return apiRequest<OffsetPage<IdolglowRankedArticle>>(
    "/api/idolglow/articles",
    {
      query: { sort, page, size },
      accessToken,
      signal,
    },
  );
}
