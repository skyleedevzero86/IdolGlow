import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  fetchGlowAlerts,
  markGlowAlertRead,
  type GlowAlertCategoryId,
  type GlowAlertPageResponse,
  type GlowAlertStatus,
} from '../../../shared/data/glowAlertsApi';
import { useAuth } from '../../../auth/AuthContext';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import styles from './GlowAlertsPage.module.css';

const PAGE_SIZE = 4;
const STATUS_TABS: ReadonlyArray<{ id: GlowAlertStatus; label: string }> = [
  { id: 'unread', label: '안읽음' },
  { id: 'read', label: '읽음' },
];

function parseStatus(raw: string | null): GlowAlertStatus {
  return raw === 'read' ? 'read' : 'unread';
}

function parseCategory(raw: string | null): GlowAlertCategoryId {
  if (raw === 'verification' || raw === 'activity' || raw === 'finance') {
    return raw;
  }
  return 'all';
}

function parsePage(raw: string | null): number {
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : 1;
}

export const GlowAlertsPage = () => {
  const { accessToken } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const status = parseStatus(searchParams.get('status'));
  const category = parseCategory(searchParams.get('category'));
  const page = parsePage(searchParams.get('page'));
  const query = (searchParams.get('q') ?? '').trim();
  const [data, setData] = useState<GlowAlertPageResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState(query);

  useEffect(() => {
    setSearchText(query);
  }, [query]);

  useEffect(() => {
    let active = true;
    setLoading(true);

    fetchGlowAlerts({ page, size: PAGE_SIZE, status, category, q: query, accessToken })
      .then(response => {
        if (active) {
          setData(response);
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [accessToken, category, page, query, status]);

  const totalPages = data?.totalPages ?? 0;
  const visiblePages = useMemo(() => {
    if (totalPages <= 0) return [1];
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }, [totalPages]);

  const updateQuery = (next: Partial<{ status: GlowAlertStatus; category: GlowAlertCategoryId; page: number; q: string }>) => {
    const params = new URLSearchParams(searchParams);
    params.set('status', next.status ?? status);
    params.set('category', next.category ?? category);
    params.set('page', String(next.page ?? page));
    const nextQuery = next.q ?? query;
    if (nextQuery.trim()) {
      params.set('q', nextQuery.trim());
    } else {
      params.delete('q');
    }
    setSearchParams(params);
  };

  const selectStatus = (nextStatus: GlowAlertStatus) => {
    updateQuery({ status: nextStatus, category: 'all', page: 1 });
  };

  const selectCategory = (nextCategory: GlowAlertCategoryId) => {
    updateQuery({ category: nextCategory, page: 1 });
  };

  const goToPage = (nextPage: number) => {
    updateQuery({ page: Math.max(1, Math.min(nextPage, Math.max(totalPages, 1))) });
  };

  const submitSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    updateQuery({ q: searchText, page: 1 });
  };

  const clearSearch = () => {
    setSearchText('');
    updateQuery({ q: '', page: 1 });
  };

  const markRead = async (alertId: number, unread: boolean) => {
    if (!unread) return;
    await markGlowAlertRead(alertId, accessToken);
    setData(current => {
      if (!current) return current;
      const nextItems = current.items
        .map(item => (item.id === alertId ? { ...item, unread: false } : item))
        .filter(item => status !== 'unread' || item.unread);

      return {
        ...current,
        items: nextItems,
        totalElements: Math.max(0, current.totalElements - 1),
      };
    });
  };

  return (
    <AdminMarketingShell
      currentPath="/glow-alerts"
      title="Glow 알림"
      description="최근 한 달 동안 받은 알림을 확인할 수 있습니다."
      statusText="읽은 알림과 읽지 않은 알림은 한 달 동안 보관되고, 한 달이 지난 알림은 목록에서 제외됩니다."
      stats={[]}
      classNames={{ main: styles.shellMain, toolbarCard: styles.toolbarCard }}
    >
      <section className={styles.contentShell} aria-labelledby="glow-alerts-title">
        <h1 id="glow-alerts-title" className={styles.srOnly}>
          Glow 알림
        </h1>

        <div className={styles.alertPanel}>
          <div className={styles.topTabs} role="tablist" aria-label="Glow 알림 구분">
            {STATUS_TABS.map(item => (
              <button
                key={item.id}
                type="button"
                className={`${styles.topTab} ${status === item.id ? styles.topTabActive : ''}`}
                onClick={() => selectStatus(item.id)}
                role="tab"
                aria-selected={status === item.id}
              >
                {item.label}
              </button>
            ))}
          </div>

            <div className={styles.alertBody}>
              <div className={styles.sectionHeader}>
                <h2>이전 알림</h2>
                {data ? (
                  <span className={styles.totalCount}>{data.totalElements}건</span>
                ) : null}
              </div>

              <form className={styles.searchForm} role="search" onSubmit={submitSearch}>
                <label className={styles.srOnly} htmlFor="glow-alert-search">
                  Glow 알림 검색
                </label>
                <input
                  id="glow-alert-search"
                  type="search"
                  value={searchText}
                  onChange={event => setSearchText(event.target.value)}
                  placeholder="알림 검색"
                  className={styles.searchInput}
                />
                {query ? (
                  <button type="button" className={styles.searchClearButton} onClick={clearSearch}>
                    지우기
                  </button>
                ) : null}
                <button type="submit" className={styles.searchButton}>
                  검색
                </button>
              </form>

              <div className={styles.filterRow} aria-label="알림 카테고리">
                {(data?.categories ?? []).map(item => (
                  <button
                    key={item.id}
                    type="button"
                    className={`${styles.filterButton} ${category === item.id ? styles.filterButtonActive : ''}`}
                    onClick={() => selectCategory(item.id)}
                  >
                    {item.label}
                  </button>
                ))}
              </div>

              <ul className={styles.alertList} aria-busy={loading}>
                {(data?.items ?? []).map(item => (
                  <li key={item.id}>
                    <button
                      type="button"
                      onClick={() => void markRead(item.id, item.unread)}
                      aria-label={`${item.senderName} 알림 ${item.unread ? '읽음 처리' : '보기'}`}
                      disabled={!item.unread}
                      className={`${styles.alertCard} ${item.unread ? styles.alertCardUnread : ''}`}
                    >
                      <span
                        className={styles.avatar}
                        style={{ backgroundColor: item.iconTone }}
                        aria-hidden="true"
                      >
                        {item.iconText}
                      </span>
                      <div className={styles.alertText}>
                        <div className={styles.metaLine}>
                          <strong>{item.senderName}</strong>
                          {item.channelLabel ? <span>{item.channelLabel}</span> : null}
                          <time dateTime={item.receivedAt}>{item.receivedAtLabel}</time>
                        </div>
                        <p>{item.message}</p>
                      </div>
                    </button>
                  </li>
                ))}
              </ul>

              {!loading && data && data.items.length === 0 ? (
                <p className={styles.emptyState}>표시할 알림이 없습니다.</p>
              ) : null}

              <nav className={styles.pagination} aria-label="Glow 알림 페이지">
                <button
                  type="button"
                  className={styles.pageButton}
                  disabled={page <= 1}
                  onClick={() => goToPage(page - 1)}
                  aria-label="이전 페이지"
                >
                  ‹
                </button>
                {visiblePages.map(item => (
                  <button
                    key={item}
                    type="button"
                    className={`${styles.pageButton} ${page === item ? styles.pageButtonActive : ''}`}
                    onClick={() => goToPage(item)}
                    aria-current={page === item ? 'page' : undefined}
                  >
                    {item}
                  </button>
                ))}
                <button
                  type="button"
                  className={styles.pageButton}
                  disabled={totalPages === 0 || page >= totalPages}
                  onClick={() => goToPage(page + 1)}
                  aria-label="다음 페이지"
                >
                  ›
                </button>
              </nav>
            </div>
          </div>
      </section>
    </AdminMarketingShell>
  );
};

export default GlowAlertsPage;
