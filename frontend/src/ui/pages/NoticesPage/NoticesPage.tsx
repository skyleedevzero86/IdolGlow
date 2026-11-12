import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  type PublicNoticeSummary,
  fetchPublicNotices,
} from "../../../shared/data/publicNoticesApi";
import styles from "./NoticesPage.module.css";

const PAGE_SIZE = 8;
const PAGE_WINDOW = 10;
const CATEGORY_LABEL = "Glow 소식";
const HIDDEN_ROW_TAGS = new Set([
  "notice-pinned",
  "notice-board",
  "event-board",
]);

function formatNewsDate(iso: string): string {
  const parsed = new Date(iso);
  if (Number.isNaN(parsed.getTime())) {
    return iso.slice(0, 10);
  }
  const month = String(parsed.getMonth() + 1).padStart(2, "0");
  const day = String(parsed.getDate()).padStart(2, "0");
  return `${parsed.getFullYear()}-${month}-${day}`;
}

function visibleTags(tags: readonly string[]): string[] {
  return tags.filter((tag) => !HIDDEN_ROW_TAGS.has(tag.trim().toLowerCase()));
}

function pageWindow(current: number, total: number): number[] {
  const start = Math.floor((current - 1) / PAGE_WINDOW) * PAGE_WINDOW + 1;
  const end = Math.min(total, start + PAGE_WINDOW - 1);
  return Array.from(
    { length: Math.max(0, end - start + 1) },
    (_, index) => start + index,
  );
}

const NextIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path
      d="M9 6l6 6-6 6"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

const PrevIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path
      d="M15 6l-6 6 6 6"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

function NewsRow({ item }: { readonly item: PublicNoticeSummary }) {
  const tags = visibleTags(item.tags);
  return (
    <li className={styles.item}>
      <Link to={`/notices/${item.documentId}`} className={styles.link}>
        {item.thumbnailImageUrl ? (
          <img src={item.thumbnailImageUrl} alt="" className={styles.thumb} />
        ) : (
          <span className={styles.thumbFallback} aria-hidden="true" />
        )}
        <div className={styles.info}>
          <p className={styles.category}>{CATEGORY_LABEL}</p>
          <h2 className={styles.subject}>{item.title}</h2>
          {tags.length > 0 ? (
            <p className={styles.tags}>
              {tags.map((tag) => `#${tag}`).join(" ")}
            </p>
          ) : null}
          <p className={styles.date}>{formatNewsDate(item.updatedAt)}</p>
        </div>
      </Link>
    </li>
  );
}

export const NoticesPage = () => {
  const [items, setItems] = useState<readonly PublicNoticeSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetchPublicNotices(page, PAGE_SIZE);
        setItems(response.items);
        setTotalPages(response.totalPages);
      } catch (loadError) {
        setError(
          loadError instanceof Error
            ? loadError.message
            : "소식을 불러오지 못했습니다.",
        );
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [page]);

  const pageNumbers = useMemo(
    () => pageWindow(page, totalPages),
    [page, totalPages],
  );

  const goToPage = (next: number) => {
    setPage(next);
    window.scrollTo({ top: 0, behavior: "auto" });
  };

  return (
    <main className={styles.main} id="main-content">
      <section className={styles.section} aria-labelledby="glow-news-title">
        <div className={styles.wrap}>
          <h1 id="glow-news-title" className={styles.title}>
            {CATEGORY_LABEL}
          </h1>
          {error ? <p className={styles.error}>{error}</p> : null}
          {loading ? <p className={styles.empty}>불러오는 중입니다.</p> : null}
          {!loading && !error && items.length === 0 ? (
            <p className={styles.empty}>등록된 소식이 없습니다.</p>
          ) : null}
          {!loading && !error && items.length > 0 ? (
            <ul className={styles.list}>
              {items.map((item) => (
                <NewsRow key={item.documentId} item={item} />
              ))}
            </ul>
          ) : null}
          {!loading && !error && totalPages > 0 && items.length > 0 ? (
            <nav className={styles.paging} aria-label="소식 페이지">
              {page > 1 ? (
                <button
                  type="button"
                  className={styles.pageArrow}
                  onClick={() => goToPage(page - 1)}
                  aria-label="이전 페이지"
                >
                  <PrevIcon />
                </button>
              ) : null}
              {pageNumbers.map((number) => (
                <button
                  key={number}
                  type="button"
                  className={`${styles.pageButton} ${
                    number === page ? styles.pageCurrent : ""
                  }`}
                  onClick={() => goToPage(number)}
                  aria-current={number === page ? "page" : undefined}
                >
                  {number}
                </button>
              ))}
              {page < totalPages ? (
                <button
                  type="button"
                  className={styles.pageArrow}
                  onClick={() => goToPage(page + 1)}
                  aria-label="다음 페이지"
                >
                  <NextIcon />
                </button>
              ) : null}
            </nav>
          ) : null}
        </div>
      </section>
    </main>
  );
};

export default NoticesPage;
