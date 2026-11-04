import { getApiBaseUrl } from "../../auth/authConfig";
import { hasNoticeBoardMarker } from "./mbrdBoardMarkers";

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

export type PublicEventSummary = {
  readonly documentId: string;
  readonly title: string;
  readonly introduction: string | null;
  readonly thumbnailImageUrl: string | null;
  readonly tags: readonly string[];
  readonly updatedAt: string;
  readonly startDate: string | null;
  readonly endDate: string | null;
};

export type PublicEventPageResponse = {
  readonly items: readonly PublicEventSummary[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
};

export type PublicEventDetail = {
  readonly documentId: string;
  readonly title: string;
  readonly markdown: string;
  readonly tags: readonly string[];
  readonly introduction: string | null;
  readonly thumbnailImageUrl: string | null;
  readonly updatedAt: string;
  readonly startDate: string | null;
  readonly endDate: string | null;
};

const META_START_PREFIX = "event-start:";
const META_END_PREFIX = "event-end:";

const normalizeAssetUrl = (url: string | null): string | null => {
  if (!url) return null;
  if (/^https?:\/\//i.test(url)) return url;
  if (url.startsWith("/")) return `${getApiBaseUrl()}${url}`;
  return url;
};

const extractDateMeta = (tags: readonly string[]) => {
  let startDate: string | null = null;
  let endDate: string | null = null;
  const visibleTags: string[] = [];
  for (const tag of tags) {
    if (tag.startsWith(META_START_PREFIX)) {
      startDate = tag.slice(META_START_PREFIX.length);
      continue;
    }
    if (tag.startsWith(META_END_PREFIX)) {
      endDate = tag.slice(META_END_PREFIX.length);
      continue;
    }
    visibleTags.push(tag);
  }
  return { startDate, endDate, visibleTags };
};

const mapSummary = (item: MbrdSummary): PublicEventSummary => {
  const { startDate, endDate, visibleTags } = extractDateMeta(item.tags);
  return {
    documentId: item.documentId,
    title: item.title,
    introduction: item.introduction,
    thumbnailImageUrl: normalizeAssetUrl(item.thumbnailImageUrl),
    tags: visibleTags,
    updatedAt: item.updatedAt,
    startDate,
    endDate,
  };
};

const LATEST_EVENT_PAGE_SIZE = 50;
const LATEST_EVENT_MAX_PAGES = 4;

export async function fetchLatestPublicEventDocumentId(): Promise<string | null> {
  for (let page = 0; page < LATEST_EVENT_MAX_PAGES; page += 1) {
    const body = await fetchPublishedDocumentPage(page, LATEST_EVENT_PAGE_SIZE);
    const latest = body.content.find(
      (item) => item.status === "published" && !hasNoticeBoardMarker(item.tags),
    );
    if (latest) {
      return latest.documentId;
    }
    if (body.content.length < LATEST_EVENT_PAGE_SIZE) {
      return null;
    }
  }
  return null;
}

async function fetchPublishedDocumentPage(page: number, size: number): Promise<MbrdPageResponse> {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
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
    throw new Error("이벤트 목록을 불러오지 못했습니다.");
  }
  return (await response.json()) as MbrdPageResponse;
}

export async function fetchPublicEvents(
  page: number,
  size = 10,
): Promise<PublicEventPageResponse> {
  const body = await fetchPublishedDocumentPage(Math.max(0, page - 1), size);
  const publishedOnly = body.content.filter(
    (item) => item.status === "published" && !hasNoticeBoardMarker(item.tags),
  );
  return {
    items: publishedOnly.map(mapSummary),
    page: body.page + 1,
    size: body.size,
    totalElements: publishedOnly.length,
    totalPages: Math.max(1, body.totalPages),
  };
}

export async function recordMbrdDocumentView(
  documentId: string,
): Promise<void> {
  const response = await fetch(
    `${getApiBaseUrl()}/api/mbrd/editor/documents/${encodeURIComponent(documentId)}/view`,
    {
      method: "POST",
      credentials: "include",
      cache: "no-store",
    },
  );
  if (!response.ok) {
    return;
  }
}

export async function fetchPublicEventDetail(
  documentId: string,
): Promise<PublicEventDetail> {
  const response = await fetch(
    `${getApiBaseUrl()}/api/mbrd/editor/documents/${encodeURIComponent(documentId)}`,
    {
      method: "GET",
      credentials: "include",
      cache: "no-store",
    },
  );
  if (!response.ok) {
    throw new Error("이벤트 상세를 불러오지 못했습니다.");
  }
  const body = (await response.json()) as MbrdDraftResponse;
  if (body.status !== "published") {
    throw new Error("임시저장 이벤트는 공개 페이지에서 볼 수 없습니다.");
  }
  const { startDate, endDate, visibleTags } = extractDateMeta(body.tags);
  return {
    documentId: body.documentId,
    title: body.title,
    markdown: body.markdown,
    tags: visibleTags,
    introduction: body.introduction,
    thumbnailImageUrl: normalizeAssetUrl(body.thumbnailImageUrl),
    updatedAt: body.updatedAt,
    startDate,
    endDate,
  };
}
