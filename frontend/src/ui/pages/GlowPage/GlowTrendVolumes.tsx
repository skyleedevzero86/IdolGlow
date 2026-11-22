import {
  type FormEvent,
  type ReactNode,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  type OffsetPage,
  apiErrorMessage,
  isAbortError,
} from "../../../shared/data/apiClient";
import {
  type IdolglowIssueFilters,
  type IdolglowIssueSummary,
  fetchIdolglowIssueFilters,
  fetchIdolglowIssues,
} from "../../../shared/data/idolglowPublicApi";
import { GlowTrendArticleDetail } from "./GlowTrendArticleDetail";
import { GlowTrendIssueDetail } from "./GlowTrendIssueDetail";
import styles from "./GlowTrendVolumes.module.css";

const VOLUMES_PER_PAGE = 4;
const PAGE_GROUP_SIZE = 10;
const LIST_PARAMS = ["year", "month", "vol", "q", "page"] as const;

type ListParam = (typeof LIST_PARAMS)[number];
type ParamUpdate = Partial<
  Record<ListParam | "issue" | "article", string | null>
>;

const parsePositiveInt = (raw: string | null): number | null => {
  if (!raw) return null;
  const value = Number(raw);
  return Number.isInteger(value) && value > 0 ? value : null;
};

const badgeClass = (category: string, index: number): string => {
  if (category === "article") return styles.badgeMuted;
  if (index === 0) return styles.badgeDark;
  return styles.badgeBlue;
};

const VolumeCard = ({
  issue,
  linkTo,
}: {
  readonly issue: IdolglowIssueSummary;
  readonly linkTo: (update: ParamUpdate) => string;
}) => {
  const coverLink = linkTo(
    issue.leadArticleSlug
      ? { issue: issue.slug, article: issue.leadArticleSlug }
      : { issue: issue.slug, article: null },
  );
  return (
    <article className={styles.volumeCard}>
      <Link
        to={coverLink}
        className={styles.volumeImage}
        tabIndex={-1}
        aria-hidden="true"
      >
        <img src={issue.coverImageUrl} alt="" loading="lazy" />
      </Link>
      <div className={styles.volumeBody}>
        <div className={styles.volumeHead}>
          <h3>
            <Link to={linkTo({ issue: issue.slug, article: null })}>
              Vol.{issue.volume}
            </Link>
          </h3>
          <span>{issue.issueDate}</span>
        </div>
        <ul className={styles.volumeItems}>
          {issue.articles.map((article, index) => (
            <li key={article.slug}>
              <span
                className={`${styles.badge} ${badgeClass(article.category, index)}`}
              >
                {article.categoryLabel}
              </span>
              <Link to={linkTo({ issue: issue.slug, article: article.slug })}>
                {article.title}
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </article>
  );
};

const SelectField = ({
  label,
  value,
  options,
  disabled,
  onChange,
}: {
  readonly label: string;
  readonly value: string;
  readonly options: readonly {
    readonly value: string;
    readonly label: string;
  }[];
  readonly disabled?: boolean;
  readonly onChange: (value: string) => void;
}) => (
  <label className={styles.selectField}>
    <span className={styles.srOnly}>{label}</span>
    <select
      value={value}
      disabled={disabled}
      onChange={(event) => onChange(event.target.value)}
    >
      <option value="">{label}</option>
      {options.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
    <span className={styles.selectIcon} aria-hidden="true" />
  </label>
);

export const GlowTrendVolumes = ({
  filterBar,
}: { readonly filterBar: ReactNode }) => {
  const [searchParams, setSearchParams] = useSearchParams();
  const year = parsePositiveInt(searchParams.get("year"));
  const month = parsePositiveInt(searchParams.get("month"));
  const volumeNo = parsePositiveInt(searchParams.get("vol"));
  const keyword = (searchParams.get("q") ?? "").trim();
  const page = parsePositiveInt(searchParams.get("page")) ?? 1;
  const issueSlug = searchParams.get("issue")?.trim() || null;
  const articleSlug = searchParams.get("article")?.trim() || null;

  const [filters, setFilters] = useState<IdolglowIssueFilters | null>(null);
  const [filtersError, setFiltersError] = useState<string | null>(null);
  const [result, setResult] = useState<OffsetPage<IdolglowIssueSummary> | null>(
    null,
  );
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [keywordInput, setKeywordInput] = useState(keyword);

  useEffect(() => {
    setKeywordInput(keyword);
  }, [keyword]);

  const buildSearch = useCallback(
    (update: ParamUpdate): string => {
      const next = new URLSearchParams(searchParams);
      next.set("tab", "trend");
      for (const [key, value] of Object.entries(update)) {
        if (value === null || value === undefined || value === "")
          next.delete(key);
        else next.set(key, value);
      }
      return next.toString();
    },
    [searchParams],
  );

  const linkTo = useCallback(
    (update: ParamUpdate): string => `/glow?${buildSearch(update)}`,
    [buildSearch],
  );

  const updateParams = useCallback(
    (update: ParamUpdate, options: { readonly push?: boolean } = {}) => {
      setSearchParams(new URLSearchParams(buildSearch(update)), {
        replace: !options.push,
      });
    },
    [buildSearch, setSearchParams],
  );

  useEffect(() => {
    const controller = new AbortController();
    setFiltersError(null);
    fetchIdolglowIssueFilters(controller.signal)
      .then(setFilters)
      .catch((loadError: unknown) => {
        if (isAbortError(loadError)) return;
        setFiltersError(
          apiErrorMessage(loadError, "호 찾기 조건을 불러오지 못했습니다."),
        );
      });
    return () => controller.abort();
  }, []);

  // biome-ignore lint/correctness/useExhaustiveDependencies: reloadKey is an intentional re-run trigger
  useEffect(() => {
    if (issueSlug) return;
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    fetchIdolglowIssues(
      {
        page: page - 1,
        size: VOLUMES_PER_PAGE,
        year,
        month,
        volume: volumeNo,
        keyword,
      },
      controller.signal,
    )
      .then((response) => {
        setResult(response);
        setLoading(false);
      })
      .catch((loadError: unknown) => {
        if (isAbortError(loadError)) return;
        setResult(null);
        setError(
          apiErrorMessage(loadError, "트렌드 호 목록을 불러오지 못했습니다."),
        );
        setLoading(false);
      });
    return () => controller.abort();
  }, [issueSlug, page, year, month, volumeNo, keyword, reloadKey]);

  const yearOptions = useMemo(
    () =>
      (filters?.years ?? []).map((y) => ({
        value: String(y),
        label: `${y}년`,
      })),
    [filters],
  );
  const monthOptions = useMemo(() => {
    if (!filters) return [];
    const months = year
      ? (filters.months[String(year)] ?? [])
      : [...new Set(Object.values(filters.months).flat())].sort(
          (a, b) => a - b,
        );
    return months.map((m) => ({ value: String(m), label: `${m}월` }));
  }, [filters, year]);
  const volumeOptions = useMemo(
    () =>
      (filters?.volumes ?? [])
        .filter(
          (v) => (!year || v.year === year) && (!month || v.month === month),
        )
        .map((v) => ({ value: String(v.volume), label: `Vol.${v.volume}` })),
    [filters, year, month],
  );

  const totalPages = Math.max(1, result?.totalPages ?? 1);
  const issues = result?.items ?? [];
  const groupStart =
    Math.floor((page - 1) / PAGE_GROUP_SIZE) * PAGE_GROUP_SIZE + 1;
  const groupEnd = Math.min(totalPages, groupStart + PAGE_GROUP_SIZE - 1);
  const pageNumbers = Array.from(
    { length: Math.max(0, groupEnd - groupStart + 1) },
    (_, i) => groupStart + i,
  );
  const hasCondition = Boolean(year || month || volumeNo || keyword);

  const goToPage = (next: number) => {
    const target = Math.min(totalPages, Math.max(1, next));
    updateParams({ page: target > 1 ? String(target) : null }, { push: true });
  };

  const submitKeyword = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    updateParams({ q: keywordInput.trim() || null, page: null });
  };

  const resetConditions = () => {
    setKeywordInput("");
    updateParams({ year: null, month: null, vol: null, q: null, page: null });
  };

  const closeDetail = () =>
    updateParams({ issue: null, article: null }, { push: true });

  const renderMain = () => {
    if (issueSlug && articleSlug) {
      return (
        <GlowTrendArticleDetail
          issueSlug={issueSlug}
          articleSlug={articleSlug}
          linkTo={linkTo}
          onBack={closeDetail}
        />
      );
    }
    if (issueSlug) {
      return (
        <GlowTrendIssueDetail
          issueSlug={issueSlug}
          linkTo={linkTo}
          onBack={closeDetail}
        />
      );
    }
    return (
      <>
        <h2 className={styles.title}>트렌드 별 보기</h2>

        {keyword ? (
          <output className={styles.resultSummary}>
            ‘{keyword}’ 검색 결과 {result?.totalElements ?? 0}개 호
          </output>
        ) : null}

        <div className={styles.volumeList} aria-busy={loading}>
          {loading ? (
            <output className={styles.empty}>
              트렌드 호를 불러오는 중입니다.
            </output>
          ) : error ? (
            <div className={styles.empty} role="alert">
              <p>{error}</p>
              <button
                type="button"
                className={styles.retryButton}
                onClick={() => setReloadKey((key) => key + 1)}
              >
                다시 시도
              </button>
            </div>
          ) : issues.length === 0 ? (
            <output className={styles.empty}>
              {hasCondition
                ? "조건에 맞는 호가 없습니다."
                : "아직 발행된 트렌드 호가 없습니다."}
            </output>
          ) : (
            issues.map((issue) => (
              <VolumeCard key={issue.slug} issue={issue} linkTo={linkTo} />
            ))
          )}
        </div>

        {!loading && !error && totalPages > 1 ? (
          <nav className={styles.pagination} aria-label="호별보기 페이지">
            {page > 1 ? (
              <button
                type="button"
                className={styles.pageArrow}
                aria-label="이전 페이지"
                onClick={() => goToPage(page - 1)}
              >
                ‹
              </button>
            ) : null}
            {pageNumbers.map((number) => (
              <button
                key={number}
                type="button"
                className={number === page ? styles.activePage : undefined}
                aria-current={number === page ? "page" : undefined}
                onClick={() => goToPage(number)}
              >
                {number}
              </button>
            ))}
            {page < totalPages ? (
              <button
                type="button"
                className={styles.pageArrow}
                aria-label="다음 페이지"
                onClick={() => goToPage(page + 1)}
              >
                ›
              </button>
            ) : null}
          </nav>
        ) : null}
      </>
    );
  };

  return (
    <div className={styles.trendLayout}>
      <div className={styles.volumeColumn}>
        {filterBar}
        {renderMain()}
      </div>

      <aside className={styles.volumeSidebar} aria-label="호 찾기">
        <p className={styles.latest}>
          트렌드 별 Vol. <strong>{filters?.latestVolume ?? "-"}</strong>
        </p>
        <div className={styles.selects}>
          <SelectField
            label="발행 연도"
            value={year ? String(year) : ""}
            options={yearOptions}
            disabled={!filters}
            onChange={(value) =>
              updateParams({
                year: value || null,
                month: null,
                vol: null,
                page: null,
                issue: null,
                article: null,
              })
            }
          />
          <SelectField
            label="발행 월"
            value={month ? String(month) : ""}
            options={monthOptions}
            disabled={!filters}
            onChange={(value) =>
              updateParams({
                month: value || null,
                vol: null,
                page: null,
                issue: null,
                article: null,
              })
            }
          />
          <SelectField
            label="mz pic"
            value={volumeNo ? String(volumeNo) : ""}
            options={volumeOptions}
            disabled={!filters}
            onChange={(value) =>
              updateParams({
                vol: value || null,
                page: null,
                issue: null,
                article: null,
              })
            }
          />
          <form className={styles.searchForm} onSubmit={submitKeyword}>
            <label className={styles.srOnly} htmlFor="glow-trend-keyword">
              트렌드 검색어
            </label>
            <input
              id="glow-trend-keyword"
              type="search"
              value={keywordInput}
              maxLength={100}
              placeholder="제목·내용 검색"
              onChange={(event) => setKeywordInput(event.target.value)}
            />
            <button type="submit">검색</button>
          </form>
          {hasCondition ? (
            <button
              type="button"
              className={styles.resetButton}
              onClick={resetConditions}
            >
              조건 초기화
            </button>
          ) : null}
          {filtersError ? (
            <p className={styles.sidebarError} role="alert">
              {filtersError}
            </p>
          ) : null}
        </div>
      </aside>
    </div>
  );
};

export default GlowTrendVolumes;
