/**
 * 헤더 하단 인라인 검색 패널 ( srch_form_wrap 레이아웃)
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './HeaderSearchPanel.module.css';

const POPULAR_TERMS = ['2025', '2026', '아시아', 'ai', '하늘', '마당'] as const;

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
  const navigate = useNavigate();
  const [value, setValue] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  const runSearch = useCallback(
    (raw: string) => {
      const q = raw.trim();
      if (!q) return;
      onClose();
      navigate({ pathname: '/articles', search: `?q=${encodeURIComponent(q)}` });
    },
    [navigate, onClose]
  );

  const handleSubmit = useCallback(
    (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      runSearch(value);
    },
    [runSearch, value]
  );

  return (
    <div className={styles.wrap}>
      <div className={styles.totalSearchBox}>
        <form
          id="srch_form"
          className={styles.form}
          method="get"
          action="/articles"
          onSubmit={handleSubmit}
          role="search"
          aria-label="아티클 검색"
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
            placeholder="검색어를 입력해 주세요."
            enterKeyHint="search"
          />
          <button type="submit" className={styles.submit} aria-label="검색">
            <span className={styles.srOnly}>검색</span>
            <SearchGlyph />
          </button>
        </form>

        <dl className={styles.popularWord}>
          <dt>자주 찾는 검색어</dt>
          <dd>
            <ul className={styles.wordList}>
              {POPULAR_TERMS.map(term => (
                <li key={term}>
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
          </dd>
        </dl>
      </div>
    </div>
  );
};

export default HeaderSearchPanel;
