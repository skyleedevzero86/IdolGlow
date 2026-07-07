import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  type PublicNoticeSummary,
  fetchPublicNotices,
} from "../../../shared/data/publicNoticesApi";
import eventStyles from "../EventsPage/EventsPage.module.css";

const PAGE_SIZE = 10;

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
            : "공지를 불러오지 못했습니다.",
        );
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [page]);

  const pageNumbers = useMemo(
    () => Array.from({ length: totalPages }, (_, index) => index + 1),
    [totalPages],
  );

  return (
    <main className={eventStyles.main}>
      <section className={eventStyles.section}>
        <div className={eventStyles.wrap}>
          <h4 className={eventStyles.title}>공지사항</h4>
          {error ? <div className={eventStyles.error}>{error}</div> : null}
          {loading ? (
            <div className={eventStyles.empty}>불러오는 중입니다.</div>
          ) : items.length === 0 ? (
            <div className={eventStyles.empty}>등록된 공지가 없습니다.</div>
          ) : (
            <ul className={eventStyles.list}>
              {items.map((item) => (
                <li key={item.documentId}>
                  <div className={eventStyles.eventBox}>
                    <Link
                      to={`/notices/${item.documentId}`}
                      className={eventStyles.eventLink}
                    >
                      <div className={eventStyles.thumbWrap}>
                        {item.thumbnailImageUrl ? (
                          <img
                            src={item.thumbnailImageUrl}
                            alt={item.title}
                            className={eventStyles.thumb}
                          />
                        ) : (
                          <div className={eventStyles.thumbFallback}>
                            NOTICE
                          </div>
                        )}
                      </div>
                      <div className={eventStyles.info}>
                        <div className={eventStyles.category}>공지</div>
                        <h5 className={eventStyles.subject}>{item.title}</h5>
                        {item.introduction ? (
                          <div className={eventStyles.subInfo}>
                            <p style={{ margin: 0, fontSize: "0.85rem" }}>
                              {item.introduction.length > 180
                                ? `${item.introduction.slice(0, 180)}…`
                                : item.introduction}
                            </p>
                          </div>
                        ) : null}
                      </div>
                    </Link>
                  </div>
                </li>
              ))}
            </ul>
          )}
          {totalPages > 1 ? (
            <div className={eventStyles.pagination}>
              {pageNumbers.map((number) => (
                <button
                  key={number}
                  type="button"
                  className={`${eventStyles.pageButton} ${
                    number === page ? eventStyles.pageCurrent : ""
                  }`}
                  onClick={() => setPage(number)}
                >
                  {number}
                </button>
              ))}
            </div>
          ) : null}
        </div>
      </section>
    </main>
  );
};

export default NoticesPage;
