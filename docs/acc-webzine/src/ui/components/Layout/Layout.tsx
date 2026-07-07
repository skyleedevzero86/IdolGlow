/**
 * Layout Component
 *
 * 공통 레이아웃 컴포넌트 - Header, Footer를 감싸는 래퍼
 */

import { Outlet } from 'react-router-dom';
import { Header } from '../Header/Header';
import { Footer } from '../Footer/Footer';
import { QuickMenu } from '../QuickMenu/QuickMenu';
import styles from './Layout.module.css';

/**
 * Layout 컴포넌트
 */
export const Layout = () => {
  return (
    <div className={styles.layout}>
      {/* 스킵 네비게이션 */}
      <a href="#main-content" className="skip-link">
        본문으로 바로가기
      </a>

      {/* 헤더 */}
      <Header />

      {/* 메인 콘텐츠 */}
      <div className={styles.content}>
        <Outlet />
      </div>

      {/* 푸터 */}
      <Footer />

      <QuickMenu />
    </div>
  );
};

export default Layout;
