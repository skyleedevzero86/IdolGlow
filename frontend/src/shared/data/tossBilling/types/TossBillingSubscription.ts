import type { TossBillingMethod } from './TossBillingMethod';

export type TossBillingSubscriptionStatus =
  | 'ACTIVE'
  | 'PAUSED'
  | 'CANCEL_SCHEDULED'
  | 'CANCELED';

export type TossBillingSubscription = {
  readonly id: number;
  readonly planCode: string;
  readonly orderName: string;
  readonly amount: number;
  readonly status: TossBillingSubscriptionStatus;
  readonly nextBillingAt: string | null;
  readonly lastChargedAt: string | null;
  readonly failCount: number;
  readonly retryAt: string | null;
  readonly endsAt: string | null;
  readonly billingMethod: TossBillingMethod;
};
