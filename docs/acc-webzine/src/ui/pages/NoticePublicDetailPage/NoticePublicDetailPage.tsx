import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  type PublicNoticeDetail,
  fetchPublicNoticeDetail,
  recordMbrdDocumentView,
} from "../../../shared/data/publicNoticesApi";
import { renderIdolGlowMarkdown } from "../../../shared/markdown/idolGlowMarkdown";
import detailStyles from "../EventPublicDetailPage/EventPublicDetailPage.module.css";

const normalizeTag = (tag: string) => {
  const clean = tag.startsWith("#") ? tag.slice(1) : tag;
  return `#${clean.replace(/_/g, " ")}`;
};

export const NoticePublicDetailPage = () => {
  const { documentId } = useParams<{ documentId: string }>();
  const [item, setItem] = useState<PublicNoticeDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!documentId) return;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetchPublicNoticeDetail(documentId);
        setItem(response);
        void recordMbrdDocumentView(documentId).catch(() => {
          /* 조회수 실패는 본문 표시에 영향 없음 */
        });
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
  }, [documentId]);

  const html = useMemo(
    () => renderIdolGlowMarkdown(item?.markdown ?? ""),
    [item?.markdown],
  );

  if (loading)
    return <main className={detailStyles.main}>불러오는 중입니다.</main>;
  if (error || !item)
    return (
      <main className={detailStyles.main}>{error ?? "공지가 없습니다."}</main>
    );

  return (
    <main className={detailStyles.main}>
      <section className={detailStyles.section}>
        <div className={detailStyles.wrap}>
          <div className={detailStyles.articleSign}>
            <p>공지사항</p>
            <h2 className={detailStyles.title}>{item.title}</h2>
            <p
              style={{
                margin: "0.5rem 0 0",
                color: "#64748b",
                fontSize: "1.05rem",
              }}
            >
              {item.author}
            </p>
          </div>
          <div className={detailStyles.content}>
            <div
              className={detailStyles.markdown}
              // biome-ignore lint/security/noDangerouslySetInnerHtml: trusted internal content
              dangerouslySetInnerHTML={{ __html: html }}
            />
            {item.tags.length > 0 ? (
              <div className={detailStyles.tagRow}>
                {item.tags.map((tag) => (
                  <span key={tag} className={detailStyles.tag}>
                    {normalizeTag(tag)}
                  </span>
                ))}
              </div>
            ) : null}
          </div>
          <div className={detailStyles.articleBottom}>
            <div className={detailStyles.backList}>
              <Link to="/notices">목록으로</Link>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
};

export default NoticePublicDetailPage;
