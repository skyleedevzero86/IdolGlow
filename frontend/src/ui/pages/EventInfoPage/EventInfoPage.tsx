import { EventInfoBrowser } from "../../components/EventInfoBrowser/EventInfoBrowser";
import shellStyles from "../AdminMarketingPage/AdminMarketingPage.module.css";
import { AdminMarketingShell } from "../AdminMarketingPage/AdminMarketingShell";
import styles from "./EventInfoPage.module.css";

export { eventInfoDetailPath } from "../../components/EventInfoBrowser/EventInfoBrowser";

export function EventInfoPage() {
  return (
    <AdminMarketingShell
      currentPath="/event-info"
      title="Glow 행사정보"
      description="날짜별 행사와 키워드·지역·분류 상세검색"
      stats={[]}
    >
      <section className={`${shellStyles.panel} ${styles.panel}`}>
        <div className={shellStyles.panelBody}>
          <EventInfoBrowser variant="page" />
        </div>
      </section>
    </AdminMarketingShell>
  );
}

export default EventInfoPage;
