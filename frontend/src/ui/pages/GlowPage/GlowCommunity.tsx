import {
  type FormEvent,
  type ReactNode,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import { apiErrorMessage, isAbortError } from "../../../shared/data/apiClient";
import {
  GLOW_COMMUNITY_TAG_LABELS,
  type GlowCommunityPost,
  type GlowCommunitySort,
  type GlowCommunityTag,
  type GlowCommunityTagFilter,
  deleteGlowCommunityPost,
  fetchGlowCommunityHighlights,
  fetchGlowCommunityKeywords,
  fetchGlowCommunityPosts,
  glowCommunityPostPath,
  glowCommunityTagLabel,
  isGlowCommunityTag,
  reportGlowCommunityPost,
  toggleGlowCommunityLike,
} from "../../../shared/data/glowCommunityApi";
import { formatIdolGlowDate } from "../../../shared/format/idolGlowDateTime";
import { SimplePager } from "../../components/SimplePager/SimplePager";
import styles from "./GlowCommunity.module.css";
import { GlowCommunityEditor } from "./GlowCommunityEditor";
import {
  ChevronIcon,
  CommentIcon,
  SearchIcon,
  ThumbIcon,
} from "./GlowCommunityIcons";
import { GlowCommunityLoginPrompt } from "./GlowCommunityLoginPrompt";
import { GlowCommunityPostDetail } from "./GlowCommunityPostDetail";

const PAGE_SIZE = 8;
const RECENT_KEYWORDS_KEY = "idolglow:glow-community-recent-keywords";
const MAX_RECENT_KEYWORDS = 10;
const COLLAPSED_KEYWORD_COUNT = 5;

const TAG_FILTERS: readonly {
  readonly id: GlowCommunityTagFilter;
  readonly label: string;
}[] = [
  { id: "all", label: "전체" },
  { id: "talk", label: GLOW_COMMUNITY_TAG_LABELS.talk },
  { id: "question", label: GLOW_COMMUNITY_TAG_LABELS.question },
  { id: "travel", label: GLOW_COMMUNITY_TAG_LABELS.travel },
];

const readRecentKeywords = (): string[] => {
  try {
    const raw = window.localStorage.getItem(RECENT_KEYWORDS_KEY);
    const parsed: unknown = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed)
      ? parsed.filter((item): item is string => typeof item === "string")
      : [];
  } catch {
    return [];
  }
};

const writeRecentKeywords = (keywords: readonly string[]) => {
  try {
    window.localStorage.setItem(RECENT_KEYWORDS_KEY, JSON.stringify(keywords));
  } catch {
    return;
  }
};

const parseTagFilter = (raw: string | null): GlowCommunityTagFilter =>
  raw && isGlowCommunityTag(raw) ? raw : "all";

const parseSort = (raw: string | null): GlowCommunitySort =>
  raw === "popular" ? "popular" : "latest";

const parsePage = (raw: string | null): number => {
  const value = Number(raw);
  return Number.isFinite(value) && value > 1 ? Math.floor(value) - 1 : 0;
};

const parsePostId = (raw: string | null): number | null => {
  const value = Number(raw);
  return Number.isInteger(value) && value > 0 ? value : null;
};

type PostActionsProps = {
  readonly post: GlowCommunityPost;
  readonly onEdit: (post: GlowCommunityPost) => void;
  readonly onDelete: (post: GlowCommunityPost) => void;
  readonly onReport: (post: GlowCommunityPost) => void;
};

const PostActions = ({
  post,
  onEdit,
  onDelete,
  onReport,
}: PostActionsProps) => {
  const [open, setOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const close = (event: MouseEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, [open]);

  return (
    <div className={styles.postMenu} ref={menuRef}>
      <button
        type="button"
        className={styles.menuButton}
        aria-label="게시글 메뉴"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        ···
      </button>
      {open ? (
        <div className={styles.menuList} role="menu">
          {post.mine ? (
            <>
              <button
                type="button"
                role="menuitem"
                onClick={() => {
                  setOpen(false);
                  onEdit(post);
                }}
              >
                수정하기
              </button>
              <button
                type="button"
                role="menuitem"
                onClick={() => {
                  setOpen(false);
                  onDelete(post);
                }}
              >
                삭제하기
              </button>
            </>
          ) : (
            <button
              type="button"
              role="menuitem"
              onClick={() => {
                setOpen(false);
                onReport(post);
              }}
            >
              신고하기
            </button>
          )}
        </div>
      ) : null}
    </div>
  );
};

export const GlowCommunity = ({
  filterBar,
}: { readonly filterBar: ReactNode }) => {
  const { authReady, user, accessToken } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const tag = parseTagFilter(searchParams.get("tag"));
  const sort = parseSort(searchParams.get("sort"));
  const page = parsePage(searchParams.get("page"));
  const keyword = (searchParams.get("q") ?? "").trim();
  const postId = parsePostId(searchParams.get("post"));

  const [keywordInput, setKeywordInput] = useState(keyword);
  const [recentKeywords, setRecentKeywords] =
    useState<string[]>(readRecentKeywords);
  const [suggestedKeywords, setSuggestedKeywords] = useState<readonly string[]>(
    [],
  );
  const [keywordsError, setKeywordsError] = useState(false);
  const [showAllKeywords, setShowAllKeywords] = useState(false);
  const [posts, setPosts] = useState<readonly GlowCommunityPost[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [listError, setListError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [highlights, setHighlights] = useState<readonly GlowCommunityPost[]>(
    [],
  );
  const [highlightError, setHighlightError] = useState<string | null>(null);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(false);
  const [editorPost, setEditorPost] = useState<GlowCommunityPost | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [loginMessage, setLoginMessage] = useState<string | null>(null);
  const [notice, setNotice] = useState("");
  const highlightRef = useRef<HTMLDivElement>(null);
  const listTopRef = useRef<HTMLDivElement>(null);
  const loggedIn = Boolean(authReady && user && accessToken);

  useEffect(() => {
    setKeywordInput(keyword);
  }, [keyword]);

  const updateParams = useCallback(
    (
      changes: Record<string, string | null>,
      options: { readonly push?: boolean } = {},
    ) => {
      const next = new URLSearchParams(searchParams);
      next.set("tab", "local");
      for (const [key, value] of Object.entries(changes)) {
        if (value === null || value === "") next.delete(key);
        else next.set(key, value);
      }
      setSearchParams(next, { replace: !options.push });
    },
    [searchParams, setSearchParams],
  );

  // biome-ignore lint/correctness/useExhaustiveDependencies: dependency is an intentional re-run trigger
  useEffect(() => {
    if (!authReady || postId !== null) return;
    const controller = new AbortController();
    setLoading(true);
    setListError(null);
    fetchGlowCommunityPosts({
      tag,
      q: keyword,
      sort,
      page,
      size: PAGE_SIZE,
      accessToken,
      signal: controller.signal,
    })
      .then((response) => {
        setPosts(response.items);
        setTotalElements(response.totalElements);
        setTotalPages(Math.max(1, response.totalPages));
      })
      .catch((error: unknown) => {
        if (isAbortError(error)) return;
        setPosts([]);
        setTotalElements(0);
        setTotalPages(1);
        setListError(apiErrorMessage(error, "게시글을 불러오지 못했습니다."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [accessToken, authReady, keyword, page, postId, reloadKey, sort, tag]);

  // biome-ignore lint/correctness/useExhaustiveDependencies: dependency is an intentional re-run trigger
  useEffect(() => {
    if (!authReady || postId !== null) return;
    let cancelled = false;
    fetchGlowCommunityHighlights(accessToken)
      .then((items) => {
        if (cancelled) return;
        setHighlights(items);
        setHighlightError(null);
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        setHighlights([]);
        setHighlightError(
          apiErrorMessage(error, "하이라이트를 불러오지 못했습니다."),
        );
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, postId, reloadKey]);

  useEffect(() => {
    let cancelled = false;
    fetchGlowCommunityKeywords()
      .then((result) => {
        if (cancelled) return;
        setSuggestedKeywords(result.keywords.filter((item) => item.trim()));
        setKeywordsError(false);
      })
      .catch(() => {
        if (!cancelled) setKeywordsError(true);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const updateScrollButtons = useCallback(() => {
    const track = highlightRef.current;
    if (!track) return;
    setCanScrollLeft(track.scrollLeft > 4);
    setCanScrollRight(
      track.scrollLeft + track.clientWidth < track.scrollWidth - 4,
    );
  }, []);

  // biome-ignore lint/correctness/useExhaustiveDependencies: dependency is an intentional re-run trigger
  useEffect(() => {
    updateScrollButtons();
    window.addEventListener("resize", updateScrollButtons);
    return () => window.removeEventListener("resize", updateScrollButtons);
  }, [highlights, updateScrollButtons]);

  useEffect(() => {
    if (!notice) return;
    const timer = window.setTimeout(() => setNotice(""), 2200);
    return () => window.clearTimeout(timer);
  }, [notice]);

  const scrollHighlights = (direction: 1 | -1) => {
    const track = highlightRef.current;
    if (!track) return;
    track.scrollBy({
      left: direction * track.clientWidth * 0.8,
      behavior: "smooth",
    });
  };

  const applyKeyword = (value: string) => {
    const trimmed = value.trim();
    setKeywordInput(trimmed);
    updateParams({ q: trimmed || null, page: null });
    if (!trimmed) return;
    const next = [
      trimmed,
      ...recentKeywords.filter((item) => item !== trimmed),
    ].slice(0, MAX_RECENT_KEYWORDS);
    setRecentKeywords(next);
    writeRecentKeywords(next);
  };

  const removeRecentKeyword = (value: string) => {
    const next = recentKeywords.filter((item) => item !== value);
    setRecentKeywords(next);
    writeRecentKeywords(next);
  };

  const handleSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    applyKeyword(keywordInput);
  };

  const goToPage = (next: number) => {
    const target = Math.min(totalPages - 1, Math.max(0, next));
    updateParams({ page: target > 0 ? String(target + 1) : null });
    listTopRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  const openPost = (id: number) => {
    updateParams({ post: String(id) }, { push: true });
    window.requestAnimationFrame(() =>
      listTopRef.current
        ?.closest("section")
        ?.scrollIntoView({ block: "start" }),
    );
  };

  const closePost = () => {
    updateParams({ post: null }, { push: true });
  };

  const handleLike = async (post: GlowCommunityPost) => {
    if (!loggedIn || !accessToken) {
      setLoginMessage("좋아요를 누르려면 로그인해 주세요.");
      return;
    }
    try {
      const result = await toggleGlowCommunityLike(post.id, accessToken);
      const apply = (item: GlowCommunityPost) =>
        item.id === post.id
          ? {
              ...item,
              liked: result.liked,
              likedByMe: result.liked,
              likeCount: result.likeCount,
            }
          : item;
      setPosts((current) => current.map(apply));
      setHighlights((current) => current.map(apply));
    } catch (error) {
      setNotice(apiErrorMessage(error, "좋아요를 처리하지 못했습니다."));
    }
  };

  const openWrite = () => {
    if (!loggedIn) {
      setLoginMessage("글을 쓰려면 로그인해 주세요.");
      return;
    }
    setEditorPost(null);
    setEditorOpen(true);
  };

  const openEdit = (post: GlowCommunityPost) => {
    setEditorPost(post);
    setEditorOpen(true);
  };

  const handleDelete = async (post: GlowCommunityPost) => {
    if (!accessToken) return;
    if (
      !window.confirm("이 게시글을 삭제할까요? 삭제한 글은 되돌릴 수 없습니다.")
    )
      return;
    try {
      await deleteGlowCommunityPost(post.id, accessToken);
      setReloadKey((value) => value + 1);
      setNotice("게시글을 삭제했습니다.");
    } catch (error) {
      setNotice(apiErrorMessage(error, "게시글을 삭제하지 못했습니다."));
    }
  };

  const handleReport = async (post: GlowCommunityPost) => {
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
    try {
      await reportGlowCommunityPost(post.id, accessToken);
      setNotice("신고가 접수되었습니다.");
    } catch (error) {
      setNotice(apiErrorMessage(error, "신고를 접수하지 못했습니다."));
    }
  };

  const defaultTag: GlowCommunityTag = tag === "all" ? "talk" : tag;
  const visibleSuggestions = showAllKeywords
    ? suggestedKeywords
    : suggestedKeywords.slice(0, COLLAPSED_KEYWORD_COUNT);

  const overlays = (
    <>
      {notice ? (
        <p className={styles.toast} aria-live="polite">
          {notice}
        </p>
      ) : null}
      {editorOpen && accessToken ? (
        <GlowCommunityEditor
          accessToken={accessToken}
          post={editorPost}
          defaultTag={defaultTag}
          onClose={() => setEditorOpen(false)}
          onSaved={(saved, created) => {
            setEditorOpen(false);
            setReloadKey((value) => value + 1);
            if (created) {
              setNotice("게시글을 등록했습니다.");
              openPost(saved.id);
            } else {
              setNotice("게시글을 수정했습니다.");
            }
          }}
        />
      ) : null}
      {loginMessage ? (
        <GlowCommunityLoginPrompt
          message={loginMessage}
          onClose={() => setLoginMessage(null)}
        />
      ) : null}
    </>
  );

  if (postId !== null) {
    return (
      <div className={styles.community}>
        {filterBar}
        <GlowCommunityPostDetail
          key={postId}
          postId={postId}
          onBack={closePost}
          onDeleted={() => {
            setNotice("게시글을 삭제했습니다.");
            setReloadKey((value) => value + 1);
            updateParams({ post: null }, { push: false });
          }}
        />
        {overlays}
      </div>
    );
  }

  const renderCounts = (post: GlowCommunityPost) => (
    <div className={styles.counts}>
      <button
        type="button"
        className={
          post.liked || post.likedByMe ? styles.likedButton : undefined
        }
        aria-pressed={post.liked || post.likedByMe}
        aria-label={`좋아요 ${post.likeCount}`}
        onClick={() => void handleLike(post)}
      >
        <ThumbIcon />
        {post.likeCount}
      </button>
      <span aria-label={`댓글 ${post.commentCount}`}>
        <CommentIcon />
        {post.commentCount}
      </span>
    </div>
  );

  return (
    <div className={styles.community}>
      {filterBar}

      <div className={styles.toolbar}>
        <div
          className={styles.tagChips}
          role="tablist"
          aria-label="게시글 말머리"
        >
          {TAG_FILTERS.map((option) => (
            <button
              key={option.id}
              type="button"
              role="tab"
              aria-selected={tag === option.id}
              className={
                tag === option.id ? styles.tagChipActive : styles.tagChip
              }
              onClick={() =>
                updateParams({
                  tag: option.id === "all" ? null : option.id,
                  page: null,
                })
              }
            >
              # {option.label}
            </button>
          ))}
        </div>
        <button
          type="button"
          className={styles.writeButton}
          onClick={openWrite}
        >
          쓰기
        </button>
      </div>

      <section
        className={styles.highlightSection}
        aria-labelledby="community-highlight-title"
      >
        <h2 id="community-highlight-title" className={styles.sectionTitle}>
          하이라이트
        </h2>
        {highlightError ? (
          <p className={styles.inlineError}>{highlightError}</p>
        ) : null}
        {!highlightError && highlights.length === 0 ? (
          <p className={styles.inlineStatus}>
            아직 하이라이트 게시글이 없습니다.
          </p>
        ) : null}
        {highlights.length > 0 ? (
          <div className={styles.highlightWrap}>
            {canScrollLeft ? (
              <button
                type="button"
                className={`${styles.slideButton} ${styles.slidePrev}`}
                aria-label="이전 하이라이트"
                onClick={() => scrollHighlights(-1)}
              >
                <ChevronIcon direction="left" />
              </button>
            ) : null}
            <div
              className={styles.highlightTrack}
              ref={highlightRef}
              onScroll={updateScrollButtons}
            >
              {highlights.map((post) => (
                <article key={post.id} className={styles.highlightCard}>
                  <span className={styles.tagBadge}>
                    {glowCommunityTagLabel(post)}
                  </span>
                  <h3>
                    <Link
                      to={glowCommunityPostPath(post.id)}
                      onClick={(event) => {
                        event.preventDefault();
                        openPost(post.id);
                      }}
                    >
                      {post.title}
                    </Link>
                  </h3>
                  {renderCounts(post)}
                </article>
              ))}
            </div>
            {canScrollRight ? (
              <button
                type="button"
                className={`${styles.slideButton} ${styles.slideNext}`}
                aria-label="다음 하이라이트"
                onClick={() => scrollHighlights(1)}
              >
                <ChevronIcon direction="right" />
              </button>
            ) : null}
          </div>
        ) : null}
      </section>

      <form
        className={styles.searchForm}
        // biome-ignore lint/a11y/useSemanticElements: search landmark on a form element
        role="search"
        onSubmit={handleSearchSubmit}
      >
        <input
          type="search"
          value={keywordInput}
          placeholder="검색어를 입력해 주세요 (최근 검색어 최대 10개가 저장됩니다)"
          aria-label="게시글 검색"
          onChange={(event) => setKeywordInput(event.target.value)}
        />
        {keywordInput ? (
          <button
            type="button"
            className={styles.clearButton}
            aria-label="검색어 지우기"
            onClick={() => applyKeyword("")}
          >
            ×
          </button>
        ) : null}
        <button type="submit" className={styles.searchButton} aria-label="검색">
          <SearchIcon />
        </button>
      </form>

      <div className={styles.keywordBlock}>
        <div className={styles.keywordRow}>
          <span className={styles.keywordLabel}>AI 추천 검색어</span>
          {suggestedKeywords.length > 0 ? (
            <ul className={styles.keywordChips} aria-label="AI 추천 검색어">
              {visibleSuggestions.map((item) => (
                <li key={item}>
                  <button
                    type="button"
                    className={
                      keyword === item ? styles.keywordActive : undefined
                    }
                    onClick={() => applyKeyword(item)}
                  >
                    {item}
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <span className={styles.keywordEmpty}>
              {keywordsError
                ? "추천 검색어를 불러오지 못했습니다."
                : "추천 검색어를 준비 중입니다."}
            </span>
          )}
          {suggestedKeywords.length > COLLAPSED_KEYWORD_COUNT ? (
            <button
              type="button"
              className={styles.showAllButton}
              aria-expanded={showAllKeywords}
              onClick={() => setShowAllKeywords((value) => !value)}
            >
              {showAllKeywords ? "− 접기" : "+ 모두 보기"}
            </button>
          ) : null}
        </div>
        {recentKeywords.length > 0 ? (
          <div className={styles.keywordRow}>
            <span className={styles.keywordLabel}>최근 검색어</span>
            <ul className={styles.keywordChips} aria-label="최근 검색어">
              {recentKeywords.map((item) => (
                <li key={item}>
                  <button type="button" onClick={() => applyKeyword(item)}>
                    {item}
                  </button>
                  <button
                    type="button"
                    className={styles.removeKeyword}
                    aria-label={`${item} 검색어 삭제`}
                    onClick={() => removeRecentKeyword(item)}
                  >
                    ×
                  </button>
                </li>
              ))}
            </ul>
          </div>
        ) : null}
      </div>

      <div className={styles.listHeader} ref={listTopRef}>
        <p>
          총 <strong>{totalElements.toLocaleString()}</strong>개의 게시글
          {keyword ? (
            <span className={styles.keywordResult}>
              {" "}
              · 「{keyword}」 검색 결과
            </span>
          ) : null}
        </p>
        <div className={styles.sortTabs} role="tablist" aria-label="정렬">
          {(["latest", "popular"] as const).map((option) => (
            <button
              key={option}
              type="button"
              role="tab"
              aria-selected={sort === option}
              className={sort === option ? styles.sortActive : undefined}
              onClick={() =>
                updateParams({
                  sort: option === "latest" ? null : option,
                  page: null,
                })
              }
            >
              {option === "latest" ? "최신" : "인기"}
            </button>
          ))}
        </div>
      </div>

      {listError ? (
        <p className={styles.errorBox} role="alert">
          {listError}
        </p>
      ) : null}

      <ul className={styles.postList} aria-busy={loading}>
        {posts.map((post) => (
          <li key={post.id} className={styles.postItem}>
            <div className={styles.postHead}>
              <span className={styles.badgeRow}>
                <span className={styles.tagBadge}>
                  {glowCommunityTagLabel(post)}
                </span>
                {post.blinded ? (
                  <span className={styles.blindBadge}>블라인드</span>
                ) : null}
              </span>
              {loggedIn ? (
                <PostActions
                  post={post}
                  onEdit={openEdit}
                  onDelete={(target) => void handleDelete(target)}
                  onReport={(target) => void handleReport(target)}
                />
              ) : null}
            </div>
            <p className={styles.postMeta}>
              <span>{post.authorName}</span>
              <time dateTime={post.createdAt ?? undefined}>
                {formatIdolGlowDate(post.createdAt).replace(/-/g, ".")}
              </time>
            </p>
            <h3 className={styles.postTitle}>
              <Link
                to={glowCommunityPostPath(post.id)}
                onClick={(event) => {
                  event.preventDefault();
                  openPost(post.id);
                }}
              >
                {post.title}
              </Link>
            </h3>
            {renderCounts(post)}
          </li>
        ))}
      </ul>

      {loading && posts.length === 0 ? (
        <p className={styles.status}>게시글을 불러오는 중입니다.</p>
      ) : null}
      {!loading && !listError && posts.length === 0 ? (
        <p className={styles.status} aria-live="polite">
          {keyword
            ? `「${keyword}」에 맞는 게시글이 없습니다.`
            : "아직 등록된 게시글이 없습니다."}
        </p>
      ) : null}

      <SimplePager
        page={page}
        totalPages={totalPages}
        onChange={goToPage}
        label="게시글 페이지"
      />

      {overlays}
    </div>
  );
};

export default GlowCommunity;
