import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import { apiErrorMessage } from "../../../shared/data/apiClient";
import {
  COMMENT_MAX_DEPTH,
  type ContentComment,
  type ContentTargetType,
  createContentComment,
  deleteContentComment,
  fetchContentComments,
  updateContentComment,
} from "../../../shared/data/contentCommentApi";
import { formatIdolGlowDateTime } from "../../../shared/format/idolGlowDateTime";
import { SafeMarkdown } from "../SafeMarkdown/SafeMarkdown";
import { SimplePager } from "../SimplePager/SimplePager";
import { CommentComposer } from "./CommentComposer";
import styles from "./ContentComments.module.css";

const PAGE_SIZE = 10;
const MAX_HASH_SEARCH_PAGES = 20;
const COMMENT_HASH = /^#comment-(\d+)$/;

export type ContentCommentsProps = {
  readonly targetType: ContentTargetType;
  readonly targetKey: string;
  readonly targetTitle: string;
  readonly targetPath: string;
  readonly heading?: string;
  readonly className?: string;
};

const containsComment = (
  comments: readonly ContentComment[],
  commentId: number,
): boolean =>
  comments.some(
    (comment) =>
      comment.id === commentId || containsComment(comment.replies, commentId),
  );

const countReplies = (comment: ContentComment): number =>
  comment.replies.reduce((sum, reply) => sum + 1 + countReplies(reply), 0);

type CommentItemProps = {
  readonly comment: ContentComment;
  readonly canModerate: boolean;
  readonly accessToken: string | null;
  readonly highlightId: number | null;
  readonly replyingTo: number | null;
  readonly editingId: number | null;
  readonly onReply: (commentId: number | null) => void;
  readonly onEdit: (commentId: number | null) => void;
  readonly onSubmitReply: (parentId: number, markdown: string) => Promise<void>;
  readonly onSubmitEdit: (commentId: number, markdown: string) => Promise<void>;
  readonly onDelete: (comment: ContentComment) => void;
  readonly onRequireLogin: () => void;
};

function CommentItem({
  comment,
  canModerate,
  accessToken,
  highlightId,
  replyingTo,
  editingId,
  onReply,
  onEdit,
  onSubmitReply,
  onSubmitEdit,
  onDelete,
  onRequireLogin,
}: CommentItemProps) {
  const depth = Math.max(1, Math.min(COMMENT_MAX_DEPTH, comment.depth || 1));
  const editing = editingId === comment.id;
  const replying = replyingTo === comment.id;
  const edited =
    Boolean(comment.updatedAt && comment.createdAt) &&
    comment.updatedAt !== comment.createdAt;
  const classes = [
    styles.comment,
    depth > 1 ? styles.reply : "",
    highlightId === comment.id ? styles.highlighted : "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <li className={styles.commentNode} data-depth={depth}>
      <article id={`comment-${comment.id}`} className={classes}>
        {comment.deleted ? (
          <p className={styles.deleted}>삭제된 댓글입니다.</p>
        ) : (
          <>
            <header className={styles.commentHead}>
              <strong className={styles.author}>
                {comment.authorName || "알 수 없음"}
                {comment.mine ? (
                  <span className={styles.mineBadge}>내 댓글</span>
                ) : null}
              </strong>
              <time
                className={styles.time}
                dateTime={comment.createdAt ?? undefined}
              >
                {formatIdolGlowDateTime(comment.createdAt)}
                {edited ? " · 수정됨" : ""}
              </time>
            </header>
            {editing && accessToken ? (
              <CommentComposer
                accessToken={accessToken}
                initialValue={comment.contentMarkdown}
                label="댓글 수정"
                submitLabel="수정"
                onSubmit={(markdown) => onSubmitEdit(comment.id, markdown)}
                onCancel={() => onEdit(null)}
              />
            ) : (
              <SafeMarkdown markdown={comment.contentMarkdown} compact />
            )}
            {!editing ? (
              <div className={styles.commentActions}>
                {depth < COMMENT_MAX_DEPTH ? (
                  <button
                    type="button"
                    onClick={() => {
                      if (!accessToken) {
                        onRequireLogin();
                        return;
                      }
                      onReply(replying ? null : comment.id);
                    }}
                    aria-expanded={replying}
                  >
                    {replying ? "답글 취소" : "답글"}
                  </button>
                ) : null}
                {comment.mine ? (
                  <button type="button" onClick={() => onEdit(comment.id)}>
                    수정
                  </button>
                ) : null}
                {comment.mine || canModerate ? (
                  <button
                    type="button"
                    className={styles.dangerAction}
                    onClick={() => onDelete(comment)}
                  >
                    삭제
                  </button>
                ) : null}
              </div>
            ) : null}
          </>
        )}
      </article>
      {replying && accessToken ? (
        <div className={styles.replyComposer}>
          <CommentComposer
            accessToken={accessToken}
            label={`${comment.authorName || "댓글"}에게 답글`}
            placeholder="답글을 입력해 주세요. 마크다운과 이미지를 쓸 수 있어요."
            submitLabel="답글 등록"
            onSubmit={(markdown) => onSubmitReply(comment.id, markdown)}
            onCancel={() => onReply(null)}
          />
        </div>
      ) : null}
      {comment.replies.length > 0 ? (
        <ul className={styles.replies}>
          {comment.replies.map((reply) => (
            <CommentItem
              key={reply.id}
              comment={reply}
              canModerate={canModerate}
              accessToken={accessToken}
              highlightId={highlightId}
              replyingTo={replyingTo}
              editingId={editingId}
              onReply={onReply}
              onEdit={onEdit}
              onSubmitReply={onSubmitReply}
              onSubmitEdit={onSubmitEdit}
              onDelete={onDelete}
              onRequireLogin={onRequireLogin}
            />
          ))}
        </ul>
      ) : null}
    </li>
  );
}

export function ContentComments({
  targetType,
  targetKey,
  targetTitle,
  targetPath,
  heading = "댓글",
  className,
}: ContentCommentsProps) {
  const { accessToken, authReady, user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [comments, setComments] = useState<readonly ContentComment[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [replyingTo, setReplyingTo] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [highlightId, setHighlightId] = useState<number | null>(null);
  const [composerKey, setComposerKey] = useState(0);
  const [loadedKey, setLoadedKey] = useState("");
  const hashSearchRef = useRef<{ hash: string; tried: number; done: boolean }>({
    hash: "",
    tried: 0,
    done: false,
  });
  const sectionRef = useRef<HTMLElement>(null);

  const canModerate = user?.role === "ADMIN";
  const loggedIn = Boolean(authReady && user && accessToken);
  const target = useMemo(
    () => ({ targetType, targetKey }),
    [targetType, targetKey],
  );
  const requestKey = `${targetType}:${targetKey}:${page}:${reloadKey}:${accessToken ? 1 : 0}`;

  // biome-ignore lint/correctness/useExhaustiveDependencies: dependency is an intentional re-run trigger
  useEffect(() => {
    setPage(0);
    setReplyingTo(null);
    setEditingId(null);
  }, [targetType, targetKey]);

  useEffect(() => {
    if (!authReady || !targetKey) return;
    let cancelled = false;
    setLoading(true);
    setError(null);
    fetchContentComments(target, page, PAGE_SIZE, accessToken)
      .then((response) => {
        if (cancelled) return;
        setComments(response.items);
        setTotalPages(Math.max(1, response.totalPages));
        setTotalElements(response.totalElements);
        setLoadedKey(requestKey);
      })
      .catch((loadError: unknown) => {
        if (cancelled) return;
        setComments([]);
        setError(apiErrorMessage(loadError, "댓글을 불러오지 못했습니다."));
        setLoadedKey(requestKey);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, page, requestKey, target, targetKey]);

  useEffect(() => {
    const match = COMMENT_HASH.exec(location.hash);
    if (!match) return;
    if (hashSearchRef.current.hash !== location.hash) {
      hashSearchRef.current = { hash: location.hash, tried: 0, done: false };
    }
    const state = hashSearchRef.current;
    if (state.done || loadedKey !== requestKey || error) return;
    const commentId = Number(match[1]);
    if (containsComment(comments, commentId)) {
      state.done = true;
      setHighlightId(commentId);
      window.requestAnimationFrame(() => {
        document
          .getElementById(`comment-${commentId}`)
          ?.scrollIntoView({ behavior: "smooth", block: "center" });
      });
      return;
    }
    if (page + 1 < totalPages && state.tried < MAX_HASH_SEARCH_PAGES) {
      state.tried += 1;
      setPage(page + 1);
      return;
    }
    state.done = true;
    setNotice("해당 댓글을 찾을 수 없습니다. 삭제되었을 수 있습니다.");
  }, [comments, error, loadedKey, location.hash, page, requestKey, totalPages]);

  useEffect(() => {
    if (!notice) return;
    const timer = window.setTimeout(() => setNotice(null), 3000);
    return () => window.clearTimeout(timer);
  }, [notice]);

  const requireLogin = useCallback(() => {
    const redirect = encodeURIComponent(
      `${location.pathname}${location.search}${location.hash}`,
    );
    navigate(`/?login=1&redirect=${redirect}`);
  }, [location.hash, location.pathname, location.search, navigate]);

  const reload = () => setReloadKey((value) => value + 1);

  const submitTopLevel = async (markdown: string) => {
    if (!accessToken) throw new Error("로그인이 필요합니다.");
    const created = await createContentComment(
      {
        targetType,
        targetKey,
        targetTitle,
        targetPath,
        contentMarkdown: markdown,
      },
      accessToken,
    );
    setComposerKey((value) => value + 1);
    const lastPage = Math.max(
      0,
      Math.ceil((totalElements + 1) / PAGE_SIZE) - 1,
    );
    hashSearchRef.current = {
      hash: `#comment-${created.id}`,
      tried: 0,
      done: false,
    };
    navigate(
      {
        pathname: location.pathname,
        search: location.search,
        hash: `comment-${created.id}`,
      },
      { replace: true },
    );
    if (lastPage !== page) setPage(lastPage);
    else reload();
  };

  const submitReply = async (parentId: number, markdown: string) => {
    if (!accessToken) throw new Error("로그인이 필요합니다.");
    const created = await createContentComment(
      {
        targetType,
        targetKey,
        targetTitle,
        targetPath,
        contentMarkdown: markdown,
        parentId,
      },
      accessToken,
    );
    setReplyingTo(null);
    hashSearchRef.current = {
      hash: `#comment-${created.id}`,
      tried: 0,
      done: false,
    };
    navigate(
      {
        pathname: location.pathname,
        search: location.search,
        hash: `comment-${created.id}`,
      },
      { replace: true },
    );
    reload();
  };

  const submitEdit = async (commentId: number, markdown: string) => {
    if (!accessToken) throw new Error("로그인이 필요합니다.");
    await updateContentComment(commentId, markdown, accessToken);
    setEditingId(null);
    reload();
  };

  const handleDelete = async (comment: ContentComment) => {
    if (!accessToken) return;
    const replyCount = countReplies(comment);
    const message =
      replyCount > 0
        ? "이 댓글을 삭제할까요? 답글이 있어 '삭제된 댓글입니다'로 표시됩니다."
        : "이 댓글을 삭제할까요?";
    if (!window.confirm(message)) return;
    try {
      await deleteContentComment(comment.id, accessToken);
      setNotice("댓글을 삭제했습니다.");
      reload();
    } catch (deleteError) {
      setNotice(apiErrorMessage(deleteError, "댓글을 삭제하지 못했습니다."));
    }
  };

  const changePage = (next: number) => {
    setPage(next);
    sectionRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  return (
    <section
      ref={sectionRef}
      className={[styles.section, className].filter(Boolean).join(" ")}
      aria-labelledby={`comments-${targetType}-${targetKey}`}
    >
      <header className={styles.sectionHead}>
        <h2
          id={`comments-${targetType}-${targetKey}`}
          className={styles.heading}
        >
          {heading}
          <span className={styles.count}>{totalElements.toLocaleString()}</span>
        </h2>
      </header>

      {loggedIn && accessToken ? (
        <CommentComposer
          key={composerKey}
          accessToken={accessToken}
          label="댓글 쓰기"
          placeholder="댓글을 입력해 주세요. 마크다운과 이미지를 쓸 수 있어요."
          submitLabel="댓글 등록"
          onSubmit={submitTopLevel}
        />
      ) : authReady ? (
        <div className={styles.loginPrompt}>
          <p>댓글을 쓰려면 로그인해 주세요.</p>
          <button
            type="button"
            className={styles.primaryButton}
            onClick={requireLogin}
          >
            로그인
          </button>
        </div>
      ) : null}

      {notice ? (
        <p className={styles.notice} aria-live="polite">
          {notice}
        </p>
      ) : null}
      {error ? (
        <p className={styles.formError} role="alert">
          {error}
        </p>
      ) : null}

      {loading && comments.length === 0 ? (
        <p className={styles.status}>댓글을 불러오는 중입니다.</p>
      ) : null}
      {!loading && !error && comments.length === 0 ? (
        <p className={styles.status}>
          아직 댓글이 없습니다. 첫 댓글을 남겨 보세요.
        </p>
      ) : null}

      {comments.length > 0 ? (
        <ul className={styles.list} aria-busy={loading}>
          {comments.map((comment) => (
            <CommentItem
              key={comment.id}
              comment={comment}
              canModerate={canModerate}
              accessToken={loggedIn ? accessToken : null}
              highlightId={highlightId}
              replyingTo={replyingTo}
              editingId={editingId}
              onReply={(commentId) => {
                setEditingId(null);
                setReplyingTo(commentId);
              }}
              onEdit={(commentId) => {
                setReplyingTo(null);
                setEditingId(commentId);
              }}
              onSubmitReply={submitReply}
              onSubmitEdit={submitEdit}
              onDelete={(target) => void handleDelete(target)}
              onRequireLogin={requireLogin}
            />
          ))}
        </ul>
      ) : null}

      <SimplePager
        page={page}
        totalPages={totalPages}
        onChange={changePage}
        label="댓글 페이지"
      />
    </section>
  );
}

export default ContentComments;
