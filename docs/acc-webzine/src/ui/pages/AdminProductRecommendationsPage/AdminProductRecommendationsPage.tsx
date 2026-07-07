import { useCallback, useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../../auth/AuthContext';
import { fetchAdminProducts } from '../../../shared/data/adminBookingApi';
import {
  fetchAdminPickedRecommendations,
  fetchLatestInKoreaRecommendations,
  replaceLatestInKoreaRecommendations,
  updateProductAdminRecommendation,
  updateProductRecommendationScore,
  type ProductRecommendationItem,
} from '../../../shared/data/productRecommendationApi';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from './AdminProductRecommendationsPage.module.css';

const parseProductIds = (raw: string): number[] => {
  const parts = raw
    .split(/[\s,]+/)
    .map(s => s.trim())
    .filter(Boolean);
  const ids = parts.map(p => Number(p)).filter(n => Number.isFinite(n) && n > 0);
  return Array.from(new Set(ids));
};

const formatMoney = (n: number) =>
  new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW', maximumFractionDigits: 0 }).format(n);

export function AdminProductRecommendationsPage() {
  const { accessToken, authReady, user } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const [latestList, setLatestList] = useState<readonly ProductRecommendationItem[]>([]);
  const [pickedList, setPickedList] = useState<readonly ProductRecommendationItem[]>([]);
  const [latestIdsText, setLatestIdsText] = useState('');
  const [pickedTag, setPickedTag] = useState('');
  const [pickedQuery, setPickedQuery] = useState('');
  const [latestTag, setLatestTag] = useState('');

  const [productSearch, setProductSearch] = useState('');
  const [pickerItems, setPickerItems] = useState<Awaited<ReturnType<typeof fetchAdminProducts>>>([]);

  const [editProductId, setEditProductId] = useState('');
  const [editRecommended, setEditRecommended] = useState(true);
  const [editScore, setEditScore] = useState('0');

  const [loadingLatest, setLoadingLatest] = useState(false);
  const [loadingPicked, setLoadingPicked] = useState(false);
  const [savingLatest, setSavingLatest] = useState(false);
  const [savingFlags, setSavingFlags] = useState(false);

  const reloadLatest = useCallback(async () => {
    setLoadingLatest(true);
    setError(null);
    try {
      const list = await fetchLatestInKoreaRecommendations(50, latestTag || null);
      setLatestList(list);
    } catch (e) {
      setLatestList([]);
      setError(e instanceof Error ? e.message : 'latest-in-korea 목록을 불러오지 못했습니다.');
    } finally {
      setLoadingLatest(false);
    }
  }, [latestTag]);

  const reloadPicked = useCallback(async () => {
    setLoadingPicked(true);
    setError(null);
    try {
      const list = await fetchAdminPickedRecommendations(50, pickedTag || null, pickedQuery || null);
      setPickedList(list);
    } catch (e) {
      setPickedList([]);
      setError(e instanceof Error ? e.message : 'admin-picked 목록을 불러오지 못했습니다.');
    } finally {
      setLoadingPicked(false);
    }
  }, [pickedQuery, pickedTag]);

  useEffect(() => {
    if (!authReady || user?.role !== 'ADMIN') {
      return;
    }
    void reloadLatest();
    void reloadPicked();
  }, [authReady, reloadLatest, reloadPicked, user?.role]);

  const loadPicker = async () => {
    if (!accessToken) {
      return;
    }
    setError(null);
    try {
      const items = await fetchAdminProducts(accessToken, productSearch || undefined);
      setPickerItems(items);
    } catch (e) {
      setPickerItems([]);
      setError(e instanceof Error ? e.message : '상품 검색에 실패했습니다.');
    }
  };

  const appendId = (id: number) => {
    const current = latestIdsText.trim();
    const next = current.length > 0 ? `${current}\n${id}` : String(id);
    setLatestIdsText(next);
  };

  const handleSaveLatest = async () => {
    if (!accessToken) {
      setError('로그인이 필요합니다.');
      return;
    }
    const ids = parseProductIds(latestIdsText);
    if (ids.length === 0) {
      setError('저장할 상품 ID를 입력해 주세요.');
      return;
    }
    setSavingLatest(true);
    setError(null);
    setMessage(null);
    try {
      const res = await replaceLatestInKoreaRecommendations(accessToken, ids);
      setMessage(`latest-in-korea 순서를 저장했습니다. (${res.productCount}건)`);
      await reloadLatest();
    } catch (e) {
      setError(e instanceof Error ? e.message : '저장에 실패했습니다.');
    } finally {
      setSavingLatest(false);
    }
  };

  const handleApplyRecommendation = async () => {
    if (!accessToken) {
      setError('로그인이 필요합니다.');
      return;
    }
    const id = Number(editProductId);
    if (!Number.isFinite(id) || id <= 0) {
      setError('유효한 상품 ID를 입력해 주세요.');
      return;
    }
    setSavingFlags(true);
    setError(null);
    setMessage(null);
    try {
      await updateProductAdminRecommendation(accessToken, id, editRecommended);
      setMessage(`상품 #${id} 관리자 추천 플래그를 반영했습니다.`);
      await reloadPicked();
    } catch (e) {
      setError(e instanceof Error ? e.message : '반영에 실패했습니다.');
    } finally {
      setSavingFlags(false);
    }
  };

  const handleApplyScore = async () => {
    if (!accessToken) {
      setError('로그인이 필요합니다.');
      return;
    }
    const id = Number(editProductId);
    const score = Number(editScore);
    if (!Number.isFinite(id) || id <= 0) {
      setError('유효한 상품 ID를 입력해 주세요.');
      return;
    }
    if (!Number.isFinite(score) || score < 0) {
      setError('추천 점수는 0 이상 숫자로 입력해 주세요.');
      return;
    }
    setSavingFlags(true);
    setError(null);
    setMessage(null);
    try {
      await updateProductRecommendationScore(accessToken, id, Math.floor(score));
      setMessage(`상품 #${id} 추천 점수를 반영했습니다.`);
      await reloadPicked();
    } catch (e) {
      setError(e instanceof Error ? e.message : '반영에 실패했습니다.');
    } finally {
      setSavingFlags(false);
    }
  };

  const stats = useMemo(
    () => [
      { label: 'latest 노출', value: loadingLatest ? '…' : String(latestList.length) },
      { label: 'admin-picked', value: loadingPicked ? '…' : String(pickedList.length) },
    ],
    [latestList.length, loadingLatest, loadingPicked, pickedList.length],
  );

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.loading}>관리자 권한을 확인하는 중입니다.</div>
      </main>
    );
  }

  if (!accessToken || user?.role !== 'ADMIN') {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.denied}>관리자만 이 화면을 사용할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/products/recommendations"
      title="상품 추천 관리"
      description="latest-in-korea 큐레이션과 admin-picked(관리자 추천) 운영"
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
        stats: shellToolbar.stats,
        statCard: shellToolbar.statCard,
        statLabel: shellToolbar.statLabel,
        statValue: shellToolbar.statValue,
      }}
      statusText={error ?? message}
      stats={stats.map(s => ({
        label: s.label,
        value: <span className={shellToolbar.statNumber}>{s.value}</span>,
      }))}
    >
      <div className={`${styles.root} ${styles.container}`}>
        <div className={styles.sheet}>
          <section className={styles.panel}>
            <h2 className={styles.panelTitle}>latest-in-korea</h2>
            <p className={styles.panelDesc}>
              노출 순서대로 상품 ID를 한 줄에 하나씩(또는 쉼표) 입력한 뒤 저장합니다. 저장 후 아래 미리보기가 갱신됩니다.
            </p>
            <div className={styles.row}>
              <span className={styles.label}>태그 필터(미리보기)</span>
              <input
                className={styles.input}
                value={latestTag}
                onChange={e => setLatestTag(e.target.value)}
                placeholder="예: 뷰티"
              />
              <button type="button" className={styles.secondaryButton} onClick={() => void reloadLatest()} disabled={loadingLatest}>
                미리보기 새로고침
              </button>
            </div>
            <textarea
              className={styles.textarea}
              value={latestIdsText}
              onChange={e => setLatestIdsText(e.target.value)}
              placeholder={'예:\n12\n5\n19'}
            />
            <div className={styles.row}>
              <button type="button" className={styles.primaryButton} onClick={() => void handleSaveLatest()} disabled={savingLatest}>
                {savingLatest ? '저장 중…' : '순서 저장'}
              </button>
            </div>

            <div className={styles.productPicker}>
              <div className={styles.row}>
                <span className={styles.label}>상품 검색으로 ID 붙여넣기</span>
                <input
                  className={styles.input}
                  value={productSearch}
                  onChange={e => setProductSearch(e.target.value)}
                  placeholder="상품명 키워드"
                />
                <button type="button" className={styles.secondaryButton} onClick={() => void loadPicker()}>
                  검색
                </button>
              </div>
              <ul className={styles.pickerList}>
                {pickerItems.slice(0, 30).map(p => (
                  <li key={p.id} className={styles.pickerRow}>
                    <span>
                      #{p.id} {p.name}
                    </span>
                    <button type="button" className={styles.miniButton} onClick={() => appendId(p.id)}>
                      ID 추가
                    </button>
                  </li>
                ))}
              </ul>
            </div>

            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th className={styles.th}>ID</th>
                    <th className={styles.th}>상품명</th>
                    <th className={`${styles.th} ${styles.num}`}>총가</th>
                  </tr>
                </thead>
                <tbody>
                  {latestList.map(p => (
                    <tr key={p.id}>
                      <td className={styles.td}>{p.id}</td>
                      <td className={styles.td}>{p.name}</td>
                      <td className={`${styles.td} ${styles.num}`}>{formatMoney(p.totalPrice)}</td>
                    </tr>
                  ))}
                  {latestList.length === 0 ? (
                    <tr>
                      <td className={styles.td} colSpan={3}>
                        {loadingLatest ? '불러오는 중…' : '등록된 latest-in-korea 추천이 없습니다.'}
                      </td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          </section>

          <section className={styles.panel}>
            <h2 className={styles.panelTitle}>admin-picked 미리보기</h2>
            <p className={styles.panelDesc}>관리자 추천 플래그가 켜진 상품을 조건에 맞게 조회합니다.</p>
            <div className={styles.row}>
              <span className={styles.label}>태그</span>
              <input className={styles.input} value={pickedTag} onChange={e => setPickedTag(e.target.value)} placeholder="태그" />
              <span className={styles.label}>검색어</span>
              <input className={styles.input} value={pickedQuery} onChange={e => setPickedQuery(e.target.value)} placeholder="상품명" />
              <button type="button" className={styles.secondaryButton} onClick={() => void reloadPicked()} disabled={loadingPicked}>
                조회
              </button>
            </div>
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th className={styles.th}>ID</th>
                    <th className={styles.th}>상품명</th>
                    <th className={`${styles.th} ${styles.num}`}>총가</th>
                  </tr>
                </thead>
                <tbody>
                  {pickedList.map(p => (
                    <tr key={p.id}>
                      <td className={styles.td}>{p.id}</td>
                      <td className={styles.td}>{p.name}</td>
                      <td className={`${styles.td} ${styles.num}`}>{formatMoney(p.totalPrice)}</td>
                    </tr>
                  ))}
                  {pickedList.length === 0 ? (
                    <tr>
                      <td className={styles.td} colSpan={3}>
                        {loadingPicked ? '불러오는 중…' : '조건에 맞는 추천 상품이 없습니다.'}
                      </td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          </section>

          <section className={styles.panel}>
            <h2 className={styles.panelTitle}>단일 상품 추천 설정</h2>
            <p className={styles.panelDesc}>상품 ID 기준으로 관리자 추천 여부와 추천 점수를 각각 저장합니다.</p>
            <div className={styles.row}>
              <span className={styles.label}>상품 ID</span>
              <input className={styles.input} value={editProductId} onChange={e => setEditProductId(e.target.value)} inputMode="numeric" />
            </div>
            <div className={styles.row}>
              <label className={styles.label}>
                <input
                  type="checkbox"
                  checked={editRecommended}
                  onChange={e => setEditRecommended(e.target.checked)}
                  style={{ marginRight: '0.35rem' }}
                />
                관리자 추천
              </label>
              <button type="button" className={styles.primaryButton} onClick={() => void handleApplyRecommendation()} disabled={savingFlags}>
                추천 플래그 저장
              </button>
            </div>
            <div className={styles.row}>
              <span className={styles.label}>추천 점수</span>
              <input className={styles.input} value={editScore} onChange={e => setEditScore(e.target.value)} inputMode="numeric" />
              <button type="button" className={styles.secondaryButton} onClick={() => void handleApplyScore()} disabled={savingFlags}>
                점수 저장
              </button>
            </div>
          </section>
        </div>
      </div>
    </AdminMarketingShell>
  );
}

export default AdminProductRecommendationsPage;
