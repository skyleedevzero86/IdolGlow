import { type FormEvent, type ReactNode, useCallback, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import {
  GLOW_COMMUNITY_SUGGESTED_KEYWORDS,
  GLOW_COMMUNITY_TAG_LABELS,
  type GlowCommunityPost,
  type GlowCommunitySort,
  type GlowCommunityTag,
  type GlowCommunityTagFilter,
  createGlowCommunityPost,
  deleteGlowCommunityPost,
  fetchGlowCommunityHighlights,
  fetchGlowCommunityPosts,
  reportGlowCommunityPost,
  toggleGlowCommunityLike,
} from '../../../shared/data/glowCommunityApi';
import glowStyles from './GlowPage.module.css';
import styles from './GlowCommunity.module.css';

const PAGE_SIZE = 8;
const PAGE_GROUP_SIZE = 5;
const RECENT_KEYWORDS_KEY = 'idolglow:glow-community-recent-keywords';
const MAX_RECENT_KEYWORDS = 10;
const COLLAPSED_KEYWORD_COUNT = 3;

const TAG_FILTERS: readonly { readonly id: GlowCommunityTagFilter; readonly label: string }[] = [
  { id: 'all', label: '모두' },
  { id: 'talk', label: GLOW_COMMUNITY_TAG_LABELS.talk },
  { id: 'question', label: GLOW_COMMUNITY_TAG_LABELS.question },
  { id: 'travel', label: GLOW_COMMUNITY_TAG_LABELS.travel },
];

const readRecentKeywords = (): string[] => {
  try {
    const raw = window.localStorage.getItem(RECENT_KEYWORDS_KEY);
    const parsed: unknown = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : [];
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

const formatDate = (value: string): string => value.split('-').join('.');

const ThumbIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
    <path d="M7 10v11H4a1 1 0 0 1-1-1v-9a1 1 0 0 1 1-1h3Zm0 0 4-7a2 2 0 0 1 3 1.7V9h5a2 2 0 0 1 2 2.3l-1.3 8A2 2 0 0 1 17.7 21H7" />
  </svg>
);

const CommentIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
    <path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1.2-4.6A8 8 0 1 1 21 12Z" />
    <path d="M8.5 12h.01M12 12h.01M15.5 12h.01" strokeLinecap="round" strokeWidth="2.4" />
  </svg>
);

const SearchIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
    <circle cx="11" cy="11" r="7" />
    <path d="m20 20-3.5-3.5" />
  </svg>
);

const SortIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
    <path d="M4 6h10M4 12h7M4 18h4M18 5v14m0 0-3-3m3 3 3-3" />
  </svg>
);

const ChevronIcon = ({ direction }: { readonly direction: 'left' | 'right' }) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
    <polyline points={direction === 'left' ? '15 18 9 12 15 6' : '9 18 15 12 9 6'} />
  </svg>
);

type PostActionsProps = {
  readonly post: GlowCommunityPost;
  readonly isMine: boolean;
  readonly onDelete: (post: GlowCommunityPost) => void;
  readonly onReport: (post: GlowCommunityPost) => void;
};

const PostActions = ({ post, isMine, onDelete, onReport }: PostActionsProps) => {
  const [open, setOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const close = (event: MouseEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, [open]);

  return (
    <div className={styles.postMenu} ref={menuRef}>
      <button
        type="button"
        className={styles.menuButton}
        aria-label="게시물 메뉴"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        ···
      </button>
      {open ? (
        <div className={styles.menuList} role="menu">
          {isMine ? (
            <button type="button" role="menuitem" onClick={() => { setOpen(false); onDelete(post); }}>
              삭제하기
            </button>
          ) : (
            <button type="button" role="menuitem" onClick={() => { setOpen(false); onReport(post); }}>
              신고하기
            </button>
          )}
        </div>
      ) : null}
    </div>
  );
};

export const GlowCommunity = ({ filterBar }: { readonly filterBar: ReactNode }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { authReady, user, accessToken } = useAuth();

  const [tag, setTag] = useState<GlowCommunityTagFilter>('all');
  const [sort, setSort] = useState<GlowCommunitySort>('latest');
  const [page, setPage] = useState(0);
  const [keywordInput, setKeywordInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [recentKeywords, setRecentKeywords] = useState<string[]>(readRecentKeywords);
  const [showAllKeywords, setShowAllKeywords] = useState(false);
  const [posts, setPosts] = useState<readonly GlowCommunityPost[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const [highlights, setHighlights] = useState<readonly GlowCommunityPost[]>([]);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(false);
  const [writeOpen, setWriteOpen] = useState(false);
  const [draftTag, setDraftTag] = useState<GlowCommunityTag>('talk');
  const [draftTitle, setDraftTitle] = useState('');
  const [draftContent, setDraftContent] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [notice, setNotice] = useState('');
  const highlightRef = useRef<HTMLDivElement>(null);
  const listTopRef = useRef<HTMLDivElement>(null);

  const requireLogin = useCallback((): boolean => {
    if (!authReady) return false;
    if (user) return true;
    const redirect = encodeURIComponent(`${location.pathname}${location.search}${location.hash}`);
    navigate(`/?login=1&redirect=${redirect}`);
    return false;
  }, [authReady, user, location.pathname, location.search, location.hash, navigate]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    void fetchGlowCommunityPosts({ tag, q: keyword, sort, page, size: PAGE_SIZE, accessToken }).then((response) => {
      if (cancelled) return;
      setPosts(response.items);
      setTotalElements(response.totalElements);
      setTotalPages(Math.max(1, response.totalPages));
      setLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, [tag, keyword, sort, page, accessToken, reloadKey]);

  useEffect(() => {
    let cancelled = false;
    void fetchGlowCommunityHighlights().then((items) => {
      if (!cancelled) setHighlights(items);
    });
    return () => {
      cancelled = true;
    };
  }, [reloadKey]);

  const updateScrollButtons = useCallback(() => {
    const track = highlightRef.current;
    if (!track) return;
    setCanScrollLeft(track.scrollLeft > 4);
    setCanScrollRight(track.scrollLeft + track.clientWidth < track.scrollWidth - 4);
  }, []);

  useEffect(() => {
    updateScrollButtons();
    window.addEventListener('resize', updateScrollButtons);
    return () => window.removeEventListener('resize', updateScrollButtons);
  }, [highlights, updateScrollButtons]);

  useEffect(() => {
    if (!notice) return;
    const timer = window.setTimeout(() => setNotice(''), 2200);
    return () => window.clearTimeout(timer);
  }, [notice]);

  useEffect(() => {
    if (!writeOpen) return;
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setWriteOpen(false);
    };
    window.addEventListener('keydown', closeOnEscape);
    return () => window.removeEventListener('keydown', closeOnEscape);
  }, [writeOpen]);

  const scrollHighlights = (direction: 1 | -1) => {
    const track = highlightRef.current;
    if (!track) return;
    track.scrollBy({ left: direction * track.clientWidth * 0.8, behavior: 'smooth' });
  };

  const applyKeyword = (value: string) => {
    const trimmed = value.trim();
    setKeywordInput(trimmed);
    setKeyword(trimmed);
    setPage(0);
    if (!trimmed) return;
    const next = [trimmed, ...recentKeywords.filter((item) => item !== trimmed)].slice(0, MAX_RECENT_KEYWORDS);
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

  const clearKeyword = () => {
    setKeywordInput('');
    setKeyword('');
    setPage(0);
  };

  const changeTag = (next: GlowCommunityTagFilter) => {
    setTag(next);
    setPage(0);
  };

  const goToPage = (next: number) => {
    const target = Math.min(totalPages - 1, Math.max(0, next));
    setPage(target);
    listTopRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const handleLike = async (post: GlowCommunityPost) => {
    if (!requireLogin()) return;
    const result = await toggleGlowCommunityLike(post.id, accessToken);
    const apply = (item: GlowCommunityPost) =>
      item.id === post.id ? { ...item, liked: result.liked, likeCount: result.likeCount } : item;
    setPosts((current) => current.map(apply));
    setHighlights((current) => current.map(apply));
  };

  const openWrite = () => {
    if (!requireLogin()) return;
    setDraftTag(tag === 'all' ? 'talk' : tag);
    setDraftTitle('');
    setDraftContent('');
    setWriteOpen(true);
  };

  const handleWriteSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!user || submitting) return;
    const title = draftTitle.trim();
    const content = draftContent.trim();
    if (!title || !content) return;
    setSubmitting(true);
    await createGlowCommunityPost({
      tag: draftTag,
      title,
      content,
      authorName: user.nickname || user.name || '사용자',
      authorId: user.id,
      accessToken,
    });
    setSubmitting(false);
    setWriteOpen(false);
    setSort('latest');
    setPage(0);
    setReloadKey((value) => value + 1);
    setNotice('게시물이 등록되었습니다.');
  };

  const handleDelete = async (post: GlowCommunityPost) => {
    if (!window.confirm('이 게시물을 삭제할까요?')) return;
    await deleteGlowCommunityPost(post.id, accessToken);
    setReloadKey((value) => value + 1);
    setNotice('게시물이 삭제되었습니다.');
  };

  const handleReport = async (post: GlowCommunityPost) => {
    if (!requireLogin()) return;
    await reportGlowCommunityPost(post.id, accessToken);
    setNotice('신고가 접수되었습니다.');
  };

  const keywordChips = recentKeywords.length > 0 ? recentKeywords : [...GLOW_COMMUNITY_SUGGESTED_KEYWORDS];
  const visibleKeywords = showAllKeywords ? keywordChips : keywordChips.slice(0, COLLAPSED_KEYWORD_COUNT);
  const groupStart = Math.floor(page / PAGE_GROUP_SIZE) * PAGE_GROUP_SIZE;
  const groupEnd = Math.min(totalPages, groupStart + PAGE_GROUP_SIZE);
  const pageNumbers = Array.from({ length: groupEnd - groupStart }, (_, index) => groupStart + index);

  return (
    <div className={styles.community}>
      {filterBar}

      <div className={styles.toolbar}>
        <div className={styles.tagChips} role="tablist" aria-label="게시물 태그">
          {TAG_FILTERS.map((option) => (
            <button
              key={option.id}
              type="button"
              role="tab"
              aria-selected={tag === option.id}
              className={tag === option.id ? styles.activeTag : undefined}
              onClick={() => changeTag(option.id)}
            >
              # {option.label}
            </button>
          ))}
        </div>
        <button type="button" className={styles.writeButton} onClick={openWrite}>
          쓰기
        </button>
      </div>

      <section className={styles.highlightSection} aria-labelledby="community-highlight-title">
        <h2 id="community-highlight-title" className={styles.sectionTitle}>하이라이트</h2>
        <div className={styles.highlightWrap}>
          {canScrollLeft ? (
            <button type="button" className={`${styles.slideButton} ${styles.slidePrev}`} aria-label="이전 하이라이트" onClick={() => scrollHighlights(-1)}>
              <ChevronIcon direction="left" />
            </button>
          ) : null}
          <div className={styles.highlightTrack} ref={highlightRef} onScroll={updateScrollButtons}>
            {highlights.map((post) => (
              <article key={post.id} className={styles.highlightCard}>
                <span className={styles.tagBadge}>{GLOW_COMMUNITY_TAG_LABELS[post.tag]}</span>
                <h3>{post.title}</h3>
                <div className={styles.counts}>
                  <button
                    type="button"
                    className={post.liked ? styles.likedButton : undefined}
                    aria-pressed={post.liked}
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
              </article>
            ))}
          </div>
          {canScrollRight ? (
            <button type="button" className={`${styles.slideButton} ${styles.slideNext}`} aria-label="다음 하이라이트" onClick={() => scrollHighlights(1)}>
              <ChevronIcon direction="right" />
            </button>
          ) : null}
        </div>
      </section>

      <form className={styles.searchForm} role="search" onSubmit={handleSearchSubmit}>
        <input
          type="search"
          value={keywordInput}
          placeholder="검색어를 입력해 주세요(최근 검색어 최대 10개가 저장됩니다)"
          aria-label="게시물 검색"
          onChange={(event) => setKeywordInput(event.target.value)}
        />
        {keywordInput ? (
          <button type="button" className={styles.clearButton} aria-label="검색어 지우기" onClick={clearKeyword}>
            ×
          </button>
        ) : null}
        <button type="submit" className={styles.searchButton} aria-label="검색">
          <SearchIcon />
        </button>
      </form>

      <div className={styles.keywordRow}>
        <ul className={styles.keywordChips} aria-label={recentKeywords.length > 0 ? '최근 검색어' : '추천 검색어'}>
          {visibleKeywords.map((item) => (
            <li key={item}>
              <button type="button" onClick={() => applyKeyword(item)}>{item}</button>
              {recentKeywords.length > 0 ? (
                <button type="button" className={styles.removeKeyword} aria-label={`${item} 검색어 삭제`} onClick={() => removeRecentKeyword(item)}>
                  ×
                </button>
              ) : null}
            </li>
          ))}
        </ul>
        {keywordChips.length > COLLAPSED_KEYWORD_COUNT ? (
          <button type="button" className={styles.showAllButton} aria-expanded={showAllKeywords} onClick={() => setShowAllKeywords((value) => !value)}>
            {showAllKeywords ? '− 접기' : '+ 모두 보기'}
          </button>
        ) : null}
      </div>

      <div className={styles.listHeader} ref={listTopRef}>
        <p>
          총 <strong>{totalElements.toLocaleString()}</strong>개의 게시물
          {keyword ? <span className={styles.keywordResult}> · 「{keyword}」 검색 결과</span> : null}
        </p>
        <button
          type="button"
          className={styles.sortButton}
          aria-label={`정렬 바꾸기, 현재 ${sort === 'latest' ? '최신순' : '인기순'}`}
          onClick={() => { setSort((value) => (value === 'latest' ? 'popular' : 'latest')); setPage(0); }}
        >
          <SortIcon />
          {sort === 'latest' ? '최신순' : '인기순'}
        </button>
      </div>

      <ul className={styles.postList} aria-busy={loading}>
        {posts.map((post) => (
          <li key={post.id} className={styles.postItem}>
            <div className={styles.postHead}>
              <span className={styles.tagBadge}>{GLOW_COMMUNITY_TAG_LABELS[post.tag]}</span>
              <PostActions
                post={post}
                isMine={Boolean(user && post.authorId === user.id)}
                onDelete={(target) => void handleDelete(target)}
                onReport={(target) => void handleReport(target)}
              />
            </div>
            <p className={styles.postMeta}>
              <span>{post.authorName}</span>
              <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time>
            </p>
            <h3 className={styles.postTitle}>{post.title}</h3>
            <div className={styles.counts}>
              <button
                type="button"
                className={post.liked ? styles.likedButton : undefined}
                aria-pressed={post.liked}
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
          </li>
        ))}
      </ul>

      {!loading && posts.length === 0 ? (
        <p className={styles.empty} role="status">
          {keyword ? `「${keyword}」에 맞는 게시물이 없습니다.` : '아직 등록된 게시물이 없습니다.'}
        </p>
      ) : null}

      <nav className={styles.pagination} aria-label="게시물 페이지">
        <button type="button" className={styles.pageArrow} aria-label="이전 페이지" disabled={page === 0} onClick={() => goToPage(page - 1)}>
          <ChevronIcon direction="left" />
        </button>
        {pageNumbers.map((number) => (
          <button
            key={number}
            type="button"
            className={number === page ? styles.activePage : undefined}
            aria-current={number === page ? 'page' : undefined}
            onClick={() => goToPage(number)}
          >
            {number + 1}
          </button>
        ))}
        <button type="button" className={styles.pageArrow} aria-label="다음 페이지" disabled={page >= totalPages - 1} onClick={() => goToPage(page + 1)}>
          <ChevronIcon direction="right" />
        </button>
      </nav>

      {notice ? <p className={styles.toast} role="status">{notice}</p> : null}

      {writeOpen ? (
        <div className={glowStyles.shareOverlay} role="presentation" onMouseDown={() => setWriteOpen(false)}>
          <form
            className={styles.writeDialog}
            role="dialog"
            aria-modal="true"
            aria-labelledby="community-write-title"
            onMouseDown={(event) => event.stopPropagation()}
            onSubmit={(event) => void handleWriteSubmit(event)}
          >
            <div className={styles.writeHeader}>
              <h2 id="community-write-title">글쓰기</h2>
              <button type="button" aria-label="글쓰기 닫기" onClick={() => setWriteOpen(false)}>×</button>
            </div>
            <fieldset className={styles.writeTags}>
              <legend>태그</legend>
              {(Object.keys(GLOW_COMMUNITY_TAG_LABELS) as GlowCommunityTag[]).map((option) => (
                <label key={option} className={draftTag === option ? styles.activeTag : undefined}>
                  <input
                    type="radio"
                    name="community-tag"
                    value={option}
                    checked={draftTag === option}
                    onChange={() => setDraftTag(option)}
                  />
                  # {GLOW_COMMUNITY_TAG_LABELS[option]}
                </label>
              ))}
            </fieldset>
            <label className={styles.writeField}>
              <span>제목</span>
              <input value={draftTitle} maxLength={100} required onChange={(event) => setDraftTitle(event.target.value)} />
            </label>
            <label className={styles.writeField}>
              <span>내용</span>
              <textarea value={draftContent} rows={7} maxLength={2000} required onChange={(event) => setDraftContent(event.target.value)} />
            </label>
            <div className={styles.writeActions}>
              <button type="button" onClick={() => setWriteOpen(false)}>취소</button>
              <button type="submit" className={styles.writeSubmit} disabled={submitting || !draftTitle.trim() || !draftContent.trim()}>
                {submitting ? '등록 중...' : '등록'}
              </button>
            </div>
          </form>
        </div>
      ) : null}
    </div>
  );
};

export default GlowCommunity;
