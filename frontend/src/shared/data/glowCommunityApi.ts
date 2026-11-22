import { type OffsetPage, apiRequest } from "./apiClient";

export type GlowCommunityTag = "talk" | "question" | "travel";
export type GlowCommunityTagFilter = "all" | GlowCommunityTag;
export type GlowCommunitySort = "latest" | "popular";

export const GLOW_COMMUNITY_TAGS: readonly GlowCommunityTag[] = [
  "talk",
  "question",
  "travel",
];

export const GLOW_COMMUNITY_TAG_LABELS: Record<GlowCommunityTag, string> = {
  talk: "토크",
  question: "질문",
  travel: "나의 여행계획",
};

export type GlowCommunityPost = {
  readonly id: number;
  readonly tag: string;
  readonly tagLabel: string;
  readonly title: string;
  readonly content: string;
  readonly authorName: string;
  readonly authorId: number | null;
  readonly createdAt: string | null;
  readonly updatedAt: string | null;
  readonly likeCount: number;
  readonly commentCount: number;
  readonly liked: boolean;
  readonly likedByMe: boolean;
  readonly mine: boolean;
  readonly blinded: boolean;
  readonly blindReason: string | null;
};

export type GlowCommunityPageResponse = OffsetPage<GlowCommunityPost>;

export type GlowCommunityKeywords = {
  readonly keywords: readonly string[];
  readonly source: string;
  readonly generatedAt: string | null;
};

export type GlowCommunityLikeResult = {
  readonly liked: boolean;
  readonly likeCount: number;
};

export type FetchGlowCommunityPostsParams = {
  readonly tag: GlowCommunityTagFilter;
  readonly q: string;
  readonly sort: GlowCommunitySort;
  readonly page: number;
  readonly size: number;
  readonly accessToken?: string | null;
  readonly signal?: AbortSignal;
};

export type GlowCommunityPostInput = {
  readonly tag: GlowCommunityTag;
  readonly title: string;
  readonly content: string;
};

export const glowCommunityPostPath = (postId: number): string =>
  `/glow?tab=local&post=${postId}`;

export const glowCommunityTagLabel = (
  post: Pick<GlowCommunityPost, "tag" | "tagLabel">,
): string =>
  post.tagLabel ||
  GLOW_COMMUNITY_TAG_LABELS[post.tag as GlowCommunityTag] ||
  post.tag;

export const isGlowCommunityTag = (value: string): value is GlowCommunityTag =>
  (GLOW_COMMUNITY_TAGS as readonly string[]).includes(value);

export function fetchGlowCommunityPosts(
  params: FetchGlowCommunityPostsParams,
): Promise<GlowCommunityPageResponse> {
  return apiRequest<GlowCommunityPageResponse>("/api/glow-community/posts", {
    accessToken: params.accessToken,
    signal: params.signal,
    query: {
      tag: params.tag,
      sort: params.sort,
      page: params.page,
      size: params.size,
      q: params.q.trim(),
    },
  });
}

export function fetchGlowCommunityPost(
  postId: number,
  accessToken?: string | null,
): Promise<GlowCommunityPost> {
  return apiRequest<GlowCommunityPost>(`/api/glow-community/posts/${postId}`, {
    accessToken,
  });
}

export function fetchGlowCommunityHighlights(
  accessToken?: string | null,
): Promise<readonly GlowCommunityPost[]> {
  return apiRequest<readonly GlowCommunityPost[]>(
    "/api/glow-community/highlights",
    { accessToken },
  );
}

export function fetchGlowCommunityKeywords(): Promise<GlowCommunityKeywords> {
  return apiRequest<GlowCommunityKeywords>("/api/glow-community/keywords");
}

export function fetchMyGlowCommunityPosts(
  accessToken: string,
  page: number,
  size: number,
): Promise<GlowCommunityPageResponse> {
  return apiRequest<GlowCommunityPageResponse>("/api/glow-community/me/posts", {
    accessToken,
    query: { page, size },
  });
}

export function createGlowCommunityPost(
  input: GlowCommunityPostInput,
  accessToken: string,
): Promise<GlowCommunityPost> {
  return apiRequest<GlowCommunityPost>("/api/glow-community/posts", {
    method: "POST",
    accessToken,
    json: input,
  });
}

export function updateGlowCommunityPost(
  postId: number,
  input: GlowCommunityPostInput,
  accessToken: string,
): Promise<GlowCommunityPost> {
  return apiRequest<GlowCommunityPost>(`/api/glow-community/posts/${postId}`, {
    method: "PUT",
    accessToken,
    json: input,
  });
}

export function deleteGlowCommunityPost(
  postId: number,
  accessToken: string,
): Promise<void> {
  return apiRequest<void>(`/api/glow-community/posts/${postId}`, {
    method: "DELETE",
    accessToken,
  });
}

export function toggleGlowCommunityLike(
  postId: number,
  accessToken: string,
): Promise<GlowCommunityLikeResult> {
  return apiRequest<GlowCommunityLikeResult>(
    `/api/glow-community/posts/${postId}/like`,
    { method: "POST", accessToken },
  );
}

export function reportGlowCommunityPost(
  postId: number,
  accessToken: string,
  reason?: string,
): Promise<void> {
  return apiRequest<void>(`/api/glow-community/posts/${postId}/report`, {
    method: "POST",
    accessToken,
    json: { reason: reason?.trim() || null },
  });
}

export type GlowCommunityReportReason = {
  readonly reason: string | null;
  readonly reportedAt: string | null;
};

export type GlowCommunityReportedPost = {
  readonly postId: number;
  readonly tag: string;
  readonly title: string;
  readonly authorId: number;
  readonly authorName: string;
  readonly reportCount: number;
  readonly lastReportedAt: string | null;
  readonly blinded: boolean;
  readonly blindedAt: string | null;
  readonly blindReason: string | null;
  readonly createdAt: string | null;
  readonly reports: readonly GlowCommunityReportReason[];
};

export function fetchGlowCommunityReports(
  accessToken: string,
  page: number,
  size: number,
): Promise<OffsetPage<GlowCommunityReportedPost>> {
  return apiRequest<OffsetPage<GlowCommunityReportedPost>>(
    "/api/admin/glow-community/reports",
    { accessToken, query: { page, size } },
  );
}

export function blindGlowCommunityPost(
  postId: number,
  accessToken: string,
  reason?: string,
): Promise<GlowCommunityReportedPost> {
  return apiRequest<GlowCommunityReportedPost>(
    `/api/admin/glow-community/posts/${postId}/blind`,
    { method: "POST", accessToken, json: { reason: reason?.trim() || null } },
  );
}

export function unblindGlowCommunityPost(
  postId: number,
  accessToken: string,
): Promise<GlowCommunityReportedPost> {
  return apiRequest<GlowCommunityReportedPost>(
    `/api/admin/glow-community/posts/${postId}/unblind`,
    { method: "POST", accessToken },
  );
}
