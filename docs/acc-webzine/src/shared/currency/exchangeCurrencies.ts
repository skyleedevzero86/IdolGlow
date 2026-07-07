import { currencyBaseCode } from '../data/exchangeBranchesApi';
import type { ExchangeRateItem } from '../data/exchangeRateApi';

/**
 * UI 기본 노출 순서: KRW, CNY, EUR, HKD, JPY, MNT, RUB, THB, TWD, USD, VND.
 * API에 없더라도 선택지는 유지하고, 실제 환산은 해당 통화 row가 있을 때만 사용한다.
 */
export const PREFERRED_EXCHANGE_BASE_CODES: readonly string[] = [
  'KRW',
  'CNY',
  'EUR',
  'HKD',
  'JPY',
  'MNT',
  'RUB',
  'THB',
  'TWD',
  'USD',
  'VND',
];

const BASE_NAME_KO: Record<string, string> = {
  KRW: '\uD55C\uAD6D \uC6D0',
  CNY: '\uC704\uC548(\uC911\uAD6D)',
  EUR: '\uC720\uB85C',
  HKD: '\uD64D\uCF69 \uB2EC\uB7EC',
  JPY: '\uC5D4(\uC77C\uBCF8)',
  MNT: '\uD22C\uADF8\uB9AD(\uBABD\uACE8)',
  RUB: '\uB8E8\uBE14(\uB7EC\uC2DC\uC544)',
  THB: '\uBC14\uD2B8(\uD0DC\uAD6D)',
  TWD: '\uB300\uB9CC \uB2EC\uB7EC',
  USD: '\uB2EC\uB7EC(\uBBF8\uAD6D)',
  VND: '\uB3D9(\uBCA0\uD2B8\uB0A8)',
};

const BASE_SHORT_NAME_KO: Record<string, string> = {
  KRW: '\uD55C\uAD6D',
  CNY: '\uC911\uAD6D',
  EUR: '\uC720\uB85C',
  HKD: '\uD64D\uCF69',
  JPY: '\uC77C\uBCF8',
  MNT: '\uBABD\uACE8',
  RUB: '\uB7EC\uC2DC\uC544',
  THB: '\uD0DC\uAD6D',
  TWD: '\uB300\uB9CC',
  USD: '\uBBF8\uAD6D',
  VND: '\uBCA0\uD2B8\uB0A8',
};

export type MergedExchangeOption = {
  readonly code: string;
  readonly label: string;
  readonly item?: ExchangeRateItem;
};

function indexRatesByBase(rates: readonly ExchangeRateItem[]): Map<string, ExchangeRateItem> {
  const m = new Map<string, ExchangeRateItem>();
  for (const r of rates) {
    const b = currencyBaseCode(r.curUnit);
    if (!/^[A-Z]{3}$/.test(b) || m.has(b)) continue;
    m.set(b, r);
  }
  return m;
}

/** 선호 통화(고정 순서) + API에만 있는 기타 통화(알파벳순) */
export function buildMergedExchangeOptions(rates: readonly ExchangeRateItem[]): MergedExchangeOption[] {
  const idx = indexRatesByBase(rates);
  const out: MergedExchangeOption[] = [];
  const seen = new Set<string>();

  for (const base of PREFERRED_EXCHANGE_BASE_CODES) {
    const item = idx.get(base);
    const code = item ? item.curUnit : base;
    const nameKo = BASE_NAME_KO[base] ?? base;
    out.push({
      code,
      label: item ? `${item.curUnit} · ${item.curNm}` : `${base} · ${nameKo}`,
      item,
    });
    seen.add(base);
  }

  const extra: MergedExchangeOption[] = [];
  for (const r of rates) {
    const b = currencyBaseCode(r.curUnit);
    if (!/^[A-Z]{3}$/.test(b) || seen.has(b)) continue;
    seen.add(b);
    extra.push({ code: r.curUnit, label: `${r.curUnit} · ${r.curNm}`, item: r });
  }
  extra.sort((a, b) => currencyBaseCode(a.code).localeCompare(currencyBaseCode(b.code)));
  return [...out, ...extra];
}

export function mergeBranchTableCurrencyBases(
  rates: readonly ExchangeRateItem[],
  currentBranchCurrency: string,
): string[] {
  const s = new Set<string>(PREFERRED_EXCHANGE_BASE_CODES);
  for (const r of rates) {
    const c = currencyBaseCode(r.curUnit);
    if (/^[A-Z]{3}$/.test(c)) s.add(c);
  }
  if (currentBranchCurrency && /^[A-Z]{3}$/.test(currentBranchCurrency)) s.add(currentBranchCurrency);
  return [...s].sort((a, b) => a.localeCompare(b));
}

export function getExchangeBaseLabel(code: string, variant: 'full' | 'short' = 'full'): string {
  const base = currencyBaseCode(code) || code.trim().toUpperCase();
  if (!base) return code;
  const labels = variant === 'short' ? BASE_SHORT_NAME_KO : BASE_NAME_KO;
  return labels[base] ?? base;
}
