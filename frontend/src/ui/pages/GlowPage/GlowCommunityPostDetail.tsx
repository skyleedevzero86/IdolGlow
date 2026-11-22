import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../../../auth/AuthContext";
import { ApiError, apiErrorMessage } from "../../../shared/data/apiClient";
import {
  type GlowCommunityPost,
  deleteGlowCommunityPost,
  fetchGlowCommunityPost,
  glowCommunityPostPath,
  glowCommunityTagLabel,
  reportGlowCommunityPost,
  toggleGlowCommunityLike,
} from "../../../shared/data/glowCommunityApi";
import { firstMarkdownImage } from "../../../shared/data/recentViewsStore";
import { formatIdolGlowDateTime } from "../../../shared/format/idolGlowDateTime";
import { ContentComments } from "../../components/ContentComments/ContentComments";
import { SafeMarkdown } from "../../components/SafeMarkdown/SafeMarkdown";
import { useRecordContentView } from "../../hooks/useContentView";
import styles from "./GlowCommunity.module.css";
import { GlowCommunityEditor } from "./GlowCommunityEditor";
import { GlowCommunityLoginPrompt } from "./GlowCommunityLoginPrompt";
import { CommentIcon, ThumbIcon } from "./GlowCommunityIcons";

type GlowCommunityPostDetailProps = {
  readonly postId: number;
  readonly onBack: () => void;
  readonly onDeleted: () => void;
};

export function GlowCommunityPostDetail({
  postId,
  onBack,
  onDeleted,
}: GlowCommunityPostDetailProps) {
  const { accessToken, authReady, user } = useAuth();
  const [post, setPost] = useState<GlowCommunityPost | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [loginMessage, setLoginMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!authReady) return;
    let cancelled = false;
    setLoading(true);
    setError(null);
    fetchGlowCommunityPost(postId, accessToken)
      .then((response) => {
        if (!cancelled) setPost(response);
      })
      .catch((loadError: unknown) => {
        if (cancelled) return;
        setPost(null);
        if (loadError instanceof ApiError && loadError.status === 404) {
          setError(
            "게시글을 찾을 수 없습니다. 삭제되었거나 블라인드된 글일 수 있습니다.",
          );
          return;
        }
        setError(apiErrorMessage(loadError, "게시글을 불러오지 못했습니다."));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, postId]);

  useEffect(() => {
    if (!notice) return;
    const timer = window.setTimeout(() => setNotice(null), 2400);
    return () => window.clearTimeout(timer);
  }, [notice]);

  const path = glowCommunityPostPath(postId);
  const viewEntry = useMemo(
    () =>
      post && !post.blinded
        ? {
            contentType: "GLOW_COMMUNITY_POST" as const,
            contentKey: String(post.id),
            title: post.title,
            path,
            imageUrl: firstMarkdownImage(post.content),
          }
        : null,
    [path, post],
  );
  useRecordContentView(viewEntry);

  const loggedIn = Boolean(user && accessToken);

  const handleLike = async () => {
    if (!post) return;
    if (!loggedIn || !accessToken) {
      setLoginMessage("좋아요를 누르려면 로그인해 주세요.");
      return;
    }
    try {
      const result = await toggleGlowCommunityLike(post.id, accessToken);
      setPost({
        ...post,
        liked: result.liked,
        likedByMe: result.liked,
        likeCount: result.likeCount,
      });
    } catch (likeError) {
      setNotice(apiErrorMessage(likeError, "좋아요를 처리하지 못했습니다."));
    }
  };

  const handleReport = async () => {
    if (!post) return;
    if (!loggedIn || !accessToken) {
      setLoginMessage("신고하려면 로그인해 주세요.");
      return;
    }
    if (
      !window.confirm(
        "이 게시글을 신고할까요? 신고가 누적되면 블라인드 처리됩니다.",
      )
    ) {
      return;
    }
    setBusy(true);
    try {
      await reportGlowCommunityPost(post.id, accessToken);
      setNotice("신고가 접수되었습니다.");
    } catch (reportError) {
      setNotice(apiErrorMessage(reportError, "신고를 접수하지 못했습니다."));
    } finally {
      setBusy(false);
    }
  };

  const handleDelete = async () => {
    if (!post || !accessToken) return;
    if (
      !window.confirm("이 게시글을 삭제할까요? 삭제한 글은 되돌릴 수 없습니다.")
    )
      return;
    setBusy(true);
    try {
      await deleteGlowCommunityPost(post.id, accessToken);
      onDeleted();
    } catch (deleteError) {
      setNotice(apiErrorMessage(deleteError, "게시글을 삭제하지 못했습니다."));
      setBusy(false);
    }
  };

  const edited =
    Boolean(post?.updatedAt && post?.createdAt) &&
    post?.updatedAt !== post?.createdAt;

  return (
    <div className={styles.detail}>
      <div className={styles.detailTopBar}>
        <button type="button" className={styles.backButton} onClick={onBack}>
          ← 지역사회 목록
        </button>
      </div>

      {loading ? (
        <p className={styles.status}>게시글을 불러오는 중입니다.</p>
      ) : null}
      {error ? (
        <p className={styles.errorBox} role="alert">
          {error}
        </p>
      ) : null}

      {post ? (
        <>
          <article className={styles.detailCard}>
            {post.blinded ? (
              <div className={styles.blindedBanner} role="note">
                <strong>블라인드된 게시글입니다.</strong>
                <span>
                  {post.blindReason
                    ? `사유: ${post.blindReason}`
                    : "신고 누적 또는 운영 정책에 따라 다른 회원에게는 보이지 않습니다."}
                </span>
              </div>
            ) : null}
            <header className={styles.detailHead}>
              <span className={styles.tagBadge}>
                {glowCommunityTagLabel(post)}
              </span>
              <h1 className={styles.detailTitle}>{post.title}</h1>
              <p className={styles.postMeta}>
                <span>{post.authorName}</span>
                <time dateTime={post.createdAt ?? undefined}>
                  {formatIdolGlowDateTime(post.createdAt)}
                </time>
                {edited ? <em>수정됨</em> : null}
              </p>
            </header>
            <SafeMarkdown
              markdown={post.content}
              className={styles.detailBody}
            />
            <footer className={styles.detailFooter}>
              <div className={styles.counts}>
                <button
                  type="button"
                  className={
                    post.liked || post.likedByMe
                      ? styles.likedButton
                      : undefined
                  }
                  aria-pressed={post.liked || post.likedByMe}
                  aria-label={`좋아요 ${post.likeCount}`}
                  onClick={() => void handleLike()}
                >
                  <ThumbIcon />
                  {post.likeCount}
                </button>
                <span aria-label={`댓글 ${post.commentCount}`}>
                  <CommentIcon />
                  {post.commentCount}
                </span>
              </div>
              <div className={styles.detailActions}>
                {post.mine ? (
                  <>
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => setEditing(true)}
                    >
                      수정
                    </button>
                    <button
                      type="button"
                      className={styles.dangerText}
                      disabled={busy}
                      onClick={() => void handleDelete()}
                    >
                      삭제
                    </button>
                  </>
                ) : (
                  <button
                    type="button"
                    className={styles.dangerText}
                    disabled={busy}
                    onClick={() => void handleReport()}
                  >
                    신고
                  </button>
                )}
              </div>
            </footer>
          </article>

          <ContentComments
            targetType="GLOW_COMMUNITY_POST"
            targetKey={String(post.id)}
            targetTitle={post.title}
            targetPath={path}
          />
        </>
      ) : null}

      {notice ? (
        <p className={styles.toast} aria-live="polite">
          {notice}
        </p>
      ) : null}

      {editing && post && accessToken ? (
        <GlowCommunityEditor
          accessToken={accessToken}
          post={post}
          onClose={() => setEditing(false)}
          onSaved={(saved) => {
            setPost(saved);
            setEditing(false);
            setNotice("게시글을 수정했습니다.");
          }}
        />
      ) : null}

      {loginMessage ? (
        <GlowCommunityLoginPrompt
          message={loginMessage}
          onClose={() => setLoginMessage(null)}
        />
      ) : null}
    </div>
  );
}

export default GlowCommunityPostDetail;
