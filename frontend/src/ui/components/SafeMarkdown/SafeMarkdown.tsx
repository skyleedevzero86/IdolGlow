import { useMemo } from "react";
import { renderSafeMarkdown } from "../../../shared/markdown/safeMarkdown";
import styles from "./SafeMarkdown.module.css";

type SafeMarkdownProps = {
  readonly markdown: string;
  readonly className?: string;
  readonly compact?: boolean;
  readonly emptyText?: string;
};

export function SafeMarkdown({
  markdown,
  className,
  compact = false,
  emptyText,
}: SafeMarkdownProps) {
  const html = useMemo(() => renderSafeMarkdown(markdown), [markdown]);
  const classes = [styles.body, compact ? styles.compact : "", className]
    .filter(Boolean)
    .join(" ");

  if (!html) {
    return emptyText ? <p className={styles.empty}>{emptyText}</p> : null;
  }

  return (
    <div
      className={classes}
      // biome-ignore lint/security/noDangerouslySetInnerHtml: renderSafeMarkdown sanitizes tags, attributes and URLs
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}

export default SafeMarkdown;
