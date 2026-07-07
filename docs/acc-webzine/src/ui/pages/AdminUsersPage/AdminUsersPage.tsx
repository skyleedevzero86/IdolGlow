import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useAuth } from '../../../auth/AuthContext';
import {
  fetchAdminUserPage,
  type AdminUserAccountStatus,
  type AdminUserPageResponse,
  type AdminUserRole,
  type AdminUserSummary,
  unlockAdminUser,
  updateAdminUserRole,
  updateAdminUserStatus,
} from '../../../shared/data/adminUserApi';
import '../../../../../samples/portal-pagination.css';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import listStyles from './AdminUsersPage.module.css';
import shellToolbar from './AdminUsersShellToolbar.module.css';

const PAGE_SIZE = 10;
const PAGE_NUMBER_GROUP_SIZE = 5;

const EMPTY_PAGE: AdminUserPageResponse = {
  users: [],
  page: 1,
  size: PAGE_SIZE,
  totalElements: 0,
  totalPages: 1,
  hasNext: false,
  totalUsers: 0,
  adminCount: 0,
  suspendedCount: 0,
  withdrawnCount: 0,
};

const ROLE_OPTIONS: ReadonlyArray<{ value: AdminUserRole | ''; label: string }> = [
  { value: '', label: '전체 권한' },
  { value: 'USER', label: '일반회원' },
  { value: 'ADMIN', label: '관리자' },
];

const STATUS_OPTIONS: ReadonlyArray<{ value: AdminUserAccountStatus | ''; label: string }> = [
  { value: '', label: '전체 상태' },
  { value: 'APPROVED', label: '사용' },
  { value: 'SUSPENDED', label: '정지' },
  { value: 'WITHDRAWN', label: '탈퇴' },
];

const STATUS_ACTION_OPTIONS = STATUS_OPTIONS.filter(option => option.value);

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

const normalizePageData = (response: AdminUserPageResponse): AdminUserPageResponse => {
  const totalPages = Math.max(1, response.totalPages ?? 1);
  const page = Math.min(Math.max(1, response.page || 1), totalPages);
  return { ...response, page, totalPages };
};

/** SNS 연동 표시: 제공자 코드만 (예: GOOGLE) */
const formatSnsProviders = (user: AdminUserSummary): string | null => {
  if (!user.oauthLinked || user.oauthProviders.length === 0) {
    return null;
  }

  return user.oauthProviders.join(', ');
};

const TABLE_COL_COUNT = 9;

const UsersToolbarStatCount = ({ n }: { readonly n: number }) => (
  <>
    <span className={shellToolbar.statNumber}>{n}</span>
    <span className={shellToolbar.statUnit}>건</span>
  </>
);

export function AdminUsersPage() {
  const { accessToken, authReady, user } = useAuth();
  const [pageData, setPageData] = useState<AdminUserPageResponse>(EMPTY_PAGE);
  const [loading, setLoading] = useState(false);
  const [actingUserId, setActingUserId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [keywordInput, setKeywordInput] = useState('');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [roleFilter, setRoleFilter] = useState<AdminUserRole | ''>('');
  const [statusFilter, setStatusFilter] = useState<AdminUserAccountStatus | ''>('');

  const loadUsers = async ({
    page = currentPage,
    keyword = searchKeyword,
  }: {
    readonly page?: number;
    readonly keyword?: string;
  } = {}) => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetchAdminUserPage(accessToken, {
        page,
        size: PAGE_SIZE,
        keyword,
        role: roleFilter,
        accountStatus: statusFilter,
      });
      const normalized = normalizePageData(response);
      setPageData(normalized);
      if (normalized.page !== page) {
        setCurrentPage(normalized.page);
      }
      return normalized;
    } catch (loadError) {
      setError(
        loadError instanceof Error ? loadError.message : '회원 목록을 불러오지 못했습니다.'
      );
      return null;
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!authReady || user?.role !== 'ADMIN') {
      return;
    }

    void loadUsers({ page: currentPage, keyword: searchKeyword });
  }, [accessToken, authReady, currentPage, roleFilter, searchKeyword, statusFilter, user?.role]);

  useEffect(() => {
    if (currentPage > pageData.totalPages) {
      setCurrentPage(pageData.totalPages);
    }
  }, [currentPage, pageData.totalPages]);

  const currentGroupStart =
    Math.floor((currentPage - 1) / PAGE_NUMBER_GROUP_SIZE) * PAGE_NUMBER_GROUP_SIZE + 1;
  const currentGroupEnd = Math.min(
    currentGroupStart + PAGE_NUMBER_GROUP_SIZE - 1,
    Math.max(1, pageData.totalPages)
  );
  const visiblePageNumbers = Array.from(
    { length: currentGroupEnd - currentGroupStart + 1 },
    (_, index) => currentGroupStart + index
  );

  const listPage = pageData.page ?? currentPage;
  const pageStartRow =
    pageData.totalElements === 0 ? 0 : (listPage - 1) * pageData.size + 1;
  const pageEndRow =
    pageData.totalElements === 0
      ? 0
      : Math.min(listPage * pageData.size, pageData.totalElements);

  const lockedCountOnPage = useMemo(
    () => pageData.users.filter(candidate => candidate.locked).length,
    [pageData.users]
  );

  const handleSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setCurrentPage(1);
    setSearchKeyword(keywordInput.trim());
  };

  const handleResetFilters = () => {
    setKeywordInput('');
    setSearchKeyword('');
    setRoleFilter('');
    setStatusFilter('');
    setCurrentPage(1);
    setError(null);
    setMessage(null);
  };

  const refreshCurrentPage = async () => {
    await loadUsers({ page: currentPage, keyword: searchKeyword });
  };

  const handleRoleChange = async (candidate: AdminUserSummary, role: AdminUserRole) => {
    if (candidate.role === role) {
      return;
    }

    setActingUserId(candidate.id);
    setError(null);
    setMessage(null);

    try {
      const updated = await updateAdminUserRole(accessToken, candidate.id, role);
      setMessage(`${updated.email} 사용자의 권한을 변경했습니다.`);
      await refreshCurrentPage();
    } catch (changeError) {
      setError(changeError instanceof Error ? changeError.message : '권한 변경에 실패했습니다.');
    } finally {
      setActingUserId(null);
    }
  };

  const handleStatusChange = async (
    candidate: AdminUserSummary,
    accountStatus: AdminUserAccountStatus
  ) => {
    if (candidate.accountStatus === accountStatus) {
      return;
    }

    setActingUserId(candidate.id);
    setError(null);
    setMessage(null);

    try {
      const updated = await updateAdminUserStatus(accessToken, candidate.id, accountStatus);
      setMessage(`${updated.email} 사용자의 상태를 변경했습니다.`);
      await refreshCurrentPage();
    } catch (changeError) {
      setError(changeError instanceof Error ? changeError.message : '상태 변경에 실패했습니다.');
    } finally {
      setActingUserId(null);
    }
  };

  const handleUnlock = async (candidate: AdminUserSummary) => {
    setActingUserId(candidate.id);
    setError(null);
    setMessage(null);

    try {
      const updated = await unlockAdminUser(accessToken, candidate.id);
      setMessage(`${updated.email} 사용자의 잠금을 해제했습니다.`);
      await refreshCurrentPage();
    } catch (unlockError) {
      setError(unlockError instanceof Error ? unlockError.message : '잠금 해제에 실패했습니다.');
    } finally {
      setActingUserId(null);
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
        <div className={shellStyles.denied}>관리자만 회원 관리 화면을 사용할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/users"
      title="회원관리"
      description=""
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
      statusText={
        error
          ? error
          : loading
            ? '회원 목록을 불러오는 중입니다.'
            : message
              ? message
              : null
      }
      stats={[
        { label: '전체 회원', value: <UsersToolbarStatCount n={pageData.totalUsers} /> },
        { label: '관리자 수', value: <UsersToolbarStatCount n={pageData.adminCount} /> },
        {
          label: '잠금 계정',
          value: <UsersToolbarStatCount n={Math.max(pageData.suspendedCount, lockedCountOnPage)} />,
        },
        { label: '탈퇴 계정', value: <UsersToolbarStatCount n={pageData.withdrawnCount} /> },
      ]}
    >
      <div className={listStyles.section}>
        {error ? <div className={listStyles.inlineError}>{error}</div> : null}

        <div className={listStyles.headRow}>
          <h2 className={listStyles.title}>
            회원 목록{' '}
            <span className={listStyles.count}>({pageData.totalElements}건)</span>
          </h2>
          <form className={listStyles.searchForm} onSubmit={handleSearchSubmit}>
            <div className={listStyles.searchAttach}>
              <input
                type="search"
                className={listStyles.searchInput}
                value={keywordInput}
                onChange={event => setKeywordInput(event.target.value)}
                placeholder="이메일 또는 닉네임 검색"
                aria-label="검색어"
              />
              <button type="submit" className={listStyles.searchBtn} disabled={loading}>
                검색
              </button>
            </div>
            <select
              className={listStyles.filterSelect}
              value={roleFilter}
              onChange={event => {
                setCurrentPage(1);
                setRoleFilter(event.target.value as AdminUserRole | '');
              }}
              aria-label="권한 필터"
            >
              {ROLE_OPTIONS.map(option => (
                <option key={option.label} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <select
              className={listStyles.filterSelect}
              value={statusFilter}
              onChange={event => {
                setCurrentPage(1);
                setStatusFilter(event.target.value as AdminUserAccountStatus | '');
              }}
              aria-label="계정 상태 필터"
            >
              {STATUS_OPTIONS.map(option => (
                <option key={option.label} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <button
              type="button"
              className={listStyles.textBtn}
              onClick={handleResetFilters}
              disabled={loading}
            >
              초기화
            </button>
            <button
              type="button"
              className={listStyles.textBtn}
              onClick={() => void refreshCurrentPage()}
              disabled={loading}
            >
              새로고침
            </button>
          </form>
        </div>

        <hr className={listStyles.rule} />

        <div className={listStyles.tableScroll}>
          <table className={listStyles.table}>
            <thead className={listStyles.thead}>
              <tr>
                <th className={listStyles.th}>번호</th>
                <th className={listStyles.th}>이메일</th>
                <th className={listStyles.th}>닉네임</th>
                <th className={listStyles.th}>권한</th>
                <th className={listStyles.th}>계정상태</th>
                <th className={listStyles.th}>SNS 사용</th>
                <th className={listStyles.th}>마지막 로그인</th>
                <th className={listStyles.th}>잠금</th>
                <th className={listStyles.th}>관리</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td className={listStyles.loadingCell} colSpan={TABLE_COL_COUNT}>
                    목록을 불러오는 중입니다.
                  </td>
                </tr>
              ) : pageData.users.length === 0 ? (
                <tr>
                  <td className={listStyles.emptyCell} colSpan={TABLE_COL_COUNT}>
                    내역이 없습니다.
                  </td>
                </tr>
              ) : (
                pageData.users.map((candidate, index) => {
                  const rowNo = (listPage - 1) * pageData.size + index + 1;
                  return (
                    <tr key={candidate.id}>
                      <td className={listStyles.td}>{rowNo}</td>
                      <td className={`${listStyles.td} ${listStyles.tdLeft}`}>{candidate.email}</td>
                      <td className={listStyles.td}>{candidate.nickname}</td>
                      <td className={listStyles.td}>{candidate.roleLabel}</td>
                      <td className={listStyles.td}>{candidate.accountStatusLabel}</td>
                      <td className={listStyles.td}>{formatSnsProviders(candidate) ?? '-'}</td>
                      <td className={listStyles.td}>{candidate.lastLoginAt ?? '-'}</td>
                      <td className={listStyles.td}>{candidate.locked ? '잠금' : '-'}</td>
                      <td className={`${listStyles.td} ${listStyles.tdActions}`}>
                        <div className={listStyles.actionRow}>
                          <select
                            className={listStyles.actionSelect}
                            value={candidate.role}
                            disabled={actingUserId === candidate.id}
                            onChange={event =>
                              void handleRoleChange(candidate, event.target.value as AdminUserRole)
                            }
                            aria-label={`${candidate.email} 권한 변경`}
                          >
                            {ROLE_OPTIONS.filter(option => option.value).map(option => (
                              <option key={`${candidate.id}-${option.value}`} value={option.value}>
                                {option.label}
                              </option>
                            ))}
                          </select>
                          <select
                            className={listStyles.actionSelect}
                            value={candidate.accountStatus}
                            disabled={actingUserId === candidate.id}
                            onChange={event =>
                              void handleStatusChange(
                                candidate,
                                event.target.value as AdminUserAccountStatus
                              )
                            }
                            aria-label={`${candidate.email} 상태 변경`}
                          >
                            {STATUS_ACTION_OPTIONS.map(option => (
                              <option key={`${candidate.id}-${option.value}`} value={option.value}>
                                {option.label}
                              </option>
                            ))}
                          </select>
                          <button
                            type="button"
                            className={listStyles.unlockBtn}
                            disabled={actingUserId === candidate.id || !candidate.locked}
                            onClick={() => void handleUnlock(candidate)}
                          >
                            잠금 해제
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {!loading && pageData.totalElements > 0 ? (
          <div className={listStyles.paginationWrap}>
            <p className={listStyles.paginationInfo} aria-live="polite">
              전체 <strong>{pageData.totalElements}</strong>건 · 페이지당{' '}
              <strong>{pageData.size}</strong>건 ·{' '}
              <strong>
                {pageStartRow}-{pageEndRow}
              </strong>
              번째 표시 · <strong>{currentPage}</strong> / <strong>{pageData.totalPages}</strong> 페이지
            </p>
            <div className={listStyles.pagination}>
              <button
                type="button"
                className={listStyles.pageArrowButton}
                onClick={() => setCurrentPage(1)}
                disabled={currentPage <= 1}
                aria-label="첫 페이지"
                title="첫 페이지"
              >
                «
              </button>
              <button
                type="button"
                className={listStyles.pageArrowButton}
                onClick={() => setCurrentPage(previous => Math.max(1, previous - 1))}
                disabled={currentPage <= 1}
                aria-label="이전 페이지"
                title="이전 페이지"
              >
                <ArrowIcon direction="left" />
              </button>

              <div className={listStyles.paginationNumbers}>
                {visiblePageNumbers.map(pageNumber => (
                  <button
                    key={pageNumber}
                    type="button"
                    className={[
                      listStyles.pageButton,
                      pageNumber === currentPage ? listStyles.pageButtonActive : '',
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
                className={listStyles.pageArrowButton}
                onClick={() =>
                  setCurrentPage(previous =>
                    Math.min(previous + 1, Math.max(1, pageData.totalPages))
                  )
                }
                disabled={currentPage >= pageData.totalPages}
                aria-label="다음 페이지"
                title="다음 페이지"
              >
                <ArrowIcon direction="right" />
              </button>
              <button
                type="button"
                className={listStyles.pageArrowButton}
                onClick={() => setCurrentPage(pageData.totalPages)}
                disabled={currentPage >= pageData.totalPages}
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

export default AdminUsersPage;
