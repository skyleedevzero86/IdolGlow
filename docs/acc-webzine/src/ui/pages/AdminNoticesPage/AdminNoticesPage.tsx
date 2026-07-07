import {
  type FormEvent,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import type { AdminEventStatus } from "../../../shared/data/adminEventsApi";
import {
  type AdminNoticeSummary,
  fetchAdminNoticePage,
} from "../../../shared/data/adminNoticesApi";
import "../../../../../samples/bbs-notice-list.css";
import "../../../../../samples/portal-pagination.css";
import shellStyles from "../AdminMarketingPage/AdminMarketingPage.module.css";
import { AdminMarketingShell } from "../AdminMarketingPage/AdminMarketingShell";
import shellToolbar from "../AdminUsersPage/AdminUsersShellToolbar.module.css";
import styles from "./AdminNoticesPage.module.css";

const PAGE_SIZE = 10;
const PAGE_WINDOW = 10;

const STATUS_OPTIONS: ReadonlyArray<{
  readonly value: AdminEventStatus;
  readonly label: string;
}> = [
  { value: "all", label: "전체" },
  { value: "published", label: "게시" },
  { value: "draft", label: "임시저장" },
];

const ArrowIcon = ({ direction }: { readonly direction: "left" | "right" }) => (
  <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path
      d={direction === "left" ? "M15 6L9 12L15 18" : "M9 6L15 12L9 18"}
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

function formatBbsDate(iso: string): string {
  const d = new Date(iso);
  const yy = String(d.getFullYear()).slice(-2);
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yy}.${mm}.${dd}`;
}

function hasNoticeAttachment(introduction: string | null): boolean {
  if (!introduction) return false;
  return introduction.includes("[notice-files]");
}

function isPinnedNotice(tags: readonly string[]): boolean {
  return tags.some((tag) => tag.trim().toLowerCase() === "notice-pinned");
}

const AttachIcon = () => (
  <svg
    className={styles.attachIcon}
    viewBox="0 0 24 24"
    fill="none"
    aria-hidden="true"
  >
    <path
      d="M8 10V18C8 19.1046 8.89543 20 10 20H18C19.1046 20 20 19.1046 20 18V9C20 7.89543 19.1046 7 18 7H10"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
    />
    <path
      d="M16 4V13C16 14.1046 15.1046 15 14 15H6C4.89543 15 4 14.1046 4 13V6C4 4.89543 4.89543 4 6 4H13"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
    />
  </svg>
);

export function AdminNoticesPage() {
  const navigate = useNavigate();
  const { accessToken, authReady, user } = useAuth();
  const [items, setItems] = useState<readonly AdminNoticeSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [searchInput, setSearchInput] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<AdminEventStatus>("all");

  const loadList = useCallback(async () => {
    if (!authReady || user?.role !== "ADMIN") {
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const response = await fetchAdminNoticePage(accessToken, {
        page: currentPage,
        size: PAGE_SIZE,
        query: searchQuery,
        status: statusFilter,
      });
      setItems(response.items);
      setTotalElements(response.totalElements);
      setTotalPages(response.totalPages);
      setCurrentPage(response.page);
    } catch (loadError) {
      setError(
        loadError instanceof Error
          ? loadError.message
          : "공지사항 목록을 불러오지 못했습니다.",
      );
    } finally {
      setLoading(false);
    }
  }, [
    accessToken,
    authReady,
    currentPage,
    searchQuery,
    statusFilter,
    user?.role,
  ]);

  useEffect(() => {
    void loadList();
  }, [loadList]);

  const pageNumbers = useMemo(() => {
    const start = Math.floor((currentPage - 1) / PAGE_WINDOW) * PAGE_WINDOW + 1;
    const end = Math.min(totalPages, start + PAGE_WINDOW - 1);
    return Array.from({ length: end - start + 1 }, (_, index) => start + index);
  }, [currentPage, totalPages]);

  const pageStartRow =
    totalElements === 0 ? 0 : (currentPage - 1) * PAGE_SIZE + 1;
  const pageEndRow = Math.min(currentPage * PAGE_SIZE, totalElements);

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setCurrentPage(1);
    setSearchQuery(searchInput.trim());
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
      title="공지사항 관리"
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
      }}
      statusText={error ?? null}
      stats={[]}
    >
      <section className={styles.panel}>
        <form className={styles.panelBody} onSubmit={handleSearch}>
          <div className={styles.topBar}>
            <p className={styles.summary}>
              전체 <strong>{totalElements}</strong>건 |{" "}
              <strong>{currentPage}</strong> / <strong>{totalPages}</strong>
              page
            </p>
            <div className={styles.inlineSearch}>
              <input
                className={styles.searchInput}
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                placeholder="검색어를 입력해주세요."
                aria-label="검색어"
              />
              <button type="submit" className={styles.searchButtonBlack}>
                검색
              </button>
            </div>
          </div>

          <div className={styles.toolbarRow}>
            <select
              className={styles.select}
              value={statusFilter}
              onChange={(event) => {
                setCurrentPage(1);
                setStatusFilter(event.target.value as AdminEventStatus);
              }}
            >
              {STATUS_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={() => {
                setCurrentPage(1);
                setSearchInput("");
                setSearchQuery("");
                setStatusFilter("all");
              }}
            >
              초기화
            </button>
            <button
              type="button"
              className={styles.primaryButton}
              onClick={() => navigate("/admin/notices/new")}
            >
              공지 등록
            </button>
          </div>
        </form>

        {loading ? (
          <div className={styles.empty}>공지사항 목록을 불러오는 중입니다.</div>
        ) : items.length === 0 ? (
          <div className={styles.empty}>등록된 공지사항이 없습니다.</div>
        ) : (
          <div className={styles.tableWrap}>
            <table className={styles.bbsTable}>
              <thead>
                <tr>
                  <th className={styles.colNo} scope="col">
                    번호
                  </th>
                  <th className={styles.colTitle} scope="col">
                    제목
                  </th>
                  <th className={styles.colAttach} scope="col">
                    첨부
                  </th>
                  <th className={styles.colDate} scope="col">
                    작성일
                  </th>
                  <th className={styles.colViews} scope="col">
                    조회수
                  </th>
                </tr>
              </thead>
              <tbody>
                {items.map((item, index) => {
                  const rowNo =
                    totalElements - (currentPage - 1) * PAGE_SIZE - index;
                  const openEdit = () =>
                    navigate(`/admin/notices/${item.documentId}/edit`);
                  return (
                    <tr
                      key={item.documentId}
                      onClick={openEdit}
                      onKeyDown={(event) => {
                        if (event.key === "Enter" || event.key === " ") {
                          event.preventDefault();
                          openEdit();
                        }
                      }}
                      tabIndex={0}
                    >
                      <td className={styles.mutedCell}>
                        {isPinnedNotice(item.tags) ? (
                          <span className={styles.badgeNotice}>공지</span>
                        ) : (
                          rowNo
                        )}
                      </td>
                      <td className={styles.titleCell}>
                        <div className={styles.titleInner}>{item.title}</div>
                      </td>
                      <td className={styles.attachCell}>
                        {hasNoticeAttachment(item.introduction) ? (
                          <AttachIcon />
                        ) : null}
                      </td>
                      <td className={styles.mutedCell}>
                        {formatBbsDate(item.updatedAt)}
                      </td>
                      <td className={styles.mutedCell}>
                        {item.viewCount.toLocaleString("ko-KR")}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {!loading && totalElements > 0 ? (
          <div className={styles.paginationWrap}>
            <p className={styles.paginationInfo} aria-live="polite">
              전체 <strong>{totalElements}</strong>건 · 페이지당{" "}
              <strong>{PAGE_SIZE}</strong>건 ·{" "}
              <strong>
                {pageStartRow}-{pageEndRow}
              </strong>
              번째 표시 · <strong>{currentPage}</strong> /{" "}
              <strong>{totalPages}</strong> 페이지
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
                onClick={() =>
                  setCurrentPage((previous) => Math.max(1, previous - 1))
                }
                disabled={currentPage <= 1}
                aria-label="이전 페이지"
                title="이전 페이지"
              >
                <ArrowIcon direction="left" />
              </button>
              <div className={styles.paginationNumbers}>
                {pageNumbers.map((number) => (
                  <button
                    key={number}
                    type="button"
                    className={`${styles.pageButton} ${number === currentPage ? styles.pageButtonActive : ""}`}
                    onClick={() => setCurrentPage(number)}
                    aria-current={number === currentPage ? "page" : undefined}
                  >
                    {number}
                  </button>
                ))}
              </div>
              <button
                type="button"
                className={styles.pageArrowButton}
                onClick={() =>
                  setCurrentPage((previous) =>
                    Math.min(totalPages, previous + 1),
                  )
                }
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
      </section>
    </AdminMarketingShell>
  );
}

export default AdminNoticesPage;
