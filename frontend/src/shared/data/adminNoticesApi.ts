import {
  type AdminEventDetail,
  type AdminEventStatus,
  deleteAdminEvent,
  fetchAdminEvent,
  fetchMbrdEditorSummariesUnfiltered,
  upsertAdminEvent,
} from "./adminEventsApi";
import {
  EVENT_BOARD_MARKER,
  NOTICE_BOARD_MARKER,
  hasNoticeBoardMarker,
} from "./mbrdBoardMarkers";
import { firstNoticeImageUrl } from "./noticeFiles";

export type AdminNoticeSummary = {
  readonly documentId: string;
  readonly title: string;
  readonly introduction: string | null;
  readonly status: AdminEventStatus;
  readonly updatedAt: string;
  readonly tags: readonly string[];
  readonly viewCount: number;
};

export type AdminNoticeDetail = {
  readonly documentId: string;
  readonly title: string;
  readonly markdown: string;
  readonly introduction: string | null;
  readonly status: AdminEventStatus;
  readonly tags: readonly string[];
  readonly updatedAt: string;
};

const normalizeNoticeTags = (tags: readonly string[]) => {
  const withoutMarker = tags
    .map((tag) => tag.trim())
    .filter(Boolean)
    .filter((tag) => {
      const low = tag.toLowerCase();
      return low !== NOTICE_BOARD_MARKER && low !== EVENT_BOARD_MARKER;
    });
  return [NOTICE_BOARD_MARKER, ...withoutMarker];
};

const toSummary = (
  item: Awaited<ReturnType<typeof fetchMbrdEditorSummariesUnfiltered>>[number],
): AdminNoticeSummary => ({
  documentId: item.documentId,
  title: item.title,
  introduction: item.introduction,
  status: item.status,
  updatedAt: item.updatedAt,
  viewCount: item.viewCount,
  tags: item.tags.filter((tag) => {
    const low = tag.toLowerCase();
    return low !== NOTICE_BOARD_MARKER && low !== EVENT_BOARD_MARKER;
  }),
});

const MARKDOWN_IMAGE_URL = /!\[[^\]]*]\(\s*<?([^)\s>]+)>?/;

function firstMarkdownImageUrl(markdown: string): string | null {
  const matched = markdown.match(MARKDOWN_IMAGE_URL);
  const url = matched?.[1]?.trim() ?? "";
  return url.length > 0 ? url : null;
}

const toDetail = (item: AdminEventDetail): AdminNoticeDetail => ({
  documentId: item.documentId,
  title: item.title,
  markdown: item.markdown,
  introduction: item.introduction,
  status: item.status,
  updatedAt: item.updatedAt,
  tags: item.tags.filter((tag) => {
    const low = tag.toLowerCase();
    return low !== NOTICE_BOARD_MARKER && low !== EVENT_BOARD_MARKER;
  }),
});

export async function fetchAdminNoticePage(
  accessToken: string | null,
  params: {
    readonly page: number;
    readonly size: number;
    readonly query: string;
    readonly status: AdminEventStatus;
  },
) {
  const base = await fetchMbrdEditorSummariesUnfiltered(accessToken, {
    query: params.query,
    status: params.status,
  });
  const notices = base.filter((item) => hasNoticeBoardMarker(item.tags));
  const totalElements = notices.length;
  const totalPages = Math.max(
    1,
    Math.ceil(totalElements / Math.max(1, params.size)),
  );
  const safePage = Math.min(Math.max(1, params.page), totalPages);
  const start = (safePage - 1) * params.size;
  const end = start + params.size;
  return {
    items: notices.slice(start, end).map(toSummary),
    page: safePage,
    size: params.size,
    totalElements,
    totalPages,
  };
}

export async function fetchAdminNotice(
  accessToken: string | null,
  documentId: string,
): Promise<AdminNoticeDetail> {
  const detail = await fetchAdminEvent(accessToken, documentId);
  if (!hasNoticeBoardMarker(detail.tags)) {
    throw new Error("공지사항 문서를 찾지 못했습니다.");
  }
  return toDetail(detail);
}

export async function upsertAdminNotice(
  accessToken: string | null,
  input: {
    readonly documentId?: string;
    readonly title: string;
    readonly markdown: string;
    readonly introduction?: string | null;
    readonly status: "draft" | "published";
    readonly tags: readonly string[];
  },
): Promise<AdminNoticeDetail> {
  const response = await upsertAdminEvent(accessToken, {
    documentId: input.documentId,
    title: input.title,
    author: "Idol Glow",
    markdown: input.markdown,
    tags: normalizeNoticeTags(input.tags),
    introduction: input.introduction ?? null,
    status: input.status,
    thumbnailImageUrl:
      firstMarkdownImageUrl(input.markdown) ??
      firstNoticeImageUrl(input.introduction ?? null),
  });
  return toDetail(response);
}

export async function deleteAdminNotice(
  accessToken: string | null,
  documentId: string,
): Promise<void> {
  const detail = await fetchAdminEvent(accessToken, documentId);
  if (!hasNoticeBoardMarker(detail.tags)) {
    throw new Error("공지사항 문서만 삭제할 수 있습니다.");
  }
  await deleteAdminEvent(accessToken, documentId);
}
