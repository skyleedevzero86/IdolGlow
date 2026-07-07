import { createArticle } from "../domain/Article";
import type {
  Article,
  ArticleDTO,
  ArticleDetailViewModel,
  ArticleViewModel,
  CategoryType,
} from "../domain/article.types";

const toCategoryType = (category: string): CategoryType => {
  const categoryMap: Record<string, CategoryType> = {
    exhibition: "exhibition",
    performance: "performance",
    exchange: "exchange",
    archive: "archive",
    video: "video",
    event: "event",
    전시: "exhibition",
    공연: "performance",
    "행사·교류": "exchange",
    아카이브: "archive",
    비디오: "video",
    이벤트: "event",
  };
  return categoryMap[category.toLowerCase()] || "exhibition";
};

const getCategoryLabel = (category: CategoryType): string => {
  const labels: Record<CategoryType, string> = {
    exhibition: "전시",
    performance: "공연",
    exchange: "행사·교류",
    archive: "아카이브",
    video: "비디오",
    event: "이벤트",
  };
  return labels[category];
};

const formatCount = (count: number): string => {
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}만`;
  }
  if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}천`;
  }
  return count.toLocaleString("ko-KR");
};

const formatDate = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}.${month}.${day}`;
};

export const mapDTOToEntity = (dto: ArticleDTO): Article => {
  return createArticle({
    id: dto.id,
    title: dto.title,
    excerpt: dto.excerpt,
    content: dto.content,
    thumbnailUrl: dto.thumbnailUrl,
    category: toCategoryType(dto.category),
    volume: dto.volume,
    publishDate: new Date(dto.publishDate),
    tags: dto.tags,
    viewCount: dto.viewCount,
    likeCount: dto.likeCount,
    authorName: dto.authorName,
  });
};

export const mapEntityToViewModel = (entity: Article): ArticleViewModel => ({
  id: entity.id,
  title: entity.title,
  excerpt: entity.excerpt,
  thumbnailUrl: entity.thumbnailUrl,
  categoryLabel: getCategoryLabel(entity.category),
  categoryType: entity.category,
  volumeLabel: `Vol. ${entity.volume}`,
  publishDateFormatted: formatDate(entity.publishDate),
  tags: entity.tags,
  viewCountFormatted: formatCount(entity.viewCount),
  likeCountFormatted: formatCount(entity.likeCount),
});

export const mapEntityToDetailViewModel = (entity: Article): ArticleDetailViewModel => ({
  ...mapEntityToViewModel(entity),
  content: entity.content,
  authorName: entity.authorName,
});

export const mapDTOsToViewModels = (dtos: readonly ArticleDTO[]): ArticleViewModel[] =>
  dtos.map(mapDTOToEntity).map(mapEntityToViewModel);
