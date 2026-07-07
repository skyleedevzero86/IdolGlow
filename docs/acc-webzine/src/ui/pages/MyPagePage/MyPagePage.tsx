import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import shellStyles from "../AdminMarketingPage/AdminMarketingPage.module.css";
import { AdminMarketingShell } from "../AdminMarketingPage/AdminMarketingShell";
import { ADMIN_SNB_GROUPS } from "../AdminMarketingPage/adminNavigation";
import styles from "./MyPageAdminBoard.module.css";
import plainStyles from "./MyPagePlain.module.css";

const ADMIN_SHORTCUTS = [
  {
    label: "호별보기",
    href: "/admin/issues",
    description:
      "호별보기 화면으로 바로 이동해 등록, 수정, 삭제 작업을 진행할 수 있습니다.",
  },
  {
    label: "뉴스레터 관리",
    href: "/admin/newsletters",
    description:
      "뉴스레터 관리 화면으로 바로 이동해 등록, 수정, 삭제 작업을 진행할 수 있습니다.",
  },
  {
    label: "구독관리",
    href: "/admin/subscriptions",
    description:
      "구독관리 화면으로 바로 이동해 등록, 수정, 삭제 작업을 진행할 수 있습니다.",
  },
  {
    label: "배너 관리",
    href: "/admin/banners",
    description:
      "배너 관리 화면으로 바로 이동해 등록, 수정, 삭제 작업을 진행할 수 있습니다.",
  },
  {
    label: "팝업 관리",
    href: "/admin/popups",
    description:
      "팝업 관리 화면으로 바로 이동해 등록, 수정, 삭제 작업을 진행할 수 있습니다.",
  },
  {
    label: "광고 관리",
    href: "/admin/ads",
    description:
      "광고 관리 화면으로 바로 이동해 등록, 수정, 삭제 작업을 진행할 수 있습니다.",
  },
] as const;

const OPERATIONS_GUIDE = [
  {
    title: "콘텐츠 관리",
    description:
      "공지사항, 뉴스레터, 설문, 이벤트, 배너, 팝업, 광고를 메뉴에서 바로 관리할 수 있습니다.",
  },
  {
    title: "아카이브 관리",
    description:
      "구독관리, 호별보기, 공지사항 메뉴로 이동해 운영 흐름을 이어갈 수 있습니다.",
  },
  {
    title: "운영지표 확인",
    description:
      "회원관리와 서버상태 메뉴를 통해 현황을 확인하고 필요한 조치를 진행할 수 있습니다.",
  },
] as const;

export function MyPagePage() {
  const { accessToken, authReady, user } = useAuth();

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.loading}>
          마이페이지 정보를 불러오는 중입니다.
        </div>
      </main>
    );
  }

  if (user?.role === "ADMIN") {
    const totalMenuCount = ADMIN_SNB_GROUPS.reduce(
      (sum, group) => sum + group.items.length,
      0,
    );
    const contentMenuCount =
      ADMIN_SNB_GROUPS.find((group) => group.id === "admin-content")?.items
        .length ?? 0;
    const operationMenuCount =
      ADMIN_SNB_GROUPS.find((group) => group.id === "admin-insight")?.items
        .length ?? 0;

    return (
      <AdminMarketingShell
        currentPath="/mypage"
        title="마이페이지"
        description={`${user.nickname || user.email || "관리자"}님 환영합니다.`}
        statusText="왼쪽 관리자 메뉴를 선택하면 해당 관리 화면으로 이동합니다."
        classNames={{
          toolbarCard: styles.heroCard,
          header: styles.heroHeader,
          title: styles.heroTitle,
          subtitle: styles.heroSubtitle,
          statusText: styles.heroStatus,
          stats: styles.heroStats,
          statCard: styles.heroStatCard,
          statLabel: styles.heroStatLabel,
          statValue: styles.heroStatValue,
        }}
        stats={[
          { label: "권한", value: "ADMIN" },
          { label: "메뉴 수", value: totalMenuCount },
          { label: "빠른 이동", value: ADMIN_SHORTCUTS.length },
        ]}
      >
        <div className={styles.dashboard}>
          <section className={styles.topGrid}>
            <section className={`${styles.panel} ${styles.shortcutsPanel}`}>
              <div className={styles.panelHeader}>
                <div>
                  <h2 className={styles.panelTitle}>빠른 바로가기</h2>
                  <p className={styles.panelDescription}>
                    자주 쓰는 관리자 메뉴를 타일로 배치했습니다.
                  </p>
                </div>
                <span className={styles.panelMeta}>6 shortcuts</span>
              </div>

              <div className={styles.shortcutGrid}>
                {ADMIN_SHORTCUTS.map((item) => (
                  <article key={item.href} className={styles.shortcutCard}>
                    <div className={styles.shortcutTop}>
                      <h3 className={styles.shortcutTitle}>{item.label}</h3>
                      <Link to={item.href} className={styles.shortcutButton}>
                        이동
                      </Link>
                    </div>
                    <p className={styles.shortcutDescription}>
                      {item.description}
                    </p>
                  </article>
                ))}
              </div>
            </section>

            <section className={`${styles.panel} ${styles.summaryPanel}`}>
              <div className={styles.panelHeader}>
                <div>
                  <h2 className={styles.panelTitle}>운영 요약</h2>
                  <p className={styles.panelDescription}>
                    오늘 필요한 관리 흐름을 한 눈에 확인할 수 있습니다.
                  </p>
                </div>
              </div>

              <div className={styles.summaryBoard}>
                <div className={styles.summaryMetrics}>
                  <article className={styles.summaryMetricCard}>
                    <span className={styles.summaryMetricLabel}>
                      콘텐츠 메뉴
                    </span>
                    <strong className={styles.summaryMetricValue}>
                      {contentMenuCount}
                    </strong>
                    <p className={styles.summaryMetricHelper}>
                      뉴스레터, 호별보기, 설문, 배너, 팝업, 광고
                    </p>
                  </article>
                  <article className={styles.summaryMetricCard}>
                    <span className={styles.summaryMetricLabel}>
                      운영지표 메뉴
                    </span>
                    <strong className={styles.summaryMetricValue}>
                      {operationMenuCount}
                    </strong>
                    <p className={styles.summaryMetricHelper}>
                      회원관리, 서버상태 등 운영 확인 메뉴
                    </p>
                  </article>
                </div>

                <div className={styles.guideList}>
                  {OPERATIONS_GUIDE.map((item) => (
                    <article key={item.title} className={styles.guideCard}>
                      <h3 className={styles.guideTitle}>{item.title}</h3>
                      <p className={styles.guideDescription}>
                        {item.description}
                      </p>
                    </article>
                  ))}
                </div>
              </div>
            </section>
          </section>

          <section className={`${styles.panel} ${styles.bottomPanel}`}>
            <div className={styles.panelHeader}>
              <div>
                <h2 className={styles.panelTitle}>메뉴 사용 안내</h2>
                <p className={styles.panelDescription}>
                  왼쪽 메뉴는 그대로 유지하면서, 마이페이지에서는 운영 요약과
                  주요 바로가기를 함께 보여줍니다.
                </p>
              </div>
              <div className={styles.badgeRow}>
                <span className={styles.infoBadge}>Admin Dashboard</span>
                <span className={styles.infoBadge}>Quick Access</span>
              </div>
            </div>

            <div className={styles.infoGrid}>
              <article className={styles.infoCard}>
                <h3 className={styles.infoTitle}>
                  왼쪽 메뉴는 그대로 유지됩니다
                </h3>
                <p className={styles.infoDescription}>
                  관리자 메뉴 그룹을 펼쳐 원하는 항목을 누르면 해당 관리
                  페이지가 열립니다. 마이페이지에서는 주요 바로가기와 운영
                  요약을 함께 보여줍니다.
                </p>
              </article>

              <article className={styles.infoCard}>
                <h3 className={styles.infoTitle}>운영 시작 동선</h3>
                <p className={styles.infoDescription}>
                  콘텐츠 등록은 호별보기와 뉴스레터부터, 발송/배치는
                  구독관리에서, 상태 확인은 회원관리와 서버상태에서 이어서
                  확인하면 편합니다.
                </p>
              </article>

              <article className={styles.infoCard}>
                <h3 className={styles.infoTitle}>관리 흐름 추천</h3>
                <p className={styles.infoDescription}>
                  오늘 올릴 콘텐츠를 등록한 뒤 배너와 팝업 노출 여부를 점검하고,
                  마지막에 서버상태에서 인프라 상태를 확인하는 흐름을
                  추천합니다.
                </p>
              </article>
            </div>
          </section>
        </div>
      </AdminMarketingShell>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/mypage"
      title="마이페이지"
      description={user ? `${user.nickname || user.email || "회원"}님 환영합니다.` : "로그인 후 이용할 수 있습니다."}
      classNames={{
        toolbarCard: plainStyles.flatToolbar,
      }}
      stats={[]}
    >
      <section className={`${shellStyles.panel} ${plainStyles.flatPanel}`}>
        <div className={shellStyles.panelHeader}>
          <h2 className={shellStyles.panelTitle}>바로가기</h2>
        </div>
        <div className={shellStyles.panelBody}>
          <div className={shellStyles.rowActions}>
            <Link to="/articles" className={shellStyles.primaryButton}>
              아티클 보기
            </Link>
            <Link to="/events" className={shellStyles.secondaryButton}>
              이벤트 보기
            </Link>
            <Link to="/my-payments" className={shellStyles.secondaryButton}>
              결제 내역
            </Link>
            <Link to="/my-survey" className={shellStyles.secondaryButton}>
              설문·맞춤 추천
            </Link>
            <Link
              to="/articles?category=community"
              className={shellStyles.secondaryButton}
            >
              커뮤니티 추천
            </Link>
          </div>
        </div>
      </section>
    </AdminMarketingShell>
  );
}

export default MyPagePage;
