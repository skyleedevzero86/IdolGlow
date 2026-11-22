import {
  type FormEvent,
  type ReactNode,
  useCallback,
  useEffect,
  useState,
} from "react";
import { useSearchParams } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import {
  ApiError,
  type OffsetPage,
  apiErrorMessage,
  isAbortError,
} from "../../../shared/data/apiClient";
import { SimplePager } from "../../components/SimplePager/SimplePager";
import styles from "./AdminSubscriptionsPage.module.css";

export const PAGE_SIZE_OPTIONS = [10, 20, 50] as const;
export const DEFAULT_PAGE_SIZE = 20;

export type ParamPatch = Record<string, string | number | null | undefined>;

export type Option<T extends string> = {
  readonly value: T;
  readonly label: string;
};

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

export function parsePageIndex(value: string | null): number {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 1 ? parsed - 1 : 0;
}

export function parsePageSize(value: string | null): number {
  const parsed = Number(value);
  return (PAGE_SIZE_OPTIONS as readonly number[]).includes(parsed)
    ? parsed
    : DEFAULT_PAGE_SIZE;
}

export function parseOption<T extends string>(
  value: string | null,
  options: readonly Option<T>[],
): T | "" {
  return options.find((option) => option.value === value)?.value ?? "";
}

export function parseDate(value: string | null): string {
  return value && DATE_PATTERN.test(value) ? value : "";
}

export function useUrlParams() {
  const [params, setParams] = useSearchParams();

  const update = useCallback(
    (patch: ParamPatch) => {
      setParams((previous) => {
        const next = new URLSearchParams(previous);
        for (const [key, value] of Object.entries(patch)) {
          if (value === null || value === undefined || value === "") {
            next.delete(key);
          } else {
            next.set(key, String(value));
          }
        }
        if (!("page" in patch)) {
          next.delete("page");
        }
        return next;
      });
    },
    [setParams],
  );

  const reset = useCallback(
    (keep: readonly string[], extra: ParamPatch = {}) => {
      setParams((previous) => {
        const next = new URLSearchParams();
        for (const key of keep) {
          const value = previous.get(key);
          if (value) next.set(key, value);
        }
        for (const [key, value] of Object.entries(extra)) {
          if (value === null || value === undefined || value === "") {
            next.delete(key);
          } else {
            next.set(key, String(value));
          }
        }
        return next;
      });
    },
    [setParams],
  );

  return { params, update, reset };
}

type QueryState<T> = {
  readonly data: T | null;
  readonly loading: boolean;
  readonly error: string | null;
};

const describeError = (error: unknown): string => {
  if (error instanceof ApiError && error.status === 403) {
    return "관리자 권한이 있어야 볼 수 있습니다.";
  }
  return apiErrorMessage(error, "데이터를 불러오지 못했습니다.");
};

export function useAdminQuery<T>(
  load: (accessToken: string, signal: AbortSignal) => Promise<T>,
): QueryState<T> {
  const { accessToken } = useAuth();
  const [state, setState] = useState<QueryState<T>>({
    data: null,
    loading: true,
    error: null,
  });

  useEffect(() => {
    if (!accessToken) return;
    const controller = new AbortController();
    setState((previous) => ({ ...previous, loading: true, error: null }));
    load(accessToken, controller.signal)
      .then((data) => setState({ data, loading: false, error: null }))
      .catch((error: unknown) => {
        if (isAbortError(error)) return;
        setState({ data: null, loading: false, error: describeError(error) });
      });
    return () => controller.abort();
  }, [accessToken, load]);

  return state;
}

export function StatusBadge({
  status,
  label,
}: {
  readonly status: string;
  readonly label: string;
}) {
  const tone =
    status === "FAILED" || status === "PARTIAL_FAILED"
      ? styles.badgeDanger
      : status === "SKIPPED" || status === "INACTIVE" || status === "RECORDED"
        ? styles.badgeMuted
        : status === "SENDING"
          ? styles.badgeInfo
          : styles.badgeOk;
  return <span className={`${styles.badge} ${tone}`}>{label}</span>;
}

export function FilterBar({
  keyword,
  keywordPlaceholder,
  onKeywordSubmit,
  onReset,
  canReset,
  children,
}: {
  readonly keyword: string;
  readonly keywordPlaceholder: string;
  readonly onKeywordSubmit: (keyword: string) => void;
  readonly onReset: () => void;
  readonly canReset: boolean;
  readonly children?: ReactNode;
}) {
  const [draft, setDraft] = useState(keyword);

  useEffect(() => {
    setDraft(keyword);
  }, [keyword]);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    onKeywordSubmit(draft.trim());
  };

  return (
    <form className={styles.filterBar} onSubmit={handleSubmit}>
      <div className={styles.searchAttach}>
        <input
          type="search"
          className={styles.searchInput}
          value={draft}
          placeholder={keywordPlaceholder}
          aria-label={keywordPlaceholder}
          onChange={(event) => setDraft(event.target.value)}
        />
        <button type="submit" className={styles.searchButton}>
          검색
        </button>
      </div>
      {children}
      {canReset ? (
        <button type="button" className={styles.resetButton} onClick={onReset}>
          초기화
        </button>
      ) : null}
    </form>
  );
}

export function FilterSelect<T extends string>({
  label,
  allLabel,
  value,
  options,
  onChange,
}: {
  readonly label: string;
  readonly allLabel: string;
  readonly value: T | "";
  readonly options: readonly Option<T>[];
  readonly onChange: (value: T | "") => void;
}) {
  return (
    <select
      className={styles.filterSelect}
      aria-label={label}
      value={value}
      onChange={(event) => onChange(event.target.value as T | "")}
    >
      <option value="">{allLabel}</option>
      {options.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  );
}

export function DateRangeFilter({
  from,
  to,
  onChange,
}: {
  readonly from: string;
  readonly to: string;
  readonly onChange: (patch: { from?: string; to?: string }) => void;
}) {
  return (
    <div className={styles.dateRange}>
      <input
        type="date"
        className={styles.dateInput}
        aria-label="시작일"
        value={from}
        max={to || undefined}
        onChange={(event) => onChange({ from: event.target.value })}
      />
      <span className={styles.dateDash} aria-hidden="true">
        ~
      </span>
      <input
        type="date"
        className={styles.dateInput}
        aria-label="종료일"
        value={to}
        min={from || undefined}
        onChange={(event) => onChange({ to: event.target.value })}
      />
    </div>
  );
}

export function ListStatus({
  loading,
  error,
  hasData,
}: {
  readonly loading: boolean;
  readonly error: string | null;
  readonly hasData: boolean;
}) {
  if (error) {
    return (
      <p className={styles.inlineError} role="alert">
        {error}
      </p>
    );
  }
  if (loading && hasData) {
    return (
      <p className={styles.inlineInfo} aria-live="polite">
        불러오는 중입니다.
      </p>
    );
  }
  return null;
}

export function TableMessageRow({
  colSpan,
  children,
}: {
  readonly colSpan: number;
  readonly children: ReactNode;
}) {
  return (
    <tr>
      <td className={styles.emptyCell} colSpan={colSpan}>
        {children}
      </td>
    </tr>
  );
}

export function ListFooter<T>({
  page,
  pageIndex,
  pageSize,
  onPageChange,
  onSizeChange,
  unit = "건",
}: {
  readonly page: OffsetPage<T> | null;
  readonly pageIndex: number;
  readonly pageSize: number;
  readonly onPageChange: (pageIndex: number) => void;
  readonly onSizeChange: (size: number) => void;
  readonly unit?: string;
}) {
  const total = page?.totalElements ?? 0;
  const totalPages = Math.max(1, page?.totalPages ?? 1);
  return (
    <div className={styles.listFooter}>
      <span className={styles.footerInfo}>
        총 <strong>{total.toLocaleString("ko-KR")}</strong>
        {unit} · {Math.min(pageIndex + 1, totalPages)}/{totalPages} 페이지
      </span>
      <div className={styles.footerPager}>
        <SimplePager
          page={pageIndex}
          totalPages={totalPages}
          onChange={onPageChange}
        />
      </div>
      <select
        className={styles.filterSelect}
        aria-label="페이지당 개수"
        value={pageSize}
        onChange={(event) => onSizeChange(Number(event.target.value))}
      >
        {PAGE_SIZE_OPTIONS.map((size) => (
          <option key={size} value={size}>
            {size}개씩
          </option>
        ))}
      </select>
    </div>
  );
}

export function MailBodyPreview({
  htmlBody,
  textBody,
  kind,
}: {
  readonly htmlBody: string | null;
  readonly textBody: string | null;
  readonly kind?: string;
}) {
  const hasHtml = Boolean(htmlBody?.trim());
  const hasText = Boolean(textBody?.trim());
  const [mode, setMode] = useState<"html" | "text">(hasHtml ? "html" : "text");

  useEffect(() => {
    setMode(hasHtml ? "html" : "text");
  }, [hasHtml]);

  if (kind === "AUTH") {
    return (
      <p className={styles.bodyNotice}>
        {textBody?.trim() ||
          htmlBody?.trim() ||
          "인증 메일 본문은 보안을 위해 저장하지 않습니다."}
      </p>
    );
  }

  if (!hasHtml && !hasText) {
    return <p className={styles.bodyNotice}>저장된 메일 본문이 없습니다.</p>;
  }

  return (
    <div className={styles.bodyPreview}>
      <div className={styles.segment} role="tablist" aria-label="본문 형식">
        <button
          type="button"
          role="tab"
          aria-selected={mode === "html"}
          className={
            mode === "html" ? styles.segmentActive : styles.segmentItem
          }
          disabled={!hasHtml}
          onClick={() => setMode("html")}
        >
          HTML
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={mode === "text"}
          className={
            mode === "text" ? styles.segmentActive : styles.segmentItem
          }
          disabled={!hasText}
          onClick={() => setMode("text")}
        >
          텍스트
        </button>
      </div>
      {mode === "html" && hasHtml ? (
        <iframe
          className={styles.bodyFrame}
          title="메일 본문 미리보기"
          sandbox=""
          referrerPolicy="no-referrer"
          srcDoc={htmlBody ?? ""}
        />
      ) : (
        <pre className={styles.bodyText}>{textBody ?? ""}</pre>
      )}
    </div>
  );
}
