import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';
import {
  sanitizePersonalizedRecommendationList,
  type PersonalizedRecommendationItem,
} from './productRecommendationApi';
import type { SurveyFormResponse, SurveyQuestionType } from './surveyFormAdminApi';

export type { SurveyFormResponse, SurveyQuestionType };
export { isWrittenSurveyQuestion, visibleChoiceLabel } from './surveyFormAdminApi';

export interface SurveySubmittedAnswerResponse {
  readonly questionId: number;
  readonly answerText: string | null;
  readonly selectedOptions: readonly string[];
}

export interface SurveySubmissionResponse {
  readonly id: number;
  readonly formId: number;
  readonly submittedAt: string | null;
  readonly answers: readonly SurveySubmittedAnswerResponse[];
}

export interface SurveyAnswerSubmitInput {
  readonly questionId: number;
  readonly answerText?: string | null;
  readonly selectedOptions?: readonly string[];
}

export interface SubmitSurveyFormInput {
  readonly answers: readonly SurveyAnswerSubmitInput[];
}

export interface SurveyRecommendedAttractionResponse {
  readonly attractionCode: string;
  readonly name: string;
  readonly areaName: string | null;
  readonly signguName: string | null;
  readonly categoryLarge: string | null;
  readonly categoryMiddle: string | null;
  readonly rank: number;
  readonly reason: string;
}

export type SurveyUsedSignalType = 'CONCEPT' | 'IDOL' | 'PLACE' | 'TAG' | 'BEHAVIOR_TAG' | 'VISIT_PERIOD';
export type SurveyUsedSignalSource = 'FORM' | 'USER_SURVEY' | 'ACTIVITY';

export interface SurveyUsedSignal {
  readonly type: SurveyUsedSignalType;
  readonly source: SurveyUsedSignalSource;
  readonly values: readonly string[];
}

export interface SurveyRecommendationResponse {
  readonly submissionId: number;
  readonly llmEnhanced: boolean;
  readonly llmPending: boolean;
  readonly title: string;
  readonly subtitle: string;
  readonly narrative: string;
  readonly attractions: readonly SurveyRecommendedAttractionResponse[];
  readonly recommendedProducts: readonly PersonalizedRecommendationItem[];
  readonly usedSignals: readonly SurveyUsedSignal[];
}

const USED_SIGNAL_TYPES: ReadonlySet<string> = new Set<SurveyUsedSignalType>([
  'CONCEPT',
  'IDOL',
  'PLACE',
  'TAG',
  'BEHAVIOR_TAG',
  'VISIT_PERIOD',
]);
const USED_SIGNAL_SOURCES: ReadonlySet<string> = new Set<SurveyUsedSignalSource>(['FORM', 'USER_SURVEY', 'ACTIVITY']);

function sanitizeUsedSignals(value: unknown): SurveyUsedSignal[] {
  if (!Array.isArray(value)) return [];
  const signals: SurveyUsedSignal[] = [];
  for (const item of value) {
    if (!item || typeof item !== 'object') continue;
    const { type, source, values } = item as { type?: unknown; source?: unknown; values?: unknown };
    if (typeof type !== 'string' || !USED_SIGNAL_TYPES.has(type)) continue;
    if (typeof source !== 'string' || !USED_SIGNAL_SOURCES.has(source)) continue;
    const cleaned = Array.isArray(values)
      ? values.filter((v): v is string => typeof v === 'string' && v.trim().length > 0).map(v => v.trim())
      : [];
    if (cleaned.length === 0) continue;
    signals.push({ type: type as SurveyUsedSignalType, source: source as SurveyUsedSignalSource, values: cleaned });
  }
  return signals;
}

function sanitizeSurveyRecommendation(raw: unknown): SurveyRecommendationResponse {
  const body = (raw ?? {}) as Partial<Record<keyof SurveyRecommendationResponse, unknown>>;
  return {
    submissionId: typeof body.submissionId === 'number' ? body.submissionId : 0,
    llmEnhanced: body.llmEnhanced === true,
    llmPending: body.llmPending === true,
    title: typeof body.title === 'string' ? body.title : '',
    subtitle: typeof body.subtitle === 'string' ? body.subtitle : '',
    narrative: typeof body.narrative === 'string' ? body.narrative : '',
    attractions: Array.isArray(body.attractions) ? (body.attractions as SurveyRecommendedAttractionResponse[]) : [],
    recommendedProducts: sanitizePersonalizedRecommendationList(body.recommendedProducts),
    usedSignals: sanitizeUsedSignals(body.usedSignals),
  };
}

interface ErrorBody {
  readonly message?: string;
}

const withAuth = (accessToken: string, headers?: HeadersInit): HeadersInit => ({
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

export async function fetchCurrentSurveyForm(accessToken: string | null): Promise<SurveyFormResponse | null> {
  const headers: HeadersInit = {
    'Accept-Language': acceptLanguageHeader(),
  };
  if (accessToken) {
    (headers as Record<string, string>).Authorization = `Bearer ${accessToken}`;
  }
  const response = await fetch(`${getApiBaseUrl()}/survey-forms/current`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers,
  });
  if (response.status === 204 || response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as SurveyFormResponse;
}

export async function fetchCurrentParticipantCount(): Promise<number> {
  const response = await fetch(`${getApiBaseUrl()}/survey-forms/current/participants`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: { 'Accept-Language': acceptLanguageHeader() },
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  const body = (await response.json()) as { readonly count?: number };
  return typeof body.count === 'number' ? body.count : 0;
}

export async function submitCurrentSurveyForm(
  accessToken: string,
  input: SubmitSurveyFormInput,
): Promise<SurveySubmissionResponse> {
  const response = await fetch(`${getApiBaseUrl()}/survey-forms/current/submissions`, {
    method: 'POST',
    credentials: 'include',
    headers: withAuth(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(input),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as SurveySubmissionResponse;
}

export async function fetchMyLatestSurveySubmission(accessToken: string): Promise<SurveySubmissionResponse | null> {
  const response = await fetch(`${getApiBaseUrl()}/survey-forms/current/submissions/me/latest`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: withAuth(accessToken),
  });
  if (response.status === 204 || response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as SurveySubmissionResponse;
}

export async function generateSurveyRecommendation(
  accessToken: string,
  submissionId: number,
  useLlm: boolean,
): Promise<SurveyRecommendationResponse> {
  const query = new URLSearchParams();
  query.set('useLlm', useLlm ? 'true' : 'false');
  const response = await fetch(
    `${getApiBaseUrl()}/survey-forms/current/submissions/${submissionId}/recommendation?${query}`,
    {
      method: 'POST',
      credentials: 'include',
      headers: withAuth(accessToken),
    },
  );
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return sanitizeSurveyRecommendation(await response.json());
}

export async function fetchMyLatestSurveyRecommendation(
  accessToken: string,
  useLlm: boolean,
): Promise<SurveyRecommendationResponse | null> {
  const query = new URLSearchParams();
  query.set('useLlm', useLlm ? 'true' : 'false');
  const response = await fetch(`${getApiBaseUrl()}/survey-forms/current/submissions/me/latest/recommendation?${query}`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: withAuth(accessToken),
  });
  if (response.status === 204 || response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return sanitizeSurveyRecommendation(await response.json());
}
