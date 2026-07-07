/**
 * SortFilter Component
 *
 * 정렬 옵션 필터 컴포넌트
 */

import { memo, useCallback, type ReactNode } from 'react';
import type { SortOption } from "../../../domains/article/domain/article.types";
import styles from './SortFilter.module.css';

interface SortFilterProps {
  readonly currentSort: SortOption;
  readonly onSortChange: (sort: SortOption) => void;
  readonly title?: string;
  /** 제목 오른쪽 · 정렬 버튼(최신순 등) 바로 왼쪽에 붙는 슬롯 */
  readonly toolbarExtra?: ReactNode;
}

// 정렬 옵션 정의
const SORT_OPTIONS: readonly { value: SortOption; label: string }[] = [
  { value: 'latest', label: '최신순' },
  { value: 'popular', label: '인기상품' },
  { value: 'likes', label: '공감 순' },
];

/**
 * SortFilter 컴포넌트
 */
export const SortFilter = memo<SortFilterProps>(({
  currentSort,
  onSortChange,
  title = 'CONTENTS',
  toolbarExtra,
}) => {
  // 버튼 클릭 핸들러
  const handleClick = useCallback((sort: SortOption) => {
    onSortChange(sort);
  }, [onSortChange]);

  return (
    <fieldset className={styles.container} aria-label="정렬 옵션">
      <h2 className={styles.title}>{title}</h2>

      <div className={styles.toolbarEnd}>
        {toolbarExtra ? <div className={styles.toolbarExtra}>{toolbarExtra}</div> : null}
        <div className={styles.buttons}>
          {SORT_OPTIONS.map(option => (
            <button
              key={option.value}
              type="button"
              className={`${styles.button} ${currentSort === option.value ? styles.buttonActive : ''}`}
              onClick={() => handleClick(option.value)}
              aria-pressed={currentSort === option.value}
            >
              {option.label}
            </button>
          ))}
        </div>
      </div>
    </fieldset>
  );
});

SortFilter.displayName = 'SortFilter';

export default SortFilter;
