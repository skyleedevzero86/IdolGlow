import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { isOpsStaff } from '../../../auth/authApi';
import { useAuth } from '../../../auth/AuthContext';
import {
  createEventMatchMerge,
  fetchAdminAuditTop,
  fetchOpsChanges,
  postOpsAction,
  type AdminAuditTop,
  type OpsChangeItem,
} from '../../../shared/data/opsChangeApi';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from './AdminOpsChangesPage.module.css';

function mergeReason(payload: string): string {
  try {
    const parsed = JSON.parse(payload) as { reason?: unknown };
    return typeof parsed.reason === 'string' && parsed.reason.trim() !== '' ? parsed.reason.trim() : '—';
  } catch {
    return '—';
  }
}

const STATUS_LABEL: Record<OpsChangeItem['status'], string> = {
  REQUESTED: '요청',
  APPROVED: '승인',
  REJECTED: '거절',
  APPLIED: '적용',
  REVERTED: '되돌림',
};

export function AdminOpsChangesPage() {
  const { authReady, user, accessToken } = useAuth();
  const [items, setItems] = useState<OpsChangeItem[]>([]);
  const [auditTop, setAuditTop] = useState<AdminAuditTop | null>(null);
  const [auditNote, setAuditNote] = useState<string | null>(null);
  const [source, setSource] = useState('KOPIS_PBLPRFR');
  const [sourceId, setSourceId] = useState('');
  const [globalId, setGlobalId] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const canRequest = user?.role === 'ADMIN' || user?.role === 'OPERATOR';
  const canApprove = user?.role === 'ADMIN' || user?.role === 'APPROVER';

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await fetchOpsChanges(accessToken));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '운영 변경 목록을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }, [accessToken]);

  const loadAuditTop = useCallback(async () => {
    if (user?.role !== 'ADMIN') {
      setAuditTop(null);
      setAuditNote(null);
      return;
    }
    try {
      setAuditTop(await fetchAdminAuditTop(accessToken));
      setAuditNote(null);
    } catch (caught) {
      setAuditTop(null);
      setAuditNote(caught instanceof Error ? caught.message : '반복된 관리자 작업을 불러오지 못했습니다.');
    }
  }, [accessToken, user?.role]);

  useEffect(() => {
    if (authReady && user && isOpsStaff(user.role)) {
      void load();
      void loadAuditTop();
    }
  }, [authReady, user, load, loadAuditTop]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await createEventMatchMerge(accessToken, source.trim(), sourceId.trim(), globalId.trim());
      setSourceId('');
      setGlobalId('');
      await load();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '운영 변경 요청에 실패했습니다.');
      setLoading(false);
    }
  };

  const act = async (requestId: number, action: 'approve' | 'reject' | 'apply' | 'revert') => {
    setLoading(true);
    setError(null);
    try {
      await postOpsAction(accessToken, requestId, action);
      await load();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '운영 변경 처리에 실패했습니다.');
      setLoading(false);
    }
  };

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.loading}>운영 변경 권한을 확인하는 중입니다.</div>
      </main>
    );
  }

  if (!user || !isOpsStaff(user.role)) {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.denied}>운영자, 승인자, 관리자만 변경 승인 화면을 사용할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/ops-changes"
      title="변경 승인"
      description="에이전트 병합 제안도 이 화면에 남고, 사람이 승인한 뒤에만 적용됩니다."
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
      }}
      statusText={error ?? (loading ? '운영 변경을 처리하는 중입니다.' : null)}
      stats={[
        { label: '요청', value: items.filter((item) => item.status === 'REQUESTED').length },
        { label: '승인', value: items.filter((item) => item.status === 'APPROVED').length },
        { label: '적용', value: items.filter((item) => item.status === 'APPLIED').length },
      ]}
    >
      {error ? <p className={styles.error}>{error}</p> : null}
      {user.role === 'ADMIN' ? (
        <section className={styles.audit}>
          <h2 className={styles.auditTitle}>반복된 관리자 작업</h2>
          {auditNote ? <p className={styles.error}>{auditNote}</p> : null}
          {auditTop ? (
            <>
              <p className={styles.basis}>{auditTop.basis}</p>
              <ol className={styles.auditList}>
                {auditTop.actions.map((action) => (
                  <li key={action.actionCode}>
                    {action.actionCode} {action.count}회
                  </li>
                ))}
              </ol>
              <p>
                자동화 후보 {auditTop.automationActionCode ?? '없음'}. {auditTop.automationReason}
              </p>
            </>
          ) : null}
        </section>
      ) : null}
      {canRequest ? (
        <form className={styles.form} onSubmit={(event) => void submit(event)}>
          <input className={styles.field} value={source} onChange={(event) => setSource(event.target.value)} placeholder="공급처" />
          <input className={styles.field} value={sourceId} onChange={(event) => setSourceId(event.target.value)} placeholder="공급처 ID" />
          <input className={styles.field} value={globalId} onChange={(event) => setGlobalId(event.target.value)} placeholder="글로벌 공연 ID" />
          <button className={styles.button} type="submit" disabled={loading || sourceId.trim() === '' || globalId.trim() === ''}>
            병합 요청
          </button>
        </form>
      ) : null}
      <table className={styles.table}>
        <thead>
          <tr>
            <th>번호</th>
            <th>대상</th>
            <th>상태</th>
            <th>요청자</th>
            <th>근거</th>
            <th>처리</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.id}>
              <td>{item.id}</td>
              <td>{item.target}</td>
              <td>{STATUS_LABEL[item.status]}</td>
              <td>{item.requestedByKind === 'AGENT' ? `에이전트 ${item.requestedBy}` : item.requestedBy}</td>
              <td className={styles.reason}>{mergeReason(item.payload)}</td>
              <td className={styles.actions}>
                {canApprove && item.status === 'REQUESTED' && item.requestedBy !== String(user.id) ? (
                  <>
                    <button className={styles.button} type="button" disabled={loading} onClick={() => void act(item.id, 'approve')}>
                      승인
                    </button>
                    <button className={`${styles.button} ${styles.secondary}`} type="button" disabled={loading} onClick={() => void act(item.id, 'reject')}>
                      거절
                    </button>
                  </>
                ) : null}
                {canRequest && item.status === 'APPROVED' ? (
                  <button className={styles.button} type="button" disabled={loading} onClick={() => void act(item.id, 'apply')}>
                    적용
                  </button>
                ) : null}
                {canRequest && item.status === 'APPLIED' ? (
                  <button className={`${styles.button} ${styles.secondary}`} type="button" disabled={loading} onClick={() => void act(item.id, 'revert')}>
                    되돌리기
                  </button>
                ) : null}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </AdminMarketingShell>
  );
}
