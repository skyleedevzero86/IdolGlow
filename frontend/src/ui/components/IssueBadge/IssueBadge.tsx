import type { IssueCategoryKey } from '../../../shared/data/mockIssues';
import { ISSUE_CATEGORY_LABELS } from '../../../shared/data/mockIssues';
import styles from './IssueBadge.module.css';

interface IssueBadgeProps {
  readonly category: IssueCategoryKey;
  readonly className?: string;
}

export const IssueBadge = ({ category, className }: IssueBadgeProps) => {
  const composedClassName = [styles.badge, styles[category], className].filter(Boolean).join(' ');

  return <span className={composedClassName}>{ISSUE_CATEGORY_LABELS[category]}</span>;
};

export default IssueBadge;
