/**
 * 나의 아카이브 — 로그인 사용자용 (헤더 하위 메뉴와 동일 탭)
 */

import { useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import styles from './MyArchivePage.module.css';

type ArchiveTab = 'all' | 'saved' | 'recent';

function parseTab(raw: string | null): ArchiveTab {
  if (raw === 'saved' || raw === 'recent') return raw;
  return 'all';
}

export const MyArchivePage = () => {
  const [searchParams] = useSearchParams();
  const tab = useMemo(() => parseTab(searchParams.get('tab')), [searchParams]);

  const title =
    tab === 'saved' ? '저장 모음' : tab === 'recent' ? '최근 본' : '전체';

  return (
    <AdminMarketingShell
      currentPath="/my-archive"
      title="나의 이야기"
      description="저장한 글과 최근 본 기사를 한곳에서 모아봅니다."
      statusText="GLOW 추천에서 이어지는 개인 보관함"
      classNames={{
        toolbarCard: styles.flatToolbar,
      }}
      stats={[
        { label: '현재 보기', value: title },
        { label: '저장 모음', value: '0' },
        { label: '최근 본', value: '0' },
      ]}
    >
      <section className={`${shellStyles.panel} ${styles.archivePanel}`} aria-label="나의 아카이브">
        <nav className={styles.tabNav} aria-label="나의 아카이브 구분">
          <ul className={styles.tabList}>
            <li>
              <Link
                to="/my-archive"
                className={tab === 'all' ? styles.tabActive : styles.tab}
                aria-current={tab === 'all' ? 'page' : undefined}
              >
                전체
              </Link>
            </li>
            <li>
              <Link
                to="/my-archive?tab=saved"
                className={tab === 'saved' ? styles.tabActive : styles.tab}
                aria-current={tab === 'saved' ? 'page' : undefined}
              >
                저장 모음
              </Link>
            </li>
            <li>
              <Link
                to="/my-archive?tab=recent"
                className={tab === 'recent' ? styles.tabActive : styles.tab}
                aria-current={tab === 'recent' ? 'page' : undefined}
              >
                최근 본
              </Link>
            </li>
          </ul>
        </nav>

        <div className={styles.panelBody}>
          <h2 className={styles.panelTitle}>{title}</h2>
          <section className={styles.archiveEmpty} aria-labelledby="my-archive-section-title">
            <h2 id="my-archive-section-title" className={styles.srOnly}>
              {title}
            </h2>
            <p className={styles.placeholder} role="status">
              {tab === 'all' && '아직 모아둔 글이 없습니다. GLOW 추천에서 마음에 드는 아티클을 저장해 보세요.'}
              {tab === 'saved' && '저장한 글이 없습니다.'}
              {tab === 'recent' && '최근 본 기록이 없습니다.'}
            </p>
          </section>
        </div>
      </section>
    </AdminMarketingShell>
  );
};

export default MyArchivePage;
