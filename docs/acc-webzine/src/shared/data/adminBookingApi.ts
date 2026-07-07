import { getApiBaseUrl } from "../../auth/authConfig";
import { acceptLanguageHeader } from "../../ui/i18n/uiLangStorage";

export type ReservationStatus =
  | "PREBOOK"
  | "PENDING"
  | "BOOKED"
  | "COMPLETED"
  | "CANCELED";

export type PaymentStatus =
  | "PENDING"
  | "SUCCEEDED"
  | "FAILED"
  | "CANCELED"
  | "EXPIRED"
  | "REFUNDED"
  | "PARTIAL_CANCELED";

export type PaymentRefundStatus = "PENDING" | "SUCCEEDED" | "FAILED";

export interface ProductLocationSummary {
  readonly name: string;
  readonly latitude: string | number;
  readonly longitude: string | number;
  readonly roadAddressName: string | null;
  readonly addressName: string | null;
  readonly displayAddress: string;
}

export interface AdminProductSummary {
  readonly id: number;
  readonly name: string;
  readonly description: string;
  /** 상품 기본가(옵션 합과 별도) */
  readonly basePrice: number;
  /** 붙은 옵션 가격 합(기본가 제외) */
  readonly optionsTotalPrice: number;
  readonly minPrice: number;
  readonly totalPrice: number;
  readonly tagNames: readonly string[];
  readonly location: ProductLocationSummary | null;
  readonly thumbnailUrl: string | null;
  readonly tourAttractionPickCount: number;
}

export interface AdminProductOption {
  readonly id: number;
  readonly name: string;
  readonly description: string;
  readonly price: number;
  readonly location: string;
  readonly imageUrls: readonly string[];
}

export interface AdminProductDetail {
  readonly id: number;
  readonly name: string;
  readonly description: string;
  readonly options: readonly AdminProductOption[];
  readonly tagNames: readonly string[];
  readonly slotStartDate: string | null;
  readonly slotEndDate: string | null;
  readonly slotStartTime: string | null;
  readonly slotEndTime: string | null;
  readonly reservationSlotCount: number;
  /** 상품 기본가(옵션 합과 별도) */
  readonly basePrice: number;
  /** 붙은 옵션 가격 합(기본가 제외) */
  readonly optionsTotalPrice: number;
  readonly minPrice: number;
  readonly totalPrice: number;
  readonly location: ProductLocationSummary | null;
  readonly thumbnailUrl: string | null;
  readonly imageUrls: readonly string[];
  readonly tourAttractionPicks?: readonly ProductTourAttractionItem[] | null;
}

export interface ProductTourAttractionItem {
  readonly attractionCode: string;
  readonly name: string;
  readonly areaName: string | null;
  readonly signguName: string | null;
  readonly categoryLarge: string | null;
  readonly categoryMiddle: string | null;
  readonly rank: number;
  readonly mapX: number | null;
  readonly mapY: number | null;
  readonly score: number;
  readonly reason: string;
}

export interface ProductTourAttractionResult {
  readonly productId: number;
  readonly productName: string;
  readonly district: string;
  readonly areaCode: number;
  readonly signguCode: number;
  readonly baseYm: string;
  readonly attractions: readonly ProductTourAttractionItem[];
}

function safeImageUrl(url: string | null | undefined): string | null {
  const trimmed = url?.trim();
  if (!trimmed) return null;
  if (trimmed.includes("mock-cloud.example")) return null;
  return trimmed;
}

function safeImageUrls(urls: readonly string[] | null | undefined): readonly string[] {
  return urls?.map((url) => safeImageUrl(url)).filter((url): url is string => Boolean(url)) ?? [];
}

export interface AdminOptionSummary {
  readonly id: number;
  readonly name: string;
  readonly description: string;
  readonly price: number;
  readonly location: string;
}

/** GET /admin/options (검색·페이지) */
export interface AdminOptionPage {
  readonly content: readonly AdminOptionSummary[];
  readonly totalElements: number;
  readonly totalPages: number;
  readonly number: number;
  readonly size: number;
}

export interface AdminSlotSummary {
  readonly id: number;
  readonly productId: number;
  readonly productName: string;
  readonly reservationDate: string;
  readonly startTime: string;
  readonly endTime: string;
  readonly booked: boolean;
  readonly holdReservationId: number | null;
  readonly holdExpiresAt: string | null;
  readonly adminNote: string | null;
}

export interface AdminReservationSummary {
  readonly reservationId: number;
  readonly userId: number;
  readonly productId: number;
  readonly productName: string;
  readonly status: ReservationStatus;
  readonly totalPrice: number;
  readonly visitDate: string;
  readonly visitStartTime: string;
  readonly visitEndTime: string;
  readonly expiresAt: string | null;
  readonly confirmedAt: string | null;
  readonly canceledAt: string | null;
  readonly cancelReason: string | null;
  readonly paymentReference: string | null;
  readonly paymentStatus: PaymentStatus | null;
  readonly paymentFailureReason: string | null;
  readonly adminMemo: string | null;
}

export interface ReservationDashboardResponse {
  readonly pendingCount: number;
  readonly bookedCount: number;
  readonly completedCount: number;
  readonly canceledCount: number;
  readonly paymentPendingCount: number;
  readonly paymentSucceededCount: number;
  readonly paymentFailedCount: number;
  readonly paymentCanceledCount: number;
  readonly paymentExpiredCount: number;
  readonly recentReservations: readonly AdminReservationSummary[];
}

export interface AdminPaymentSummary {
  readonly paymentId: number;
  readonly reservationId: number;
  readonly userId: number;
  readonly productId: number;
  readonly productName: string;
  readonly provider: string;
  readonly paymentReference: string;
  readonly amount: number;
  readonly cancelAmount: number;
  readonly status: PaymentStatus;
  readonly failureReason: string | null;
  readonly approvedAt: string | null;
  readonly failedAt: string | null;
  readonly expiredAt: string | null;
  readonly visitDate: string;
  readonly visitStartTime: string;
  readonly visitEndTime: string;
}

export interface AdminPaymentOverview {
  readonly totalCount: number;
  readonly pendingCount: number;
  readonly succeededCount: number;
  readonly failedCount: number;
  readonly canceledCount: number;
  readonly expiredCount: number;
  readonly refundedCount: number;
  readonly partialCanceledCount: number;
  readonly cancelableCount: number;
  readonly grossAmount: number;
  readonly refundedAmount: number;
  readonly netAmount: number;
}

export interface AdminPaymentStatusChartPoint {
  readonly status: string;
  readonly count: number;
}

export interface AdminPaymentMonthlyChartPoint {
  readonly month: string;
  readonly totalCount: number;
  readonly succeededCount: number;
  readonly failedCount: number;
  readonly canceledCount: number;
}

export interface AdminPaymentCharts {
  readonly byStatus: readonly AdminPaymentStatusChartPoint[];
  readonly byMonth: readonly AdminPaymentMonthlyChartPoint[];
}

export interface AdminMenuStats {
  readonly productsCount: number;
  readonly optionsCount: number;
  readonly slotsCount: number;
  readonly reservationsPendingCount: number;
  readonly reservationsBookedCount: number;
  readonly reservationsCompletedCount: number;
  readonly reservationsCanceledCount: number;
  readonly paymentsPendingCount: number;
  readonly paymentsSucceededCount: number;
  readonly paymentsFailedCount: number;
  readonly paymentsCanceledCount: number;
  readonly paymentsExpiredCount: number;
  readonly paymentsRefundedCount: number;
  readonly paymentsPartialCanceledCount: number;
}

export interface AdminPaymentLogSummary {
  readonly logId: number;
  readonly logType: string;
  readonly step: string | null;
  readonly requestUrl: string | null;
  readonly httpMethod: string | null;
  readonly httpStatus: number | null;
  readonly errorCode: string | null;
  readonly errorMessage: string | null;
  readonly createdAt: string | null;
}

export interface AdminPaymentRefundSummary {
  readonly refundId: number;
  readonly paymentId: number;
  readonly reservationId: number;
  readonly cancelAmount: number;
  readonly cancelReason: string;
  readonly status: PaymentRefundStatus;
  readonly requestedBy: string;
  readonly externalTransactionKey: string | null;
  readonly failCode: string | null;
  readonly failMessage: string | null;
}

export interface AdminPaymentDetail extends AdminPaymentSummary {
  readonly paymentNo: string;
  readonly orderId: string;
  readonly paymentKey: string | null;
  readonly orderName: string | null;
  readonly currency: string | null;
  readonly gatewayMethod: string | null;
  readonly gatewayType: string | null;
  readonly externalStatus: string | null;
  readonly failCode: string | null;
  readonly canceledAt: string | null;
  readonly cardCompany: string | null;
  readonly cardNumber: string | null;
  readonly easyPayProvider: string | null;
  readonly virtualAccountBank: string | null;
  readonly virtualAccountNumber: string | null;
  readonly virtualAccountDueDate: string | null;
  readonly canCancel: boolean;
  readonly receiptAvailable: boolean;
  readonly refunds: readonly AdminPaymentRefundSummary[];
  readonly logs: readonly AdminPaymentLogSummary[];
}

export interface ProductBrowseResponse {
  readonly items: readonly AdminProductSummary[];
  readonly nextCursor: number | null;
  readonly nextOffset: number | null;
}

export interface ProductCommandInput {
  readonly name: string;
  readonly description: string;
  /** 상품 기본가(원, 옵션 합과 별도) */
  readonly basePrice: number;
  readonly slotStartDate?: string | null;
  readonly slotEndDate?: string | null;
  readonly slotStartHour: number;
  readonly slotEndHour: number;
  readonly slotStartTime?: string | null;
  readonly slotEndTime?: string | null;
  readonly optionIds: readonly number[];
  readonly tagNames: readonly string[];
  readonly location?: ProductLocationInput | null;
  readonly tourAttractionPicks?: readonly ProductTourAttractionItem[] | null;
}

export interface ProductLocationInput {
  readonly name: string;
  readonly latitude: number;
  readonly longitude: number;
  readonly roadAddressName?: string | null;
  readonly addressName?: string | null;
  readonly kakaoPlaceId: string;
}

export interface OptionCommandInput {
  readonly name: string;
  readonly description: string;
  readonly price: number;
  readonly location: string;
}

export interface CreateSlotsInput {
  readonly startDate: string;
  readonly endDate: string;
  readonly startHour: number;
  readonly endHour: number;
  readonly excludeWeekends: boolean;
  readonly adminNote?: string | null;
}

interface ErrorBody {
  readonly message?: string;
}

const withBaseHeaders = (
  accessToken?: string | null,
  headers?: HeadersInit,
): HeadersInit => ({
  ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
  "Accept-Language": acceptLanguageHeader(),
  ...headers,
});

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ErrorBody;
    if (body.message?.trim()) {
      return body.message;
    }
  } catch {}

  return response.status >= 500
    ? "요청 처리 중 서버 오류가 발생했습니다."
    : "요청을 처리하지 못했습니다.";
}

async function requestJson<T>(
  input: RequestInfo | URL,
  init: RequestInit,
  retryWithoutBearer = false,
): Promise<T> {
  let response = await fetch(input, { ...init, credentials: "include" });

  if (
    retryWithoutBearer &&
    response.status === 401 &&
    init.headers instanceof Object &&
    "Authorization" in (init.headers as Record<string, string>)
  ) {
    const nextHeaders = { ...(init.headers as Record<string, string>) };
    delete nextHeaders.Authorization;
    response = await fetch(input, {
      ...init,
      headers: nextHeaders,
      credentials: "include",
    });
  }

  if (!response.ok) {
    throw new Error(`[${response.status}] ${await readErrorMessage(response)}`);
  }

  return (await response.json()) as T;
}

async function requestVoid(
  input: RequestInfo | URL,
  init: RequestInit,
  retryWithoutBearer = false,
): Promise<void> {
  let response = await fetch(input, { ...init, credentials: "include" });

  if (
    retryWithoutBearer &&
    response.status === 401 &&
    init.headers instanceof Object &&
    "Authorization" in (init.headers as Record<string, string>)
  ) {
    const nextHeaders = { ...(init.headers as Record<string, string>) };
    delete nextHeaders.Authorization;
    response = await fetch(input, {
      ...init,
      headers: nextHeaders,
      credentials: "include",
    });
  }

  if (!response.ok) {
    throw new Error(`[${response.status}] ${await readErrorMessage(response)}`);
  }
}

async function requestBlob(
  input: RequestInfo | URL,
  init: RequestInit,
  retryWithoutBearer = false,
): Promise<Blob> {
  let response = await fetch(input, { ...init, credentials: "include" });

  if (
    retryWithoutBearer &&
    response.status === 401 &&
    init.headers instanceof Object &&
    "Authorization" in (init.headers as Record<string, string>)
  ) {
    const nextHeaders = { ...(init.headers as Record<string, string>) };
    delete nextHeaders.Authorization;
    response = await fetch(input, {
      ...init,
      headers: nextHeaders,
      credentials: "include",
    });
  }

  if (!response.ok) {
    throw new Error(`[${response.status}] ${await readErrorMessage(response)}`);
  }

  return response.blob();
}

async function requestJsonWithAdminPathFallback<T>(
  adminPath: string,
  init: RequestInit,
  retryWithoutBearer = false,
): Promise<T> {
  const baseUrl = getApiBaseUrl();

  try {
    return await requestJson<T>(
      `${baseUrl}${adminPath}`,
      init,
      retryWithoutBearer,
    );
  } catch (error) {
    if (!(error instanceof Error) || !error.message.startsWith("[404]")) {
      throw error;
    }

    const fallbackPath = adminPath.startsWith("/admin/")
      ? `/api${adminPath}`
      : adminPath.replace("/api/admin/", "/admin/");

    return requestJson<T>(
      `${baseUrl}${fallbackPath}`,
      init,
      retryWithoutBearer,
    );
  }
}

async function requestVoidWithAdminPathFallback(
  adminPath: string,
  init: RequestInit,
  retryWithoutBearer = false,
): Promise<void> {
  const baseUrl = getApiBaseUrl();

  try {
    await requestVoid(`${baseUrl}${adminPath}`, init, retryWithoutBearer);
    return;
  } catch (error) {
    if (!(error instanceof Error) || !error.message.startsWith("[404]")) {
      throw error;
    }

    const fallbackPath = adminPath.startsWith("/admin/")
      ? `/api${adminPath}`
      : adminPath.replace("/api/admin/", "/admin/");

    await requestVoid(`${baseUrl}${fallbackPath}`, init, retryWithoutBearer);
  }
}

async function requestBlobWithAdminPathFallback(
  adminPath: string,
  init: RequestInit,
  retryWithoutBearer = false,
): Promise<Blob> {
  const baseUrl = getApiBaseUrl();

  try {
    return await requestBlob(
      `${baseUrl}${adminPath}`,
      init,
      retryWithoutBearer,
    );
  } catch (error) {
    if (!(error instanceof Error) || !error.message.startsWith("[404]")) {
      throw error;
    }

    const fallbackPath = adminPath.startsWith("/admin/")
      ? `/api${adminPath}`
      : adminPath.replace("/api/admin/", "/admin/");

    return requestBlob(`${baseUrl}${fallbackPath}`, init, retryWithoutBearer);
  }
}

function triggerDownload(blob: Blob, filename: string): void {
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.append(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(url);
}

function asNumber(value: number | string): number {
  return typeof value === "number" ? value : Number(value);
}

function toProductPayload(input: ProductCommandInput) {
  return {
    name: input.name.trim(),
    description: input.description.trim(),
    basePrice: input.basePrice,
    slotStartDate: input.slotStartDate?.trim() ? input.slotStartDate : null,
    slotEndDate: input.slotEndDate?.trim() ? input.slotEndDate : null,
    slotStartHour: input.slotStartHour,
    slotEndHour: input.slotEndHour,
    slotStartTime: input.slotStartTime?.trim() ? input.slotStartTime : null,
    slotEndTime: input.slotEndTime?.trim() ? input.slotEndTime : null,
    optionIds: input.optionIds,
    tagNames: input.tagNames,
    location: input.location
      ? {
          name: input.location.name.trim(),
          latitude: input.location.latitude,
          longitude: input.location.longitude,
          roadAddressName: input.location.roadAddressName?.trim() || null,
          addressName: input.location.addressName?.trim() || null,
          kakaoPlaceId: input.location.kakaoPlaceId.trim(),
        }
      : null,
    tourAttractionPicks: (input.tourAttractionPicks ?? []).map((item) => ({
      attractionCode: item.attractionCode,
      name: item.name,
      areaName: item.areaName,
      signguName: item.signguName,
      categoryLarge: item.categoryLarge,
      categoryMiddle: item.categoryMiddle,
      rank: item.rank,
      mapX: item.mapX,
      mapY: item.mapY,
      score: item.score,
      reason: item.reason,
    })),
  };
}

function toOptionPayload(input: OptionCommandInput) {
  return {
    name: input.name.trim(),
    description: input.description.trim(),
    price: input.price,
    location: input.location.trim(),
  };
}

export async function fetchAdminProducts(
  accessToken: string | null,
  keyword?: string,
  options?: {
    readonly limit?: number;
  },
): Promise<readonly AdminProductSummary[]> {
  const limit = Math.max(1, options?.limit ?? 100);
  const pageSize = Math.min(50, limit);
  const items: AdminProductSummary[] = [];
  let lastId: number | null = null;
  let nextOffset: number | null = null;

  while (items.length < limit) {
    const query = new URLSearchParams({
      size: String(Math.min(pageSize, limit - items.length)),
    });
    if (keyword?.trim()) {
      query.set("keyword", keyword.trim());
    }
    if (lastId != null) {
      query.set("lastId", String(lastId));
    } else if (nextOffset != null) {
      query.set("offset", String(nextOffset));
    }

    const response = await requestJson<ProductBrowseResponse>(
      `${getApiBaseUrl()}/products?${query.toString()}`,
      {
        method: "GET",
        headers: withBaseHeaders(accessToken),
        cache: "no-store",
      },
      true,
    );

    items.push(
      ...response.items.map((item) => ({
        ...item,
        basePrice: asNumber(item.basePrice),
        optionsTotalPrice: asNumber(item.optionsTotalPrice),
        minPrice: asNumber(item.minPrice),
        totalPrice: asNumber(item.totalPrice),
        thumbnailUrl: safeImageUrl(item.thumbnailUrl),
        tourAttractionPickCount: asNumber(
          (item as { tourAttractionPickCount?: number | string }).tourAttractionPickCount ?? 0,
        ),
      })),
    );

    if (response.nextCursor != null) {
      lastId = response.nextCursor;
      nextOffset = null;
    } else if (response.nextOffset != null) {
      nextOffset = response.nextOffset;
      lastId = null;
    } else {
      break;
    }
  }

  return items;
}

export async function fetchAdminProduct(
  accessToken: string | null,
  productId: number,
): Promise<AdminProductDetail> {
  const response = await requestJson<AdminProductDetail>(
    `${getApiBaseUrl()}/products/${productId}`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
      cache: "no-store",
    },
    true,
  );

  const tourPicks = Array.isArray(response.tourAttractionPicks)
    ? response.tourAttractionPicks.map((item) => ({
        ...item,
        rank: asNumber(item.rank),
        mapX: item.mapX == null ? null : asNumber(item.mapX),
        mapY: item.mapY == null ? null : asNumber(item.mapY),
        score: asNumber(item.score),
      }))
    : [];

  return {
    ...response,
    basePrice: asNumber(response.basePrice),
    optionsTotalPrice: asNumber(response.optionsTotalPrice),
    minPrice: asNumber(response.minPrice),
    totalPrice: asNumber(response.totalPrice),
    thumbnailUrl: safeImageUrl(response.thumbnailUrl),
    imageUrls: safeImageUrls(response.imageUrls),
    options: response.options.map((option) => ({
      ...option,
      price: asNumber(option.price),
      imageUrls: safeImageUrls(option.imageUrls),
    })),
    tourAttractionPicks: tourPicks,
  };
}

export async function fetchPublicProductDetail(
  productId: number,
): Promise<AdminProductDetail> {
  return fetchAdminProduct(null, productId);
}

export async function fetchProductTourAttractions(
  accessToken: string | null,
  productId: number,
  size = 8,
  areaCode = 11,
  signguCode = 11530,
  baseYm?: string,
): Promise<ProductTourAttractionResult> {
  const query = new URLSearchParams();
  query.set("size", String(size));
  query.set("areaCode", String(areaCode));
  query.set("signguCode", String(signguCode));
  if (baseYm?.trim()) {
    query.set("base_ym", baseYm.trim());
  }
  const response = await requestJson<ProductTourAttractionResult>(
    `${getApiBaseUrl()}/products/${productId}/tour-attractions?${query.toString()}`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
      cache: "no-store",
    },
    true,
  );

  return {
    ...response,
    attractions: response.attractions.map((item) => ({
      ...item,
      mapX: item.mapX == null ? null : asNumber(item.mapX),
      mapY: item.mapY == null ? null : asNumber(item.mapY),
      score: asNumber(item.score),
    })),
  };
}

export async function fetchPublicProductTourAttractions(
  productId: number,
  size = 8,
  areaCode = 11,
  signguCode = 11530,
  baseYm?: string,
): Promise<ProductTourAttractionResult> {
  return fetchProductTourAttractions(null, productId, size, areaCode, signguCode, baseYm);
}

export async function createAdminProduct(
  accessToken: string | null,
  input: ProductCommandInput,
): Promise<{ readonly id: number }> {
  return requestJson<{ readonly id: number }>(
    `${getApiBaseUrl()}/products`,
    {
      method: "POST",
      headers: withBaseHeaders(accessToken, {
        "Content-Type": "application/json",
      }),
      body: JSON.stringify(toProductPayload(input)),
    },
    true,
  );
}

export async function updateAdminProduct(
  accessToken: string | null,
  productId: number,
  input: ProductCommandInput,
): Promise<void> {
  await requestVoidWithAdminPathFallback(
    `/admin/products/${productId}`,
    {
      method: "PUT",
      headers: withBaseHeaders(accessToken, {
        "Content-Type": "application/json",
      }),
      body: JSON.stringify(toProductPayload(input)),
    },
    true,
  );
}

export async function deleteAdminProduct(
  accessToken: string | null,
  productId: number,
): Promise<void> {
  await requestVoidWithAdminPathFallback(
    `/admin/products/${productId}`,
    {
      method: "DELETE",
      headers: withBaseHeaders(accessToken),
    },
    true,
  );
}

export async function fetchAdminOptions(
  accessToken: string | null,
): Promise<readonly AdminOptionSummary[]> {
  const response = await requestJson<readonly AdminOptionSummary[]>(
    `${getApiBaseUrl()}/options`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
      cache: "no-store",
    },
    true,
  );

  return response.map((option) => ({
    ...option,
    price: asNumber(option.price),
  }));
}

/**
 * 관리자 옵션 검색(이름/장소/설명) + 서버 페이징(기본 1~50건/페이지).
 * 상품에 연결할 “추가 아이템”을 수백~수천 건일 때 목록/검색용.
 */
export async function fetchAdminOptionsPage(
  accessToken: string | null,
  params: { readonly q?: string; readonly page: number; readonly size: number },
): Promise<AdminOptionPage> {
  const search = new URLSearchParams();
  if (params.q?.trim()) {
    search.set("q", params.q.trim());
  }
  search.set("page", String(params.page));
  search.set("size", String(params.size));
  const response = await requestJsonWithAdminPathFallback<AdminOptionPage>(
    `/admin/options?${search.toString()}`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
      cache: "no-store",
    },
    true,
  );
  return {
    ...response,
    content: response.content.map((option) => ({
      ...option,
      price: asNumber(option.price),
    })),
  };
}

export async function fetchAdminOption(
  accessToken: string | null,
  optionId: number,
): Promise<AdminOptionSummary> {
  const response = await requestJson<AdminOptionSummary>(
    `${getApiBaseUrl()}/options/${optionId}`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
      cache: "no-store",
    },
    true,
  );

  return { ...response, price: asNumber(response.price) };
}

export async function createAdminOption(
  accessToken: string | null,
  input: OptionCommandInput,
): Promise<AdminOptionSummary> {
  const response = await requestJson<AdminOptionSummary>(
    `${getApiBaseUrl()}/options`,
    {
      method: "POST",
      headers: withBaseHeaders(accessToken, {
        "Content-Type": "application/json",
      }),
      body: JSON.stringify(toOptionPayload(input)),
    },
    true,
  );

  return { ...response, price: asNumber(response.price) };
}

export async function updateAdminOption(
  accessToken: string | null,
  optionId: number,
  input: OptionCommandInput,
): Promise<AdminOptionSummary> {
  const response = await requestJson<AdminOptionSummary>(
    `${getApiBaseUrl()}/options/${optionId}`,
    {
      method: "PUT",
      headers: withBaseHeaders(accessToken, {
        "Content-Type": "application/json",
      }),
      body: JSON.stringify(toOptionPayload(input)),
    },
    true,
  );

  return { ...response, price: asNumber(response.price) };
}

export async function deleteAdminOption(
  accessToken: string | null,
  optionId: number,
): Promise<void> {
  await requestVoidWithAdminPathFallback(
    `/admin/options/${optionId}`,
    {
      method: "DELETE",
      headers: withBaseHeaders(accessToken),
    },
    true,
  );
}

export async function fetchAdminSlots(
  accessToken: string | null,
  productId: number,
): Promise<readonly AdminSlotSummary[]> {
  return requestJsonWithAdminPathFallback<readonly AdminSlotSummary[]>(
    `/admin/products/${productId}/slots`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
      cache: "no-store",
    },
    true,
  );
}

export async function createAdminSlots(
  accessToken: string | null,
  productId: number,
  input: CreateSlotsInput,
): Promise<readonly AdminSlotSummary[]> {
  return requestJsonWithAdminPathFallback<readonly AdminSlotSummary[]>(
    `/admin/products/${productId}/slots`,
    {
      method: "POST",
      headers: withBaseHeaders(accessToken, {
        "Content-Type": "application/json",
      }),
      body: JSON.stringify(input),
    },
    true,
  );
}

export async function updateAdminSlotNote(
  accessToken: string | null,
  slotId: number,
  markdown: string,
): Promise<AdminSlotSummary> {
  return requestJsonWithAdminPathFallback<AdminSlotSummary>(
    `/admin/slots/${slotId}/note`,
    {
      method: "PATCH",
      headers: withBaseHeaders(accessToken, {
        "Content-Type": "application/json",
      }),
      body: JSON.stringify({ markdown }),
    },
    true,
  );
}

export async function deleteAdminSlot(
  accessToken: string | null,
  slotId: number,
): Promise<void> {
  await requestVoidWithAdminPathFallback(
    `/admin/slots/${slotId}`,
    {
      method: "DELETE",
      headers: withBaseHeaders(accessToken),
    },
    true,
  );
}

export async function fetchAdminReservationDashboard(
  accessToken: string | null,
  params?: {
    readonly fromDate?: string;
    readonly toDate?: string;
    readonly recentSize?: number;
  },
): Promise<ReservationDashboardResponse> {
  const query = new URLSearchParams();
  if (params?.fromDate) {
    query.set("fromDate", params.fromDate);
  }
  if (params?.toDate) {
    query.set("toDate", params.toDate);
  }
  if (params?.recentSize) {
    query.set("recentSize", String(params.recentSize));
  }

  const suffix = query.toString() ? `?${query.toString()}` : "";

  return requestJsonWithAdminPathFallback<ReservationDashboardResponse>(
    `/admin/reservations/dashboard${suffix}`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
      cache: "no-store",
    },
    true,
  );
}

export async function fetchAdminReservations(
  accessToken: string | null,
  params: {
    readonly status?: ReservationStatus | "";
    readonly visitDate?: string;
    readonly productId?: number | "";
    readonly size?: number;
  },
): Promise<readonly AdminReservationSummary[]> {
  const query = new URLSearchParams({
    size: String(params.size ?? 100),
  });
  if (params.status) {
    query.set("status", params.status);
  }
  if (params.visitDate?.trim()) {
    query.set("visitDate", params.visitDate.trim());
  }
  if (params.productId) {
    query.set("productId", String(params.productId));
  }

  const response = await requestJsonWithAdminPathFallback<
    readonly AdminReservationSummary[]
  >(
    `/admin/reservations?${query.toString()}`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
      cache: "no-store",
    },
    true,
  );

  return response.map((item) => ({
    ...item,
    totalPrice: asNumber(item.totalPrice),
  }));
}

export async function cancelAdminReservation(
  accessToken: string | null,
  reservationId: number,
): Promise<AdminReservationSummary> {
  const response =
    await requestJsonWithAdminPathFallback<AdminReservationSummary>(
      `/admin/reservations/${reservationId}/cancel`,
      {
        method: "POST",
        headers: withBaseHeaders(accessToken),
      },
      true,
    );

  return { ...response, totalPrice: asNumber(response.totalPrice) };
}

export async function updateAdminReservationMemo(
  accessToken: string | null,
  reservationId: number,
  markdown: string,
): Promise<AdminReservationSummary> {
  const response =
    await requestJsonWithAdminPathFallback<AdminReservationSummary>(
      `/admin/reservations/${reservationId}/memo`,
      {
        method: "PATCH",
        headers: withBaseHeaders(accessToken, {
          "Content-Type": "application/json",
        }),
        body: JSON.stringify({ markdown }),
      },
      true,
    );

  return { ...response, totalPrice: asNumber(response.totalPrice) };
}

export async function fetchAdminPayments(
  accessToken: string | null,
  params: {
    readonly status?: PaymentStatus | "";
    readonly visitDate?: string;
    readonly productId?: number | "";
    readonly size?: number;
  },
): Promise<readonly AdminPaymentSummary[]> {
  const query = new URLSearchParams({
    size: String(params.size ?? 100),
  });
  if (params.status) {
    query.set("status", params.status);
  }
  if (params.visitDate?.trim()) {
    query.set("visitDate", params.visitDate.trim());
  }
  if (params.productId) {
    query.set("productId", String(params.productId));
  }

  const response = await requestJsonWithAdminPathFallback<
    readonly AdminPaymentSummary[]
  >(
    `/admin/payments?${query.toString()}`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
      cache: "no-store",
    },
    true,
  );

  return response.map((item) => ({
    ...item,
    amount: asNumber(item.amount),
    cancelAmount: asNumber(item.cancelAmount),
  }));
}

export async function fetchAdminPaymentOverview(
  accessToken: string | null,
  params: {
    readonly status?: PaymentStatus | "";
    readonly visitDate?: string;
    readonly productId?: number | "";
  },
): Promise<AdminPaymentOverview> {
  const query = new URLSearchParams();
  if (params.status) {
    query.set("status", params.status);
  }
  if (params.visitDate?.trim()) {
    query.set("visitDate", params.visitDate.trim());
  }
  if (params.productId) {
    query.set("productId", String(params.productId));
  }

  const suffix = query.toString() ? `?${query.toString()}` : "";
  const response = await requestJsonWithAdminPathFallback<AdminPaymentOverview>(
    `/admin/payments/overview${suffix}`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
      cache: "no-store",
    },
    true,
  );

  return {
    ...response,
    grossAmount: asNumber(response.grossAmount),
    refundedAmount: asNumber(response.refundedAmount),
    netAmount: asNumber(response.netAmount),
  };
}

export async function fetchAdminPaymentCharts(
  accessToken: string | null,
  params: {
    readonly status?: PaymentStatus | "";
    readonly visitDate?: string;
    readonly productId?: number | "";
  },
): Promise<AdminPaymentCharts> {
  const query = new URLSearchParams();
  if (params.status) {
    query.set("status", params.status);
  }
  if (params.visitDate?.trim()) {
    query.set("visitDate", params.visitDate.trim());
  }
  if (params.productId) {
    query.set("productId", String(params.productId));
  }
  const suffix = query.toString() ? `?${query.toString()}` : "";
  const response = await requestJsonWithAdminPathFallback<AdminPaymentCharts>(
    `/admin/payments/charts${suffix}`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
      cache: "no-store",
    },
    true,
  );
  return response;
}

export async function fetchAdminMenuStats(
  accessToken: string | null,
): Promise<AdminMenuStats> {
  return requestJsonWithAdminPathFallback<AdminMenuStats>(
    "/admin/operations/analytics/menu-stats",
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
      cache: "no-store",
    },
    true,
  );
}

export async function fetchAdminPaymentDetail(
  accessToken: string | null,
  paymentId: number,
): Promise<AdminPaymentDetail> {
  const response = await requestJsonWithAdminPathFallback<AdminPaymentDetail>(
    `/admin/payments/${paymentId}`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
      cache: "no-store",
    },
    true,
  );

  return {
    ...response,
    amount: asNumber(response.amount),
    cancelAmount: asNumber(response.cancelAmount),
    refunds: response.refunds.map((item) => ({
      ...item,
      cancelAmount: asNumber(item.cancelAmount),
    })),
  };
}

export async function cancelAdminPayment(
  accessToken: string | null,
  paymentId: number,
  reason?: string,
): Promise<AdminPaymentDetail> {
  const response = await requestJsonWithAdminPathFallback<AdminPaymentDetail>(
    `/admin/payments/${paymentId}/cancel`,
    {
      method: "POST",
      headers: withBaseHeaders(accessToken, {
        "Content-Type": "application/json",
      }),
      body: JSON.stringify({ reason: reason?.trim() || null }),
    },
    true,
  );

  return {
    ...response,
    amount: asNumber(response.amount),
    cancelAmount: asNumber(response.cancelAmount),
    refunds: response.refunds.map((item) => ({
      ...item,
      cancelAmount: asNumber(item.cancelAmount),
    })),
  };
}

export async function fetchAdminPaymentRefunds(
  accessToken: string | null,
  paymentId: number,
): Promise<readonly AdminPaymentRefundSummary[]> {
  const response = await requestJsonWithAdminPathFallback<
    readonly AdminPaymentRefundSummary[]
  >(
    `/admin/payments/${paymentId}/refunds`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
      cache: "no-store",
    },
    true,
  );

  return response.map((item) => ({
    ...item,
    cancelAmount: asNumber(item.cancelAmount),
  }));
}

export async function retryAdminPaymentRefund(
  accessToken: string | null,
  paymentId: number,
): Promise<AdminPaymentRefundSummary> {
  const response =
    await requestJsonWithAdminPathFallback<AdminPaymentRefundSummary>(
      `/admin/payments/${paymentId}/refunds/retry`,
      {
        method: "POST",
        headers: withBaseHeaders(accessToken),
      },
      true,
    );

  return {
    ...response,
    cancelAmount: asNumber(response.cancelAmount),
  };
}

export async function downloadAdminPaymentsExcel(
  accessToken: string | null,
  params: {
    readonly status?: PaymentStatus | "";
    readonly visitDate?: string;
    readonly productId?: number | "";
  },
): Promise<void> {
  const query = new URLSearchParams();
  if (params.status) {
    query.set("status", params.status);
  }
  if (params.visitDate?.trim()) {
    query.set("visitDate", params.visitDate.trim());
  }
  if (params.productId) {
    query.set("productId", String(params.productId));
  }

  const suffix = query.toString() ? `?${query.toString()}` : "";
  const blob = await requestBlobWithAdminPathFallback(
    `/admin/payments/export.xlsx${suffix}`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
    },
    true,
  );
  triggerDownload(blob, "payments.xlsx");
}

export async function downloadAdminReservationsExcel(
  accessToken: string | null,
  params: {
    readonly visitDate?: string;
  },
): Promise<void> {
  const today = new Date().toISOString().slice(0, 10);
  const date = params.visitDate?.trim() || today;
  const query = new URLSearchParams({
    visitDateFrom: date,
    visitDateTo: date,
  });
  const blob = await requestBlobWithAdminPathFallback(
    `/admin/operations/export/reservations.xlsx?${query.toString()}`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
    },
    true,
  );
  triggerDownload(blob, `reservations-${date}.xlsx`);
}

export async function downloadAdminProductsExcel(
  accessToken: string | null,
): Promise<void> {
  const blob = await requestBlobWithAdminPathFallback(
    "/admin/operations/export/products.xlsx",
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
    },
    true,
  );
  triggerDownload(blob, "products.xlsx");
}

export async function downloadAdminOptionsExcel(
  accessToken: string | null,
): Promise<void> {
  const blob = await requestBlobWithAdminPathFallback(
    "/admin/operations/export/options.xlsx",
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
    },
    true,
  );
  triggerDownload(blob, "options.xlsx");
}

export async function downloadAdminSlotsExcel(
  accessToken: string | null,
  params: {
    readonly productId: number;
    readonly dateFrom?: string;
    readonly dateTo?: string;
  },
): Promise<void> {
  const query = new URLSearchParams({
    productId: String(params.productId),
  });
  if (params.dateFrom?.trim()) {
    query.set("dateFrom", params.dateFrom.trim());
  }
  if (params.dateTo?.trim()) {
    query.set("dateTo", params.dateTo.trim());
  }
  const blob = await requestBlobWithAdminPathFallback(
    `/admin/operations/export/slots.xlsx?${query.toString()}`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
    },
    true,
  );
  triggerDownload(blob, `slots-${params.productId}.xlsx`);
}

export async function downloadAdminPaymentReceiptPdf(
  accessToken: string | null,
  paymentId: number,
): Promise<void> {
  const blob = await requestBlobWithAdminPathFallback(
    `/admin/payments/${paymentId}/receipt.pdf`,
    {
      method: "GET",
      headers: withBaseHeaders(accessToken),
    },
    true,
  );
  triggerDownload(blob, `payment-${paymentId}-receipt.pdf`);
}
