/**
 * SearchModal Component
 *
 * 검색 모달 컴포넌트 - 제목, 태그, 카테고리 검색
 */

import { memo, useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useSearchArticles } from '../../hooks/useArticles';
import { LoadingSpinner } from '../LoadingSpinner/LoadingSpinner';
import styles from './SearchModal.module.css';

interface SearchModalProps {
  readonly isOpen: boolean;
  readonly onClose: () => void;
}

// 아이콘 컴포넌트
const SearchIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
    <circle cx="11" cy="11" r="8" />
    <path d="M21 21l-4.35-4.35" />
  </svg>
);

const CloseIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
    <path d="M18 6L6 18M6 6l12 12" />
  </svg>
);

/**
 * SearchModal 컴포넌트
 */
export const SearchModal = memo<SearchModalProps>(({ isOpen, onClose }) => {
  const [inputValue, setInputValue] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);
  const { results, loading, searchTerm, search, clearSearch } = useSearchArticles();

  // 모달 열릴 때 인풋 포커스
  useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isOpen]);

  // ESC 키로 모달 닫기
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  // 배경 스크롤 방지
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }

    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  // 디바운스된 검색
  useEffect(() => {
    const timer = setTimeout(() => {
      search(inputValue);
    }, 300);

    return () => clearTimeout(timer);
  }, [inputValue, search]);

  // 입력 핸들러
  const handleInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(e.target.value);
  }, []);

  // 모달 닫기 및 초기화
  const handleClose = useCallback(() => {
    setInputValue('');
    clearSearch();
    onClose();
  }, [clearSearch, onClose]);

  // 결과 클릭 시 모달 닫기
  const handleResultClick = useCallback(() => {
    handleClose();
  }, [handleClose]);

  // 오버레이 클릭 시 닫기
  const handleOverlayClick = useCallback((e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      handleClose();
    }
  }, [handleClose]);

  return (
    <div
      className={`${styles.overlay} ${isOpen ? styles.overlayOpen : ''}`}
      onClick={handleOverlayClick}
      aria-label="검색 오버레이"
    >
      <div className={styles.modal}>
        {/* 검색 입력 */}
        <div className={styles.searchInputWrapper}>
          <span className={styles.searchIcon}>
            <SearchIcon />
          </span>
          <input
            ref={inputRef}
            type="text"
            className={styles.searchInput}
            placeholder="제목, 태그, 카테고리로 검색..."
            value={inputValue}
            onChange={handleInputChange}
            aria-label="검색어 입력"
          />
          <button
            type="button"
            className={styles.closeButton}
            onClick={handleClose}
            aria-label="검색 닫기"
          >
            <CloseIcon />
          </button>
        </div>

        {/* 결과 영역 */}
        <div className={styles.results}>
          {/* 로딩 */}
          {loading && (
            <div className={styles.loading}>
              <LoadingSpinner text="검색 중..." />
            </div>
          )}

          {/* 검색 전 힌트 */}
          {!loading && !searchTerm && (
            <p className={styles.hint}>
              제목, 태그, 카테고리로 검색할 수 있습니다.
            </p>
          )}

          {/* 검색 결과 없음 */}
          {!loading && searchTerm && results.length === 0 && (
            <p className={styles.noResults}>
              "{searchTerm}"에 대한 검색 결과가 없습니다.
            </p>
          )}

          {/* 검색 결과 */}
          {!loading && results.map(article => (
            <Link
              key={article.id}
              to={`/articles/${article.id}`}
              className={styles.resultItem}
              onClick={handleResultClick}
            >
              <img
                src={article.thumbnailUrl}
                alt=""
                className={styles.resultImage}
                loading="lazy"
              />
              <div className={styles.resultContent}>
                <span className={styles.resultCategory}>
                  {article.categoryLabel}
                </span>
                <h3 className={styles.resultTitle}>{article.title}</h3>
                <div className={styles.resultTags}>
                  {article.tags.slice(0, 3).map(tag => (
                    <span key={tag} className={styles.resultTag}>{tag}</span>
                  ))}
                </div>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
});

SearchModal.displayName = 'SearchModal';

export default SearchModal;
