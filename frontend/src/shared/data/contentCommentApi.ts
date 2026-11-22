import { type OffsetPage, apiRequest } from "./apiClient";

export type ContentTargetType =
  | "ARTICLE"
  | "PRODUCT"
  | "GLOW"
  | "GLOW_COMMUNITY_POST"
  | "EVENT_INFO"
  | "ARCHIVE"
  | "SUBWAY"
  | "AIRPORT_CROWD"
  | "EXCHANGE_RATE"
  | "GLOW_WEATHER"
  | "GLOW_MAP"
  | "OTHER";

export const CONTENT_TARGET_TYPE_LABELS: Record<ContentTargetType, string> = {
  ARTICLE: "아티클",
  PRODUCT: "상품",
  GLOW: "GLOW",
  GLOW_COMMUNITY_POST: "지역사회",
  EVENT_INFO: "행사정보",
  ARCHIVE: "아카이브",
  SUBWAY: "지하철",
  AIRPORT_CROWD: "공항인파",
  EXCHANGE_RATE: "환율",
  GLOW_WEATHER: "날씨",
  GLOW_MAP: "지도",
  OTHER: "기타",
};

export const contentTargetTypeLabel = (value: string): string =>
  CONTENT_TARGET_TYPE_LABELS[value as ContentTargetType] ?? value;

export const COMMENT_MAX_DEPTH = 5;

export type ContentComment = {
  readonly id: number;
  readonly targetType: string;
  readonly targetKey: string;
  readonly parentId: number | null;
  readonly depth: number;
  readonly contentMarkdown: string;
  readonly authorName: string | null;
  readonly authorId: number | null;
  readonly createdAt: string | null;
  readonly updatedAt: string | null;
  readonly mine: boolean;
  readonly deleted: boolean;
  readonly replies: readonly ContentComment[];
};

export type MyReview = {
  readonly commentId: number;
  readonly targetType: string;
  readonly targetKey: string;
  readonly targetTitle: string;
  readonly targetPath: string | null;
  readonly contentMarkdown: string;
  readonly excerpt: string;
  readonly parentId: number | null;
  readonly parentExcerpt: string | null;
  readonly depth: number;
  readonly createdAt: string | null;
  readonly updatedAt: string | null;
};

export type ContentCommentTargetRef = {
  readonly targetType: ContentTargetType;
  readonly targetKey: string;
};

export type CreateContentCommentInput = ContentCommentTargetRef & {
  readonly targetTitle?: string | null;
  readonly targetPath?: string | null;
  readonly contentMarkdown: string;
  readonly parentId?: number | null;
};

export function fetchContentComments(
  target: ContentCommentTargetRef,
  page: number,
  size: number,
  accessToken?: string | null,
): Promise<OffsetPage<ContentComment>> {
  return apiRequest<OffsetPage<ContentComment>>("/api/comments", {
    accessToken,
    query: {
      targetType: target.targetType,
      targetKey: target.targetKey,
      page,
      size,
      sort: "oldest",
    },
  });
}

export function createContentComment(
  input: CreateContentCommentInput,
  accessToken: string,
): Promise<ContentComment> {
  return apiRequest<ContentComment>("/api/comments", {
    method: "POST",
    accessToken,
    json: {
      targetType: input.targetType,
      targetKey: input.targetKey,
      targetTitle: input.targetTitle?.slice(0, 300) || null,
      targetPath: input.targetPath || null,
      contentMarkdown: input.contentMarkdown,
      parentId: input.parentId ?? null,
    },
  });
}

export function updateContentComment(
  commentId: number,
  contentMarkdown: string,
  accessToken: string,
): Promise<ContentComment> {
  return apiRequest<ContentComment>(`/api/comments/${commentId}`, {
    method: "PUT",
    accessToken,
    json: { contentMarkdown },
  });
}

export function deleteContentComment(
  commentId: number,
  accessToken: string,
): Promise<void> {
  return apiRequest<void>(`/api/comments/${commentId}`, {
    method: "DELETE",
    accessToken,
  });
}

export async function uploadCommentImage(
  file: File,
  accessToken: string,
): Promise<string> {
  const formData = new FormData();
  formData.append("file", file);
  const result = await apiRequest<{ url: string }>("/api/comments/images", {
    method: "POST",
    accessToken,
    formData,
  });
  return result.url;
}

export function fetchMyContentReviews(
  accessToken: string,
  page: number,
  size: number,
): Promise<OffsetPage<MyReview>> {
  return apiRequest<OffsetPage<MyReview>>("/me/reviews", {
    accessToken,
    query: { page, size },
  });
}
