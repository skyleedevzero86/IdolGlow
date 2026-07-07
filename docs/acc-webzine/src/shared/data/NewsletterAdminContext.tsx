import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react';
import { mockNewsletters, type AdminNewsletterItem } from './mockNewsletters';

const STORAGE_KEY = 'idolglow.acc-webzine.newsletters-admin.v1';

export interface NewsletterAdminInput {
  readonly title: string;
  readonly categoryLabel: string;
  readonly publishedAt: string;
  readonly imageUrl: string;
  readonly tags: readonly string[];
  readonly summary: string;
  readonly paragraphs: readonly string[];
}

interface NewsletterAdminContextValue {
  readonly newsletters: readonly AdminNewsletterItem[];
  getNewsletterBySlug: (newsletterSlug: string) => AdminNewsletterItem | undefined;
  createNewsletter: (input: NewsletterAdminInput) => string | null;
  updateNewsletter: (newsletterSlug: string, input: NewsletterAdminInput) => string | null;
  deleteNewsletter: (newsletterSlug: string) => boolean;
}

const NewsletterAdminContext = createContext<NewsletterAdminContextValue | null>(null);

const trimOrEmpty = (value: string | null | undefined): string => value?.trim() ?? '';

const unique = (values: readonly string[]): string[] =>
  Array.from(new Set(values.map(value => trimOrEmpty(value)).filter(Boolean)));

const splitParagraphs = (value: readonly string[]): string[] =>
  value
    .map(item => trimOrEmpty(item))
    .filter(Boolean);

const slugify = (value: string): string => {
  const normalized = value
    .toLowerCase()
    .normalize('NFKD')
    .replace(/[^\w\s-]/g, '')
    .trim()
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-');

  return normalized || `newsletter-${Date.now()}`;
};

const cloneInitialNewsletters = (): AdminNewsletterItem[] =>
  mockNewsletters.map(newsletter => ({
    ...newsletter,
    tags: [...newsletter.tags],
    paragraphs: [...newsletter.paragraphs],
  }));

const sortNewsletters = (newsletters: readonly AdminNewsletterItem[]): AdminNewsletterItem[] =>
  [...newsletters].sort((left, right) => {
    const byDate = right.publishedAt.localeCompare(left.publishedAt);

    if (byDate !== 0) {
      return byDate;
    }

    return right.id - left.id;
  });

const buildNewsletterPayload = (
  newsletters: readonly AdminNewsletterItem[],
  input: NewsletterAdminInput,
  existing?: AdminNewsletterItem
): AdminNewsletterItem => {
  const title = trimOrEmpty(input.title) || '제목 없는 소식지';
  const imageUrl = trimOrEmpty(input.imageUrl);
  const summary = trimOrEmpty(input.summary) || '소식지 요약을 입력해 주세요.';
  const paragraphs = splitParagraphs(input.paragraphs);

  return {
    id: existing?.id ?? Math.max(0, ...newsletters.map(newsletter => newsletter.id)) + 1,
    slug: existing?.slug ?? slugify(title),
    title,
    categoryLabel: trimOrEmpty(input.categoryLabel) || '',
    publishedAt: trimOrEmpty(input.publishedAt) || new Date().toISOString().slice(0, 10).replace(/-/g, '.'),
    imageUrl:
      imageUrl ||
      existing?.imageUrl ||
      'https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&w=1200&q=80',
    tags: unique(input.tags),
    summary,
    paragraphs:
      paragraphs.length > 0 ? paragraphs : [summary || '본문 내용을 입력해 주세요.'],
  };
};

const ensureUniqueSlug = (
  newsletters: readonly AdminNewsletterItem[],
  candidate: AdminNewsletterItem,
  currentSlug?: string
): AdminNewsletterItem => {
  const siblingSlugs = newsletters
    .filter(newsletter => newsletter.slug !== currentSlug)
    .map(newsletter => newsletter.slug);

  if (!siblingSlugs.includes(candidate.slug)) {
    return candidate;
  }

  let suffix = 2;
  let nextSlug = `${candidate.slug}-${suffix}`;

  while (siblingSlugs.includes(nextSlug)) {
    suffix += 1;
    nextSlug = `${candidate.slug}-${suffix}`;
  }

  return {
    ...candidate,
    slug: nextSlug,
  };
};

export const NewsletterAdminProvider = ({ children }: PropsWithChildren) => {
  const [newsletters, setNewsletters] = useState<AdminNewsletterItem[]>(() =>
    sortNewsletters(cloneInitialNewsletters())
  );

  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }

    try {
      const stored = window.localStorage.getItem(STORAGE_KEY);

      if (!stored) {
        return;
      }

      const parsed = JSON.parse(stored) as AdminNewsletterItem[];

      if (Array.isArray(parsed) && parsed.length > 0) {
        setNewsletters(sortNewsletters(parsed));
      }
    } catch {
      setNewsletters(sortNewsletters(cloneInitialNewsletters()));
    }
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }

    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(newsletters));
  }, [newsletters]);

  const value = useMemo<NewsletterAdminContextValue>(() => {
    const getNewsletterBySlug = (newsletterSlug: string) =>
      newsletters.find(newsletter => newsletter.slug === newsletterSlug);

    const createNewsletter = (input: NewsletterAdminInput): string | null => {
      let createdSlug: string | null = null;

      setNewsletters(currentNewsletters => {
        const nextNewsletter = ensureUniqueSlug(
          currentNewsletters,
          buildNewsletterPayload(currentNewsletters, input)
        );
        createdSlug = nextNewsletter.slug;
        return sortNewsletters([nextNewsletter, ...currentNewsletters]);
      });

      return createdSlug;
    };

    const updateNewsletter = (newsletterSlug: string, input: NewsletterAdminInput): string | null => {
      let updatedSlug: string | null = null;

      setNewsletters(currentNewsletters => {
        const currentNewsletter = currentNewsletters.find(
          newsletter => newsletter.slug === newsletterSlug
        );

        if (!currentNewsletter) {
          return currentNewsletters;
        }

        const nextNewsletter = ensureUniqueSlug(
          currentNewsletters,
          buildNewsletterPayload(currentNewsletters, input, currentNewsletter),
          newsletterSlug
        );
        updatedSlug = nextNewsletter.slug;

        return sortNewsletters(
          currentNewsletters.map(newsletter =>
            newsletter.slug === newsletterSlug ? nextNewsletter : newsletter
          )
        );
      });

      return updatedSlug;
    };

    const deleteNewsletter = (newsletterSlug: string): boolean => {
      let deleted = false;

      setNewsletters(currentNewsletters => {
        const nextNewsletters = currentNewsletters.filter(
          newsletter => newsletter.slug !== newsletterSlug
        );
        deleted = nextNewsletters.length !== currentNewsletters.length;
        return nextNewsletters;
      });

      return deleted;
    };

    return {
      newsletters,
      getNewsletterBySlug,
      createNewsletter,
      updateNewsletter,
      deleteNewsletter,
    };
  }, [newsletters]);

  return (
    <NewsletterAdminContext.Provider value={value}>
      {children}
    </NewsletterAdminContext.Provider>
  );
};

export const useNewsletterAdmin = (): NewsletterAdminContextValue => {
  const context = useContext(NewsletterAdminContext);

  if (!context) {
    throw new Error('useNewsletterAdmin must be used within a NewsletterAdminProvider');
  }

  return context;
};
