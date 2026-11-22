import { Outlet } from 'react-router-dom';
import { Header } from '../Header/Header';
import { Footer } from '../Footer/Footer';
import { QuickMenu } from '../QuickMenu/QuickMenu';
import { useStaticContentViewTracker } from '../../hooks/useContentView';
import styles from './Layout.module.css';

export const Layout = () => {
  useStaticContentViewTracker();

  return (
    <div className={styles.layout}>
      <a href="#main-content" className="skip-link">
        본문으로 바로가기
      </a>

      <Header />

      <div className={styles.content}>
        <Outlet />
      </div>

      <Footer />

      <QuickMenu />
    </div>
  );
};

export default Layout;
