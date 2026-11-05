import type { TossBillingCharge } from './TossBillingCharge';
import type { TossBillingMethod } from './TossBillingMethod';
import type { TossBillingSubscription } from './TossBillingSubscription';

export type TossBillingOverview = {
  readonly method: TossBillingMethod | null;
  readonly methods: readonly TossBillingMethod[];
  readonly subscription: TossBillingSubscription | null;
  readonly charges: readonly TossBillingCharge[];
};
