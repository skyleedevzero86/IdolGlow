import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";
import {
  type RecordContentViewInput,
  recordContentView,
} from "../../shared/data/contentViewApi";
import {
  LOCAL_ONLY_RECENT_VIEW_TYPES,
  RECENT_VIEW_DETAIL_TYPES,
  pushRecentView,
  resolveRecentViewImage,
} from "../../shared/data/recentViewsStore";

export type ContentViewEntry = Omit<RecordContentViewInput, "contentType"> & {
  readonly contentType:
    | RecordContentViewInput["contentType"]
    | "NOTICE"
    | "EVENT";
};

const STORAGE_KEY = "idolglow:content-views-recorded";
const DUPLICATE_WINDOW_MS = 10 * 60 * 1000;
const BLOCKED_PREFIXES = [
  "/admin",
  "/mypage",
  "/notices",
  "/events",
  "/myreviewsfh",
  "/my-",
] as const;

const LOCAL_BLOCKED_PREFIXES = [
  "/admin",
  "/mypage",
  "/myreviewsfh",
  "/my-",
] as const;

const memoryRecorded = new Map<string, number>();

const readRecorded = (): Record<string, number> => {
  try {
    const raw = window.sessionStorage.getItem(STORAGE_KEY);
    const parsed: unknown = raw ? JSON.parse(raw) : {};
    return parsed && typeof parsed === "object"
      ? (parsed as Record<string, number>)
      : {};
  } catch {
    return {};
  }
};

const writeRecorded = (key: string, at: number) => {
  memoryRecorded.set(key, at);
  try {
    const current = readRecorded();
    const now = Date.now();
    const next: Record<string, number> = {};
    for (const [entryKey, value] of Object.entries(current)) {
      if (now - value < DUPLICATE_WINDOW_MS) next[entryKey] = value;
    }
    next[key] = at;
    window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  } catch {
    return;
  }
};

const recentlyRecorded = (key: string): boolean => {
  const now = Date.now();
  const memory = memoryRecorded.get(key);
  if (memory !== undefined && now - memory < DUPLICATE_WINDOW_MS) return true;
  const stored = readRecorded()[key];
  return typeof stored === "number" && now - stored < DUPLICATE_WINDOW_MS;
};

const matchesPrefix = (path: string, prefixes: readonly string[]): boolean => {
  const lower = path.toLowerCase();
  return prefixes.some((prefix) => {
    if (prefix.endsWith("-")) return lower.startsWith(prefix);
    return (
      lower === prefix ||
      lower.startsWith(`${prefix}/`) ||
      lower.startsWith(`${prefix}?`) ||
      lower.startsWith(`${prefix}#`)
    );
  });
};

const isBlockedPath = (path: string): boolean =>
  matchesPrefix(path, BLOCKED_PREFIXES);

const isLocalBlockedPath = (path: string): boolean =>
  matchesPrefix(path, LOCAL_BLOCKED_PREFIXES);

const isServerContentType = (
  contentType: ContentViewEntry["contentType"],
): contentType is RecordContentViewInput["contentType"] =>
  !LOCAL_ONLY_RECENT_VIEW_TYPES.has(contentType);

export function useRecordContentView(entry: ContentViewEntry | null) {
  const { accessToken, authReady, user } = useAuth();
  const contentType = entry?.contentType;
  const contentKey = entry?.contentKey;
  const title = entry?.title;
  const path = entry?.path;
  const imageUrl = entry?.imageUrl;
  const userId = user?.id;

  useEffect(() => {
    if (!contentType || !contentKey || !path) return;
    if (isLocalBlockedPath(path)) return;
    if (!RECENT_VIEW_DETAIL_TYPES.has(contentType)) return;
    pushRecentView({
      contentType,
      contentKey,
      title: title || path,
      path,
      imageUrl: resolveRecentViewImage(imageUrl),
    });
  }, [contentKey, contentType, imageUrl, path, title]);

  useEffect(() => {
    if (!authReady || !accessToken || !userId) return;
    if (!contentType || !contentKey || !path) return;
    if (!isServerContentType(contentType)) return;
    if (isBlockedPath(path)) return;
    const key = `${userId}:${contentType}:${contentKey}`;
    if (recentlyRecorded(key)) return;
    writeRecorded(key, Date.now());
    void recordContentView(
      {
        contentType,
        contentKey,
        title,
        path,
        imageUrl: resolveRecentViewImage(imageUrl),
      },
      accessToken,
    ).catch(() => {
      memoryRecorded.delete(key);
    });
  }, [
    accessToken,
    authReady,
    contentKey,
    contentType,
    imageUrl,
    path,
    title,
    userId,
  ]);
}

const STATIC_CONTENT_ROUTES: Readonly<Record<string, RecordContentViewInput>> =
  {
    "/glow": {
      contentType: "GLOW",
      contentKey: "glow",
      title: "GLOW",
      path: "/glow",
    },
    "/archive": {
      contentType: "ARCHIVE",
      contentKey: "archive",
      title: "아카이브",
      path: "/archive",
    },
    "/subway": {
      contentType: "SUBWAY",
      contentKey: "subway",
      title: "Glow 지하철",
      path: "/subway",
    },
    "/airport-crowd": {
      contentType: "AIRPORT_CROWD",
      contentKey: "airport-crowd",
      title: "Glow 공항인파",
      path: "/airport-crowd",
    },
    "/exchange-rate": {
      contentType: "EXCHANGE_RATE",
      contentKey: "exchange-rate",
      title: "오늘의 환율",
      path: "/exchange-rate",
    },
    "/glow-weather": {
      contentType: "GLOW_WEATHER",
      contentKey: "glow-weather",
      title: "Glow 날씨",
      path: "/glow-weather",
    },
    "/glow_map": {
      contentType: "GLOW_MAP",
      contentKey: "glow-map",
      title: "Glow 지도",
      path: "/glow_map",
    },
  };

export function useStaticContentViewTracker() {
  const location = useLocation();
  const pathname = location.pathname.replace(/\/+$/, "") || "/";
  const hasDetail = new URLSearchParams(location.search).has("post");
  useRecordContentView(
    hasDetail ? null : (STATIC_CONTENT_ROUTES[pathname] ?? null),
  );
}
