import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';
import { sanitizeProductRecommendationItem, type ProductRecommendationItem } from './productRecommendationApi';
import type { SurveyFormResponse, SurveyQuestionType } from './surveyFormAdminApi';

export type { SurveyFormResponse, SurveyQuestionType };

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

export interface SurveyRecommendationResponse {
  readonly submissionId: number;
  readonly llmEnhanced: boolean;
  readonly title: string;
  readonly subtitle: string;
  readonly narrative: string;
  readonly attractions: readonly SurveyRecommendedAttractionResponse[];
  readonly recommendedProducts: readonly ProductRecommendationItem[];
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
  } catch {
    // ignore
  }
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
  const body = (await response.json()) as SurveyRecommendationResponse;
  return {
    ...body,
    recommendedProducts: body.recommendedProducts.map(sanitizeProductRecommendationItem),
  };
}
