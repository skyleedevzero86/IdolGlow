import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

export type SurveyQuestionType = 'SHORT_TEXT' | 'LONG_TEXT' | 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TEXT';

export function isWrittenSurveyQuestion(type: SurveyQuestionType): boolean {
  return type === 'SHORT_TEXT' || type === 'LONG_TEXT' || type === 'TEXT';
}

export function visibleChoiceLabel(option: string): string {
  const marker = ' — ';
  const index = option.lastIndexOf(marker);
  return index > 0 ? option.slice(0, index) : option;
}

export type SurveyFormStatus = 'PLANNED' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED';
export type SurveyFormPrimaryCategory =
  | 'ALL'
  | 'TOUR_EXPERIENCE'
  | 'TRAVEL_ESSENTIALS'
  | 'BEAUTY'
  | 'MEDICAL'
  | 'ETC';
export type SurveyFormSecondaryCategory =
  | 'ACTIVITY'
  | 'FOOD'
  | 'K_POP'
  | 'ATTRACTIONS_TICKETS'
  | 'PHOTO'
  | 'TOUR'
  | 'WIFI_SIM'
  | 'TRANSPORTATION'
  | 'TRAVEL_SERVICE'
  | 'EXCHANGE'
  | 'INSURANCE'
  | 'HAIR_SALON'
  | 'K_BEAUTY'
  | 'SKIN_CARE'
  | 'CLINIC'
  | 'PHARMACY'
  | 'VISION_CORRECTION'
  | 'HEALTH_CHECKUP'
  | 'KOREAN_MEDICINE'
  | 'SPA_HEALING'
  | 'COUPON';

export interface SurveyFormQuestionResponse {
  readonly id: number;
  readonly order: number;
  readonly title: string;
  readonly description: string | null;
  readonly type: SurveyQuestionType;
  readonly required: boolean;
  readonly options: readonly string[];
}

export interface SurveyFormResponse {
  readonly id: number;
  readonly title: string;
  readonly description: string | null;
  readonly descriptionTags: readonly string[];
  readonly status: SurveyFormStatus;
  readonly statusLabel: string;
  readonly primaryCategory: SurveyFormPrimaryCategory;
  readonly primaryCategoryLabel: string;
  readonly secondaryCategory: SurveyFormSecondaryCategory | null;
  readonly secondaryCategoryLabel: string | null;
  readonly createdAt?: string | null;
  readonly questions: readonly SurveyFormQuestionResponse[];
}

export interface SurveyFormSummaryResponse {
  readonly id: number;
  readonly title: string;
  readonly description: string | null;
  readonly descriptionTags: readonly string[];
  readonly active: boolean;
  readonly status: SurveyFormStatus;
  readonly statusLabel: string;
  readonly primaryCategory: SurveyFormPrimaryCategory;
  readonly primaryCategoryLabel: string;
  readonly secondaryCategory: SurveyFormSecondaryCategory | null;
  readonly secondaryCategoryLabel: string | null;
  readonly questionCount: number;
  readonly requiredQuestionCount: number;
  readonly choiceQuestionCount: number;
  readonly createdAt: string | null;
  readonly updatedAt: string | null;
}

export interface SurveyFormPageResponse {
  readonly content: readonly SurveyFormSummaryResponse[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
  readonly hasNext: boolean;
}

export interface FetchAdminSurveyFormsPageParams {
  readonly page: number;
  readonly size: number;
  readonly keyword?: string;
  readonly status?: SurveyFormStatus | '';
  readonly primaryCategory?: SurveyFormPrimaryCategory | '';
  readonly secondaryCategory?: SurveyFormSecondaryCategory | '';
}

export interface SurveyFormQuestionInput {
  readonly order: number;
  readonly title: string;
  readonly description: string;
  readonly type: SurveyQuestionType;
  readonly required: boolean;
  readonly options: readonly string[];
}

export interface UpsertSurveyFormInput {
  readonly title: string;
  readonly description: string | null;
  readonly descriptionTags: readonly string[];
  readonly status: SurveyFormStatus;
  readonly primaryCategory: SurveyFormPrimaryCategory;
  readonly secondaryCategory: SurveyFormSecondaryCategory | null;
  readonly questions: readonly SurveyFormQuestionInput[];
}

interface ErrorBody {
  readonly message?: string;
}

const withBaseHeaders = (accessToken: string, headers?: HeadersInit): HeadersInit => ({
  Authorization: `Bearer ${accessToken}`,
  'Accept-Language': acceptLanguageHeader(),
  ...headers,
});

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ErrorBody;
    if (body.message?.trim()) {
      return body.message;
    }
  } catch {}
  return response.status >= 500 ? '서버 처리 중 오류가 발생했습니다.' : '요청을 처리하지 못했습니다.';
}

export async function fetchAdminCurrentSurveyForm(accessToken: string): Promise<SurveyFormResponse | null> {
  const response = await fetch(`${getApiBaseUrl()}/admin/survey-forms/current`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: withBaseHeaders(accessToken),
  });
  if (response.status === 204 || response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as SurveyFormResponse;
}

export async function fetchAdminSurveyForms(
  accessToken: string,
  keyword = '',
): Promise<readonly SurveyFormSummaryResponse[]> {
  const params = new URLSearchParams();
  const normalizedKeyword = keyword.trim();
  if (normalizedKeyword) {
    params.set('keyword', normalizedKeyword);
  }
  const query = params.toString();
  const response = await fetch(`${getApiBaseUrl()}/admin/survey-forms${query ? `?${query}` : ''}`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: withBaseHeaders(accessToken),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as readonly SurveyFormSummaryResponse[];
}

export async function fetchAdminSurveyFormPage(
  accessToken: string,
  params: FetchAdminSurveyFormsPageParams,
): Promise<SurveyFormPageResponse> {
  const search = new URLSearchParams({
    page: String(params.page),
    size: String(params.size),
  });
  const normalizedKeyword = params.keyword?.trim();
  if (normalizedKeyword) {
    search.set('keyword', normalizedKeyword);
  }
  if (params.status) {
    search.set('status', params.status);
  }
  if (params.primaryCategory) {
    search.set('primaryCategory', params.primaryCategory);
  }
  if (params.secondaryCategory) {
    search.set('secondaryCategory', params.secondaryCategory);
  }

  const response = await fetch(`${getApiBaseUrl()}/admin/survey-forms/page?${search.toString()}`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: withBaseHeaders(accessToken),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as SurveyFormPageResponse;
}

export interface SurveyOptionStatistics {
  readonly label: string;
  readonly count: number;
  readonly percent: number;
}

export interface SurveyQuestionStatistics {
  readonly questionId: number;
  readonly order: number;
  readonly title: string;
  readonly required: boolean;
  readonly written: boolean;
  readonly responseCount: number;
  readonly options: readonly SurveyOptionStatistics[];
}

export interface SurveyFormStatistics {
  readonly formId: number;
  readonly title: string;
  readonly participantCount: number;
  readonly questions: readonly SurveyQuestionStatistics[];
}

export async function fetchAdminSurveyFormStatistics(
  accessToken: string,
  id: number,
): Promise<SurveyFormStatistics> {
  const response = await fetch(`${getApiBaseUrl()}/admin/survey-forms/${id}/statistics`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: withBaseHeaders(accessToken),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as SurveyFormStatistics;
}

function statisticsStamp(now: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0');
  const date = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}`;
  const time = `${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`;
  return `${date}-${time}`;
}

function statisticsFileName(title: string, now: Date): string {
  const cleaned = title.replace(/[\\/:*?"<>|]/g, ' ').trim();
  return `${cleaned || '설문'}-통계-${statisticsStamp(now)}.xlsx`;
}

export async function downloadAdminSurveyStatistics(
  accessToken: string,
  id: number,
  title: string,
): Promise<void> {
  const response = await fetch(`${getApiBaseUrl()}/admin/survey-forms/${id}/statistics.xlsx`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: withBaseHeaders(accessToken),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = statisticsFileName(title, new Date());
  anchor.click();
  URL.revokeObjectURL(url);
}

export async function fetchAdminSurveyForm(
  accessToken: string,
  id: number,
): Promise<SurveyFormResponse> {
  const response = await fetch(`${getApiBaseUrl()}/admin/survey-forms/${id}`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: withBaseHeaders(accessToken),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as SurveyFormResponse;
}

export async function createAdminSurveyForm(
  accessToken: string,
  input: UpsertSurveyFormInput,
): Promise<SurveyFormResponse> {
  const response = await fetch(`${getApiBaseUrl()}/admin/survey-forms`, {
    method: 'POST',
    credentials: 'include',
    headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(input),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as SurveyFormResponse;
}

export async function updateAdminSurveyForm(
  accessToken: string,
  id: number,
  input: UpsertSurveyFormInput,
): Promise<SurveyFormResponse> {
  const response = await fetch(`${getApiBaseUrl()}/admin/survey-forms/${id}`, {
    method: 'PUT',
    credentials: 'include',
    headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(input),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as SurveyFormResponse;
}

export async function upsertAdminCurrentSurveyForm(
  accessToken: string,
  input: UpsertSurveyFormInput,
): Promise<SurveyFormResponse> {
  const response = await fetch(`${getApiBaseUrl()}/admin/survey-forms/current`, {
    method: 'PUT',
    credentials: 'include',
    headers: withBaseHeaders(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(input),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as SurveyFormResponse;
}
