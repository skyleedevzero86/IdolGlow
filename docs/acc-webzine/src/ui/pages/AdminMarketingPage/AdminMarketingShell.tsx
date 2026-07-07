import { useEffect, useReducer, useState, type ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { formatMyPageProfileLine } from '../../../auth/authApi';
import { formatAdminSessionRemaining } from '../../../auth/adminSessionWall';
import { useAuth } from '../../../auth/AuthContext';
import styles from './AdminMarketingPage.module.css';
import { ADMIN_SNB_GROUPS, avatarInitial } from './adminNavigation';

const ChevronIcon = ({ open }: { readonly open: boolean }) => (
  <svg
    viewBox="0 0 20 20"
    fill="none"
    aria-hidden="true"
    className={open ? styles.chevronOpen : styles.chevron}
  >
    <path
      d="M6 8L10 12L14 8"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

const EditIcon = () => (
  <svg
    className={styles.thumbRoundBtnIcon}
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    <path d="M12 20h9" />
    <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
  </svg>
);

type AdminMarketingShellProps = {
  readonly currentPath: string;
  readonly title: string;
  readonly description: string;
  readonly statusText?: string | null;
  readonly headerAside?: ReactNode;
  readonly classNames?: {
    readonly main?: string;
    readonly toolbarCard?: string;
    readonly header?: string;
    readonly title?: string;
    readonly subtitle?: string;
    readonly statusText?: string;
    readonly stats?: string;
    readonly statCard?: string;
    readonly statLabel?: string;
    readonly statValue?: string;
  };
  readonly stats: ReadonlyArray<{
    readonly label: string;
    readonly value: ReactNode;
  }>;
  readonly children: ReactNode;
};

export function AdminMarketingShell({
  currentPath,
  title,
  description,
  statusText,
  headerAside,
  classNames,
  stats,
  children,
}: AdminMarketingShellProps) {
  const location = useLocation();
  const { user, accessToken, adminSessionDeadlineMs } = useAuth();
  const [, forceSessionTick] = useReducer((n: number) => n + 1, 0);
  const visibleGroups = user?.role === 'ADMIN'
    ? ADMIN_SNB_GROUPS
    : ADMIN_SNB_GROUPS.filter(group => group.id === 'admin-archive' || group.id === 'admin-glow-picks');
  const resolveDefaultOpenGroups = (role?: string): Record<string, boolean> => {
    const activeGroup = visibleGroups.find(group =>
      group.items.some(item => {
        const hashIndex = item.href.indexOf('#');
        const itemPath = hashIndex > 0 ? item.href.slice(0, hashIndex) : item.href;
        return itemPath === currentPath;
      })
    );
    const openId = activeGroup?.id ?? (role === 'ADMIN' ? 'admin-insight' : 'admin-glow-picks');
    return Object.fromEntries(visibleGroups.map(group => [group.id, group.id === openId]));
  };
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>(() =>
    resolveDefaultOpenGroups(user?.role)
  );

  useEffect(() => {
    setOpenGroups(resolveDefaultOpenGroups(user?.role));
  }, [user?.role]);

  useEffect(() => {
    if (adminSessionDeadlineMs == null) {
      return;
    }
    const id = window.setInterval(() => forceSessionTick(), 1000);
    return () => window.clearInterval(id);
  }, [adminSessionDeadlineMs]);

  const sessionRemainingLabel =
    adminSessionDeadlineMs != null
      ? formatAdminSessionRemaining(adminSessionDeadlineMs, Date.now())
      : null;

  const toggleGroup = (id: string) => {
    setOpenGroups(previous => ({ ...previous, [id]: !previous[id] }));
  };

  const isMenuItemActive = (href: string) => {
    if (href === currentPath) {
      return true;
    }
    const hashIndex = href.indexOf('#');
    if (hashIndex > 0) {
      const path = href.slice(0, hashIndex);
      const hash = href.slice(hashIndex);
      return location.pathname === path && location.hash === hash;
    }
    return false;
  };

  return (
    <main className={styles.page} id="main-content">
      <div className={styles.inner}>
        <aside className={styles.aside} aria-label="사이드 메뉴">
          <div className={styles.profileCard}>
            <div className={styles.thumbWrap}>
              <div className={styles.thumbStack}>
                {user?.picture ? (
                  <img
                    src={user.picture}
                    alt=""
                    className={styles.avatarImage}
                    referrerPolicy="no-referrer"
                  />
                ) : (
                  <span className={styles.avatarFallback}>
                    {avatarInitial(user?.nickname, user?.email)}
                  </span>
                )}
                {accessToken && user ? (
                  <div className={styles.thumbActionCluster}>
                    <Link
                      to="/mypage/userInfo"
                      className={styles.thumbRoundBtn}
                      aria-label="개인정보 변경"
                    >
                      <EditIcon />
                    </Link>
                  </div>
                ) : null}
              </div>
            </div>

            <div className={styles.profileName}>
              {user ? formatMyPageProfileLine(user) : '관리자'}
            </div>
            <p className={styles.profileMeta}>어서오세요!</p>
            {sessionRemainingLabel ? (
              <p className={styles.profileSessionHint} aria-live="polite">
                자동 로그아웃까지 {sessionRemainingLabel}
              </p>
            ) : null}
          </div>

          <nav className={styles.menuCard}>
            {visibleGroups.map(group => {
              const isOpen = openGroups[group.id];

              return (
                <section
                  key={group.id}
                  className={[styles.menuGroup, isOpen ? styles.menuGroupOpen : ''].filter(Boolean).join(' ')}
                >
                  <button
                    type="button"
                    className={styles.menuGroupButton}
                    onClick={() => toggleGroup(group.id)}
                    aria-expanded={isOpen}
                  >
                    <span>{group.title}</span>
                    <ChevronIcon open={isOpen} />
                  </button>

                  {isOpen ? (
                    <ul className={styles.menuList}>
                      {group.items.map((item, index) => {
                        const isActive = isMenuItemActive(item.href);

                        return (
                          <li key={`${group.id}-${item.href}-${item.label}-${index}`}>
                            <Link
                              to={item.href}
                              className={[styles.menuLink, isActive ? styles.menuLinkActive : '']
                                .filter(Boolean)
                                .join(' ')}
                            >
                              {item.label}
                            </Link>
                          </li>
                        );
                      })}
                    </ul>
                  ) : null}
                </section>
              );
            })}
          </nav>
        </aside>

        <div className={[styles.main, classNames?.main].filter(Boolean).join(' ')}>
          <section className={[styles.toolbarCard, classNames?.toolbarCard].filter(Boolean).join(' ')}>
            <header className={[styles.header, classNames?.header].filter(Boolean).join(' ')}>
              <div>
                <h1 className={[styles.title, classNames?.title].filter(Boolean).join(' ')}>{title}</h1>
                {description ? (
                  <p className={[styles.subtitle, classNames?.subtitle].filter(Boolean).join(' ')}>
                    {description}
                  </p>
                ) : null}
              </div>
              {headerAside}
            </header>

            {statusText ? (
              <p className={[styles.sectionDescription, classNames?.statusText].filter(Boolean).join(' ')}>
                {statusText}
              </p>
            ) : null}

            {stats.length > 0 ? (
              <div className={[styles.stats, classNames?.stats].filter(Boolean).join(' ')}>
                {stats.map(stat => (
                  <div
                    key={stat.label}
                    className={[styles.statCard, classNames?.statCard].filter(Boolean).join(' ')}
                  >
                    <span
                      className={[styles.statLabel, classNames?.statLabel].filter(Boolean).join(' ')}
                    >
                      {stat.label}
                    </span>
                    <strong
                      className={[styles.statValue, classNames?.statValue].filter(Boolean).join(' ')}
                    >
                      {stat.value}
                    </strong>
                  </div>
                ))}
              </div>
            ) : null}
          </section>

          {children}
        </div>
      </div>
    </main>
  );
}

export default AdminMarketingShell;
