import { type ReactNode, useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { type GlowPopupItem, fetchGlowPopups } from '../../../shared/data/glowPopupApi';
import glowStyles from './GlowPage.module.css';
import styles from './GlowInfoPopups.module.css';

const PAGE_SIZE = 8;
const STRIP_DAYS = 14;
const WEEKDAYS = ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'] as const;

const pad = (value: number): string => String(value).padStart(2, '0');

const toIsoDate = (date: Date): string =>
  `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

const fromIsoDate = (value: string): Date => {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
};

const addDays = (date: Date, days: number): Date => {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
};

const formatPeriod = (start: string, end: string): string => `${start} ~ ${end}`;

const ChevronIcon = ({ direction }: { readonly direction: 'left' | 'right' }) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
    <polyline points={direction === 'left' ? '15 18 9 12 15 6' : '9 18 15 12 9 6'} />
  </svg>
);

const CalendarIcon = () => (
  <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
    <path d="M7 2h2v2h6V2h2v2h3a1 1 0 0 1 1 1v15a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1h3V2Zm12 8H5v9h14v-9Z" />
  </svg>
);

const PinIcon = () => (
  <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
    <path d="M12 2a7 7 0 0 0-7 7c0 5.25 7 13 7 13s7-7.75 7-13a7 7 0 0 0-7-7Zm0 9.5A2.5 2.5 0 1 1 12 6.5a2.5 2.5 0 0 1 0 5Z" />
  </svg>
);

const PopupCard = ({ popup }: { readonly popup: GlowPopupItem }) => (
  <article className={styles.popupCard}>
    <div className={styles.popupText}>
      <h3>
        <Link to={popup.linkUrl}>{popup.title}</Link>
        {popup.emoji ? <span className={styles.emoji} aria-hidden="true">{popup.emoji}</span> : null}
      </h3>
      <p className={styles.period}>{formatPeriod(popup.startDate, popup.endDate)}</p>
      <p className={styles.region}>
        <PinIcon />
        {popup.regionLabel}
      </p>
    </div>
    <Link to={popup.linkUrl} className={styles.popupImage} tabIndex={-1} aria-hidden="true">
      <img src={popup.imageUrl} alt="" loading="lazy" />
    </Link>
  </article>
);

export const GlowInfoPopups = ({ filterBar }: { readonly filterBar: ReactNode }) => {
  const today = useMemo(() => toIsoDate(new Date()), []);
  const [selectedDate, setSelectedDate] = useState(today);
  const [stripStart, setStripStart] = useState(() => toIsoDate(addDays(new Date(), -1)));
  const [popups, setPopups] = useState<readonly GlowPopupItem[]>([]);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(false);
  const dateInputRef = useRef<HTMLInputElement>(null);

  const stripDays = useMemo(
    () => Array.from({ length: STRIP_DAYS }, (_, index) => addDays(fromIsoDate(stripStart), index)),
    [stripStart],
  );

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setPage(0);

    void fetchGlowPopups({ date: selectedDate, page: 0, size: PAGE_SIZE }).then((response) => {
      if (cancelled) return;
      setPopups(response.items);
      setHasNext(response.hasNext);
      setLoading(false);
    });

    return () => {
      cancelled = true;
    };
  }, [selectedDate]);

  const handleLoadMore = () => {
    if (loading) return;
    setLoading(true);
    if (!hasNext) {
      window.setTimeout(() => setLoading(false), 550);
      return;
    }
    const nextPage = page + 1;

    void fetchGlowPopups({ date: selectedDate, page: nextPage, size: PAGE_SIZE }).then((response) => {
      setPopups((current) => [...current, ...response.items]);
      setHasNext(response.hasNext);
      setPage(nextPage);
      setLoading(false);
    });
  };

  const selectDate = (value: string) => {
    if (!value) return;
    setSelectedDate(value);
    const inStrip = stripDays.some((day) => toIsoDate(day) === value);
    if (!inStrip) setStripStart(toIsoDate(addDays(fromIsoDate(value), -1)));
  };

  const shiftStrip = (days: number) => {
    setStripStart((current) => toIsoDate(addDays(fromIsoDate(current), days)));
  };

  const openDatePicker = () => {
    const input = dateInputRef.current;
    if (!input) return;
    if (typeof input.showPicker === 'function') input.showPicker();
    else input.click();
  };

  const selected = fromIsoDate(selectedDate);

  return (
    <div className={styles.glowInfo}>
      {filterBar}

      <div className={styles.dateBar}>
        <button type="button" className={styles.stripArrow} aria-label="이전 주" onClick={() => shiftStrip(-7)}>
          <ChevronIcon direction="left" />
        </button>

        <ol className={styles.dateStrip} aria-label="날짜 선택">
          {stripDays.map((day) => {
            const iso = toIsoDate(day);
            const isToday = iso === today;
            const isSelected = iso === selectedDate;
            const weekday = day.getDay();
            const className = [
              styles.dateButton,
              isToday ? styles.today : '',
              isSelected ? styles.selectedDate : '',
              weekday === 0 ? styles.sunday : '',
              weekday === 6 ? styles.saturday : '',
            ]
              .filter(Boolean)
              .join(' ');

            return (
              <li key={iso}>
                <button
                  type="button"
                  className={className}
                  aria-pressed={isSelected}
                  aria-label={`${iso} ${WEEKDAYS[weekday]}${isToday ? ' 오늘' : ''}`}
                  onClick={() => setSelectedDate(iso)}
                >
                  <span className={styles.dateWeekday}>{isToday ? '오늘' : WEEKDAYS[weekday]}</span>
                  <span className={styles.dateNumber}>{day.getDate()}</span>
                </button>
              </li>
            );
          })}
        </ol>

        <button type="button" className={styles.stripArrow} aria-label="다음 주" onClick={() => shiftStrip(7)}>
          <ChevronIcon direction="right" />
        </button>

        <div className={styles.calendarPicker}>
          <button type="button" className={styles.calendarButton} aria-label="달력에서 날짜 선택" onClick={openDatePicker}>
            <CalendarIcon />
          </button>
          <input
            ref={dateInputRef}
            type="date"
            className={styles.dateInput}
            value={selectedDate}
            tabIndex={-1}
            aria-hidden="true"
            onChange={(event) => selectDate(event.target.value)}
          />
        </div>
      </div>

      <p className={styles.selectedLabel}>
        {selectedDate} ({WEEKDAYS[selected.getDay()]})
      </p>

      <div className={styles.popupGrid}>
        {popups.map((popup) => (
          <PopupCard key={popup.id} popup={popup} />
        ))}
      </div>

      {!loading && popups.length === 0 ? (
        <p className={styles.empty} role="status">선택한 날짜에 진행 중인 팝업이 없습니다.</p>
      ) : null}

      <button
        type="button"
        className={glowStyles.moreButton}
        aria-label={loading ? '팝업 불러오는 중' : '팝업 더 보기'}
        aria-busy={loading}
        disabled={loading}
        onClick={handleLoadMore}
      >
        {loading ? <span className={glowStyles.loadingSpinner} aria-hidden="true" /> : '+'}
      </button>
    </div>
  );
};

export default GlowInfoPopups;
