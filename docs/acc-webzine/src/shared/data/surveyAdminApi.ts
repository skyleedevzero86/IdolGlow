/**
 * 설문관리(관리자 화면)에서 쓰는 «회원 설문(UserSurvey)» API 래퍼.
 * 백엔드는 `GET/POST /surveys` (로그인 사용자 본인 데이터) 한 종류뿐이라
 * 관리자로 로그인했을 때는 **관리자 계정**의 UserSurvey 를 읽/씁니다.
 * 운영 **동적 문항**은 `surveyFormAdminApi` (`/admin/survey-forms/current`) 입니다.
 */
import {
  saveUserSurvey,
  fetchUserSurvey,
  clearUserSurveyPlaces,
  type UserSurveyConceptType,
  type CreateUserSurveyBody,
  type UserSurveyResponse,
} from './userSurveyApi';

export type SurveyConceptType = UserSurveyConceptType;
export type AdminSurveyResponse = UserSurveyResponse;
export type AdminSurveyInput = CreateUserSurveyBody;

export const fetchAdminSurvey = fetchUserSurvey;
export const upsertAdminSurvey = saveUserSurvey;
export const clearAdminSurveyPlaces = clearUserSurveyPlaces;
