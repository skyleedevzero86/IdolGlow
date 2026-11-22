import { useCallback, useEffect, useMemo, useState } from "react";
import { useAuth } from "../../auth/AuthContext";
import type {
  ArticleViewModel,
  CategoryType,
  SortOption,
} from "../../domains/article/domain/article.types";
import {
  type IdolglowRankedArticle,
  fetchIdolglowArticleCards,
  idolglowArticlePath,
} from "../../shared/data/idolglowPublicApi";
import type { VolumeSectionCard } from "../components/VolumeSection/VolumeSection";

const PAGE_SIZE = 12;

const CATEGORY_TYPES: Readonly<Record<string, CategoryType>> = {
  exhibition: "exhibition",
  performance: "performance",
  forum: "exchange",
  event: "event",
  article: "archive",
  video: "video",
};

export type IdolglowHomeGroup = {
  readonly key: string;
  readonly volume: number | null;
  readonly date?: string;
  readonly cards: readonly VolumeSectionCard[];
};

const toCard = (article: IdolglowRankedArticle): VolumeSectionCard => {
  const view: ArticleViewModel = {
    id: article.likeTargetKey,
    title: article.title,
    excerpt: article.summary,
    thumbnailUrl: article.cardImageUrl,
    categoryLabel: article.categoryLabel,
    categoryType: CATEGORY_TYPES[article.category] ?? "archive",
    volumeLabel: `Vol. ${article.volume}`,
    publishDateFormatted: article.issueDate,
    tags: article.tags,
    viewCountFormatted: article.viewerCount.toLocaleString(),
    likeCount: article.likeCount,
    likeCountFormatted: article.likeCount.toLocaleString(),
  };
  return {
    article: view,
    linkTo: idolglowArticlePath(article.issueSlug, article.articleSlug),
    likeTarget: {
      type: article.likeTargetType,
      key: article.likeTargetKey,
      liked: article.liked,
    },
  };
};

const groupByVolume = (
  articles: readonly IdolglowRankedArticle[],
): IdolglowHomeGroup[] => {
  const groups: { volume: number; date: string; cards: VolumeSectionCard[] }[] =
    [];
  for (const article of articles) {
    const last = groups[groups.length - 1];
    if (last && last.volume === article.volume) {
      last.cards.push(toCard(article));
    } else {
      groups.push({
        volume: article.volume,
        date: article.issueDate,
        cards: [toCard(article)],
      });
    }
  }
  return groups.map((group) => ({ key: `vol-${group.volume}`, ...group }));
};

export function useIdolglowHomeArticles(sort: SortOption) {
  const { accessToken, authReady, user } = useAuth();
  const token = user ? accessToken : null;
  const [articles, setArticles] = useState<readonly IdolglowRankedArticle[]>(
    [],
  );
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (!authReady) return;
    const controller = new AbortController();
    setLoading(true);
    setError(false);
    fetchIdolglowArticleCards(sort, 0, PAGE_SIZE, token, controller.signal)
      .then((result) => {
        setArticles(result.items);
        setPage(0);
        setHasNext(result.hasNext);
      })
      .catch(() => {
        if (controller.signal.aborted) return;
        setArticles([]);
        setHasNext(false);
        setError(true);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [authReady, sort, token]);

  const loadMore = useCallback(async () => {
    if (loadingMore || !hasNext) return;
    setLoadingMore(true);
    try {
      const next = await fetchIdolglowArticleCards(
        sort,
        page + 1,
        PAGE_SIZE,
        token,
      );
      setArticles((current) => {
        const seen = new Set(current.map((item) => item.likeTargetKey));
        return [
          ...current,
          ...next.items.filter((item) => !seen.has(item.likeTargetKey)),
        ];
      });
      setPage(next.page);
      setHasNext(next.hasNext);
    } catch {
      setHasNext(true);
    } finally {
      setLoadingMore(false);
    }
  }, [hasNext, loadingMore, page, sort, token]);

  const groups = useMemo<readonly IdolglowHomeGroup[]>(
    () =>
      sort === "latest"
        ? groupByVolume(articles)
        : articles.length > 0
          ? [{ key: `rank-${sort}`, volume: null, cards: articles.map(toCard) }]
          : [],
    [articles, sort],
  );

  return { groups, loading, error, hasNext, loadingMore, loadMore };
}
