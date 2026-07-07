import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import {
  deleteAdminNewsletter,
  fetchAdminNewsletter,
  type AdminNewsletterDetail,
} from '../../../shared/data/newsletterAdminApi';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import styles from '../AdminMarketingPage/AdminMarketingPage.module.css';

export function AdminNewsletterDetailPage() {
  const navigate = useNavigate();
  const { newsletterSlug } = useParams<{ readonly newsletterSlug: string }>();
  const { accessToken, authReady, user } = useAuth();
  const [newsletter, setNewsletter] = useState<AdminNewsletterDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!authReady || !accessToken || user?.role !== 'ADMIN' || !newsletterSlug) {
      return;
    }

    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetchAdminNewsletter(accessToken, newsletterSlug);
        if (!cancelled) {
          setNewsletter(response);
        }
      } catch (loadError) {
        if (!cancelled) {
          setError(
            loadError instanceof Error ? loadError.message : '뉴스레터 상세 정보를 불러오지 못했습니다.'
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    void load();

    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, newsletterSlug, user?.role]);

  const handleDelete = async () => {
    if (!accessToken || !newsletterSlug || !newsletter) {
      return;
    }

    if (!window.confirm(`'${newsletter.title}'을 삭제할까요?`)) {
      return;
    }

    try {
      await deleteAdminNewsletter(accessToken, newsletterSlug);
      navigate('/admin/newsletters');
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : '뉴스레터를 삭제하지 못했습니다.');
    }
  };

  if (!authReady) {
    return (
      <main className={styles.page}>
        <div className={styles.loading}>관리자 권한을 확인하는 중입니다.</div>
      </main>
    );
  }

  if (!accessToken || user?.role !== 'ADMIN') {
    return (
      <main className={styles.page}>
        <div className={styles.denied}>관리자만 뉴스레터 상세를 확인할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/newsletters"
      title="뉴스레터 상세"
      description="등록된 뉴스레터 내용을 확인합니다."
      statusText={
        error
          ? error
          : loading
            ? '뉴스레터 상세 정보를 불러오는 중입니다.'
            : newsletter
              ? newsletter.title
              : '뉴스레터를 찾을 수 없습니다.'
      }
      stats={[
        { label: '카테고리', value: newsletter?.categoryLabel ?? '-' },
        { label: '발행일', value: newsletter?.publishedAt ?? '-' },
        { label: '태그 수', value: newsletter?.tags.length ?? 0 },
      ]}
    >
      <section className={styles.panel}>
        <div className={styles.panelHeader}>
          <h2 className={styles.panelTitle}>뉴스레터 정보</h2>
          <div className={styles.rowActions}>
            <Link to="/admin/newsletters" className={styles.secondaryButton}>
              목록
            </Link>
            {newsletter ? (
              <Link
                to={`/admin/newsletters/${newsletter.slug}/edit`}
                className={styles.secondaryButton}
              >
                수정
              </Link>
            ) : null}
            {newsletter ? (
              <button type="button" className={styles.dangerButton} onClick={handleDelete}>
                삭제
              </button>
            ) : null}
          </div>
        </div>
        <div className={styles.panelBody}>
          {loading ? (
            <div className={styles.empty}>뉴스레터 상세 정보를 불러오는 중입니다.</div>
          ) : error ? (
            <div className={styles.empty}>{error}</div>
          ) : !newsletter ? (
            <div className={styles.empty}>뉴스레터를 찾을 수 없습니다.</div>
          ) : (
            <>
              {newsletter.imageUrl ? (
                <div className={styles.preview}>
                  <img src={newsletter.imageUrl} alt={newsletter.title} />
                </div>
              ) : null}
              <div className={styles.cardBody} style={{ marginTop: '1rem' }}>
                <h3 className={styles.name}>{newsletter.title}</h3>
                <div className={styles.chips}>
                  <span className={styles.chip}>{newsletter.categoryLabel}</span>
                  {newsletter.tags.map(tag => (
                    <span key={tag} className={styles.chip}>
                      #{tag}
                    </span>
                  ))}
                </div>
                <p className={styles.meta}>{newsletter.summary}</p>
                {newsletter.paragraphs.map((paragraph, index) => (
                  <p key={`${newsletter.slug}-${index}`} className={styles.meta}>
                    {paragraph}
                  </p>
                ))}
              </div>
            </>
          )}
        </div>
      </section>
    </AdminMarketingShell>
  );
}

export default AdminNewsletterDetailPage;
