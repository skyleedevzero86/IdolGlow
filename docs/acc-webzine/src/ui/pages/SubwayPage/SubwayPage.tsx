import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import { SubwaySection } from './SubwaySection';
import pageStyles from './SubwayPage.module.css';

export const SubwayPage = () => {
  return (
    <AdminMarketingShell
      currentPath="/subway"
      title="Glow 지하철"
      description=""
      stats={[]}
    >
      <section className={`${shellStyles.panel} ${pageStyles.shellPanel}`}>
        <div className={shellStyles.panelBody}>
          <SubwaySection />
        </div>
      </section>
    </AdminMarketingShell>
  );
};

export default SubwayPage;
