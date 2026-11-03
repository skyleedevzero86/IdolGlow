import { getApiBaseUrl } from '../../auth/authConfig';
import { acceptLanguageHeader } from '../../ui/i18n/uiLangStorage';

export type ServerStatusLevel = 'UP' | 'DOWN' | 'DEGRADED' | 'NOT_CONFIGURED' | 'UNKNOWN';

export interface AdminServerStatusResponse {
  readonly generatedAt: string;
  readonly overallStatus: ServerStatusLevel;
  readonly summary: AdminServerSummary;
  readonly system: AdminSystemStatus;
  readonly infrastructure: readonly AdminInfrastructureStatus[];
  readonly actuator: AdminActuatorStatus;
}

export interface AdminServerSummary {
  readonly cpuUsagePercent: number | null;
  readonly memoryUsagePercent: number;
  readonly diskUsagePercent: number;
  readonly jvmHeapUsagePercent: number;
  readonly uptimeSeconds: number;
}

export interface AdminSystemStatus {
  readonly cpu: AdminCpuStatus;
  readonly memory: AdminMemoryStatus;
  readonly disk: AdminDiskStatus;
  readonly jvm: AdminJvmStatus;
}

export interface AdminCpuStatus {
  readonly systemUsagePercent: number | null;
  readonly processUsagePercent: number | null;
  readonly systemLoadAverage: number | null;
  readonly availableProcessors: number;
}

export interface AdminMemoryStatus {
  readonly totalBytes: number;
  readonly freeBytes: number;
  readonly usedBytes: number;
  readonly maxBytes: number;
  readonly usagePercent: number;
}

export interface AdminDiskStatus {
  readonly mountPath: string;
  readonly fileStoreName: string;
  readonly totalBytes: number;
  readonly freeBytes: number;
  readonly usedBytes: number;
  readonly usagePercent: number;
}

export interface AdminJvmStatus {
  readonly heapUsedBytes: number;
  readonly heapCommittedBytes: number;
  readonly heapMaxBytes: number;
  readonly heapUsagePercent: number;
  readonly nonHeapUsedBytes: number;
  readonly nonHeapCommittedBytes: number;
  readonly liveThreadCount: number;
  readonly daemonThreadCount: number;
  readonly peakThreadCount: number;
  readonly uptimeSeconds: number;
  readonly startTime: string;
}

export interface AdminInfrastructureStatus {
  readonly type: string;
  readonly label: string;
  readonly status: ServerStatusLevel;
  readonly message: string;
  readonly responseTimeMs: number | null;
  readonly details: Record<string, string>;
}

export interface AdminActuatorStatus {
  readonly enabled: boolean;
  readonly healthEndpoint: string;
  readonly metricsEndpoint: string;
  readonly metrics: readonly AdminActuatorMetric[];
}

export interface AdminActuatorMetric {
  readonly name: string;
  readonly value: number | null;
}

interface ErrorBody {
  readonly message?: string;
}

const withBaseHeaders = (accessToken?: string | null, headers?: HeadersInit): HeadersInit => ({
  ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
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
    return response.status >= 500
      ? '서버 상태를 가져오는 중 오류가 발생했습니다.'
      : '서버 상태 요청을 처리하지 못했습니다.';
  }

  return response.status >= 500
    ? '서버 상태를 가져오는 중 오류가 발생했습니다.'
    : '서버 상태 요청을 처리하지 못했습니다.';
}

async function requestJson<T>(
  input: RequestInfo | URL,
  init: RequestInit,
  retryWithoutBearer = false
): Promise<T> {
  let response = await fetch(input, { ...init, credentials: 'include' });

  if (
    retryWithoutBearer &&
    response.status === 401 &&
    init.headers instanceof Object &&
    'Authorization' in (init.headers as Record<string, string>)
  ) {
    const nextHeaders = { ...(init.headers as Record<string, string>) };
    delete nextHeaders.Authorization;
    response = await fetch(input, {
      ...init,
      headers: nextHeaders,
      credentials: 'include',
    });
  }

  if (!response.ok) {
    throw new Error(`[${response.status}] ${await readErrorMessage(response)}`);
  }

  return (await response.json()) as T;
}

async function requestJsonWithAdminPathFallback<T>(
  adminPath: string,
  init: RequestInit,
  retryWithoutBearer = false
): Promise<T> {
  const baseUrl = getApiBaseUrl();

  try {
    return await requestJson<T>(`${baseUrl}${adminPath}`, init, retryWithoutBearer);
  } catch (error) {
    if (!(error instanceof Error) || !error.message.startsWith('[404]')) {
      throw error;
    }

    const fallbackPath = adminPath.startsWith('/admin/')
      ? `/api${adminPath}`
      : adminPath.replace('/api/admin/', '/admin/');

    return requestJson<T>(`${baseUrl}${fallbackPath}`, init, retryWithoutBearer);
  }
}

export async function fetchAdminServerStatus(
  accessToken: string | null
): Promise<AdminServerStatusResponse> {
  return requestJsonWithAdminPathFallback<AdminServerStatusResponse>(
    '/admin/server-status',
    {
      method: 'GET',
      headers: withBaseHeaders(accessToken),
      cache: 'no-store',
    },
    true
  );
}
