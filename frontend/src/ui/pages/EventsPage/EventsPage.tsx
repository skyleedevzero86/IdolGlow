import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchPublicEvents, type PublicEventSummary } from '../../../shared/data/publicEventsApi';
import styles from './EventsPage.module.css';

const PAGE_SIZE = 10;

const isEnded = (event: PublicEventSummary) => {
  if (!event.endDate) return false;
  const end = new Date(`${event.endDate}T23:59:59`);
  return Number.isFinite(end.getTime()) ? Date.now() > end.getTime() : false;
};

export const EventsPage = () => {
  const [items, setItems] = useState<readonly PublicEventSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetchPublicEvents(page, PAGE_SIZE);
        setItems(response.items);
        setTotalPages(response.totalPages);
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : '이벤트를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [page]);

  const pageNumbers = useMemo(
    () => Array.from({ length: totalPages }, (_, index) => index + 1),
    [totalPages]
  );

  return (
    <main className={styles.main}>
      <section className={styles.section}>
        <div className={styles.wrap}>
          <h4 className={styles.title}>GLOW 이벤트</h4>
          {error ? <div className={styles.error}>{error}</div> : null}
          {loading ? (
            <div className={styles.empty}>이벤트를 불러오는 중입니다.</div>
          ) : items.length === 0 ? (
            <div className={styles.empty}>등록된 이벤트가 없습니다.</div>
          ) : (
            <ul className={styles.list}>
              {items.map(item => (
                <li key={item.documentId}>
                  <div className={styles.eventBox}>
                    <Link to={`/events/${item.documentId}`} className={styles.eventLink}>
                      <div className={styles.thumbWrap}>
                        {item.thumbnailImageUrl ? (
                          <>
                            <img src={item.thumbnailImageUrl} alt={item.title} className={styles.thumb} />
                            {isEnded(item) ? <span className={styles.endedOverlay}>종료</span> : null}
                          </>
                        ) : (
                          <div className={styles.thumbFallback}>NO IMAGE</div>
                        )}
                      </div>
                      <div className={styles.info}>
                        <div className={`${styles.category} ${isEnded(item) ? styles.categoryEnded : ''}`}>이벤트</div>
                        <h5 className={styles.subject}>
                          {isEnded(item) ? <span className={styles.status}>[종료]</span> : null} {item.title}
                        </h5>
                        <div className={styles.subInfo}>
                          {item.startDate && item.endDate ? (
                            <ul className={styles.subList}>
                              <li>참여 기간 : {item.startDate} ~ {item.endDate}</li>
                            </ul>
                          ) : null}
                        </div>
                      </div>
                    </Link>
                  </div>
                </li>
              ))}
            </ul>
          )}
          <div className={styles.pagination}>
            {pageNumbers.map(number => (
              <button
                key={number}
                type="button"
                className={`${styles.pageButton} ${number === page ? styles.pageCurrent : ''}`}
                onClick={() => setPage(number)}
              >
                {number}
              </button>
            ))}
          </div>
        </div>
      </section>
    </main>
  );
};

export default EventsPage;
