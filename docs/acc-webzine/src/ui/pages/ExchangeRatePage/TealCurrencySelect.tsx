import {
  useCallback,
  useEffect,
  useId,
  useLayoutEffect,
  useRef,
  useState,
  type KeyboardEvent,
} from 'react';
import { createPortal } from 'react-dom';
import { currencyToFlagCountry } from '../../../shared/currency/currencyToFlagCountry';
import { currencyBaseCode } from '../../../shared/data/exchangeBranchesApi';
import styles from './TealCurrencySelect.module.css';

type Option = { readonly value: string; readonly label?: string };

type TealCurrencySelectProps = {
  readonly value: string;
  readonly onChange: (value: string) => void;
  readonly options: readonly Option[];
  readonly 'aria-label': string;
  readonly variant: 'field' | 'calcCol' | 'table';
  readonly disabled?: boolean;
};

const MENU_GAP = 2;

function CurrencyFlagThumb({ unit, px }: { readonly unit: string; readonly px: number }) {
  const [imgErr, setImgErr] = useState(false);
  const base = currencyBaseCode(unit);
  const cc = currencyToFlagCountry(base);
  const h = Math.max(12, Math.round(px * 0.72));

  if (!cc || imgErr) {
    return (
      <span className={styles.currencyFlagBox} style={{ width: px, height: h }} aria-hidden>
        <span className={styles.currencyFlagFallback}>{base.slice(0, 2) || '--'}</span>
      </span>
    );
  }

  return (
    <span className={styles.currencyFlagBox} style={{ width: px, height: h }} aria-hidden>
      <img
        className={styles.currencyFlagImg}
        src={`https://flagcdn.com/w40/${cc}.png`}
        srcSet={`https://flagcdn.com/w80/${cc}.png 2x`}
        alt=""
        width={px}
        height={h}
        loading="lazy"
        decoding="async"
        onError={() => setImgErr(true)}
      />
    </span>
  );
}

export function TealCurrencySelect({
  value,
  onChange,
  options,
  'aria-label': ariaLabel,
  variant,
  disabled = false,
}: TealCurrencySelectProps) {
  const baseId = useId();
  const [open, setOpen] = useState(false);
  const [listStyle, setListStyle] = useState<{ top: number; left: number; width: number } | null>(null);
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const listRef = useRef<HTMLUListElement | null>(null);

  const listboxId = `${baseId}-listbox`;

  const updatePosition = useCallback(() => {
    const el = triggerRef.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    setListStyle({
      top: r.bottom + MENU_GAP,
      left: r.left,
      width: r.width,
    });
  }, []);

  useLayoutEffect(() => {
    if (!open) return;
    updatePosition();
  }, [open, updatePosition, value, options.length]);

  useEffect(() => {
    if (!open) return;
    const onScroll = () => updatePosition();
    const onResize = () => updatePosition();
    window.addEventListener('scroll', onScroll, true);
    window.addEventListener('resize', onResize);
    return () => {
      window.removeEventListener('scroll', onScroll, true);
      window.removeEventListener('resize', onResize);
    };
  }, [open, updatePosition]);

  useEffect(() => {
    if (!open) return;
    const onDocDown = (e: MouseEvent) => {
      const target = e.target;
      if (!(target instanceof Node)) return;
      if (triggerRef.current?.contains(target) || listRef.current?.contains(target)) return;
      setOpen(false);
    };
    document.addEventListener('mousedown', onDocDown);
    return () => document.removeEventListener('mousedown', onDocDown);
  }, [open]);

  const activeIndex = options.findIndex(o => o.value === value);
  const [highlight, setHighlight] = useState(0);

  useEffect(() => {
    if (open) {
      setHighlight(activeIndex >= 0 ? activeIndex : 0);
    }
  }, [open, activeIndex, options.length]);

  const flagUnit = value || '--';
  const flagPx = variant === 'table' ? 16 : variant === 'calcCol' ? 18 : 20;
  const items = options.length ? options : [{ value: value || '--', label: value || '--' }];
  const selectedOption = items.find(o => o.value === value);
  const displayLabel = selectedOption?.label ?? (value ? currencyBaseCode(value) || value : '--');

  const onKeyDown = (e: KeyboardEvent) => {
    if (disabled) return;

    if (e.key === 'Escape' && open) {
      e.preventDefault();
      setOpen(false);
      return;
    }

    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      if (open) {
        const option = items[Math.min(highlight, items.length - 1)];
        if (option) {
          onChange(option.value);
        }
        setOpen(false);
      } else {
        setOpen(true);
      }
      return;
    }

    if (!open && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) {
      e.preventDefault();
      setOpen(true);
      return;
    }

    if (open && e.key === 'ArrowDown') {
      e.preventDefault();
      setHighlight(i => Math.min(i + 1, items.length - 1));
      return;
    }

    if (open && e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlight(i => Math.max(i - 1, 0));
    }
  };

  return (
    <>
      <div
        className={
          variant === 'field' ? styles.wrap : variant === 'calcCol' ? styles.wrapCalcCol : styles.tableWrap
        }
      >
        <button
          type="button"
          ref={triggerRef}
          id={`${baseId}-trigger`}
          className={[
            styles.trigger,
            variant === 'field'
              ? styles.triggerField
              : variant === 'calcCol'
                ? styles.triggerCalcCol
                : styles.triggerTable,
          ]
            .filter(Boolean)
            .join(' ')}
          aria-label={ariaLabel}
          aria-haspopup="listbox"
          aria-expanded={open}
          aria-controls={listboxId}
          disabled={disabled || options.length === 0}
          onKeyDown={onKeyDown}
          onClick={() => {
            if (!disabled && options.length) {
              setOpen(v => !v);
            }
          }}
        >
          <span className={styles.triggerMain}>
            <CurrencyFlagThumb unit={flagUnit} px={flagPx} />
            <span className={styles.triggerValue}>{displayLabel}</span>
          </span>
          <span className={[styles.chevron, open ? styles.chevronOpen : ''].filter(Boolean).join(' ')} aria-hidden>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
              <path d="M7 10l5 5 5-5z" />
            </svg>
          </span>
        </button>
      </div>

      {open && listStyle
        ? createPortal(
            <ul
              id={listboxId}
              ref={listRef}
              role="listbox"
              className={styles.list}
              style={{
                position: 'fixed',
                zIndex: 10050,
                top: listStyle.top,
                left: listStyle.left,
                width: Math.max(listStyle.width, 168),
              }}
            >
              {items.map((opt, idx) => (
                <li key={`${opt.value}-${idx}`} role="none">
                  <button
                    type="button"
                    role="option"
                    aria-selected={value === opt.value}
                    className={[
                      styles.item,
                      value === opt.value ? styles.itemActive : idx === highlight ? styles.itemHighlight : '',
                    ]
                      .filter(Boolean)
                      .join(' ')}
                    onMouseDown={event => {
                      event.preventDefault();
                    }}
                    onClick={() => {
                      onChange(opt.value);
                      setOpen(false);
                    }}
                  >
                    <span className={styles.itemInner}>
                      <CurrencyFlagThumb unit={opt.value} px={18} />
                      <span className={styles.itemLabel}>
                        {opt.label ?? (currencyBaseCode(opt.value) || opt.value)}
                      </span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>,
            document.body,
          )
        : null}
    </>
  );
}
