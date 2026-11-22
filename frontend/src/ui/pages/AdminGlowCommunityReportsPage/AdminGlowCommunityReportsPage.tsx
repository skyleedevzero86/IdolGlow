import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import { apiErrorMessage } from "../../../shared/data/apiClient";
import {
  GLOW_COMMUNITY_TAG_LABELS,
  type GlowCommunityReportedPost,
  type GlowCommunityTag,
  blindGlowCommunityPost,
  fetchGlowCommunityReports,
  glowCommunityPostPath,
  unblindGlowCommunityPost,
} from "../../../shared/data/glowCommunityApi";
import { formatIdolGlowDateTime } from "../../../shared/format/idolGlowDateTime";
import { SimplePager } from "../../components/SimplePager/SimplePager";
import { AdminMarketingShell } from "../AdminMarketingPage/AdminMarketingShell";
import styles from "./AdminGlowCommunityReportsPage.module.css";

const PAGE_SIZE = 20;

const tagLabel = (tag: string): string =>
  GLOW_COMMUNITY_TAG_LABELS[tag as GlowCommunityTag] ?? tag;

export function AdminGlowCommunityReportsPage() {
  const { accessToken } = useAuth();
  const [items, setItems] = useState<readonly GlowCommunityReportedPost[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [expanded, setExpanded] = useState<ReadonlySet<number>>(new Set());

  const load = useCallback(async () => {
    if (!accessToken) return;
    setLoading(true);
    setError(null);
    try {
      const response = await fetchGlowCommunityReports(
        accessToken,
        page + 1,
        PAGE_SIZE,
      );
      setItems(response.items);
      setTotalPages(Math.max(1, response.totalPages));
      setTotalElements(response.totalElements);
    } catch (loadError) {
      setItems([]);
      setError(
        apiErrorMessage(loadError, "신고 게시물을 불러오지 못했습니다."),
      );
    } finally {
      setLoading(false);
    }
  }, [accessToken, page]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!message) return;
    const timer = window.setTimeout(() => setMessage(null), 2600);
    return () => window.clearTimeout(timer);
  }, [message]);

  const replaceItem = (updated: GlowCommunityReportedPost) => {
    setItems((current) =>
      current.map((item) =>
        item.postId === updated.postId
          ? {
              ...updated,
              reports:
                updated.reports.length > 0 ? updated.reports : item.reports,
            }
          : item,
      ),
    );
  };

  const handleBlind = async (item: GlowCommunityReportedPost) => {
    if (!accessToken) return;
    const reason = window.prompt(
      `「${item.title}」 게시글을 블라인드합니다. 사유를 입력해 주세요. (비우면 기본 문구)`,
      "운영 정책 위반",
    );
    if (reason === null) return;
    setBusyId(item.postId);
    try {
      replaceItem(
        await blindGlowCommunityPost(
          item.postId,
          accessToken,
          reason.slice(0, 100),
        ),
      );
      setMessage("게시글을 블라인드했습니다.");
    } catch (actionError) {
      setError(apiErrorMessage(actionError, "블라인드 처리하지 못했습니다."));
    } finally {
      setBusyId(null);
    }
  };

  const handleUnblind = async (item: GlowCommunityReportedPost) => {
    if (!accessToken) return;
    if (!window.confirm(`「${item.title}」 게시글의 블라인드를 해제할까요?`))
      return;
    setBusyId(item.postId);
    try {
      replaceItem(await unblindGlowCommunityPost(item.postId, accessToken));
      setMessage("블라인드를 해제했습니다.");
    } catch (actionError) {
      setError(apiErrorMessage(actionError, "블라인드를 해제하지 못했습니다."));
    } finally {
      setBusyId(null);
    }
  };

  const toggleExpanded = (postId: number) => {
    setExpanded((current) => {
      const next = new Set(current);
      if (next.has(postId)) next.delete(postId);
      else next.add(postId);
      return next;
    });
  };

  const blindedCount = items.filter((item) => item.blinded).length;

  return (
    <AdminMarketingShell
      currentPath="/admin/glow-community/reports"
      title="신고 게시물"
      description="GLOW 지역사회에서 신고된 게시글을 확인하고 블라인드 처리하거나 해제합니다. 서로 다른 회원 3명이 신고하면 자동 블라인드됩니다."
      stats={[
        {
          label: "신고 게시글",
          value: loading ? "…" : totalElements.toLocaleString(),
        },
        {
          label: "이 페이지 블라인드",
          value: loading ? "…" : String(blindedCount),
        },
        {
          label: "이 페이지 노출 중",
          value: loading ? "…" : String(items.length - blindedCount),
        },
      ]}
    >
      <section className={styles.panel}>
        <header className={styles.panelHead}>
          <h2>신고 목록</h2>
          <button
            type="button"
            className={styles.ghostButton}
            onClick={() => void load()}
            disabled={loading}
          >
            새로고침
          </button>
        </header>
        {message ? (
          <p className={styles.message} aria-live="polite">
            {message}
          </p>
        ) : null}
        {error ? (
          <p className={styles.error} role="alert">
            {error}
          </p>
        ) : null}
        {loading && items.length === 0 ? (
          <p className={styles.status}>신고 게시물을 불러오는 중입니다.</p>
        ) : null}
        {!loading && !error && items.length === 0 ? (
          <p className={styles.status}>신고된 게시글이 없습니다.</p>
        ) : null}
        {items.length > 0 ? (
          <div className={styles.tableWrap}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th scope="col">게시글</th>
                  <th scope="col">작성자</th>
                  <th scope="col">신고</th>
                  <th scope="col">최근 신고</th>
                  <th scope="col">상태</th>
                  <th scope="col">처리</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr
                    key={item.postId}
                    className={item.blinded ? styles.blindedRow : undefined}
                  >
                    <td>
                      <div className={styles.postCell}>
                        <span className={styles.tag}>{tagLabel(item.tag)}</span>
                        <Link
                          to={glowCommunityPostPath(item.postId)}
                          className={styles.postLink}
                        >
                          {item.title}
                        </Link>
                        <span className={styles.subtle}>
                          #{item.postId} · 작성{" "}
                          {formatIdolGlowDateTime(item.createdAt)}
                        </span>
                        {item.reports.length > 0 ? (
                          <button
                            type="button"
                            className={styles.linkButton}
                            onClick={() => toggleExpanded(item.postId)}
                          >
                            {expanded.has(item.postId)
                              ? "신고 사유 접기"
                              : `신고 사유 ${item.reports.length}건 보기`}
                          </button>
                        ) : null}
                        {expanded.has(item.postId) ? (
                          <ul className={styles.reasons}>
                            {item.reports.map((report, index) => (
                              <li
                                key={`${item.postId}-${report.reportedAt ?? index}-${index}`}
                              >
                                <span>
                                  {report.reason?.trim() || "사유 없음"}
                                </span>
                                <time>
                                  {formatIdolGlowDateTime(report.reportedAt)}
                                </time>
                              </li>
                            ))}
                          </ul>
                        ) : null}
                      </div>
                    </td>
                    <td>
                      {item.authorName}
                      <span className={styles.subtle}>
                        회원 #{item.authorId}
                      </span>
                    </td>
                    <td className={styles.countCell}>
                      {item.reportCount.toLocaleString()}
                    </td>
                    <td>{formatIdolGlowDateTime(item.lastReportedAt)}</td>
                    <td>
                      {item.blinded ? (
                        <>
                          <span className={styles.blindBadge}>블라인드</span>
                          <span className={styles.subtle}>
                            {formatIdolGlowDateTime(item.blindedAt)}
                          </span>
                          {item.blindReason ? (
                            <span className={styles.subtle}>
                              {item.blindReason}
                            </span>
                          ) : null}
                        </>
                      ) : (
                        <span className={styles.openBadge}>노출 중</span>
                      )}
                    </td>
                    <td>
                      {item.blinded ? (
                        <button
                          type="button"
                          className={styles.ghostButton}
                          disabled={busyId === item.postId}
                          onClick={() => void handleUnblind(item)}
                        >
                          해제
                        </button>
                      ) : (
                        <button
                          type="button"
                          className={styles.dangerButton}
                          disabled={busyId === item.postId}
                          onClick={() => void handleBlind(item)}
                        >
                          블라인드
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <SimplePager
          page={page}
          totalPages={totalPages}
          onChange={setPage}
          label="신고 목록 페이지"
        />
      </section>
    </AdminMarketingShell>
  );
}

export default AdminGlowCommunityReportsPage;
