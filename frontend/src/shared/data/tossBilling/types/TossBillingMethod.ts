export type TossBillingMethod = {
  readonly id: number;
  readonly customerKey: string;
  readonly cardCompany: string | null;
  readonly cardNumber: string | null;
  readonly cardType: string | null;
  readonly ownerType: string | null;
  readonly active: boolean;
  readonly isDefault: boolean;
  readonly authenticatedAt: string | null;
};
