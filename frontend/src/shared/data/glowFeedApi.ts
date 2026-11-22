import { type OffsetPage, apiRequest } from "./apiClient";

export type GlowFeedCategory = "trend" | "news" | "recent" | "local";

export type GlowFeedKind =
  | "ARTICLE"
  | "EVENT"
  | "PRODUCT_REVIEW"
  | "COMMENT"
  | "COMMUNITY_POST";

export type GlowFeedItem = {
  readonly id: string;
  readonly category: GlowFeedCategory;
  readonly kind: GlowFeedKind;
  readonly title: string;
  readonly imageUrl: string | null;
  readonly path: string;
  readonly volume: number | null;
  readonly meta: string | null;
  readonly tags: readonly string[];
  readonly publishedAt: string;
  readonly likeTargetType: string;
  readonly likeTargetKey: string;
  readonly likeCount: number;
  readonly liked: boolean;
  readonly viewerCount: number;
};

export type GlowFeedHighlightSort = "latest" | "popular";

export const GLOW_FEED_CATEGORY_LABELS: Record<GlowFeedCategory, string> = {
  trend: "트렌드",
  news: "glow정보",
  recent: "최근리뷰",
  local: "지역사회",
};

export function fetchGlowFeed(
  page: number,
  size: number,
  accessToken?: string | null,
  signal?: AbortSignal,
): Promise<OffsetPage<GlowFeedItem>> {
  return apiRequest<OffsetPage<GlowFeedItem>>("/api/glow/feed", {
    query: { page, size },
    accessToken,
    signal,
  });
}

export function fetchGlowFeedHighlights(
  sort: GlowFeedHighlightSort,
  accessToken?: string | null,
  signal?: AbortSignal,
): Promise<readonly GlowFeedItem[]> {
  return apiRequest<readonly GlowFeedItem[]>("/api/glow/feed/highlights", {
    query: { sort },
    accessToken,
    signal,
  });
}
