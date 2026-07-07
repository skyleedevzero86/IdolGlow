import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  type PublicEventDetail,
  fetchPublicEventDetail,
  recordMbrdDocumentView,
} from "../../../shared/data/publicEventsApi";
import { renderIdolGlowMarkdown } from "../../../shared/markdown/idolGlowMarkdown";
import styles from "./EventPublicDetailPage.module.css";

const normalizeTag = (tag: string) => {
  const clean = tag.startsWith("#") ? tag.slice(1) : tag;
  return `#${clean.replace(/_/g, " ")}`;
};

export const EventPublicDetailPage = () => {
  const { documentId } = useParams<{ documentId: string }>();
  const [item, setItem] = useState<PublicEventDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!documentId) return;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetchPublicEventDetail(documentId);
        setItem(response);
        void recordMbrdDocumentView(documentId).catch(() => {
          /* 조회수 실패는 본문 표시에 영향 없음 */
        });
      } catch (loadError) {
        setError(
          loadError instanceof Error
            ? loadError.message
            : "이벤트를 불러오지 못했습니다.",
        );
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [documentId]);

  const html = useMemo(
    () => renderIdolGlowMarkdown(item?.markdown ?? ""),
    [item?.markdown],
  );

  if (loading)
    return <main className={styles.main}>이벤트를 불러오는 중입니다.</main>;
  if (error || !item)
    return (
      <main className={styles.main}>
        {error ?? "이벤트가 존재하지 않습니다."}
      </main>
    );

  return (
    <main className={styles.main}>
      <section className={styles.section}>
        <div className={styles.wrap}>
          <div className={styles.articleSign}>
            <p>이벤트</p>
            <h2 className={styles.title}>{item.title}</h2>
          </div>
          <div className={styles.content}>
            <div
              className={styles.markdown}
              // biome-ignore lint/security/noDangerouslySetInnerHtml: trusted internal content
              dangerouslySetInnerHTML={{ __html: html }}
            />
            {item.tags.length > 0 ? (
              <div className={styles.tagRow}>
                {item.tags.map((tag) => (
                  <span key={tag} className={styles.tag}>
                    {normalizeTag(tag)}
                  </span>
                ))}
              </div>
            ) : null}
          </div>
          <div className={styles.articleBottom}>
            <div className={styles.backList}>
              <Link to="/events">목록보기</Link>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
};

export default EventPublicDetailPage;
