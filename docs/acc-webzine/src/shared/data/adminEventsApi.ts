import { getApiBaseUrl } from "../../auth/authConfig";
import { acceptLanguageHeader } from "../../ui/i18n/uiLangStorage";
import { filterOutNoticeBoardDocuments } from "./mbrdBoardMarkers";

type ApiErrorBody = {
  readonly message?: string;
  readonly errorCode?: string;
  readonly name?: string;
};

export type AdminEventStatus = "all" | "published" | "draft";

export type AdminEventSummary = {
  readonly documentId: string;
  readonly title: string;
  readonly author: string;
  readonly introduction: string | null;
  readonly thumbnailImageUrl: string | null;
  readonly tags: readonly string[];
  readonly status: AdminEventStatus;
  readonly updatedAt: string;
  readonly viewCount: number;
};

export type AdminEventPageResponse = {
  readonly items: readonly AdminEventSummary[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
};

export type AdminEventDetail = {
  readonly documentId: string;
  readonly title: string;
  readonly author: string;
  readonly markdown: string;
  readonly tags: readonly string[];
  readonly urlSlug: string | null;
  readonly introduction: string | null;
  readonly thumbnailImageUrl: string | null;
  readonly status: AdminEventStatus;
  readonly updatedAt: string;
  readonly viewCount: number;
};

export type UpsertAdminEventInput = {
  readonly documentId?: string | null;
  readonly title: string;
  readonly author: string;
  readonly markdown: string;
  readonly tags: readonly string[];
  readonly urlSlug?: string | null;
  readonly introduction?: string | null;
  readonly thumbnailImageUrl?: string | null;
  readonly status: AdminEventStatus;
};

export type AdminEventImageUploadResponse = {
  readonly url: string;
  readonly objectKey: string;
  readonly contentType: string;
  readonly size: number;
};

type MbrdSummary = {
  readonly documentId: string;
  readonly title: string;
  readonly author: string;
  readonly introduction: string | null;
  readonly thumbnailImageUrl: string | null;
  readonly tags: readonly string[];
  readonly status: AdminEventStatus;
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
  readonly status: AdminEventStatus;
  readonly updatedAt: string;
  readonly viewCount?: number;
};

type MbrdImageUploadResponse = {
  readonly imageUrl: string;
  readonly storedFileName: string;
  readonly size: number;
};

const withBaseHeaders = (
  accessToken: string,
  headers?: HeadersInit,
): HeadersInit => ({
  Authorization: `Bearer ${accessToken}`,
  "Accept-Language": acceptLanguageHeader(),
  ...headers,
});

function normalizePageResponse(
  response: AdminEventPageResponse,
): AdminEventPageResponse {
  const safeSize = Math.max(1, response.size || 10);
  const computedTotalPages = Math.max(
    1,
    Math.ceil(response.totalElements / safeSize),
  );
  return {
    ...response,
    size: safeSize,
    totalPages: Math.max(1, response.totalPages || computedTotalPages),
  };
}

async function readErrorMessage(response: Response): Promise<string> {
  const raw = await response.text();
  const fallback =
    response.status >= 500
      ? "서버 요청 처리 중 오류가 발생했습니다."
      : "요청을 처리하지 못했습니다.";
  if (!raw.trim()) {
    return fallback;
  }
  try {
    const body = JSON.parse(raw) as ApiErrorBody;
    if (typeof body.message === "string" && body.message.trim()) {
      return body.message.trim();
    }
  } catch {
    return raw.trim().slice(0, 800);
  }
  return fallback;
}

function normalizeStatusForMbrd(
  status: AdminEventStatus,
): "published" | "draft" {
  return status === "draft" ? "draft" : "published";
}

function normalizeAssetUrl(url: string | null): string | null {
  if (!url) return null;
  if (/^https?:\/\//i.test(url)) return url;
  if (url.startsWith("/")) {
    return `${getApiBaseUrl()}${url}`;
  }
  return url;
}

function mapMbrdSummary(summary: MbrdSummary): AdminEventSummary {
  return {
    ...summary,
    thumbnailImageUrl: normalizeAssetUrl(summary.thumbnailImageUrl),
    viewCount: summary.viewCount ?? 0,
  };
}

function mapMbrdDraft(response: MbrdDraftResponse): AdminEventDetail {
  return {
    documentId: response.documentId,
    title: response.title,
    author: response.author,
    markdown: response.markdown,
    tags: response.tags,
    urlSlug: response.urlSlug,
    introduction: response.introduction,
    thumbnailImageUrl: normalizeAssetUrl(response.thumbnailImageUrl),
    status: response.status,
    updatedAt: response.updatedAt,
    viewCount: response.viewCount ?? 0,
  };
}

/**
 * 태그 필터 없이 mbrd 문서 목록(공지+이벤트 혼합). 공지 전용 목록은 adminNoticesApi에서 사용.
 */
export async function fetchMbrdEditorSummariesUnfiltered(
  accessToken: string | null,
  params: {
    readonly query: string;
    readonly status: AdminEventStatus;
  },
): Promise<readonly AdminEventSummary[]> {
  if (!accessToken) {
    throw new Error("관리자 토큰이 없습니다.");
  }
  const baseUrl = getApiBaseUrl();
  const fetchMbrdByStatus = async (
    status: "published" | "draft",
    pageZeroBased: number,
    size: number,
  ) => {
    const mbrdQuery = new URLSearchParams();
    mbrdQuery.set("page", String(pageZeroBased));
    mbrdQuery.set("size", String(size));
    mbrdQuery.set("query", params.query.trim());
    mbrdQuery.set("status", status);
    const response = await fetch(
      `${baseUrl}/api/mbrd/editor/documents?${mbrdQuery.toString()}`,
      {
        method: "GET",
        headers: withBaseHeaders(accessToken),
        credentials: "include",
        cache: "no-store",
      },
    );
    if (!response.ok) {
      throw new Error(await readErrorMessage(response));
    }
    return (await response.json()) as MbrdPageResponse;
  };

  if (params.status !== "all") {
    const single = await fetchMbrdByStatus(
      normalizeStatusForMbrd(params.status),
      0,
      1000,
    );
    return single.content.map(mapMbrdSummary);
  }

  const [publishedPage, draftPage] = await Promise.all([
    fetchMbrdByStatus("published", 0, 1000),
    fetchMbrdByStatus("draft", 0, 1000),
  ]);
  const merged = [...publishedPage.content, ...draftPage.content]
    .filter(
      (item, index, arr) =>
        arr.findIndex(
          (candidate) => candidate.documentId === item.documentId,
        ) === index,
    )
    .sort(
      (a, b) =>
        new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime(),
    );
  return merged.map(mapMbrdSummary);
}

export async function fetchAdminEvents(
  accessToken: string | null,
  params: {
    readonly page: number;
    readonly size: number;
    readonly query: string;
    readonly status: AdminEventStatus;
  },
): Promise<AdminEventPageResponse> {
  const all = await fetchMbrdEditorSummariesUnfiltered(accessToken, {
    query: params.query,
    status: params.status,
  });
  const filtered = filterOutNoticeBoardDocuments(all);
  const totalElements = filtered.length;
  const safeSize = Math.max(1, params.size);
  const totalPages = Math.max(1, Math.ceil(totalElements / safeSize));
  const safePage = Math.min(Math.max(1, params.page), totalPages);
  const fromIndex = (safePage - 1) * safeSize;
  const toIndex = Math.min(fromIndex + safeSize, totalElements);

  return normalizePageResponse({
    items: filtered.slice(fromIndex, toIndex),
    page: safePage,
    size: safeSize,
    totalElements,
    totalPages,
  });
}

export async function fetchAdminEvent(
  accessToken: string | null,
  documentId: string,
): Promise<AdminEventDetail> {
  if (!accessToken) {
    throw new Error("관리자 토큰이 없습니다.");
  }
  const fallbackResponse = await fetch(
    `${getApiBaseUrl()}/api/mbrd/editor/documents/${encodeURIComponent(documentId)}`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
      credentials: "include",
      cache: "no-store",
    },
  );
  if (!fallbackResponse.ok) {
    throw new Error(await readErrorMessage(fallbackResponse));
  }
  return mapMbrdDraft((await fallbackResponse.json()) as MbrdDraftResponse);
}

export async function upsertAdminEvent(
  accessToken: string | null,
  input: UpsertAdminEventInput,
): Promise<AdminEventDetail> {
  if (!accessToken) {
    throw new Error("관리자 토큰이 없습니다.");
  }
  const fallbackResponse = await fetch(
    `${getApiBaseUrl()}/api/mbrd/editor/draft`,
    {
      method: "POST",
      headers: withBaseHeaders(accessToken, {
        "Content-Type": "application/json",
      }),
      credentials: "include",
      body: JSON.stringify(input),
    },
  );
  if (!fallbackResponse.ok) {
    throw new Error(await readErrorMessage(fallbackResponse));
  }
  return mapMbrdDraft((await fallbackResponse.json()) as MbrdDraftResponse);
}

export async function deleteAdminEvent(
  accessToken: string | null,
  documentId: string,
): Promise<void> {
  if (!accessToken) {
    throw new Error("관리자 토큰이 없습니다.");
  }
  const fallbackResponse = await fetch(
    `${getApiBaseUrl()}/api/mbrd/editor/documents/${encodeURIComponent(documentId)}`,
    {
      method: "DELETE",
      headers: withBaseHeaders(accessToken),
      credentials: "include",
    },
  );
  if (!fallbackResponse.ok) {
    throw new Error(await readErrorMessage(fallbackResponse));
  }
}

export async function uploadAdminEventImage(
  accessToken: string | null,
  file: File,
): Promise<AdminEventImageUploadResponse> {
  if (!accessToken) {
    throw new Error("관리자 토큰이 없습니다.");
  }
  const formData = new FormData();
  formData.append("file", file);
  const baseUrl = getApiBaseUrl();
  const uploadEndpoints: ReadonlyArray<{
    readonly url: string;
    readonly mapper: (body: unknown) => AdminEventImageUploadResponse;
  }> = [
    {
      url: `${baseUrl}/api/mbrd/editor/images`,
      mapper: (body) => {
        const mapped = body as MbrdImageUploadResponse;
        return {
          url: mapped.imageUrl,
          objectKey: mapped.storedFileName,
          contentType: file.type || "image/*",
          size: mapped.size,
        };
      },
    },
  ];

  const failures: string[] = [];
  for (const endpoint of uploadEndpoints) {
    const response = await fetch(endpoint.url, {
      method: "POST",
      headers: withBaseHeaders(accessToken),
      credentials: "include",
      body: formData,
    });
    if (response.ok) {
      return endpoint.mapper(await response.json());
    }
    const reason = await readErrorMessage(response);
    failures.push(`${response.status}(${endpoint.url}) ${reason}`);
  }

  throw new Error(`이미지 업로드에 실패했습니다. ${failures.join(" | ")}`);
}
