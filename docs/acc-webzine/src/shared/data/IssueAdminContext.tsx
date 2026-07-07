import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react';
import {
  mockIssueFeed,
  type IssueArticle,
  type IssueArticleSection,
  type IssueCategoryKey,
  type IssueVolume,
  type RelatedIssueContent,
} from './mockIssues';

const STORAGE_KEY = 'idolglow.acc-webzine.issue-admin.v1';

export interface ManagedIssueArticle extends IssueArticle {
  readonly galleryImageUrls: readonly string[];
}

export interface ManagedIssueVolume extends Omit<IssueVolume, 'articles'> {
  readonly articles: readonly ManagedIssueArticle[];
}

export interface IssueAdminSectionInput {
  readonly heading: string;
  readonly body: string;
  readonly note: string;
}

export interface IssueAdminArticleInput {
  readonly title: string;
  readonly kicker: string;
  readonly summary: string;
  readonly category: IssueCategoryKey;
  readonly formatLabel: string;
  readonly heroImageUrl: string;
  readonly cardImageUrl: string;
  readonly galleryImageUrls: readonly string[];
  readonly tags: readonly string[];
  readonly authorName: string;
  readonly authorEmail: string;
  readonly creditLine: string;
  readonly highlightQuote: string;
  readonly sections: readonly IssueAdminSectionInput[];
}

export interface IssueAdminIssueInput {
  readonly volume: number;
  readonly issueDate: string;
  readonly coverImageUrl: string;
  readonly teaser: string;
}

interface IssueAdminContextValue {
  readonly issues: readonly ManagedIssueVolume[];
  readonly latestVolume: number;
  getIssueBySlug: (issueSlug: string) => ManagedIssueVolume | undefined;
  getArticleBySlug: (
    issueSlug: string,
    articleSlug: string
  ) => ManagedIssueArticle | undefined;
  createIssue: (input: IssueAdminIssueInput) => string | null;
  createArticle: (issueSlug: string, input: IssueAdminArticleInput) => string | null;
  updateArticle: (
    issueSlug: string,
    articleSlug: string,
    input: IssueAdminArticleInput
  ) => string | null;
  deleteArticle: (issueSlug: string, articleSlug: string) => boolean;
}

const IssueAdminContext = createContext<IssueAdminContextValue | null>(null);

const trimOrEmpty = (value: string | undefined | null): string => value?.trim() ?? '';

const unique = (values: readonly string[]): string[] =>
  Array.from(new Set(values.map(value => trimOrEmpty(value)).filter(Boolean)));

const splitParagraphs = (body: string): string[] =>
  body
    .split(/\n{2,}/)
    .map(paragraph => paragraph.trim())
    .filter(Boolean);

const normalizeTags = (tags: readonly string[]): string[] =>
  unique(
    tags.flatMap(tag =>
      tag
        .split(',')
        .map(item => item.replace(/^#/, '').trim())
        .filter(Boolean)
    )
  );

const normalizeSections = (sections: readonly IssueAdminSectionInput[]): IssueArticleSection[] => {
  const normalized = sections.reduce<IssueArticleSection[]>((accumulator, section, index) => {
      const paragraphs = splitParagraphs(section.body);

      if (paragraphs.length === 0) {
        return accumulator;
      }

      accumulator.push({
        id: `section-${Date.now()}-${index + 1}`,
        heading: trimOrEmpty(section.heading) || undefined,
        paragraphs,
        note: trimOrEmpty(section.note) || undefined,
      });

      return accumulator;
    }, []);

  return normalized.length > 0
    ? normalized
    : [
        {
          id: `section-${Date.now()}-1`,
          paragraphs: ['본문 내용을 입력해 주세요.'],
        },
      ];
};

const slugify = (value: string): string => {
  const normalized = value
    .toLowerCase()
    .normalize('NFKD')
    .replace(/[^\w\s-]/g, '')
    .trim()
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-');

  return normalized || `article-${Date.now()}`;
};

const createManagedIssues = (issues: readonly IssueVolume[]): ManagedIssueVolume[] =>
  issues.map(issue => ({
    ...issue,
    articles: issue.articles.map(article => ({
      ...article,
      galleryImageUrls: unique([
        article.heroImageUrl,
        article.cardImageUrl,
        issue.coverImageUrl,
        ...article.relatedContents.map(content => content.imageUrl),
      ]),
    })),
  }));

const cloneInitialIssues = (): ManagedIssueVolume[] => createManagedIssues(mockIssueFeed);

const sortIssues = (issues: readonly ManagedIssueVolume[]): ManagedIssueVolume[] =>
  [...issues].sort((left, right) => {
    if (right.volume !== left.volume) {
      return right.volume - left.volume;
    }

    return right.issueDate.localeCompare(left.issueDate);
  });

const buildIssuePayload = (input: IssueAdminIssueInput): ManagedIssueVolume => ({
  id: `issue-vol-${input.volume}`,
  slug: `vol-${input.volume}`,
  volume: input.volume,
  issueDate: trimOrEmpty(input.issueDate),
  coverImageUrl: trimOrEmpty(input.coverImageUrl),
  teaser: trimOrEmpty(input.teaser) || `Vol.${input.volume} 소개 문구를 입력해 주세요.`,
  articles: [],
});

const buildRelatedContents = (
  issue: ManagedIssueVolume,
  excludeSlug: string,
  fallbackImageUrl: string
): RelatedIssueContent[] => {
  const related = issue.articles
    .filter(article => article.slug !== excludeSlug)
    .slice(0, 3)
    .map(article => ({
      id: `related-${article.id}`,
      title: article.title,
      category: article.category,
      imageUrl: article.cardImageUrl || article.heroImageUrl || fallbackImageUrl,
    }));

  if (related.length > 0) {
    return related;
  }

  return [
    {
      id: `related-${issue.id}-fallback`,
      title: `${issue.volume}호 다른 기사도 준비해 보세요`,
      category: 'article',
      imageUrl: fallbackImageUrl,
    },
  ];
};

const buildArticlePayload = (
  issue: ManagedIssueVolume,
  input: IssueAdminArticleInput,
  existingArticle?: ManagedIssueArticle
): ManagedIssueArticle => {
  const title = trimOrEmpty(input.title) || '제목 없는 기사';
  const heroImageUrl = trimOrEmpty(input.heroImageUrl) || issue.coverImageUrl;
  const cardImageUrl = trimOrEmpty(input.cardImageUrl) || heroImageUrl;
  const galleryImageUrls = unique([
    heroImageUrl,
    cardImageUrl,
    ...input.galleryImageUrls,
    issue.coverImageUrl,
  ]);
  const tags = normalizeTags(input.tags);
  const sections = normalizeSections(input.sections);
  const summary =
    trimOrEmpty(input.summary) ||
    sections[0]?.paragraphs.join(' ').slice(0, 140) ||
    '요약을 입력해 주세요.';

  return {
    id: existingArticle?.id ?? `article-${Date.now()}`,
    slug: existingArticle?.slug ?? slugify(title),
    issueSlug: issue.slug,
    volume: issue.volume,
    issueDate: issue.issueDate,
    title,
    kicker: trimOrEmpty(input.kicker) || `Vol.${issue.volume} 기사`,
    summary,
    heroImageUrl,
    cardImageUrl,
    category: input.category,
    formatLabel: trimOrEmpty(input.formatLabel) || '아티클',
    tags,
    authorName: trimOrEmpty(input.authorName) || '관리자',
    authorEmail: trimOrEmpty(input.authorEmail) || 'admin@idolglow.local',
    creditLine: trimOrEmpty(input.creditLine) || 'Photo Idol Glow Archive',
    highlightQuote: trimOrEmpty(input.highlightQuote) || undefined,
    sections,
    relatedContents:
      existingArticle?.relatedContents ?? buildRelatedContents(issue, existingArticle?.slug ?? '', cardImageUrl),
    galleryImageUrls,
  };
};

const ensureUniqueSlug = (
  issue: ManagedIssueVolume,
  article: ManagedIssueArticle,
  currentArticleSlug?: string
): ManagedIssueArticle => {
  const siblingSlugs = issue.articles
    .filter(item => item.slug !== currentArticleSlug)
    .map(item => item.slug);

  if (!siblingSlugs.includes(article.slug)) {
    return article;
  }

  let suffix = 2;
  let nextSlug = `${article.slug}-${suffix}`;

  while (siblingSlugs.includes(nextSlug)) {
    suffix += 1;
    nextSlug = `${article.slug}-${suffix}`;
  }

  return {
    ...article,
    slug: nextSlug,
  };
};

export const IssueAdminProvider = ({ children }: PropsWithChildren) => {
  const [issues, setIssues] = useState<ManagedIssueVolume[]>(() => sortIssues(cloneInitialIssues()));

  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }

    try {
      const stored = window.localStorage.getItem(STORAGE_KEY);

      if (!stored) {
        return;
      }

      const parsed = JSON.parse(stored) as IssueVolume[];

      if (Array.isArray(parsed) && parsed.length > 0) {
        setIssues(sortIssues(createManagedIssues(parsed)));
      }
    } catch {
      setIssues(sortIssues(cloneInitialIssues()));
    }
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }

    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(issues));
  }, [issues]);

  const value = useMemo<IssueAdminContextValue>(() => {
    const getIssueBySlug = (issueSlug: string) => issues.find(issue => issue.slug === issueSlug);

    const getArticleBySlug = (issueSlug: string, articleSlug: string) =>
      getIssueBySlug(issueSlug)?.articles.find(article => article.slug === articleSlug);

    const createIssue = (input: IssueAdminIssueInput): string | null => {
      const nextVolume = Number(input.volume);

      if (!Number.isFinite(nextVolume) || nextVolume <= 0) {
        return null;
      }

      const nextSlug = `vol-${nextVolume}`;
      const alreadyExists = issues.some(
        issue => issue.volume === nextVolume || issue.slug === nextSlug
      );

      if (alreadyExists) {
        return null;
      }

      setIssues(currentIssues => sortIssues([...currentIssues, buildIssuePayload(input)]));
      return nextSlug;
    };

    const createArticle = (issueSlug: string, input: IssueAdminArticleInput): string | null => {
      let createdSlug: string | null = null;

      setIssues(currentIssues =>
        currentIssues.map(issue => {
          if (issue.slug !== issueSlug) {
            return issue;
          }

          const nextArticle = ensureUniqueSlug(issue, buildArticlePayload(issue, input));
          createdSlug = nextArticle.slug;

          return {
            ...issue,
            articles: [nextArticle, ...issue.articles],
          };
        })
      );

      return createdSlug;
    };

    const updateArticle = (
      issueSlug: string,
      articleSlug: string,
      input: IssueAdminArticleInput
    ): string | null => {
      let updatedSlug: string | null = null;

      setIssues(currentIssues =>
        currentIssues.map(issue => {
          if (issue.slug !== issueSlug) {
            return issue;
          }

          const currentArticle = issue.articles.find(article => article.slug === articleSlug);

          if (!currentArticle) {
            return issue;
          }

          const nextArticle = ensureUniqueSlug(
            issue,
            buildArticlePayload(issue, input, currentArticle),
            articleSlug
          );

          updatedSlug = nextArticle.slug;

          return {
            ...issue,
            articles: issue.articles.map(article =>
              article.slug === articleSlug ? nextArticle : article
            ),
          };
        })
      );

      return updatedSlug;
    };

    const deleteArticle = (issueSlug: string, articleSlug: string): boolean => {
      let deleted = false;

      setIssues(currentIssues =>
        currentIssues.map(issue => {
          if (issue.slug !== issueSlug) {
            return issue;
          }

          const filteredArticles = issue.articles.filter(article => article.slug !== articleSlug);
          deleted = filteredArticles.length !== issue.articles.length;

          return {
            ...issue,
            articles: filteredArticles,
          };
        })
      );

      return deleted;
    };

    return {
      issues,
      latestVolume: issues[0]?.volume ?? 0,
      getIssueBySlug,
      getArticleBySlug,
      createIssue,
      createArticle,
      updateArticle,
      deleteArticle,
    };
  }, [issues]);

  return <IssueAdminContext.Provider value={value}>{children}</IssueAdminContext.Provider>;
};

export const useIssueAdmin = (): IssueAdminContextValue => {
  const context = useContext(IssueAdminContext);

  if (!context) {
    throw new Error('useIssueAdmin must be used within an IssueAdminProvider');
  }

  return context;
};
