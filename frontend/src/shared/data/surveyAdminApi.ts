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
