import { getApiBaseUrl } from "../../auth/authConfig";

export type RecentViewEntry = {
  readonly contentType: string;
  readonly contentKey: string;
  readonly title: string;
  readonly path: string;
  readonly imageUrl?: string | null;
  readonly viewedAt: string;
};

export const RECENT_VIEWS_STORAGE_KEY = "idolglow.recentViews.v2";
export const RECENT_VIEWS_CHANGED_EVENT = "idolglow:recent-views-changed";
export const LOCAL_ONLY_RECENT_VIEW_TYPES: ReadonlySet<string> = new Set([
  "NOTICE",
  "EVENT",
]);
export const RECENT_VIEW_DETAIL_TYPES: ReadonlySet<string> = new Set([
  "PRODUCT",
  "ARTICLE",
  "GLOW_COMMUNITY_POST",
  "EVENT_INFO",
  ...LOCAL_ONLY_RECENT_VIEW_TYPES,
]);

const LEGACY_STORAGE_KEYS = ["idolglow.recentViews"] as const;
const MAX_ENTRIES = 10;

const RECENT_VIEW_TYPE_LABELS: Readonly<Record<string, string>> = {
  NOTICE: "소식",
  EVENT: "이벤트",
};

export const recentViewTypeLabel = (value: string): string | null =>
  RECENT_VIEW_TYPE_LABELS[value] ?? null;

let legacyCleared = false;

const clearLegacyStorage = () => {
  if (legacyCleared) return;
  legacyCleared = true;
  try {
    for (const key of LEGACY_STORAGE_KEYS) {
      window.localStorage.removeItem(key);
    }
  } catch {
    return;
  }
};

const entryIdentity = (
  entry: Pick<RecentViewEntry, "contentType" | "contentKey">,
) => `${entry.contentType}:${entry.contentKey}`;

const dedupeEntries = (
  entries: readonly RecentViewEntry[],
): RecentViewEntry[] => {
  const seenPaths = new Set<string>();
  const seenKeys = new Set<string>();
  const result: RecentViewEntry[] = [];
  for (const entry of entries) {
    const identity = entryIdentity(entry);
    if (seenPaths.has(entry.path) || seenKeys.has(identity)) continue;
    seenPaths.add(entry.path);
    seenKeys.add(identity);
    result.push(entry);
  }
  return result;
};

const isEntry = (value: unknown): value is RecentViewEntry => {
  if (!value || typeof value !== "object") return false;
  const entry = value as Record<string, unknown>;
  return (
    typeof entry.contentType === "string" &&
    typeof entry.contentKey === "string" &&
    typeof entry.title === "string" &&
    typeof entry.path === "string" &&
    typeof entry.viewedAt === "string"
  );
};

const isSafePath = (path: string): boolean =>
  path.startsWith("/") && !path.startsWith("//");

export function readRecentViews(): RecentViewEntry[] {
  clearLegacyStorage();
  try {
    const raw = window.localStorage.getItem(RECENT_VIEWS_STORAGE_KEY);
    const parsed: unknown = raw ? JSON.parse(raw) : [];
    if (!Array.isArray(parsed)) return [];
    return dedupeEntries(
      parsed
        .filter(isEntry)
        .filter(
          (entry) =>
            RECENT_VIEW_DETAIL_TYPES.has(entry.contentType) &&
            isSafePath(entry.path),
        ),
    ).slice(0, MAX_ENTRIES);
  } catch {
    return [];
  }
}

const writeRecentViews = (entries: readonly RecentViewEntry[]) => {
  try {
    window.localStorage.setItem(
      RECENT_VIEWS_STORAGE_KEY,
      JSON.stringify(entries.slice(0, MAX_ENTRIES)),
    );
  } catch {
    return;
  }
  try {
    window.dispatchEvent(new CustomEvent(RECENT_VIEWS_CHANGED_EVENT));
  } catch {
    return;
  }
};

export function pushRecentView(
  entry: Omit<RecentViewEntry, "viewedAt"> & { readonly viewedAt?: string },
): void {
  if (!RECENT_VIEW_DETAIL_TYPES.has(entry.contentType)) return;
  if (!isSafePath(entry.path)) return;
  const next: RecentViewEntry = {
    contentType: entry.contentType,
    contentKey: entry.contentKey,
    title: entry.title.trim() || entry.path,
    path: entry.path,
    imageUrl: entry.imageUrl || null,
    viewedAt: entry.viewedAt ?? new Date().toISOString(),
  };
  writeRecentViews(dedupeEntries([next, ...readRecentViews()]));
}

export function seedRecentViews(entries: readonly RecentViewEntry[]): void {
  if (readRecentViews().length > 0) return;
  const filtered = dedupeEntries(
    entries.filter(
      (entry) =>
        RECENT_VIEW_DETAIL_TYPES.has(entry.contentType) &&
        !LOCAL_ONLY_RECENT_VIEW_TYPES.has(entry.contentType) &&
        isSafePath(entry.path),
    ),
  );
  if (filtered.length > 0) writeRecentViews(filtered);
}

export function resolveRecentViewImage(
  url: string | null | undefined,
): string | null {
  const trimmed = url?.trim();
  if (!trimmed) return null;
  if (/^https?:\/\//i.test(trimmed)) return trimmed;
  if (trimmed.startsWith("/") && !trimmed.startsWith("//")) {
    return trimmed.startsWith("/uploads")
      ? `${getApiBaseUrl()}${trimmed}`
      : trimmed;
  }
  return null;
}

export function firstMarkdownImage(
  markdown: string | null | undefined,
): string | null {
  const match = /!\[[^\]]*\]\(([^)\s]+)\)/.exec(markdown ?? "");
  return match ? resolveRecentViewImage(match[1]) : null;
}
