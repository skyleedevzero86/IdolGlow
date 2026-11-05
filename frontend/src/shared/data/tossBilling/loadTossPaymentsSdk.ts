type TossPaymentsFactory = (clientKey: string) => {
  payment: (options: { customerKey: string }) => {
    requestBillingAuth: (options: {
      method: 'CARD';
      successUrl: string;
      failUrl: string;
      customerEmail?: string;
      customerName?: string;
    }) => Promise<void>;
  };
};

declare global {
  interface Window {
    TossPayments?: TossPaymentsFactory;
  }
}

const SDK_SRC = 'https://js.tosspayments.com/v2/standard';

export async function loadTossPaymentsSdk(): Promise<TossPaymentsFactory> {
  if (typeof window.TossPayments === 'function') {
    return window.TossPayments;
  }

  await new Promise<void>((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(`script[src="${SDK_SRC}"]`);
    if (existing) {
      existing.addEventListener('load', () => resolve(), { once: true });
      existing.addEventListener('error', () => reject(new Error('토스페이먼츠 SDK 로드에 실패했습니다.')), {
        once: true,
      });
      return;
    }
    const script = document.createElement('script');
    script.src = SDK_SRC;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('토스페이먼츠 SDK 로드에 실패했습니다.'));
    document.head.appendChild(script);
  });

  if (typeof window.TossPayments !== 'function') {
    throw new Error('토스페이먼츠 SDK를 초기화하지 못했습니다.');
  }
  return window.TossPayments;
}
