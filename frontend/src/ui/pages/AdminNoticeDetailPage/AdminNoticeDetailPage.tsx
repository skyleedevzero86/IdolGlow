import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import { getApiBaseUrl } from "../../../auth/authConfig";
import {
  type AdminNoticeDetail,
  deleteAdminNotice,
  fetchAdminNotice,
} from "../../../shared/data/adminNoticesApi";
import {
  isNoticeImage,
  parseNoticeAttachments,
} from "../../../shared/data/noticeFiles";
import { formatIdolGlowDateTime } from "../../../shared/format/idolGlowDateTime";
import { renderIdolGlowMarkdown } from "../../../shared/markdown/idolGlowMarkdown";
import styles from "../AdminEventsPage/AdminEventsPage.module.css";
import shellStyles from "../AdminMarketingPage/AdminMarketingPage.module.css";
import { AdminMarketingShell } from "../AdminMarketingPage/AdminMarketingShell";
import shellToolbar from "../AdminUsersPage/AdminUsersShellToolbar.module.css";

const noticeAssetUrl = (url: string): string => {
  if (/^https?:\/\//i.test(url)) return url;
  if (url.startsWith("/")) return `${getApiBaseUrl()}${url}`;
  return url;
};

const statusLabel = (status: AdminNoticeDetail["status"]): string => {
  if (status === "published") return "게시";
  if (status === "draft") return "임시저장";
  return status;
};

export function AdminNoticeDetailPage() {
  const { documentId } = useParams<{ readonly documentId: string }>();
  const navigate = useNavigate();
  const { accessToken, authReady, user } = useAuth();
  const [item, setItem] = useState<AdminNoticeDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!authReady || user?.role !== "ADMIN" || !documentId) return;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetchAdminNotice(accessToken, documentId);
        setItem(response);
      } catch (loadError) {
        setItem(null);
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
  }, [authReady, user?.role, accessToken, documentId]);

  const rendered = useMemo(
    () => renderIdolGlowMarkdown(item?.markdown ?? ""),
    [item?.markdown],
  );
  const images = useMemo(
    () =>
      parseNoticeAttachments(item?.introduction ?? null).filter(isNoticeImage),
    [item?.introduction],
  );

  const handleDelete = async () => {
    if (!item) return;
    if (!window.confirm(`"${item.title}" 공지를 삭제할까요?`)) return;
    try {
      await deleteAdminNotice(accessToken, item.documentId);
      navigate("/admin/notices");
    } catch (deleteError) {
      setError(
        deleteError instanceof Error
          ? deleteError.message
          : "공지 삭제에 실패했습니다.",
      );
    }
  };

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.loading}>
          관리자 권한을 확인하는 중입니다.
        </div>
      </main>
    );
  }

  if (user?.role !== "ADMIN") {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.denied}>
          관리자만 공지사항 화면을 사용할 수 있습니다.
        </div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/notices"
      title="공지 상세"
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
      }}
      statusText={loading ? "공지를 불러오는 중입니다." : error}
      stats={[]}
    >
      <section className={styles.panel}>
        <div className={styles.panelHeader}>
          <h2 className={styles.panelTitle}>{item?.title ?? "공지 상세"}</h2>
        </div>
        <div className={styles.panelBody}>
          {item ? (
            <>
              <p className={`${styles.cardMeta} ${styles.metaRight}`}>
                {statusLabel(item.status)} · 최근 수정{" "}
                {formatIdolGlowDateTime(item.updatedAt)}
              </p>
              <div className={`${styles.formActions} ${styles.detailActions}`}>
                <button
                  type="button"
                  className={styles.primaryButton}
                  onClick={() =>
                    navigate(`/admin/notices/${item.documentId}/edit`)
                  }
                >
                  수정
                </button>
                <button
                  type="button"
                  className={styles.dangerButton}
                  onClick={() => void handleDelete()}
                >
                  삭제
                </button>
                <button
                  type="button"
                  className={styles.secondaryButton}
                  onClick={() => navigate("/admin/notices")}
                >
                  목록
                </button>
              </div>
              <section className={styles.previewSection}>
                <div className={styles.previewHeader}>
                  <h3 className={styles.previewTitle}>본문</h3>
                  {item.tags.length > 0 ? (
                    <div className={styles.previewTagsRight}>
                      {item.tags.map((tag) => (
                        <span key={tag} className={styles.tagChip}>
                          #{tag.replace(/^#/, "").replace(/_/g, " ")}
                        </span>
                      ))}
                    </div>
                  ) : null}
                </div>
                <div className={styles.previewContent}>
                  {images.map((file) => (
                    <img
                      key={file.url}
                      src={noticeAssetUrl(file.url)}
                      alt={file.name}
                      className={styles.noticePreviewImage}
                    />
                  ))}
                  <div
                    // biome-ignore lint/security/noDangerouslySetInnerHtml: trusted internal content
                    dangerouslySetInnerHTML={{ __html: rendered }}
                  />
                </div>
              </section>
            </>
          ) : (
            <div className={styles.empty}>표시할 공지가 없습니다.</div>
          )}
        </div>
      </section>
    </AdminMarketingShell>
  );
}

export default AdminNoticeDetailPage;
