/**
 * LoadingSpinner Component
 */

import { memo } from 'react';
import styles from './LoadingSpinner.module.css';

interface LoadingSpinnerProps {
  readonly text?: string;
}

export const LoadingSpinner = memo<LoadingSpinnerProps>(({ text = '로딩 중...' }) => {
  return (
    <output className={styles.container} aria-live="polite">
      <div className={styles.spinner} aria-hidden="true" />
      <span className={styles.text}>{text}</span>
    </output>
  );
});

LoadingSpinner.displayName = 'LoadingSpinner';

export default LoadingSpinner;
