import { getApiBaseUrl } from "../../auth/authConfig";
import { hasNoticeBoardMarker } from "./mbrdBoardMarkers";
import {
  type NoticeAttachment,
  firstNoticeImageUrl,
  parseNoticeAttachments,
} from "./noticeFiles";

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
  readonly viewCount?: number;
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
  readonly viewCount: number;
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
  readonly attachments: readonly NoticeAttachment[];
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

const noticeThumbnail = (
  thumbnailImageUrl: string | null,
  introduction: string | null,
): string | null =>
  normalizeAssetUrl(thumbnailImageUrl) ??
  normalizeAssetUrl(firstNoticeImageUrl(introduction));

const mapSummary = (item: MbrdSummary): PublicNoticeSummary => ({
  documentId: item.documentId,
  title: item.title,
  introduction: item.introduction,
  thumbnailImageUrl: noticeThumbnail(item.thumbnailImageUrl, item.introduction),
  tags: item.tags.filter((t) => !isHiddenSystemTag(t)),
  updatedAt: item.updatedAt,
  viewCount: item.viewCount ?? 0,
});

const byNewestFirst = (left: MbrdSummary, right: MbrdSummary): number =>
  new Date(right.updatedAt).getTime() - new Date(left.updatedAt).getTime();

async function fetchPublishedNoticeRows(): Promise<readonly MbrdSummary[]> {
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
  return body.content.filter(
    (item) => item.status === "published" && hasNoticeBoardMarker(item.tags),
  );
}

export async function fetchPublishedNotices(): Promise<
  readonly PublicNoticeSummary[]
> {
  const rows = await fetchPublishedNoticeRows();
  return rows.map(mapSummary);
}

export async function fetchPublicNotices(
  page: number,
  size = 10,
): Promise<PublicNoticePageResponse> {
  const noticesOnly = [...(await fetchPublishedNoticeRows())].sort(
    byNewestFirst,
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

const GLOW_BUZZ_SIZE = 3;
const GLOW_BUZZ_PICK_TAGS = ["ai-recommend", "mcp-recommend"];

const isGlowBuzzPick = (tags: readonly string[]): boolean =>
  tags.some((tag) => GLOW_BUZZ_PICK_TAGS.includes(tag.trim().toLowerCase()));

export async function fetchGlowBuzzPosts(): Promise<
  readonly PublicNoticeSummary[]
> {
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
    throw new Error("게시글을 불러오지 못했습니다.");
  }
  const body = (await response.json()) as MbrdPageResponse;
  return body.content
    .filter(
      (item) => item.status === "published" && !hasNoticeBoardMarker(item.tags),
    )
    .sort((left, right) => {
      const pickGap =
        Number(isGlowBuzzPick(right.tags)) - Number(isGlowBuzzPick(left.tags));
      if (pickGap !== 0) {
        return pickGap;
      }
      return (right.viewCount ?? 0) - (left.viewCount ?? 0);
    })
    .slice(0, GLOW_BUZZ_SIZE)
    .map(mapSummary);
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
    thumbnailImageUrl: noticeThumbnail(
      body.thumbnailImageUrl,
      body.introduction,
    ),
    attachments: parseNoticeAttachments(body.introduction).map((file) => ({
      name: file.name,
      url: normalizeAssetUrl(file.url) ?? file.url,
    })),
    updatedAt: body.updatedAt,
  };
}
