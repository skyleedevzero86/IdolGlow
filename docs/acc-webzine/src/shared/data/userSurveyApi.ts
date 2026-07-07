import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

/** `com.sleekydz86.idolglow.user.user.domain.vo.ConceptType` */
export type UserSurveyConceptType =
  | 'GIRL_CRUSH'
  | 'LOVELY_FRESH'
  | 'ELEGANT_GLAM'
  | 'DREAMY'
  | 'HIGHTEEN'
  | 'ETC';

export const USER_SURVEY_CONCEPT_OPTIONS: ReadonlyArray<{
  readonly value: UserSurveyConceptType;
  readonly label: string;
}> = [
  { value: 'GIRL_CRUSH', label: '걸크러시' },
  { value: 'LOVELY_FRESH', label: '러블리 / 청순' },
  { value: 'ELEGANT_GLAM', label: '우아 / 글램' },
  { value: 'DREAMY', label: '몽환' },
  { value: 'HIGHTEEN', label: '하이틴' },
  { value: 'ETC', label: '기타' },
];

export interface UserSurveyResponse {
  readonly id: number;
  readonly concept: UserSurveyConceptType;
  readonly idolName: string;
  readonly visitStartDate: string;
  readonly visitEndDate: string;
  readonly visitStartTime?: string | null;
  readonly visitEndTime?: string | null;
  readonly places: readonly string[];
}

export interface CreateUserSurveyBody {
  readonly concept: UserSurveyConceptType;
  readonly idolName: string;
  readonly visitStartDate: string;
  readonly visitEndDate: string;
  readonly visitStartTime?: string | null;
  readonly visitEndTime?: string | null;
  readonly places: readonly string[];
}

interface ErrorBody {
  readonly message?: string;
  readonly error?: string;
  readonly errors?: unknown;
  readonly fieldErrors?: unknown;
}

function extractValidationMessage(input: unknown): string | null {
  if (!input) return null;
  if (Array.isArray(input)) {
    for (const item of input) {
      const found = extractValidationMessage(item);
      if (found) return found;
    }
    return null;
  }
  if (typeof input === 'object') {
    const record = input as Record<string, unknown>;
    const direct = record.defaultMessage ?? record.message ?? record.reason;
    if (typeof direct === 'string' && direct.trim()) {
      return direct.trim();
    }
    for (const value of Object.values(record)) {
      const found = extractValidationMessage(value);
      if (found) return found;
    }
  }
  return null;
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
    if (body.error?.trim()) {
      return body.error;
    }
    const validationMessage = extractValidationMessage(body.errors) ?? extractValidationMessage(body.fieldErrors);
    if (validationMessage) {
      return validationMessage;
    }
  } catch {
    // ignore
  }
  if (response.status === 400) {
    return '입력값 검증에 실패했습니다. 날짜/장소/아이돌 값을 다시 확인해 주세요.';
  }
  return response.status >= 500 ? '서버 처리 중 오류가 발생했습니다.' : '요청을 처리하지 못했습니다.';
}

/**
 * 회원 설문(컨셉·아이돌·여행기간·장소) — GET /surveys
 * 백엔드: `UserSurveyController` / `UserSurveyResponse`
 */
export async function fetchUserSurvey(accessToken: string): Promise<UserSurveyResponse | null> {
  const response = await fetch(`${getApiBaseUrl()}/surveys`, {
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
  return (await response.json()) as UserSurveyResponse;
}

/**
 * 회원 설문 저장(신규/수정) — POST /surveys
 * 백엔드: `UserSurveyCommandService.saveUserSurvey`
 */
export async function saveUserSurvey(accessToken: string, body: CreateUserSurveyBody): Promise<{ readonly id: number }> {
  const response = await fetch(`${getApiBaseUrl()}/surveys`, {
    method: 'POST',
    credentials: 'include',
    headers: withAuth(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as { readonly id: number };
}

export async function clearUserSurveyPlaces(accessToken: string): Promise<void> {
  const response = await fetch(`${getApiBaseUrl()}/surveys/places`, {
    method: 'DELETE',
    credentials: 'include',
    headers: withAuth(accessToken),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
}
