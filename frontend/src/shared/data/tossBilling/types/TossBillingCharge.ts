export type TossBillingChargeStatus = 'PENDING' | 'DONE' | 'FAILED';

export type TossBillingChargeAttempt = {
  readonly id: number;
  readonly billingMethodId: number;
  readonly cardCompany: string | null;
  readonly cardNumber: string | null;
  readonly attemptNo: number;
  readonly tossOrderId: string;
  readonly paymentKey: string | null;
  readonly status: TossBillingChargeStatus;
  readonly failMessage: string | null;
  readonly approvedAt: string | null;
};

export type TossBillingCharge = {
  readonly id: number;
  readonly orderId: string;
  readonly orderName: string;
  readonly amount: number;
  readonly billingMethodId: number;
  readonly paymentKey: string | null;
  readonly status: TossBillingChargeStatus;
  readonly failMessage: string | null;
  readonly approvedAt: string | null;
  readonly attempts: readonly TossBillingChargeAttempt[];
};
