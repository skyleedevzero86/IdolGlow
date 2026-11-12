import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import {
  fetchIntegratedSearch,
  highlightKeyword,
  type IntegratedSearchResult,
  type IntegratedSearchSection,
} from '../../../shared/data/integratedSearchApi';
import { recordSearchKeyword } from '../../../shared/data/popularSearchApi';
import styles from './IntegratedSearchPage.module.css';

function SectionBlock({
  section,
  keyword,
  previewLimit,
}: {
  readonly section: IntegratedSearchSection;
  readonly keyword: string;
  readonly previewLimit: number | null;
}) {
  const isListStyle = section.id === 'menu' || section.id === 'event';
  const visibleItems =
    previewLimit == null ? section.items : section.items.slice(0, previewLimit);
  const showMore = Boolean(section.moreHref) && section.totalCount > visibleItems.length;

  return (
    <section className={styles.section} aria-labelledby={`search-section-${section.id}`}>
      <header className={styles.sectionHeader}>
        <span className={styles.sectionIcon} aria-hidden="true" />
        <h2 id={`search-section-${section.id}`} className={styles.sectionTitle}>
          {section.title}
          {section.totalCount > 0 ? (
            <span className={styles.sectionCount}>
              {' '}
              ({section.totalCount.toLocaleString('ko-KR')}건)
            </span>
          ) : null}
        </h2>
      </header>

      {section.emptyActionLabel && section.emptyActionHref ? (
        <Link to={section.emptyActionHref} className={styles.emptyAction}>
          {section.emptyActionLabel}
        </Link>
      ) : null}

      {section.totalCount === 0 && !section.emptyActionLabel ? (
        <p className={styles.emptyText}>검색 결과가 없습니다.</p>
      ) : null}

      {section.totalCount > 0 && isListStyle ? (
        <ul className={styles.bulletList}>
          {visibleItems.map(item => (
            <li key={item.id}>
              <Link
                to={item.href}
                className={styles.bulletLink}
                dangerouslySetInnerHTML={{ __html: highlightKeyword(item.title, keyword) }}
              />
            </li>
          ))}
        </ul>
      ) : null}

      {section.totalCount > 0 && !isListStyle ? (
        <ul className={styles.cardList}>
          {visibleItems.map(item => (
            <li key={item.id} className={styles.cardItem}>
              {item.pathLabel ? <p className={styles.cardPath}>{item.pathLabel}</p> : null}
              <div className={styles.cardTitleRow}>
                {item.sourceLabel ? <span className={styles.cardSource}>{item.sourceLabel}</span> : null}
                <Link
                  to={item.href}
                  className={styles.cardTitle}
                  dangerouslySetInnerHTML={{ __html: highlightKeyword(item.title, keyword) }}
                />
                {item.dateLabel ? <span className={styles.cardDate}>| {item.dateLabel}</span> : null}
              </div>
              {item.snippet ? (
                <p
                  className={styles.cardSnippet}
                  dangerouslySetInnerHTML={{ __html: highlightKeyword(item.snippet, keyword) }}
                />
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}

      {showMore && section.moreHref ? (
        <Link to={section.moreHref} className={styles.moreButton}>
          {section.title} 검색결과 더보기 +
        </Link>
      ) : null}
    </section>
  );
}

export function IntegratedSearchPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const keywordFromUrl = (searchParams.get('q') ?? '').trim();
  const [inputValue, setInputValue] = useState(keywordFromUrl);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<IntegratedSearchResult | null>(null);

  useEffect(() => {
    setInputValue(keywordFromUrl);
  }, [keywordFromUrl]);

  useEffect(() => {
    if (!keywordFromUrl) {
      setResult(null);
      setError(null);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    void (async () => {
      try {
        const next = await fetchIntegratedSearch(keywordFromUrl);
        if (!cancelled) {
          setResult(next);
        }
      } catch {
        if (!cancelled) {
          setError('검색 결과를 불러오지 못했습니다.');
          setResult(null);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [keywordFromUrl]);

  const sectionFilter = searchParams.get('section');
  const visibleSections = useMemo(() => {
    if (!result) return [];
    const filtered = sectionFilter
      ? result.sections.filter(section => section.id === sectionFilter)
      : result.sections.filter(
          section => section.totalCount > 0 || Boolean(section.emptyActionLabel),
        );
    return filtered;
  }, [result, sectionFilter]);

  const previewLimit = sectionFilter ? null : 5;

  const handleSubmit = useCallback(
    (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      const next = inputValue.trim();
      if (!next) return;
      void recordSearchKeyword(next);
      setSearchParams({ q: next });
    },
    [inputValue, setSearchParams],
  );

  return (
    <main className={styles.page}>
      <div className={styles.container}>
        <nav className={styles.breadcrumb} aria-label="breadcrumb">
          <Link to="/">HOME</Link>
          <span aria-hidden="true"> &gt; </span>
          <span>통합검색</span>
        </nav>

        <header className={styles.header}>
          <h1 className={styles.title}>통합검색</h1>
        </header>

        <form className={styles.searchBox} onSubmit={handleSubmit} role="search">
          <label className={styles.searchLabel} htmlFor="integrated-search-input">
            검색어
          </label>
          <div className={styles.searchRow}>
            <input
              id="integrated-search-input"
              className={styles.searchInput}
              value={inputValue}
              onChange={event => setInputValue(event.target.value)}
              placeholder="검색어를 입력해 주세요."
              autoComplete="off"
            />
            <button type="submit" className={styles.searchButton}>
              검색
            </button>
          </div>
        </form>

        {keywordFromUrl ? (
          <p className={styles.summary} role="status">
            &apos;{keywordFromUrl}&apos;(으)로 총{' '}
            <strong className={styles.summaryCount}>
              {(result?.totalCount ?? 0).toLocaleString('ko-KR')}건
            </strong>
            이 검색되었습니다.
          </p>
        ) : (
          <p className={styles.summary}>검색어를 입력해 주세요.</p>
        )}

        {loading ? <p className={styles.status}>검색 중입니다...</p> : null}
        {error ? (
          <p className={styles.error} role="alert">
            {error}
          </p>
        ) : null}

        {!loading &&
          !error &&
          keywordFromUrl &&
          visibleSections.map(section => (
            <SectionBlock
              key={section.id}
              section={section}
              keyword={keywordFromUrl}
              previewLimit={previewLimit}
            />
          ))}

        {!loading && !error && keywordFromUrl && (result?.totalCount ?? 0) === 0 ? (
          <div className={styles.zeroBox}>
            <p>검색 결과가 없습니다.</p>
            <button type="button" className={styles.searchButton} onClick={() => navigate('/articles')}>
              아티클 둘러보기
            </button>
          </div>
        ) : null}
      </div>
    </main>
  );
}

export default IntegratedSearchPage;
