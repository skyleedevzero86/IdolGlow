'use client';

import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  fetchPopularSearchKeywords,
  recordSearchKeyword,
} from '../../../shared/data/popularSearchApi';
import styles from './HeaderSearchPanel.module.css';

const FALLBACK_TERMS = [
  '2025',
  '2026',
  '아시아',
  'ai',
  '하늘',
  '마당',
  '이벤트',
  '아티클',
  '공지',
  '상품',
  'GLOW',
  'WISH',
] as const;
const PAGE_SIZE = 6;
const AUTO_INTERVAL_MS = 3500;

const SearchGlyph = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
    <circle cx="11" cy="11" r="8" />
    <path d="M21 21l-4.35-4.35" />
  </svg>
);

export type HeaderSearchPanelProps = {
  readonly onClose: () => void;
};

export const HeaderSearchPanel = ({ onClose }: HeaderSearchPanelProps) => {
  const router = useRouter();
  const [value, setValue] = useState('');
  const [popularTerms, setPopularTerms] = useState<readonly string[]>(FALLBACK_TERMS);
  const [pageIndex, setPageIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const [direction, setDirection] = useState<1 | -1>(1);
  const inputRef = useRef<HTMLInputElement>(null);

  const pageCount = useMemo(
    () => Math.max(1, Math.ceil(popularTerms.length / PAGE_SIZE)),
    [popularTerms.length],
  );

  const visibleTerms = useMemo(() => {
    const start = (pageIndex % pageCount) * PAGE_SIZE;
    return popularTerms.slice(start, start + PAGE_SIZE);
  }, [pageIndex, pageCount, popularTerms]);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const items = await fetchPopularSearchKeywords(60);
        if (cancelled) {
          return;
        }
        const keywords = items.map(item => item.keyword).filter(Boolean);
        const merged =
          keywords.length > 0
            ? Array.from(new Set([...keywords, ...FALLBACK_TERMS])).slice(0, 60)
            : [...FALLBACK_TERMS];
        setPopularTerms(merged);
        setPageIndex(0);
      } catch {
        if (!cancelled) {
          setPopularTerms([...FALLBACK_TERMS]);
          setPageIndex(0);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (paused || pageCount <= 1) {
      return;
    }
    const timer = window.setInterval(() => {
      setPageIndex(current => {
        if (pageCount <= 1) {
          return 0;
        }
        if (direction === 1 && current >= pageCount - 1) {
          setDirection(-1);
          return Math.max(0, current - 1);
        }
        if (direction === -1 && current <= 0) {
          setDirection(1);
          return Math.min(pageCount - 1, current + 1);
        }
        return current + direction;
      });
    }, AUTO_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, [paused, pageCount, direction]);

  const goPrev = useCallback(() => {
    setDirection(-1);
    setPageIndex(current => (current - 1 + pageCount) % pageCount);
  }, [pageCount]);

  const goNext = useCallback(() => {
    setDirection(1);
    setPageIndex(current => (current + 1) % pageCount);
  }, [pageCount]);

  const togglePause = useCallback(() => {
    setPaused(current => !current);
  }, []);

  const runSearch = useCallback(
    (raw: string) => {
      const q = raw.trim();
      if (!q) return;
      void recordSearchKeyword(q);
      onClose();
      router.push(`/search?q=${encodeURIComponent(q)}`);
    },
    [router, onClose],
  );

  const handleSubmit = useCallback(
    (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      runSearch(value);
    },
    [runSearch, value],
  );

  return (
    <div className={styles.wrap}>
      <div className={styles.totalSearchBox}>
        <form
          id="srch_form"
          className={styles.form}
          method="get"
          action="/search"
          onSubmit={handleSubmit}
          role="search"
          aria-label="통합검색"
        >
          <label htmlFor="srch_kwd" className={styles.srOnly}>
            검색어
          </label>
          <input
            ref={inputRef}
            type="search"
            name="q"
            id="srch_kwd"
            className={styles.input}
            value={value}
            onChange={e => setValue(e.target.value)}
            autoComplete="off"
            placeholder="검색어를 입력하세요"
            enterKeyHint="search"
          />
          <button type="submit" className={styles.submit} aria-label="검색">
            <span className={styles.submitLabel}>검색</span>
            <SearchGlyph />
          </button>
        </form>

        <div className={styles.popularSlider} aria-label="자주 찾는 검색어">
          <strong className={styles.popularLabel}>인기검색어</strong>
          <div className={styles.sliderViewport}>
            <ul key={pageIndex} className={styles.wordList} data-direction={direction}>
              {visibleTerms.map(term => (
                <li key={`${pageIndex}-${term}`}>
                  <button
                    type="button"
                    className={styles.wordLink}
                    onClick={() => {
                      setValue(term);
                      runSearch(term);
                    }}
                  >
                    {term}
                  </button>
                </li>
              ))}
            </ul>
          </div>
          <div className={styles.sliderControls}>
            <button type="button" className={styles.controlButton} onClick={goPrev} aria-label="이전 인기검색어">
              ‹
            </button>
            <button
              type="button"
              className={styles.controlButton}
              onClick={togglePause}
              aria-label={paused ? '자동 이동 재생' : '자동 이동 일시정지'}
              aria-pressed={paused}
            >
              {paused ? '▶' : '❚❚'}
            </button>
            <button type="button" className={styles.controlButton} onClick={goNext} aria-label="다음 인기검색어">
              ›
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HeaderSearchPanel;
