import { Fragment, useCallback, useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import { currencyBaseCode, fetchExchangeBranches, type ExchangeBranchItem } from '../../../shared/data/exchangeBranchesApi';
import {
  buildMergedExchangeOptions,
  getExchangeBaseLabel,
  mergeBranchTableCurrencyBases,
} from '../../../shared/currency/exchangeCurrencies';
import { fetchDailyExchangeRates, type ExchangeRateItem } from '../../../shared/data/exchangeRateApi';
import { TealCurrencySelect } from './TealCurrencySelect';
import styles from './ExchangeRatePage.module.css';

const KRW_CODE = 'KRW';

const EXCHANGE_ARTICLE_THUMB = {
  guide: '/exchange-article-guide-thumb.png',
  today: '/exchange-article-today-thumb.png',
  reviews: '/exchange-article-reviews-thumb.png',
} as const;

function formatUpdateTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

function formatBranchRate(n: number): string {
  if (!Number.isFinite(n)) return '—';
  const s = String(n);
  if (s.includes('e') || s.includes('E')) return n.toPrecision(6);
  return s.replace(/\.?0+$/, '');
}

function formatDrivingMinutes(min: number | null): string {
  if (min === null) return '—';
  return `약 ${min}분`;
}

function mapsSearchUrl(lat: number, lng: number): string {
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${lat},${lng}`)}`;
}

function parseNumeric(s: string): number {
  const n = parseFloat(s.replace(/,/g, '').trim());
  return Number.isFinite(n) ? n : NaN;
}

function krwPerOneUnit(item: ExchangeRateItem): number {
  const deal = parseNumeric(item.dealBasR);
  if (!Number.isFinite(deal)) return NaN;
  const m = item.curUnit.match(/\((\d+)\)\s*$/);
  const divisor = m ? parseInt(m[1], 10) : 1;
  return deal / divisor;
}

type Option = ReturnType<typeof buildMergedExchangeOptions>[number];

/** curUnit이 JPY(100)처럼 괄호 단위를 쓰는데 state가 JPY만 있을 때 등, ISO 베이스로 매칭 */
function findRateOption(options: readonly Option[], code: string): Option | undefined {
  const trimmed = code.trim();
  if (!trimmed) return undefined;
  const exact = options.find(o => o.code === trimmed);
  if (exact?.item != null) return exact;
  const base = currencyBaseCode(trimmed);
  if (base === KRW_CODE) {
    return options.find(o => currencyBaseCode(o.code) === KRW_CODE) ?? exact;
  }
  if (!/^[A-Z]{3}$/.test(base)) {
    return exact;
  }
  const sameBase = options.filter(o => currencyBaseCode(o.code) === base);
  const withItem = sameBase.find(o => o.item != null);
  return withItem ?? exact ?? sameBase[0];
}

export function ExchangeRateSection() {
  const { accessToken, authReady } = useAuth();
  const [rates, setRates] = useState<readonly ExchangeRateItem[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [fromCode, setFromCode] = useState('JPY');
  const [toCode, setToCode] = useState(KRW_CODE);
  const [amountText, setAmountText] = useState('1');
  const [resultText, setResultText] = useState('—');
  const [alertTarget, setAlertTarget] = useState('9.73');
  const [branches, setBranches] = useState<readonly ExchangeBranchItem[]>([]);
  const [branchError, setBranchError] = useState<string | null>(null);
  const [branchUpdatedAt, setBranchUpdatedAt] = useState<Date | null>(null);
  const [branchLoading, setBranchLoading] = useState(false);
  const [branchListCurrency, setBranchListCurrency] = useState('JPY');
  const [alertFromCurrency, setAlertFromCurrency] = useState('CNY');
  const [appUpdateModalOpen, setAppUpdateModalOpen] = useState(false);

  const options = useMemo(() => buildMergedExchangeOptions(rates), [rates]);

  const branchCurrencySelectOptions = useMemo(
    () => mergeBranchTableCurrencyBases(rates, branchListCurrency),
    [rates, branchListCurrency],
  );

  const alertSelectOptions = useMemo(
    () => branchCurrencySelectOptions.map(c => ({ value: c, label: c })),
    [branchCurrencySelectOptions],
  );

  useEffect(() => {
    setAlertFromCurrency(prev => {
      if (branchCurrencySelectOptions.includes(prev)) return prev;
      if (branchCurrencySelectOptions.includes('CNY')) return 'CNY';
      return (
        branchCurrencySelectOptions.find(c => c !== KRW_CODE) ??
        branchCurrencySelectOptions[0] ??
        'JPY'
      );
    });
  }, [branchCurrencySelectOptions]);

  useEffect(() => {
    if (!rates.length) return;
    setBranchListCurrency(prev => {
      const opts = mergeBranchTableCurrencyBases(rates, prev);
      if (/^[A-Z]{3}$/.test(prev) && opts.includes(prev)) return prev;
      const jpy = opts.find(c => c === 'JPY');
      return jpy ?? opts.find(c => c !== KRW_CODE) ?? prev;
    });
  }, [rates]);

  /** API가 JPY(100) 등으로 갱신되면 동일 통화의 `value` 문자열을 옵션에 맞춤(베이스만 일치할 때는 고시 row가 있는 code로 정규화) */
  useEffect(() => {
    if (options.length === 0) return;
    const resolve = (prev: string, fallback: string): string => {
      const b = currencyBaseCode(prev) || prev;
      const same = options.filter(o => currencyBaseCode(o.code) === b);
      if (b === KRW_CODE) {
        const k = same[0] ?? options.find(o => currencyBaseCode(o.code) === KRW_CODE);
        return k?.code ?? fallback;
      }
      const exact = same.find(o => o.code === prev);
      if (exact?.item != null) return prev;
      const withItem = same.find(o => o.item != null);
      if (withItem) return withItem.code;
      return same[0]?.code ?? options.find(o => currencyBaseCode(o.code) === b)?.code ?? fallback;
    };
    setFromCode(prev => resolve(prev, options[0]!.code));
    setToCode(prev => resolve(prev, KRW_CODE));
  }, [options]);

  useEffect(() => {
    if (!authReady) return;
    if (!accessToken) {
      setLoadError('로그인이 필요합니다.');
      return;
    }
    let cancelled = false;
    void (async () => {
      try {
        const list = await fetchDailyExchangeRates(accessToken);
        if (cancelled) return;
        setRates(list);
        if (list.length === 0) {
          setLoadError('공식 환율이 아직 없습니다. 비영업일이거나 한국수출입은행 당일 고시 전일 수 있습니다.');
          return;
        }
        setLoadError(null);
        const jpy = list.find(r => r.curUnit.startsWith('JPY'));
        if (jpy) {
          setFromCode(jpy.curUnit);
          setToCode(KRW_CODE);
        }
      } catch (e) {
        if (!cancelled) {
          setLoadError(e instanceof Error ? e.message : '환율을 불러오지 못했습니다.');
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [authReady, accessToken]);

  useEffect(() => {
    if (!authReady || !accessToken) {
      setBranches([]);
      setBranchError(null);
      setBranchUpdatedAt(null);
      return;
    }
    const code = branchListCurrency.trim();
    if (!code) return;
    let cancelled = false;
    setBranchLoading(true);
    void (async () => {
      try {
        const list = await fetchExchangeBranches(accessToken, code);
        if (cancelled) return;
        setBranches(list);
        setBranchError(null);
        setBranchUpdatedAt(new Date());
      } catch (e) {
        if (!cancelled) {
          setBranches([]);
          setBranchError(e instanceof Error ? e.message : '환전소 목록을 불러오지 못했습니다.');
          setBranchUpdatedAt(null);
        }
      } finally {
        if (!cancelled) setBranchLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [authReady, accessToken, branchListCurrency]);

  const convert = useCallback(
    (amount: number, from: string, to: string): number | null => {
      if (!Number.isFinite(amount)) return null;
      const fromBase = currencyBaseCode(from) || from;
      const toBase = currencyBaseCode(to) || to;
      if (fromBase === toBase) {
        return amount;
      }
      const toKrw = (code: string, amt: number): number | null => {
        const b = currencyBaseCode(code) || code;
        if (b === KRW_CODE) return amt;
        const opt = findRateOption(options, code);
        if (!opt?.item) return null;
        const k = krwPerOneUnit(opt.item);
        if (!Number.isFinite(k) || k <= 0) return null;
        return amt * k;
      };
      const fromKrw = (code: string, krw: number): number | null => {
        const b = currencyBaseCode(code) || code;
        if (b === KRW_CODE) return krw;
        const opt = findRateOption(options, code);
        if (!opt?.item) return null;
        const k = krwPerOneUnit(opt.item);
        if (!Number.isFinite(k) || k <= 0) return null;
        return krw / k;
      };
      const mid = toKrw(from, amount);
      if (mid == null) return null;
      return fromKrw(to, mid);
    },
    [options],
  );

  useEffect(() => {
    const amt = parseFloat(amountText.replace(/,/g, ''));
    if (!Number.isFinite(amt)) {
      setResultText('—');
      return;
    }
    const out = convert(amt, fromCode, toCode);
    if (out == null) {
      setResultText('—');
      return;
    }
    const rounded = Math.round(out * 1e6) / 1e6;
    setResultText(
      Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(4).replace(/\.?0+$/, ''),
    );
  }, [amountText, fromCode, toCode, convert]);

  useEffect(() => {
    if (!appUpdateModalOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setAppUpdateModalOpen(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [appUpdateModalOpen]);

  return (
    <section className={styles.exchangePage} id="exchange-rate" aria-labelledby="exchange-rate-title">
      <div className={styles.banner}>
        <img
          src="/exchange-rate-coupon-banner.png"
          alt="IdolGlow 쿠폰팩. 호텔, 환전, 여행자 보험까지"
          className={styles.bannerImg}
          width={1024}
          height={154}
          decoding="async"
        />
      </div>

      <div className={styles.calcCard}>
        <h2 id="exchange-rate-title" className={styles.exchangeTitle}>
          공식 환율 계산기
        </h2>
        {loadError ? <p className={styles.errorText}>{loadError}</p> : null}
        <div className={styles.ctCalcRow}>
          <div className={styles.ctCalcCol}>
            <TealCurrencySelect
              variant="calcCol"
              aria-label="기준 통화"
              value={fromCode}
              onChange={setFromCode}
              options={options.map(o => ({
                value: o.code,
                label: currencyBaseCode(o.code) || o.code,
              }))}
            />
            <div className={styles.ctAmountWrap}>
              <input
                className={styles.ctAmountInput}
                type="text"
                inputMode="decimal"
                value={amountText}
                onChange={e => setAmountText(e.target.value)}
                aria-label="환산할 금액"
              />
            </div>
          </div>
          <span className={styles.ctCalcEquals} aria-hidden="true">
            =
          </span>
          <div className={styles.ctCalcCol}>
            <TealCurrencySelect
              variant="calcCol"
              aria-label="환산 통화"
              value={toCode}
              onChange={setToCode}
              options={options.map(o => ({
                value: o.code,
                label: currencyBaseCode(o.code) || o.code,
              }))}
            />
            <div className={styles.ctAmountWrap}>
              <input className={styles.ctAmountInput} type="text" readOnly value={resultText} aria-label="환산 결과" />
            </div>
          </div>
        </div>
        <p className={styles.hint}>
          <strong>한국수출입은행</strong> 일일 환율(매매기준율, AP01)입니다. 비영업일·영업 당일 11시 이전 조회는 값이
          없을 수 있습니다(당행 기준 11시 전후 갱신). <code className={styles.hintCode}>JPY(100)</code> 등
          괄호는 해당 단위 기준 → 1단위로 환산해 계산합니다.
        </p>
      </div>

      <div className={styles.noticeCard}>
        <div className={styles.noticePill}>
          <span className={styles.bell} aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2zm-2 1H8v-6c0-2.48 1.51-4.5 4-4.5s4 2.02 4 4.5v6z" />
            </svg>
          </span>
          환율 공지
        </div>
        <div className={styles.noticeInfoBox}>
          <span className={styles.noticeInfoIcon} aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" />
            </svg>
          </span>
          <span>선호하는 환율을 설정하고 알림을 받으세요</span>
        </div>
        <div className={styles.alertRowCt}>
          <span className={styles.alertOne}>1</span>
          <div className={styles.alertSelectWrap}>
            <TealCurrencySelect
              variant="calcCol"
              aria-label="알림 기준 통화"
              value={alertFromCurrency}
              onChange={setAlertFromCurrency}
              options={alertSelectOptions}
            />
          </div>
          <span className={styles.alertEq}>=</span>
          <div className={styles.alertInputWrap}>
            <input
              className={styles.alertTargetFull}
              type="text"
              inputMode="decimal"
              value={alertTarget}
              onChange={e => setAlertTarget(e.target.value)}
              aria-label="목표 환율"
            />
          </div>
          <span className={styles.alertKrw}>KRW</span>
        </div>
        <button type="button" className={styles.cta} onClick={() => setAppUpdateModalOpen(true)}>
          알림 받으세요
        </button>
      </div>

      <div className={styles.ratesCard} aria-labelledby="exchange-branches-title">
        <div className={styles.branchScrollArea}>
          <h3 id="exchange-branches-title" className={styles.branchHeroTitle}>
            환율
          </h3>
          {branchUpdatedAt && !branchLoading ? (
            <p className={styles.branchHeroSub}>업데이트 시간 : {formatUpdateTime(branchUpdatedAt)}</p>
          ) : null}
          {accessToken ? (
            <table className={styles.ctTable}>
              <thead>
                <tr>
                  <th scope="col" className={styles.ctThFirst}>
                    이름
                  </th>
                  <th scope="col" className={styles.ctThMid}>
                    {branchCurrencySelectOptions.length > 0 ? (
                      <TealCurrencySelect
                        variant="table"
                        aria-label="환전소 표 통화"
                        value={branchListCurrency}
                        onChange={setBranchListCurrency}
                        options={branchCurrencySelectOptions.map(c => ({
                          value: c,
                          label: getExchangeBaseLabel(c, 'short'),
                        }))}
                      />
                    ) : (
                      <span className={styles.ctThSelectText}>{branchListCurrency}</span>
                    )}
                  </th>
                  <th scope="col" className={styles.ctThLast}>
                    공항에서 필요한 시간
                  </th>
                </tr>
              </thead>
              <tbody>
                {branchLoading ? (
                  <tr className={styles.ctTr}>
                    <td colSpan={3} className={styles.ctEmptyStateTd}>
                      <p className={`${styles.branchHeroSub} ${styles.ctEmptyStateMsg}`}>
                        환전소·이동 시간을 불러오는 중…
                      </p>
                    </td>
                  </tr>
                ) : branchError ? (
                  <tr className={styles.ctTr}>
                    <td colSpan={3} className={styles.ctEmptyStateTd}>
                      <p className={`${styles.errorText} ${styles.ctEmptyStateMsg}`}>{branchError}</p>
                    </td>
                  </tr>
                ) : branches.length === 0 ? (
                  <tr className={styles.ctTr}>
                    <td colSpan={3} className={styles.ctEmptyStateTd}>
                      <p className={`${styles.branchEmptyLine} ${styles.ctEmptyStateMsg}`}>
                        이 통화({branchListCurrency})에 등록된 환전소가 없습니다. 다른 통화를 선택해 보세요.
                      </p>
                    </td>
                  </tr>
                ) : (
                  branches.map((b, i) => (
                    <Fragment key={b.branchId}>
                      <tr
                        className={
                          b.airportHub
                            ? `${styles.ctTr} ${styles.ctTrHub}`
                            : `${styles.ctTr} ${styles.ctTrHover}`
                        }
                      >
                        <th scope="row" className={b.airportHub ? styles.ctNameHub : styles.ctName}>
                          {b.name}
                        </th>
                        <td className={b.airportHub ? styles.ctRateHub : styles.ctRate}>{formatBranchRate(b.rate)}</td>
                        <td className={styles.ctTimeTd}>
                          <div className={styles.ctTimeStack}>
                            <a
                              href={mapsSearchUrl(b.lat, b.lng)}
                              target="_blank"
                              rel="noopener noreferrer"
                              className={styles.ctMapLink}
                              aria-label={`${b.name} 위치 지도`}
                            >
                              <span className={styles.ctMapIcon} aria-hidden="true">
                                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                                  <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5S10.62 6.5 12 6.5s2.5 1.12 2.5 2.5S13.38 11.5 12 11.5z" />
                                </svg>
                              </span>
                            </a>
                            <span className={b.airportHub ? styles.ctTimeLabelHub : styles.ctTimeLabel}>
                              {formatDrivingMinutes(b.durationMinutesFromAirport)}
                            </span>
                          </div>
                        </td>
                      </tr>
                      {b.airportHub && i === 0 ? (
                        <tr className={styles.ctPromoTr}>
                          <td colSpan={3} className={styles.ctPromoTd}>
                            <Link to="/glow" className={styles.ctPromoAnchor}>
                              <div className={styles.ctPromoRow}>
                                <div className={styles.ctPromoCoin} aria-hidden="true">
                                  ₩
                                </div>
                                <div className={styles.ctPromoTexts}>
                                  <span className={styles.ctPromoMuted}>공항에서 저렴하게 환전하는 방법</span>
                                  <span className={styles.ctPromoBold}>공항에서 바로 환전하고 여행을 즐기세요</span>
                                </div>
                                <span className={styles.ctPromoArrow} aria-hidden="true">
                                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                                    <path
                                      d="m9 18 6-6-6-6"
                                      stroke="currentColor"
                                      strokeWidth="1.4"
                                      strokeLinecap="round"
                                      strokeLinejoin="round"
                                    />
                                  </svg>
                                </span>
                              </div>
                            </Link>
                          </td>
                        </tr>
                      ) : null}
                    </Fragment>
                  ))
                )}
              </tbody>
            </table>
          ) : null}
        </div>
      </div>

      <nav className={styles.exchangeArticleList} aria-label="환율 관련 읽을거리">
        <Link to="/articles" className={styles.exchangeArticleCard} aria-label="통화 교환 가이드 — 한국의 환전">
          <div className={styles.exchangeArticleThumb}>
            <img
              src={EXCHANGE_ARTICLE_THUMB.guide}
              alt=""
              width={628}
              height={411}
              loading="lazy"
              decoding="async"
            />
          </div>
          <div className={styles.exchangeArticleBody}>
            <span className={styles.exchangeArticleTitle}>통화 교환 가이드</span>
            <span className={styles.exchangeArticleSub}>한국의 환전</span>
          </div>
          <span className={styles.exchangeArticleChevron} aria-hidden>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path
                d="m9 18 6-6-6-6"
                stroke="currentColor"
                strokeWidth="1.4"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </span>
        </Link>
        <Link
          to="/exchange-rate#exchange-rate-title"
          className={styles.exchangeArticleCard}
          aria-label="오늘의 환율 — 한국에 도착하면 공항에서 싸게 환전하세요"
        >
          <div className={styles.exchangeArticleThumb}>
            <img
              src={EXCHANGE_ARTICLE_THUMB.today}
              alt=""
              width={1024}
              height={537}
              loading="lazy"
              decoding="async"
            />
          </div>
          <div className={styles.exchangeArticleBody}>
            <span className={styles.exchangeArticleTitle}>오늘의 환율</span>
            <span className={styles.exchangeArticleSub}>한국에 도착하면 공항에서 싸게 환전하세요!</span>
          </div>
          <span className={styles.exchangeArticleChevron} aria-hidden>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path
                d="m9 18 6-6-6-6"
                stroke="currentColor"
                strokeWidth="1.4"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </span>
        </Link>
        <Link
          to="/glow"
          className={styles.exchangeArticleCard}
          aria-label="리뷰 찾고 있어요 — 한국 여행 경험에 대해 말씀해 주세요"
        >
          <div className={styles.exchangeArticleThumb}>
            <img
              src={EXCHANGE_ARTICLE_THUMB.reviews}
              alt=""
              width={627}
              height={398}
              loading="lazy"
              decoding="async"
            />
          </div>
          <div className={styles.exchangeArticleBody}>
            <span className={styles.exchangeArticleTitle}>리뷰 찾고 있어요</span>
            <span className={styles.exchangeArticleSub}>한국 여행 경험에 대해 말씀해 주세요 🙌 (포인트 선물)</span>
          </div>
          <span className={styles.exchangeArticleChevron} aria-hidden>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path
                d="m9 18 6-6-6-6"
                stroke="currentColor"
                strokeWidth="1.4"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </span>
        </Link>
      </nav>

      {appUpdateModalOpen
        ? createPortal(
            <div
              className={styles.updateModalBackdrop}
              role="presentation"
              onClick={e => {
                if (e.target === e.currentTarget) setAppUpdateModalOpen(false);
              }}
            >
              <div
                className={styles.updateModalPanel}
                role="dialog"
                aria-modal="true"
                aria-labelledby="exchange-app-update-modal-title"
                onClick={e => e.stopPropagation()}
              >
                <button
                  type="button"
                  className={styles.updateModalClose}
                  aria-label="닫기"
                  onClick={() => setAppUpdateModalOpen(false)}
                >
                  <span aria-hidden="true">×</span>
                </button>
                <p id="exchange-app-update-modal-title" className={styles.updateModalText}>
                  {"앱을 업데이트해야 합니다. \"확인\"을 눌러 스토어로 가세요."}
                </p>
                <button type="button" className={styles.updateModalConfirm} onClick={() => setAppUpdateModalOpen(false)}>
                  확인해
                </button>
              </div>
            </div>,
            document.body,
          )
        : null}
    </section>
  );
}
