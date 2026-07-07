import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { Layout } from "./ui/components/Layout/Layout";
import {
  RequireAdmin,
  RequireAuth,
} from "./ui/components/RouteGuard/RouteGuards";
import { AdminAdsPage } from "./ui/pages/AdminAdsPage/AdminAdsPage";
import { AdminAuthVerificationPage } from "./ui/pages/AdminAuthVerificationPage/AdminAuthVerificationPage";
import { AdminBannersPage } from "./ui/pages/AdminBannersPage/AdminBannersPage";
import { AdminEventDetailPage } from "./ui/pages/AdminEventDetailPage/AdminEventDetailPage";
import { AdminEventEditorPage } from "./ui/pages/AdminEventEditorPage/AdminEventEditorPage";
import { AdminEventsPage } from "./ui/pages/AdminEventsPage/AdminEventsPage";
import { AdminGlowRecommendationPage } from "./ui/pages/AdminGlowRecommendationPage/AdminGlowRecommendationPage";
import { AdminIssueArticleEditorPage } from "./ui/pages/AdminIssueArticleEditorPage/AdminIssueArticleEditorPage";
import { AdminIssueArticlePage } from "./ui/pages/AdminIssueArticlePage/AdminIssueArticlePage";
import { AdminIssueArticlesPage } from "./ui/pages/AdminIssueArticlesPage/AdminIssueArticlesPage";
import { AdminIssueVolumeEditorPage } from "./ui/pages/AdminIssueVolumeEditorPage/AdminIssueVolumeEditorPage";
import { AdminIssuesPage } from "./ui/pages/AdminIssuesPage/AdminIssuesPage";
import { AdminNewsletterDetailPage } from "./ui/pages/AdminNewsletterDetailPage/AdminNewsletterDetailPage";
import { AdminNewsletterEditorPage } from "./ui/pages/AdminNewsletterEditorPage/AdminNewsletterEditorPage";
import { AdminNewslettersPage } from "./ui/pages/AdminNewslettersPage/AdminNewslettersPage";
import { AdminNoticeEditorPage } from "./ui/pages/AdminNoticeEditorPage/AdminNoticeEditorPage";
import { AdminNoticesPage } from "./ui/pages/AdminNoticesPage/AdminNoticesPage";
import { AdminOptionEditorPage } from "./ui/pages/AdminOptionEditorPage/AdminOptionEditorPage";
import { AdminOptionsPage } from "./ui/pages/AdminOptionsPage/AdminOptionsPage";
import { AdminPaymentsPage } from "./ui/pages/AdminPaymentsPage/AdminPaymentsPage";
import { AdminPopupsPage } from "./ui/pages/AdminPopupsPage/AdminPopupsPage";
import { AdminProductEditorPage } from "./ui/pages/AdminProductEditorPage/AdminProductEditorPage";
import { AdminProductsPage } from "./ui/pages/AdminProductsPage/AdminProductsPage";
import { AdminReservationsPage } from "./ui/pages/AdminReservationsPage/AdminReservationsPage";
import { AdminReviewsPage } from "./ui/pages/AdminReviewsPage/AdminReviewsPage";
import { AdminSchedulesPage } from "./ui/pages/AdminSchedulesPage/AdminSchedulesPage";
import { AdminServerStatusPage } from "./ui/pages/AdminServerStatusPage/AdminServerStatusPage";
import { AdminSlotsPage } from "./ui/pages/AdminSlotsPage/AdminSlotsPage";
import { AdminSubscriptionsPage } from "./ui/pages/AdminSubscriptionsPage/AdminSubscriptionsPage";
import { AdminSurveyEditorPage, AdminSurveysPage } from "./ui/pages/AdminSurveysPage/AdminSurveysPage";
import { AdminUsersPage } from "./ui/pages/AdminUsersPage/AdminUsersPage";
import { ArchivePage } from "./ui/pages/ArchivePage/ArchivePage";
import { ArticleDetailPage } from "./ui/pages/ArticleDetailPage/ArticleDetailPage";
import { ArticleListPage } from "./ui/pages/ArticleListPage/ArticleListPage";
import { AirportCrowdPage } from "./ui/pages/AirportCrowdPage/AirportCrowdPage";
import { EventPublicDetailPage } from "./ui/pages/EventPublicDetailPage/EventPublicDetailPage";
import { EventsPage } from "./ui/pages/EventsPage/EventsPage";
import { EventInfoDetailPage } from "./ui/pages/EventInfoPage/EventInfoDetailPage";
import { EventInfoPage } from "./ui/pages/EventInfoPage/EventInfoPage";
import { ExchangeRatePage } from "./ui/pages/ExchangeRatePage/ExchangeRatePage";
import { GlowAlertsPage } from "./ui/pages/GlowAlertsPage/GlowAlertsPage";
import { GlowMapPage } from "./ui/pages/GlowMapPage/GlowMapPage";
import { GlowWeatherPage } from "./ui/pages/GlowWeatherPage/GlowWeatherPage";
import { SubwayPage } from "./ui/pages/SubwayPage/SubwayPage";
import { GlowPage } from "./ui/pages/GlowPage/GlowPage";
import { HomePage } from "./ui/pages/HomePage/HomePage";
import { MyArchivePage } from "./ui/pages/MyArchivePage/MyArchivePage";
import { MyPagePage } from "./ui/pages/MyPagePage/MyPagePage";
import { MyPaymentsPage } from "./ui/pages/MyPaymentsPage/MyPaymentsPage";
import { MyReviewsPage } from "./ui/pages/MyReviewsPage/MyReviewsPage";
import { MySurveyPage } from "./ui/pages/MySurveyPage/MySurveyPage";
import { MyUserInfoPage } from "./ui/pages/MyUserInfoPage/MyUserInfoPage";
import { NoticePublicDetailPage } from "./ui/pages/NoticePublicDetailPage/NoticePublicDetailPage";
import { NoticesPage } from "./ui/pages/NoticesPage/NoticesPage";
import { ProductPublicDetailPage } from "./ui/pages/ProductPublicDetailPage/ProductPublicDetailPage";
import { WishPage } from "./ui/pages/WishPage/WishPage";
import { AuthCallbackPage } from "./ui/pages/AuthCallbackPage/AuthCallbackPage";

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/auth/callback" element={<AuthCallbackPage />} />
          <Route path="/" element={<Layout />}>
            <Route index element={<HomePage />} />
            <Route path="wish" element={<WishPage />} />
            <Route path="glow" element={<GlowPage />} />
            <Route path="events" element={<EventsPage />} />
            <Route path="events/:documentId" element={<EventPublicDetailPage />} />
            <Route path="notices" element={<NoticesPage />} />
            <Route path="notices/:documentId" element={<NoticePublicDetailPage />} />
            <Route path="archive" element={<ArchivePage />} />
            <Route path="glow-alerts" element={<GlowAlertsPage />} />
            <Route path="products/:id" element={<ProductPublicDetailPage />} />
            <Route path="articles" element={<ArticleListPage />} />
            <Route path="articles/:id" element={<ArticleDetailPage />} />
            <Route element={<RequireAuth />}>
              <Route path="mypage" element={<MyPagePage />} />
              <Route path="mypage/userInfo" element={<MyUserInfoPage />} />
              <Route path="my-survey" element={<MySurveyPage />} />
              <Route path="my-payments" element={<MyPaymentsPage />} />
              <Route path="myreviewsfh" element={<MyReviewsPage />} />
              <Route path="my-archive" element={<MyArchivePage />} />
              <Route path="exchange-rate" element={<ExchangeRatePage />} />
              <Route path="subway" element={<SubwayPage />} />
              <Route path="event-info" element={<EventInfoPage />} />
              <Route path="event-info/:source/:contentId" element={<EventInfoDetailPage />} />
              <Route path="airport-crowd" element={<AirportCrowdPage />} />
              <Route path="glow_map" element={<GlowMapPage />} />
              <Route path="glow-weather" element={<GlowWeatherPage />} />
            </Route>
            <Route element={<RequireAdmin />}>
              <Route path="admin/products" element={<AdminProductsPage />} />
              <Route path="admin/products/new" element={<AdminProductEditorPage />} />
              <Route path="admin/products/:productId/edit" element={<AdminProductEditorPage />} />
              <Route path="admin/slots" element={<AdminSlotsPage />} />
              <Route path="admin/options" element={<AdminOptionsPage />} />
              <Route path="admin/options/new" element={<AdminOptionEditorPage />} />
              <Route path="admin/options/:optionId/edit" element={<AdminOptionEditorPage />} />
              <Route path="admin/schedules" element={<AdminSchedulesPage />} />
              <Route path="admin/reservations" element={<AdminReservationsPage />} />
              <Route path="admin/payments" element={<AdminPaymentsPage />} />
              <Route path="admin/issues" element={<AdminIssuesPage />} />
              <Route path="admin/issues/new" element={<AdminIssueVolumeEditorPage />} />
              <Route path="admin/issues/:issueSlug/edit" element={<AdminIssueVolumeEditorPage />} />
              <Route path="admin/issues/:issueSlug" element={<AdminIssueArticlesPage />} />
              <Route path="admin/issues/:issueSlug/articles/new" element={<AdminIssueArticleEditorPage />} />
              <Route path="admin/issues/:issueSlug/articles/:articleSlug/edit" element={<AdminIssueArticleEditorPage />} />
              <Route path="admin/issues/:issueSlug/articles/:articleSlug" element={<AdminIssueArticlePage />} />
              <Route path="admin/banners" element={<AdminBannersPage />} />
              <Route path="admin/popups" element={<AdminPopupsPage />} />
              <Route path="admin/ads" element={<AdminAdsPage />} />
              <Route path="admin/newsletters" element={<AdminNewslettersPage />} />
              <Route path="admin/surveys" element={<AdminSurveysPage />} />
              <Route path="admin/surveys/new" element={<AdminSurveyEditorPage />} />
              <Route path="admin/surveys/:surveyId/edit" element={<AdminSurveyEditorPage />} />
              <Route path="admin/glow-recommendation" element={<AdminGlowRecommendationPage />} />
              <Route path="admin/notices" element={<AdminNoticesPage />} />
              <Route path="admin/notices/new" element={<AdminNoticeEditorPage />} />
              <Route path="admin/notices/:documentId/edit" element={<AdminNoticeEditorPage />} />
              <Route path="admin/events" element={<AdminEventsPage />} />
              <Route path="admin/events/new" element={<AdminEventEditorPage />} />
              <Route path="admin/events/:documentId" element={<AdminEventDetailPage />} />
              <Route path="admin/events/:documentId/edit" element={<AdminEventEditorPage />} />
              <Route path="admin/subscriptions" element={<AdminSubscriptionsPage />} />
              <Route path="admin/users" element={<AdminUsersPage />} />
              <Route path="admin/auth-verifications" element={<AdminAuthVerificationPage />} />
              <Route path="admin/reviews" element={<AdminReviewsPage />} />
              <Route path="admin/server-status" element={<AdminServerStatusPage />} />
              <Route path="admin/newsletters/new" element={<AdminNewsletterEditorPage />} />
              <Route path="admin/newsletters/:newsletterSlug/edit" element={<AdminNewsletterEditorPage />} />
              <Route path="admin/newsletters/:newsletterSlug" element={<AdminNewsletterDetailPage />} />
            </Route>
            <Route path="*" element={<HomePage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
