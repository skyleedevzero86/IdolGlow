import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { isNoticeImage } from "../../../shared/data/noticeFiles";
import {
  type PublicNoticeDetail,
  type PublicNoticeSummary,
  fetchPublicNoticeDetail,
  fetchPublishedNotices,
  recordMbrdDocumentView,
} from "../../../shared/data/publicNoticesApi";
import { renderIdolGlowMarkdown } from "../../../shared/markdown/idolGlowMarkdown";
import styles from "./NoticePublicDetailPage.module.css";

const CATEGORY_LABEL = "Glow 소식";
const RELATED_VISIBLE = 3;
const SLIDE_MS = 4000;
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

function byNewest(
  left: PublicNoticeSummary,
  right: PublicNoticeSummary,
): number {
  return (
    new Date(right.updatedAt).getTime() - new Date(left.updatedAt).getTime()
  );
}

function byPopular(
  left: PublicNoticeSummary,
  right: PublicNoticeSummary,
): number {
  const viewGap = right.viewCount - left.viewCount;
  return viewGap !== 0 ? viewGap : byNewest(left, right);
}

function pickRelatedNotices(
  notices: readonly PublicNoticeSummary[],
  documentId: string,
): PublicNoticeSummary[] {
  const others = notices.filter((notice) => notice.documentId !== documentId);
  const popular = Math.random() < 0.5;
  const sorted = [...others].sort(popular ? byPopular : byNewest);
  return sorted.slice(0, RELATED_VISIBLE);
}

async function loadRelatedNotices(
  documentId: string,
): Promise<readonly PublicNoticeSummary[]> {
  try {
    const notices = await fetchPublishedNotices();
    return pickRelatedNotices(notices, documentId);
  } catch {
    return [];
  }
}

function useSlideColumns(): number {
  const [columns, setColumns] = useState(3);
  useEffect(() => {
    const query = window.matchMedia("(max-width: 800px)");
    const apply = () => setColumns(query.matches ? 1 : 3);
    apply();
    query.addEventListener("change", apply);
    return () => query.removeEventListener("change", apply);
  }, []);
  return columns;
}

const ListIcon = () => (
  <svg className={styles.listIcon} viewBox="0 0 22 16" aria-hidden="true">
    <rect x="0" y="1" width="3.2" height="3.2" rx="0.4" fill="currentColor" />
    <rect x="6" y="1.7" width="16" height="2" rx="0.4" fill="currentColor" />
    <rect x="0" y="6.4" width="3.2" height="3.2" rx="0.4" fill="currentColor" />
    <rect x="6" y="7.1" width="16" height="2" rx="0.4" fill="currentColor" />
    <rect
      x="0"
      y="11.8"
      width="3.2"
      height="3.2"
      rx="0.4"
      fill="currentColor"
    />
    <rect x="6" y="12.5" width="16" height="2" rx="0.4" fill="currentColor" />
  </svg>
);

const Chevron = ({ direction }: { readonly direction: "left" | "right" }) => (
  <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path
      d={direction === "left" ? "M15 6l-6 6 6 6" : "M9 6l6 6-6 6"}
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

function scheduleSlide(
  count: number,
  paused: { current: boolean },
  move: () => void,
) {
  if (count < 1) return 0;
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return 0;
  return window.setInterval(() => {
    if (paused.current || document.hidden) return;
    move();
  }, SLIDE_MS);
}

function slideLimit(count: number, columns: number): number {
  if (count < 1) return 0;
  return Math.max(1, count - columns);
}

function RelatedCard({
  item,
  accent,
}: {
  readonly item: PublicNoticeSummary;
  readonly accent: boolean;
}) {
  return (
    <Link to={`/notices/${item.documentId}`} className={styles.card}>
      {item.thumbnailImageUrl ? (
        <img src={item.thumbnailImageUrl} alt="" className={styles.cardImage} />
      ) : (
        <span className={styles.cardFallback} aria-hidden="true" />
      )}
      <span className={accent ? styles.chip : styles.chipPlain}>
        {CATEGORY_LABEL}
      </span>
      <p className={styles.cardTitle}>{item.title}</p>
    </Link>
  );
}

function RelatedTrack({
  items,
  columns,
}: {
  readonly items: readonly PublicNoticeSummary[];
  readonly columns: number;
}) {
  const count = items.length;
  const maxIndex = slideLimit(count, columns);
  const canSlide = count >= 1;
  const [index, setIndex] = useState(0);
  const [animated, setAnimated] = useState(true);
  const paused = useRef(false);
  const timer = useRef(0);
  const restart = useRef<() => void>(() => {});
  const shift = `calc(${-index} * (100cqi + 1.35rem) / ${columns})`;

  const moveNext = () => {
    setAnimated(true);
    setIndex((current) => (current >= maxIndex ? 0 : current + 1));
  };

  useEffect(() => {
    const countNow = items.length;
    const maxNow = slideLimit(countNow, columns);
    const start = () => {
      window.clearInterval(timer.current);
      timer.current = scheduleSlide(countNow, paused, () => {
        setAnimated(true);
        setIndex((current) => (current >= maxNow ? 0 : current + 1));
      });
    };
    restart.current = start;
    start();
    return () => window.clearInterval(timer.current);
  }, [items.length, columns]);

  const goPrev = () => {
    setAnimated(true);
    setIndex((current) => (current <= 0 ? maxIndex : current - 1));
    restart.current();
  };

  if (count === 0) return null;

  return (
    <section
      className={styles.related}
      aria-labelledby="related-notices-title"
      aria-roledescription="carousel"
      onMouseEnter={() => {
        paused.current = true;
      }}
      onMouseLeave={() => {
        paused.current = false;
      }}
    >
      <h2 id="related-notices-title" className={styles.relatedTitle}>
        같이 보면 좋은 콘텐츠
      </h2>
      {canSlide ? (
        <>
          <button
            type="button"
            className={`${styles.arrow} ${styles.arrowPrev}`}
            onClick={goPrev}
            aria-label="이전 콘텐츠"
          >
            <Chevron direction="left" />
          </button>
          <button
            type="button"
            className={`${styles.arrow} ${styles.arrowNext}`}
            onClick={() => {
              moveNext();
              restart.current();
            }}
            aria-label="다음 콘텐츠"
          >
            <Chevron direction="right" />
          </button>
        </>
      ) : null}
      <div className={styles.viewport} data-columns={columns}>
        <div
          className={styles.track}
          style={{
            transform: `translateX(${shift})`,
            transition: animated ? undefined : "none",
          }}
        >
          {items.map((item, position) => (
            <RelatedCard
              key={item.documentId}
              item={item}
              accent={position === 0}
            />
          ))}
        </div>
      </div>
    </section>
  );
}

function RelatedContents({
  items,
}: {
  readonly items: readonly PublicNoticeSummary[];
}) {
  const columns = useSlideColumns();
  const slideKey = `${columns}:${items.map((item) => item.documentId).join("|")}`;
  return <RelatedTrack key={slideKey} items={items} columns={columns} />;
}

export const NoticePublicDetailPage = () => {
  const { documentId } = useParams<{ documentId: string }>();
  const [item, setItem] = useState<PublicNoticeDetail | null>(null);
  const [related, setRelated] = useState<readonly PublicNoticeSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!documentId) return;
    let cancelled = false;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetchPublicNoticeDetail(documentId);
        if (cancelled) return;
        setItem(response);
        void recordMbrdDocumentView(documentId).catch(() => {});
        const others = await loadRelatedNotices(documentId);
        if (cancelled) return;
        setRelated(others);
      } catch (loadError) {
        if (cancelled) return;
        setItem(null);
        setRelated([]);
        setError(
          loadError instanceof Error
            ? loadError.message
            : "공지를 불러오지 못했습니다.",
        );
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    void load();
    return () => {
      cancelled = true;
    };
  }, [documentId]);

  const html = useMemo(
    () => renderIdolGlowMarkdown(item?.markdown ?? ""),
    [item?.markdown],
  );
  const tags = visibleTags(item?.tags ?? []);
  const images = item?.attachments.filter(isNoticeImage) ?? [];
  const files = item?.attachments.filter((file) => !isNoticeImage(file)) ?? [];

  if (loading) {
    return (
      <main className={styles.main}>
        <p className={styles.status}>불러오는 중입니다.</p>
      </main>
    );
  }
  if (error || !item) {
    return (
      <main className={styles.main}>
        <p className={styles.status}>{error ?? "공지가 없습니다."}</p>
      </main>
    );
  }

  return (
    <main className={styles.main} id="main-content">
      <article className={styles.section}>
        <div className={styles.wrap}>
          <h1 className={styles.title}>{item.title}</h1>
          <div className={styles.meta}>
            <span className={styles.badge}>{CATEGORY_LABEL}</span>
            <p className={styles.date}>{formatNewsDate(item.updatedAt)}</p>
          </div>
          {tags.length > 0 ? (
            <p className={styles.tags}>
              {tags.map((tag) => `#${tag}`).join(" ")}
            </p>
          ) : null}
          {images.map((file) => (
            <img
              key={file.url}
              src={file.url}
              alt={file.name}
              className={styles.hero}
            />
          ))}
          {files.map((file) => (
            <a key={file.url} href={file.url} className={styles.fileLink}>
              {file.name}
            </a>
          ))}
          <div
            className={styles.body}
            // biome-ignore lint/security/noDangerouslySetInnerHtml: trusted internal content
            dangerouslySetInnerHTML={{ __html: html }}
          />
          <div className={styles.listWrap}>
            <Link to="/notices" className={styles.listButton}>
              목록보기
              <ListIcon />
            </Link>
          </div>
          <RelatedContents key={item.documentId} items={related} />
        </div>
      </article>
    </main>
  );
};

export default NoticePublicDetailPage;
