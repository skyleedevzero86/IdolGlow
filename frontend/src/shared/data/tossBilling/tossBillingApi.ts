import { getApiBaseUrl } from '../../../auth/authConfig';
import { acceptLanguageHeader } from '../../../ui/i18n/uiLangStorage';
import { BILLING_DEFAULT_ORDER_NAME, BILLING_MONTHLY_AMOUNT } from './billingDefaults';
import type { TossBillingCharge } from './types/TossBillingCharge';
import type { TossBillingClientConfig } from './types/TossBillingClientConfig';
import type { TossBillingMethod } from './types/TossBillingMethod';
import type { TossBillingOverview } from './types/TossBillingOverview';
import type { TossBillingSubscription } from './types/TossBillingSubscription';

type ErrorBody = {
  readonly message?: string;
};

type RawTossBillingOverview = {
  readonly method?: TossBillingMethod | null;
  readonly methods?: readonly TossBillingMethod[] | null;
  readonly subscription?: TossBillingSubscription | null;
  readonly charges?: readonly Partial<TossBillingCharge>[] | null;
};

function normalizeOverview(raw: RawTossBillingOverview): TossBillingOverview {
  const method = raw.method ?? null;
  const methods = raw.methods?.length ? raw.methods : method ? [method] : [];
  const charges = (raw.charges ?? []).map(charge => ({
    id: charge.id ?? 0,
    orderId: charge.orderId ?? '',
    orderName: charge.orderName ?? '',
    amount: charge.amount ?? 0,
    billingMethodId: charge.billingMethodId ?? method?.id ?? 0,
    paymentKey: charge.paymentKey ?? null,
    status: charge.status ?? 'FAILED',
    failMessage: charge.failMessage ?? null,
    approvedAt: charge.approvedAt ?? null,
    attempts: charge.attempts ?? [],
  }));
  return {
    method,
    methods,
    subscription: raw.subscription ?? null,
    charges,
  };
}

function withAuth(accessToken: string, headers?: HeadersInit): HeadersInit {
  return {
    Authorization: `Bearer ${accessToken}`,
    'Accept-Language': acceptLanguageHeader(),
    ...headers,
  };
}

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ErrorBody;
    if (body.message?.trim()) {
      return body.message;
    }
  } catch {
  }
  return response.status >= 500 ? '서버 처리 중 오류가 발생했습니다.' : '요청을 처리하지 못했습니다.';
}

export async function fetchTossBillingClientConfig(accessToken: string): Promise<TossBillingClientConfig> {
  const response = await fetch(`${getApiBaseUrl()}/payments/toss/billing/client-config`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: withAuth(accessToken),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as TossBillingClientConfig;
}

export async function fetchTossBillingOverview(accessToken: string): Promise<TossBillingOverview> {
  const response = await fetch(`${getApiBaseUrl()}/payments/toss/billing/overview`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    headers: withAuth(accessToken),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return normalizeOverview((await response.json()) as RawTossBillingOverview);
}

export async function issueTossBillingKey(
  accessToken: string,
  input: {
    readonly authKey: string;
    readonly customerKey: string;
    readonly setAsDefault?: boolean;
  },
): Promise<TossBillingMethod> {
  const response = await fetch(`${getApiBaseUrl()}/payments/toss/billing/issue`, {
    method: 'POST',
    credentials: 'include',
    headers: withAuth(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({
      authKey: input.authKey,
      customerKey: input.customerKey,
      setAsDefault: input.setAsDefault ?? true,
    }),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as TossBillingMethod;
}

export async function setTossBillingDefaultMethod(
  accessToken: string,
  methodId: number,
): Promise<TossBillingMethod> {
  const response = await fetch(`${getApiBaseUrl()}/payments/toss/billing/methods/${methodId}/default`, {
    method: 'POST',
    credentials: 'include',
    headers: withAuth(accessToken),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as TossBillingMethod;
}

export async function deactivateTossBillingMethod(
  accessToken: string,
  methodId: number,
): Promise<TossBillingMethod> {
  const response = await fetch(`${getApiBaseUrl()}/payments/toss/billing/methods/${methodId}/deactivate`, {
    method: 'POST',
    credentials: 'include',
    headers: withAuth(accessToken),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as TossBillingMethod;
}

export async function retryTossBillingCharge(
  accessToken: string,
  chargeId: number,
  input: { readonly methodId: number; readonly setAsDefault?: boolean },
): Promise<TossBillingCharge> {
  const response = await fetch(`${getApiBaseUrl()}/payments/toss/billing/charges/${chargeId}/retry`, {
    method: 'POST',
    credentials: 'include',
    headers: withAuth(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({
      methodId: input.methodId,
      setAsDefault: input.setAsDefault ?? false,
    }),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as TossBillingCharge;
}

export async function startTossBillingSubscription(
  accessToken: string,
  input?: {
    readonly amount?: number;
    readonly orderName?: string;
    readonly chargeImmediately?: boolean;
  },
): Promise<TossBillingSubscription> {
  const response = await fetch(`${getApiBaseUrl()}/payments/toss/billing/subscriptions/start`, {
    method: 'POST',
    credentials: 'include',
    headers: withAuth(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({
      amount: input?.amount ?? BILLING_MONTHLY_AMOUNT,
      orderName: input?.orderName ?? BILLING_DEFAULT_ORDER_NAME,
      chargeImmediately: input?.chargeImmediately ?? true,
    }),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as TossBillingSubscription;
}

export async function cancelTossBillingSubscription(accessToken: string): Promise<TossBillingSubscription> {
  const response = await fetch(`${getApiBaseUrl()}/payments/toss/billing/subscriptions/cancel`, {
    method: 'POST',
    credentials: 'include',
    headers: withAuth(accessToken),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as TossBillingSubscription;
}

export async function withdrawTossBillingCancel(accessToken: string): Promise<TossBillingSubscription> {
  const response = await fetch(`${getApiBaseUrl()}/payments/toss/billing/subscriptions/withdraw-cancel`, {
    method: 'POST',
    credentials: 'include',
    headers: withAuth(accessToken),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as TossBillingSubscription;
}

export async function chargeTossBillingNow(
  accessToken: string,
  input?: { readonly amount?: number; readonly orderName?: string },
): Promise<TossBillingCharge> {
  const response = await fetch(`${getApiBaseUrl()}/payments/toss/billing/charge`, {
    method: 'POST',
    credentials: 'include',
    headers: withAuth(accessToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({
      amount: input?.amount ?? BILLING_MONTHLY_AMOUNT,
      orderName: input?.orderName ?? BILLING_DEFAULT_ORDER_NAME,
    }),
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }
  return (await response.json()) as TossBillingCharge;
}
