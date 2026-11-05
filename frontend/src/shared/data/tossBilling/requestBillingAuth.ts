import { loadTossPaymentsSdk } from './loadTossPaymentsSdk';
import type { TossBillingClientConfig } from './types/TossBillingClientConfig';

export async function requestBillingAuth(options: {
  readonly config: TossBillingClientConfig;
  readonly customerEmail?: string | null;
  readonly customerName?: string | null;
}): Promise<void> {
  const TossPayments = await loadTossPaymentsSdk();
  const tossPayments = TossPayments(options.config.clientKey);
  const payment = tossPayments.payment({ customerKey: options.config.customerKey });
  await payment.requestBillingAuth({
    method: 'CARD',
    successUrl: options.config.successUrl,
    failUrl: options.config.failUrl,
    customerEmail: options.customerEmail?.trim() || undefined,
    customerName: options.customerName?.trim() || undefined,
  });
}
