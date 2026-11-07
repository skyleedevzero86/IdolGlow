import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  resolveSiteContentImageUrl,
  type HomePopup,
} from '../../../shared/data/siteContentApi';
import styles from './HomePopupModal.module.css';

const STORAGE_KEY = 'idolglow:hidden-home-popups';
const AUTO_SLIDE_MS = 4000;

type HiddenPopupState = Record<string, string>;

const readHiddenPopupState = (): HiddenPopupState => {
  if (typeof window === 'undefined') {
    return {};
  }

  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return {};
    }

    const parsed = JSON.parse(raw) as HiddenPopupState;
    if (!parsed || typeof parsed !== 'object') {
      return {};
    }

    const now = Date.now();
    const nextState = Object.fromEntries(
      Object.entries(parsed).filter(([, expiresAt]) => {
        const timestamp = Number(expiresAt);
        return Number.isFinite(timestamp) && timestamp > now;
      })
    );

    if (Object.keys(nextState).length !== Object.keys(parsed).length) {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(nextState));
    }

    return nextState;
  } catch {
    return {};
  }
};

const popupLink = (popup: HomePopup): string => popup.linkUrl?.trim() ?? '';

const openPopupLink = (popup: HomePopup, navigate: (path: string) => void) => {
  const href = popupLink(popup);
  if (!href) {
    return;
  }
  const target = popup.linkTarget?.trim() || '_self';
  if (target === '_blank') {
    window.open(href, '_blank', 'noopener,noreferrer');
    return;
  }
  if (href.startsWith('/')) {
    navigate(href);
    return;
  }
  window.location.assign(href);
};

const writeHiddenPopupState = (state: HiddenPopupState) => {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
};

export interface HomePopupModalProps {
  readonly popups: readonly HomePopup[];
}

export function HomePopupModal({ popups }: HomePopupModalProps) {
  const navigate = useNavigate();
  const [dismissedIds, setDismissedIds] = useState<ReadonlySet<string>>(() => new Set());
  const [currentIndex, setCurrentIndex] = useState(0);
  const [autoPlay, setAutoPlay] = useState(true);
  const [hideForTodayByPopupId, setHideForTodayByPopupId] = useState<Record<string, boolean>>({});
  const [modalClosed, setModalClosed] = useState(false);
  const [brokenImageIds, setBrokenImageIds] = useState<ReadonlySet<string>>(() => new Set());

  useEffect(() => {
    const hiddenState = readHiddenPopupState();
    setDismissedIds(new Set(Object.keys(hiddenState)));
  }, []);

  const visiblePopups = useMemo(
    () => popups.filter(popup => !dismissedIds.has(popup.popupId)).slice(0, 5),
    [dismissedIds, popups]
  );

  useEffect(() => {
    setModalClosed(false);
  }, [visiblePopups.length]);

  useEffect(() => {
    if (currentIndex <= visiblePopups.length - 1) {
      return;
    }
    setCurrentIndex(0);
  }, [currentIndex, visiblePopups.length]);

  useEffect(() => {
    if (!autoPlay || visiblePopups.length <= 1) {
      return;
    }

    const timer = window.setInterval(() => {
      setCurrentIndex(index => (index + 1) % visiblePopups.length);
    }, AUTO_SLIDE_MS);

    return () => window.clearInterval(timer);
  }, [autoPlay, visiblePopups.length]);

  const popup = visiblePopups[currentIndex];

  if (!popup || modalClosed) {
    return null;
  }

  const popupImageUrl = brokenImageIds.has(popup.popupId) ? null : resolveSiteContentImageUrl(popup.imageUrl);
  const href = popupLink(popup);

  const dismissPopup = (persistForToday: boolean) => {
    const nextIds = new Set(dismissedIds);
    nextIds.add(popup.popupId);
    setDismissedIds(nextIds);

    if (persistForToday) {
      const hiddenState = readHiddenPopupState();
      hiddenState[popup.popupId] = String(Date.now() + 24 * 60 * 60 * 1000);
      writeHiddenPopupState(hiddenState);
    }
  };

  const goToPrevious = () => {
    setCurrentIndex(index => (index - 1 + visiblePopups.length) % visiblePopups.length);
  };

  const goToNext = () => {
    setCurrentIndex(index => (index + 1) % visiblePopups.length);
  };

  const hideForToday = hideForTodayByPopupId[popup.popupId] ?? false;

  const closeModal = () => {
    if (hideForToday) {
      dismissPopup(true);
      setModalClosed(true);
      return;
    }

    setModalClosed(true);
  };

  return (
    <div className={styles.backdrop} role="presentation">
      <section
        className={styles.modal}
        role="dialog"
        aria-modal="true"
        aria-label={`팝업공지 ${popup.title}`}
      >
        <aside className={styles.sidebar}>
          <div className={styles.sidebarHeader}>팝업공지 ({visiblePopups.length}건)</div>
          <div className={styles.sidebarList}>
            {visiblePopups.map((item, index) => (
              <button
                key={item.popupId}
                type="button"
                className={`${styles.sidebarItem} ${index === currentIndex ? styles.sidebarItemActive : ''}`}
                onClick={() => {
                  setCurrentIndex(index);
                  setAutoPlay(true);
                }}
                aria-current={index === currentIndex ? 'true' : undefined}
              >
                {item.title}
              </button>
            ))}
          </div>
          <div className={styles.sidebarFooter}>
            <label className={styles.checkboxRow}>
              <input
                type="checkbox"
                className={styles.checkbox}
                checked={hideForToday}
                onChange={event => {
                  const nextChecked = event.target.checked;
                  setHideForTodayByPopupId(current => ({
                    ...current,
                    [popup.popupId]: nextChecked,
                  }));
                }}
              />
              <span>하루동안 보지 않기</span>
            </label>

            <button
              type="button"
              className={styles.textButton}
              onClick={closeModal}
            >
              닫기
            </button>
          </div>
        </aside>

        <div className={styles.viewer}>
          <div className={styles.viewerFrame}>
            {popupImageUrl ? (
              <img
                src={popupImageUrl}
                alt={popup.title}
                className={`${styles.image} ${href ? styles.imageLink : ''}`}
                role={href ? 'link' : undefined}
                tabIndex={href ? 0 : undefined}
                onClick={() => openPopupLink(popup, navigate)}
                onKeyDown={event => {
                  if (!href || (event.key !== 'Enter' && event.key !== ' ')) {
                    return;
                  }
                  event.preventDefault();
                  openPopupLink(popup, navigate);
                }}
                onError={() => {
                  setBrokenImageIds(current => new Set(current).add(popup.popupId));
                }}
              />
            ) : (
              <div
                className={`${styles.imageFallback} ${href ? styles.imageLink : ''}`}
                onClick={() => openPopupLink(popup, navigate)}
              >
                <h2 className={styles.imageFallbackTitle}>{popup.title}</h2>
                <p>등록된 팝업 이미지가 없습니다.</p>
              </div>
            )}
          </div>

          <div className={styles.controls}>
            <button type="button" className={styles.iconButton} onClick={goToPrevious} aria-label="이전 팝업">
              &#8249;
            </button>
            <button
              type="button"
              className={styles.iconButton}
              onClick={() => setAutoPlay(value => !value)}
              aria-label={autoPlay ? '자동 슬라이드 일시정지' : '자동 슬라이드 재생'}
            >
              {autoPlay ? '||' : '\u25B6'}
            </button>
            <button type="button" className={styles.iconButton} onClick={goToNext} aria-label="다음 팝업">
              &#8250;
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}

export default HomePopupModal;
