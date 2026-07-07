import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useAuth } from '../../../auth/AuthContext';
import {
  fetchAdminAuthVerificationPage,
  type AdminAuthVerificationLog,
  type AdminAuthVerificationType,
} from '../../../shared/data/adminAuthVerificationApi';
import '../../../../../samples/portal-pagination.css';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from '../AdminUsersPage/AdminUsersPage.module.css';

const PAGE_SIZE = 10;
const PAGE_NUMBER_GROUP_SIZE = 5;

const TYPE_OPTIONS: ReadonlyArray<{ readonly value: AdminAuthVerificationType; readonly label: string }> = [
  { value: '', label: '전체 유형' },
  { value: 'SIGNUP_EMAIL_CHECK', label: '회원가입 이메일 확인' },
  { value: 'ACCOUNT_RECOVERY_INITIATE', label: '아이디/이메일 찾기 인증' },
  { value: 'ACCOUNT_ID_FIND', label: '아이디 찾기 메일 발송' },
  { value: 'SIGNUP_EMAIL_VERIFICATION_REQUEST', label: '이메일 인증 메일 발송' },
  { value: 'SIGNUP_EMAIL_VERIFICATION_CONFIRM', label: '이메일 인증 완료' },
  { value: 'SIGNUP_ACCOUNT_CONFIRM_REQUEST', label: '가입 후 계정 확인 메일 발송' },
  { value: 'SIGNUP_ACCOUNT_CONFIRM_RESULT', label: '가입 후 계정 확인 결과' },
  { value: 'PASSWORD_TEMP_ISSUED', label: '임시 비밀번호 발급' },
  { value: 'PASSWORD_CHANGED', label: '비밀번호 변경 완료' },
];

const ArrowIcon = ({ direction }: { readonly direction: 'left' | 'right' }) => (
  <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path
      d={direction === 'left' ? 'M15 6L9 12L15 18' : 'M9 6L15 12L9 18'}
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

const TABLE_COL_COUNT = 8;

const mapTypeLabel = (type: string) => {
  if (type === 'SIGNUP_EMAIL_CHECK') return '회원가입 이메일 확인';
  if (type === 'ACCOUNT_RECOVERY_INITIATE') return '아이디/이메일 찾기 인증';
  if (type === 'ACCOUNT_ID_FIND') return '아이디 찾기 메일 발송';
  if (type === 'SIGNUP_EMAIL_VERIFICATION_REQUEST') return '이메일 인증 메일 발송';
  if (type === 'SIGNUP_EMAIL_VERIFICATION_CONFIRM') return '이메일 인증 완료';
  if (type === 'SIGNUP_ACCOUNT_CONFIRM_REQUEST') return '가입 후 계정 확인 메일 발송';
  if (type === 'SIGNUP_ACCOUNT_CONFIRM_RESULT') return '가입 후 계정 확인 결과';
  if (type === 'PASSWORD_TEMP_ISSUED') return '임시 비밀번호 발급';
  if (type === 'PASSWORD_CHANGED') return '비밀번호 변경 완료';
  return type;
};

export function AdminAuthVerificationPage() {
  const { accessToken, authReady, user } = useAuth();
  const [logs, setLogs] = useState<readonly AdminAuthVerificationLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [keywordInput, setKeywordInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [typeFilter, setTypeFilter] = useState<AdminAuthVerificationType>('');

  useEffect(() => {
    if (!authReady || user?.role !== 'ADMIN') return;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const page = await fetchAdminAuthVerificationPage(accessToken, {
          page: currentPage,
          size: PAGE_SIZE,
          verificationType: typeFilter,
          keyword,
        });
        setLogs(page.logs);
        setTotalElements(page.totalElements);
        setTotalPages(Math.max(1, page.totalPages));
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : '인증 로그를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [accessToken, authReady, currentPage, keyword, typeFilter, user?.role]);

  const visiblePageNumbers = useMemo(() => {
    const currentGroupStart =
      Math.floor((currentPage - 1) / PAGE_NUMBER_GROUP_SIZE) * PAGE_NUMBER_GROUP_SIZE + 1;
    const currentGroupEnd = Math.min(
      currentGroupStart + PAGE_NUMBER_GROUP_SIZE - 1,
      Math.max(1, totalPages)
    );
    return Array.from(
      { length: currentGroupEnd - currentGroupStart + 1 },
      (_, index) => currentGroupStart + index
    );
  }, [currentPage, totalPages]);

  const pageStartRow = totalElements === 0 ? 0 : (currentPage - 1) * PAGE_SIZE + 1;
  const pageEndRow = totalElements === 0 ? 0 : Math.min(currentPage * PAGE_SIZE, totalElements);

  const handleSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setCurrentPage(1);
    setKeyword(keywordInput.trim());
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
        <div className={shellStyles.denied}>관리자만 인증관리 화면을 사용할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/auth-verifications"
      title="인증관리"
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
      }}
      statusText={error}
      stats={[]}
    >
      <div className={styles.section}>
        <div className={styles.headRow}>
          <h2 className={styles.title}>
            인증 로그 <span className={styles.count}>({totalElements}건)</span>
          </h2>
          <form className={styles.searchForm} onSubmit={handleSearchSubmit}>
            <div className={styles.searchAttach}>
              <input
                type="search"
                className={styles.searchInput}
                value={keywordInput}
                onChange={event => setKeywordInput(event.target.value)}
                placeholder="이메일 또는 아이디 검색"
              />
              <button type="submit" className={styles.searchBtn} disabled={loading}>
                검색
              </button>
            </div>
            <select
              className={styles.filterSelect}
              value={typeFilter}
              onChange={event => {
                setCurrentPage(1);
                setTypeFilter(event.target.value as AdminAuthVerificationType);
              }}
            >
              {TYPE_OPTIONS.map(option => (
                <option key={option.label} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </form>
        </div>
        <hr className={styles.rule} />

        <div className={styles.tableScroll}>
          <table className={styles.table}>
            <thead className={styles.thead}>
              <tr>
                <th className={styles.th}>번호</th>
                <th className={styles.th}>인증유형</th>
                <th className={styles.th}>이메일</th>
                <th className={styles.th}>아이디</th>
                <th className={styles.th}>결과</th>
                <th className={styles.th}>IP</th>
                <th className={styles.th}>시간</th>
                <th className={styles.th}>상세</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td className={styles.loadingCell} colSpan={TABLE_COL_COUNT}>
                    인증 로그를 불러오는 중입니다.
                  </td>
                </tr>
              ) : logs.length === 0 ? (
                <tr>
                  <td className={styles.emptyCell} colSpan={TABLE_COL_COUNT}>
                    내역이 없습니다.
                  </td>
                </tr>
              ) : (
                logs.map((log, index) => (
                  <tr key={log.id}>
                    <td className={styles.td}>{(currentPage - 1) * PAGE_SIZE + index + 1}</td>
                    <td className={styles.td}>{mapTypeLabel(log.verificationType)}</td>
                    <td className={`${styles.td} ${styles.tdLeft}`}>{log.email ?? '-'}</td>
                    <td className={styles.td}>{log.username ?? '-'}</td>
                    <td className={styles.td}>{log.success ? '성공' : '실패'}</td>
                    <td className={styles.td}>{log.ipAddress}</td>
                    <td className={styles.td}>{new Date(log.createdAt).toLocaleString('ko-KR')}</td>
                    <td className={`${styles.td} ${styles.tdLeft}`}>{log.detail ?? '-'}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {!loading && totalElements > 0 ? (
          <div className={styles.paginationWrap}>
            <p className={styles.paginationInfo}>
              전체 <strong>{totalElements}</strong>건 · 페이지당 <strong>{PAGE_SIZE}</strong>건 ·{' '}
              <strong>
                {pageStartRow}-{pageEndRow}
              </strong>
              번째 표시 · <strong>{currentPage}</strong> / <strong>{totalPages}</strong> 페이지
            </p>
            <div className={styles.pagination}>
              <button
                type="button"
                className={styles.pageArrowButton}
                onClick={() => setCurrentPage(1)}
                disabled={currentPage <= 1}
                aria-label="첫 페이지"
                title="첫 페이지"
              >
                «
              </button>
              <button
                type="button"
                className={styles.pageArrowButton}
                onClick={() => setCurrentPage(previous => Math.max(1, previous - 1))}
                disabled={currentPage <= 1}
                aria-label="이전 페이지"
                title="이전 페이지"
              >
                <ArrowIcon direction="left" />
              </button>
              <div className={styles.paginationNumbers}>
                {visiblePageNumbers.map(pageNumber => (
                  <button
                    key={pageNumber}
                    type="button"
                    className={[
                      styles.pageButton,
                      pageNumber === currentPage ? styles.pageButtonActive : '',
                    ]
                      .filter(Boolean)
                      .join(' ')}
                    onClick={() => setCurrentPage(pageNumber)}
                    aria-current={pageNumber === currentPage ? 'page' : undefined}
                  >
                    {pageNumber}
                  </button>
                ))}
              </div>
              <button
                type="button"
                className={styles.pageArrowButton}
                onClick={() => setCurrentPage(previous => Math.min(previous + 1, Math.max(1, totalPages)))}
                disabled={currentPage >= totalPages}
                aria-label="다음 페이지"
                title="다음 페이지"
              >
                <ArrowIcon direction="right" />
              </button>
              <button
                type="button"
                className={styles.pageArrowButton}
                onClick={() => setCurrentPage(totalPages)}
                disabled={currentPage >= totalPages}
                aria-label="마지막 페이지"
                title="마지막 페이지"
              >
                »
              </button>
            </div>
          </div>
        ) : null}
      </div>
    </AdminMarketingShell>
  );
}

export default AdminAuthVerificationPage;
