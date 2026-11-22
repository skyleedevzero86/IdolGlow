import { ApiError, type OffsetPage, apiRequest } from "./apiClient";
import type { ContentTargetType } from "./contentCommentApi";

export type ContentView = {
  readonly id: number;
  readonly contentType: string;
  readonly contentKey: string;
  readonly title: string;
  readonly path: string;
  readonly imageUrl?: string | null;
  readonly viewedAt: string;
};

export type RecordContentViewInput = {
  readonly contentType: ContentTargetType;
  readonly contentKey: string;
  readonly title?: string | null;
  readonly path: string;
  readonly imageUrl?: string | null;
};

export async function recordContentView(
  input: RecordContentViewInput,
  accessToken: string,
): Promise<ContentView> {
  const body = {
    contentType: input.contentType,
    contentKey: input.contentKey.slice(0, 200),
    title: input.title?.slice(0, 300) || null,
    path: input.path.slice(0, 500),
  };
  const imageUrl = input.imageUrl?.trim();
  if (!imageUrl) {
    return apiRequest<ContentView>("/me/content-views", {
      method: "POST",
      accessToken,
      json: body,
    });
  }
  try {
    return await apiRequest<ContentView>("/me/content-views", {
      method: "POST",
      accessToken,
      json: { ...body, imageUrl: imageUrl.slice(0, 1000) },
    });
  } catch (error) {
    if (error instanceof ApiError && error.status === 400) {
      return apiRequest<ContentView>("/me/content-views", {
        method: "POST",
        accessToken,
        json: body,
      });
    }
    throw error;
  }
}

export function fetchMyContentViews(
  accessToken: string,
  page: number,
  size: number,
): Promise<OffsetPage<ContentView>> {
  return apiRequest<OffsetPage<ContentView>>("/me/content-views", {
    accessToken,
    query: { page, size },
  });
}

export function deleteContentView(
  viewId: number,
  accessToken: string,
): Promise<void> {
  return apiRequest<void>(`/me/content-views/${viewId}`, {
    method: "DELETE",
    accessToken,
  });
}

export function clearContentViews(accessToken: string): Promise<void> {
  return apiRequest<void>("/me/content-views", {
    method: "DELETE",
    accessToken,
  });
}
