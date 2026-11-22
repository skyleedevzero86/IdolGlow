import { apiRequest } from "./apiClient";

export type ContentLikeTargetType =
  | "ARTICLE"
  | "EVENT_INFO"
  | "PRODUCT_REVIEW"
  | "CONTENT_COMMENT"
  | "GLOW_COMMUNITY_POST";

export type ContentLikeResult = {
  readonly liked: boolean;
  readonly likeCount: number;
};

const likePath = (targetType: string, targetKey: string): string =>
  `/api/likes/${encodeURIComponent(targetType)}/${targetKey
    .split("/")
    .map((part) => encodeURIComponent(part))
    .join("/")}`;

export function setContentLiked(
  targetType: string,
  targetKey: string,
  liked: boolean,
  accessToken: string,
): Promise<ContentLikeResult> {
  return apiRequest<ContentLikeResult>(likePath(targetType, targetKey), {
    method: liked ? "POST" : "DELETE",
    accessToken,
  });
}
