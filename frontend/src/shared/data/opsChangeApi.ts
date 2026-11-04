import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

export type OpsChangeStatus = 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'APPLIED' | 'REVERTED';

export interface OpsChangeItem {
  readonly id: number;
  readonly type: string;
  readonly target: string;
  readonly payload: string;
  readonly requestedBy: string;
  readonly requestedByKind: string;
  readonly status: OpsChangeStatus;
  readonly beforeSnapshot: string | null;
  readonly approvedBy: string | null;
  readonly appliedAt: string | null;
}

interface ErrorBody {
  readonly message?: string;
}

const withHeaders = (accessToken: string | null, json = false): HeadersInit => ({
  ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
  'Accept-Language': acceptLanguageHeader(),
  ...(json ? { 'Content-Type': 'application/json' } : {}),
});

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ErrorBody;
    if (body.message?.trim()) {
      return body.message;
    }
  } catch {
    return response.status >= 500
      ? '서버 요청 처리 중 오류가 발생했습니다.'
      : '운영 변경 요청을 처리하지 못했습니다.';
  }

  return response.status >= 500
    ? '서버 요청 처리 중 오류가 발생했습니다.'
    : '운영 변경 요청을 처리하지 못했습니다.';
}

async function requestJson<T>(path: string, init: RequestInit): Promise<T> {
  const response = await fetch(`${getApiBaseUrl()}${path}`, { ...init, credentials: 'include' });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as T;
}

export function fetchOpsChanges(accessToken: string | null): Promise<OpsChangeItem[]> {
  return requestJson<OpsChangeItem[]>('/admin/ops-changes', {
    method: 'GET',
    headers: withHeaders(accessToken),
    cache: 'no-store',
  });
}

export interface AdminAuditTopAction {
  readonly rank: number;
  readonly actionCode: string;
  readonly count: number;
}

export interface AdminAuditTop {
  readonly basis: string;
  readonly actions: readonly AdminAuditTopAction[];
  readonly automationActionCode: string | null;
  readonly automationReason: string;
}

export function fetchAdminAuditTop(accessToken: string | null): Promise<AdminAuditTop> {
  return requestJson<AdminAuditTop>('/admin/operations/audit-logs/top', {
    method: 'GET',
    headers: withHeaders(accessToken),
    cache: 'no-store',
  });
}

export function createEventMatchMerge(
  accessToken: string | null,
  source: string,
  sourceId: string,
  globalId: string,
): Promise<OpsChangeItem> {
  return requestJson<OpsChangeItem>('/admin/ops-changes', {
    method: 'POST',
    headers: withHeaders(accessToken, true),
    body: JSON.stringify({
      type: 'EVENT_MATCH_MERGE',
      target: `${source}|${sourceId}`,
      payload: JSON.stringify({ globalId }),
      requestedByKind: 'USER',
    }),
  });
}

export function postOpsAction(
  accessToken: string | null,
  requestId: number,
  action: 'approve' | 'reject' | 'apply' | 'revert',
): Promise<OpsChangeItem> {
  return requestJson<OpsChangeItem>(`/admin/ops-changes/${requestId}/${action}`, {
    method: 'POST',
    headers: withHeaders(accessToken),
  });
}
