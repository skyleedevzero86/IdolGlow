import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import { deleteAdminEvent, fetchAdminEvent, type AdminEventDetail } from '../../../shared/data/adminEventsApi';
import { hasNoticeBoardMarker, isHiddenBoardMarkerTag } from '../../../shared/data/mbrdBoardMarkers';
import { renderIdolGlowMarkdown } from '../../../shared/markdown/idolGlowMarkdown';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from '../AdminEventsPage/AdminEventsPage.module.css';

const META_START_PREFIX = 'event-start:';
const META_END_PREFIX = 'event-end:';

export function AdminEventDetailPage() {
  const { documentId } = useParams<{ readonly documentId: string }>();
  const navigate = useNavigate();
  const { accessToken, authReady, user } = useAuth();
  const [item, setItem] = useState<AdminEventDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!authReady || user?.role !== 'ADMIN' || !documentId) return;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetchAdminEvent(accessToken, documentId);
        if (hasNoticeBoardMarker(response.tags)) {
          setError('이 문서는 공지사항입니다. 공지사항 관리(/admin/notices)에서 확인해 주세요.');
          setItem(null);
          return;
        }
        setItem(response);
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : '이벤트를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [authReady, user?.role, accessToken, documentId]);

  const rendered = useMemo(() => renderIdolGlowMarkdown(item?.markdown ?? ''), [item?.markdown]);
  const visibleTags = useMemo(
    () =>
      (item?.tags ?? [])
        .map(tag => tag.trim())
        .filter(Boolean)
        .filter(
          tag =>
            !tag.startsWith(META_START_PREFIX) &&
            !tag.startsWith(META_END_PREFIX) &&
            !isHiddenBoardMarkerTag(tag),
        ),
    [item?.tags],
  );

  const handleDelete = async () => {
    if (!item) return;
    if (!window.confirm(`"${item.title}" 이벤트를 삭제할까요?`)) return;
    try {
      await deleteAdminEvent(accessToken, item.documentId);
      navigate('/admin/events');
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : '이벤트 삭제에 실패했습니다.');
    }
  };

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.loading}>관리자 권한을 확인하는 중입니다.</div>
      </main>
    );
  }

  if (user?.role !== 'ADMIN') {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.denied}>관리자만 이벤트 관리 화면을 사용할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/events"
      title="이벤트 상세"
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
      }}
      statusText={loading ? '이벤트를 불러오는 중입니다.' : error}
      stats={[]}
    >
      <section className={styles.panel}>
        <div className={styles.panelHeader}>
          <h2 className={styles.panelTitle}>{item?.title ?? '이벤트 상세'}</h2>
        </div>
        <div className={styles.panelBody}>
          {item ? (
            <>
              {item.introduction ? <p className={styles.cardIntro}>{item.introduction}</p> : null}
              <p className={`${styles.cardMeta} ${styles.metaRight}`}>
                최근 수정 {new Date(item.updatedAt).toLocaleString('ko-KR')}
              </p>
              <div className={`${styles.formActions} ${styles.detailActions}`}>
                <button
                  type="button"
                  className={styles.primaryButton}
                  onClick={() => navigate(`/admin/events/${item.documentId}/edit`)}
                >
                  수정
                </button>
                <button type="button" className={styles.dangerButton} onClick={handleDelete}>
                  삭제
                </button>
                <button type="button" className={styles.secondaryButton} onClick={() => navigate('/admin/events')}>
                  목록
                </button>
              </div>
              <section className={styles.previewSection}>
                <div className={styles.previewHeader}>
                  <h3 className={styles.previewTitle}>본문</h3>
                  {visibleTags.length > 0 ? (
                    <div className={styles.previewTagsRight}>
                      {visibleTags.map(tag => (
                        <span key={tag} className={styles.tagChip}>
                          #{tag.replace(/^#/, '').replace(/_/g, ' ')}
                        </span>
                      ))}
                    </div>
                  ) : null}
                </div>
                <div className={styles.previewContent} dangerouslySetInnerHTML={{ __html: rendered }} />
              </section>
            </>
          ) : (
            <div className={styles.empty}>표시할 이벤트가 없습니다.</div>
          )}
        </div>
      </section>
    </AdminMarketingShell>
  );
}

export default AdminEventDetailPage;
