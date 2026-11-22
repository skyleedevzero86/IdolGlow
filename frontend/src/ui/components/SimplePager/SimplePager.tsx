import styles from "./SimplePager.module.css";

type SimplePagerProps = {
  readonly page: number;
  readonly totalPages: number;
  readonly onChange: (page: number) => void;
  readonly label?: string;
  readonly groupSize?: number;
};

const Chevron = ({ direction }: { readonly direction: "left" | "right" }) => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    aria-hidden="true"
  >
    <polyline
      points={direction === "left" ? "15 18 9 12 15 6" : "9 18 15 12 9 6"}
    />
  </svg>
);

export function SimplePager({
  page,
  totalPages,
  onChange,
  label = "페이지",
  groupSize = 5,
}: SimplePagerProps) {
  const pages = Math.max(1, totalPages);
  if (pages <= 1) return null;
  const groupStart = Math.floor(page / groupSize) * groupSize;
  const groupEnd = Math.min(pages, groupStart + groupSize);
  const numbers = Array.from(
    { length: groupEnd - groupStart },
    (_, index) => groupStart + index,
  );

  return (
    <nav className={styles.pager} aria-label={label}>
      <button
        type="button"
        className={styles.arrow}
        aria-label="이전 페이지"
        disabled={page <= 0}
        onClick={() => onChange(page - 1)}
      >
        <Chevron direction="left" />
      </button>
      {numbers.map((number) => (
        <button
          key={number}
          type="button"
          className={number === page ? styles.active : styles.number}
          aria-current={number === page ? "page" : undefined}
          onClick={() => onChange(number)}
        >
          {number + 1}
        </button>
      ))}
      <button
        type="button"
        className={styles.arrow}
        aria-label="다음 페이지"
        disabled={page >= pages - 1}
        onClick={() => onChange(page + 1)}
      >
        <Chevron direction="right" />
      </button>
    </nav>
  );
}

export default SimplePager;
