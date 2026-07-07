import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import { ExchangeRateSection } from './ExchangeRateSection';
import pageStyles from './ExchangeRatePage.module.css';

export const ExchangeRatePage = () => {
  return (
    <AdminMarketingShell
      currentPath="/exchange-rate"
      title="오늘의 환율"
      description="한국수출입은행 고시 매매기준율로 환산합니다."
      stats={[]}
    >
      <section className={`${shellStyles.panel} ${pageStyles.shellPanelFlat}`}>
        <div className={shellStyles.panelBody}>
          <ExchangeRateSection />
        </div>
      </section>
    </AdminMarketingShell>
  );
};

export default ExchangeRatePage;
