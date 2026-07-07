import type { Article, CategoryType } from "./article.types";

export const createArticle = (params: {
  id: string;
  title: string;
  excerpt: string;
  content: string;
  thumbnailUrl: string;
  category: CategoryType;
  volume: number;
  publishDate: Date;
  tags: readonly string[];
  viewCount: number;
  likeCount: number;
  authorName: string;
}): Article => {
  if (!params.id || params.id.trim() === "") {
    throw new Error("Article ID는 필수입니다.");
  }
  if (!params.title || params.title.trim() === "") {
    throw new Error("Article 제목은 필수입니다.");
  }
  if (params.volume < 1) {
    throw new Error("Volume은 1 이상이어야 합니다.");
  }
  return Object.freeze({
    id: params.id,
    title: params.title,
    excerpt: params.excerpt,
    content: params.content,
    thumbnailUrl: params.thumbnailUrl,
    category: params.category,
    volume: params.volume,
    publishDate: params.publishDate,
    tags: Object.freeze([...params.tags]),
    viewCount: params.viewCount,
    likeCount: params.likeCount,
    authorName: params.authorName,
  });
};

export const isPopular = (article: Article): boolean => article.viewCount >= 1000;

export const isRecent = (article: Article): boolean => {
  const thirtyDaysAgo = new Date();
  thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
  return article.publishDate >= thirtyDaysAgo;
};

export const belongsToCategory = (article: Article, category: CategoryType): boolean =>
  article.category === category;

export const hasTag = (article: Article, tag: string): boolean => article.tags.includes(tag);

export const isSameVolume = (article1: Article, article2: Article): boolean =>
  article1.volume === article2.volume;
