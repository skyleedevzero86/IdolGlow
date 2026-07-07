import { getApiBaseUrl } from "../../auth/authConfig";
import { hasNoticeBoardMarker } from "./mbrdBoardMarkers";

export { recordMbrdDocumentView } from "./publicEventsApi";

type MbrdSummary = {
  readonly documentId: string;
  readonly title: string;
  readonly author: string;
  readonly introduction: string | null;
  readonly thumbnailImageUrl: string | null;
  readonly tags: readonly string[];
  readonly status: "published" | "draft";
  readonly updatedAt: string;
};

type MbrdPageResponse = {
  readonly content: readonly MbrdSummary[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
};

type MbrdDraftResponse = {
  readonly documentId: string;
  readonly title: string;
  readonly author: string;
  readonly markdown: string;
  readonly tags: readonly string[];
  readonly urlSlug: string | null;
  readonly introduction: string | null;
  readonly thumbnailImageUrl: string | null;
  readonly status: "published" | "draft";
  readonly updatedAt: string;
};

export type PublicNoticeSummary = {
  readonly documentId: string;
  readonly title: string;
  readonly introduction: string | null;
  readonly thumbnailImageUrl: string | null;
  readonly tags: readonly string[];
  readonly updatedAt: string;
};

export type PublicNoticePageResponse = {
  readonly items: readonly PublicNoticeSummary[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
};

export type PublicNoticeDetail = {
  readonly documentId: string;
  readonly title: string;
  readonly author: string;
  readonly markdown: string;
  readonly tags: readonly string[];
  readonly introduction: string | null;
  readonly thumbnailImageUrl: string | null;
  readonly updatedAt: string;
};

const normalizeAssetUrl = (url: string | null): string | null => {
  if (!url) return null;
  if (/^https?:\/\//i.test(url)) return url;
  if (url.startsWith("/")) return `${getApiBaseUrl()}${url}`;
  return url;
};

const isHiddenSystemTag = (tag: string) => {
  const low = tag.trim().toLowerCase();
  return low === "notice-board" || low === "event-board";
};

const mapSummary = (item: MbrdSummary): PublicNoticeSummary => ({
  documentId: item.documentId,
  title: item.title,
  introduction: item.introduction,
  thumbnailImageUrl: normalizeAssetUrl(item.thumbnailImageUrl),
  tags: item.tags.filter((t) => !isHiddenSystemTag(t)),
  updatedAt: item.updatedAt,
});

/**
 * 공개 공지 목록. 서버는 태그 필터가 없으므로 published 대량 조회 후 notice-board만 남긴 뒤 페이지를 나눈다.
 */
export async function fetchPublicNotices(
  page: number,
  size = 10,
): Promise<PublicNoticePageResponse> {
  const query = new URLSearchParams({
    page: "0",
    size: "2000",
    query: "",
    status: "published",
  });
  const response = await fetch(
    `${getApiBaseUrl()}/api/mbrd/editor/documents?${query.toString()}`,
    {
      method: "GET",
      credentials: "include",
      cache: "no-store",
    },
  );
  if (!response.ok) {
    throw new Error("공지 목록을 불러오지 못했습니다.");
  }
  const body = (await response.json()) as MbrdPageResponse;
  const noticesOnly = body.content.filter(
    (item) => item.status === "published" && hasNoticeBoardMarker(item.tags),
  );
  const totalElements = noticesOnly.length;
  const safeSize = Math.max(1, size);
  const totalPages = Math.max(1, Math.ceil(totalElements / safeSize));
  const safePage = Math.min(Math.max(1, page), totalPages);
  const from = (safePage - 1) * safeSize;
  return {
    items: noticesOnly.slice(from, from + safeSize).map(mapSummary),
    page: safePage,
    size: safeSize,
    totalElements,
    totalPages,
  };
}

export async function fetchPublicNoticeDetail(
  documentId: string,
): Promise<PublicNoticeDetail> {
  const response = await fetch(
    `${getApiBaseUrl()}/api/mbrd/editor/documents/${encodeURIComponent(documentId)}`,
    {
      method: "GET",
      credentials: "include",
      cache: "no-store",
    },
  );
  if (!response.ok) {
    throw new Error("공지를 불러오지 못했습니다.");
  }
  const body = (await response.json()) as MbrdDraftResponse;
  if (body.status !== "published") {
    throw new Error("게시되지 않은 공지는 볼 수 없습니다.");
  }
  if (!hasNoticeBoardMarker(body.tags)) {
    throw new Error("공지사항 문서가 아닙니다.");
  }
  return {
    documentId: body.documentId,
    title: body.title,
    author: body.author,
    markdown: body.markdown,
    tags: body.tags.filter((t) => !isHiddenSystemTag(t)),
    introduction: body.introduction,
    thumbnailImageUrl: normalizeAssetUrl(body.thumbnailImageUrl),
    updatedAt: body.updatedAt,
  };
}
