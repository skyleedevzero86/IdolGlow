import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../../auth/AuthContext';
import '../../../../../samples/portal-stat-bar.css';
import {
  fetchAdminSubscriptionOverview,
  type AdminSubscriptionDispatch,
  type AdminSubscriptionLatestContent,
  type AdminSubscriptionOverviewResponse,
  type AdminSubscriptionSchedule,
  type AdminSubscriptionSubscriber,
  type SubscriptionContentType,
  type SubscriptionScheduleDayOfWeek,
  type SubscriptionScheduleFrequency,
  upsertAdminSubscriptionSchedule,
} from '../../../shared/data/subscriptionAdminApi';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from './AdminSubscriptionsPage.module.css';

const SUBSCRIBER_PAGE_SIZE = 10;
const DISPATCH_PAGE_SIZE = 10;

const DAY_OPTIONS: ReadonlyArray<{ value: SubscriptionScheduleDayOfWeek; label: string }> = [
  { value: 'MONDAY', label: '월요일' },
  { value: 'TUESDAY', label: '화요일' },
  { value: 'WEDNESDAY', label: '수요일' },
  { value: 'THURSDAY', label: '목요일' },
  { value: 'FRIDAY', label: '금요일' },
  { value: 'SATURDAY', label: '토요일' },
  { value: 'SUNDAY', label: '일요일' },
];

type ScheduleFormState = {
  readonly frequencyType: SubscriptionScheduleFrequency;
  readonly dayOfWeek: SubscriptionScheduleDayOfWeek | null;
  readonly dispatchTime: string;
  readonly active: boolean;
};

const EMPTY_OVERVIEW: AdminSubscriptionOverviewResponse = {
  totalActive: 0,
  totalSubscribers: 0,
  newsletterSubscriberCount: 0,
  issueSubscriberCount: 0,
  totalDispatches: 0,
  subscribers: [],
  subscriberPage: 1,
  subscriberSize: SUBSCRIBER_PAGE_SIZE,
  subscriberTotalElements: 0,
  subscriberTotalPages: 1,
  subscriberHasNext: false,
  dispatches: [],
  dispatchPage: 1,
  dispatchSize: DISPATCH_PAGE_SIZE,
  dispatchTotalElements: 0,
  dispatchTotalPages: 1,
  dispatchHasNext: false,
  schedules: [],
  latestContents: [],
};

const DEFAULT_SCHEDULES: Record<SubscriptionContentType, ScheduleFormState> = {
  NEWSLETTER: {
    frequencyType: 'WEEKLY',
    dayOfWeek: 'MONDAY',
    dispatchTime: '09:00',
    active: true,
  },
  WEBZINE_ISSUE: {
    frequencyType: 'WEEKLY',
    dayOfWeek: 'FRIDAY',
    dispatchTime: '10:00',
    active: false,
  },
};

const MANAGED_CONTENT_TYPES = ['NEWSLETTER'] as const;

const scheduleToFormState = (schedule?: AdminSubscriptionSchedule | null): ScheduleFormState =>
  schedule
    ? {
        frequencyType: schedule.frequencyType,
        dayOfWeek: schedule.dayOfWeek,
        dispatchTime: schedule.dispatchTime,
        active: schedule.active,
      }
    : DEFAULT_SCHEDULES.NEWSLETTER;

const buildInitialForms = (
  schedules: readonly AdminSubscriptionSchedule[]
): Record<SubscriptionContentType, ScheduleFormState> => ({
  NEWSLETTER: {
    ...DEFAULT_SCHEDULES.NEWSLETTER,
    ...scheduleToFormState(schedules.find(schedule => schedule.contentType === 'NEWSLETTER')),
  },
  WEBZINE_ISSUE: {
    ...DEFAULT_SCHEDULES.WEBZINE_ISSUE,
    ...scheduleToFormState(schedules.find(schedule => schedule.contentType === 'WEBZINE_ISSUE')),
  },
});

const SubscriptionStatCount = ({ n }: { readonly n: number }) => (
  <>
    <span className={shellToolbar.statNumber}>{n}</span>
    <span className={shellToolbar.statUnit}>명</span>
  </>
);

const SubscriptionScheduleCount = ({ n }: { readonly n: number }) => (
  <>
    <span className={shellToolbar.statNumber}>{n}</span>
    <span className={shellToolbar.statUnit}>개</span>
  </>
);

export function AdminSubscriptionsPage() {
  const { accessToken, authReady, user } = useAuth();
  const [overview, setOverview] = useState<AdminSubscriptionOverviewResponse>(EMPTY_OVERVIEW);
  const [forms, setForms] = useState<Record<SubscriptionContentType, ScheduleFormState>>(
    buildInitialForms([])
  );
  const [loading, setLoading] = useState(false);
  const [savingType, setSavingType] = useState<SubscriptionContentType | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!authReady || !accessToken || user?.role !== 'ADMIN') {
      return;
    }

    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);

      try {
        const response = await fetchAdminSubscriptionOverview(accessToken, {
          subscriberPage: 1,
          subscriberSize: SUBSCRIBER_PAGE_SIZE,
          dispatchPage: 1,
          dispatchSize: DISPATCH_PAGE_SIZE,
        });

        if (!cancelled) {
          setOverview(response);
          setForms(buildInitialForms(response.schedules));
        }
      } catch (loadError) {
        if (!cancelled) {
          setError(
            loadError instanceof Error ? loadError.message : '구독 관리 데이터를 불러오지 못했습니다.'
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    void load();

    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, user?.role]);

  const activeScheduleCount = useMemo(
    () => overview.schedules.filter(schedule => schedule.contentType === 'NEWSLETTER' && schedule.active).length,
    [overview.schedules]
  );

  const latestContentByType = useMemo(() => {
    const map = new Map<SubscriptionContentType, AdminSubscriptionLatestContent>();
    overview.latestContents.forEach(content => map.set(content.contentType, content));
    return map;
  }, [overview.latestContents]);

  const handleFieldChange = (
    contentType: SubscriptionContentType,
    patch: Partial<ScheduleFormState>
  ) => {
    setForms(current => ({
      ...current,
      [contentType]: {
        ...current[contentType],
        ...patch,
      },
    }));
  };

  const handleSaveSchedule = async (contentType: SubscriptionContentType) => {
    if (!accessToken) {
      return;
    }

    const current = forms[contentType];
    setSavingType(contentType);
    setError(null);
    setMessage(null);

    try {
      const saved = await upsertAdminSubscriptionSchedule(accessToken, contentType, {
        frequencyType: current.frequencyType,
        dayOfWeek: current.frequencyType === 'WEEKLY' ? current.dayOfWeek : null,
        dispatchTime: current.dispatchTime,
        active: current.active,
      });

      setOverview(previous => {
        const otherSchedules = previous.schedules.filter(schedule => schedule.contentType !== contentType);
        return {
          ...previous,
          schedules: [...otherSchedules, saved].sort((left, right) =>
            left.contentType.localeCompare(right.contentType)
          ),
        };
      });
      setForms(currentForms => ({
        ...currentForms,
        [contentType]: {
          frequencyType: saved.frequencyType,
          dayOfWeek: saved.dayOfWeek,
          dispatchTime: saved.dispatchTime,
          active: saved.active,
        },
      }));
      setMessage('뉴스레터 예약 발송 설정을 저장했습니다.');
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : '예약 발송 설정 저장에 실패했습니다.');
    } finally {
      setSavingType(null);
    }
  };

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={styles.loading}>관리자 권한을 확인하는 중입니다.</div>
      </main>
    );
  }

  if (!accessToken || user?.role !== 'ADMIN') {
    return (
      <main className={shellStyles.page}>
        <div className={styles.denied}>관리자만 구독 관리 화면을 확인할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/subscriptions"
      title="구독 관리"
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
        stats: shellToolbar.stats,
        statCard: shellToolbar.statCard,
        statLabel: shellToolbar.statLabel,
        statValue: shellToolbar.statValue,
      }}
      statusText={
        error
          ? error
          : loading
            ? '구독 관리 데이터를 불러오는 중입니다.'
            : null
      }
      stats={[
        { label: '전체 구독자', value: <SubscriptionStatCount n={overview.totalSubscribers} /> },
        { label: '활성 구독자', value: <SubscriptionStatCount n={overview.totalActive} /> },
        { label: '활성 스케줄', value: <SubscriptionScheduleCount n={activeScheduleCount} /> },
      ]}
    >
      <div className={styles.grid}>
        {message ? <div className={styles.message}>{message}</div> : null}
        {error ? <div className={styles.error}>{error}</div> : null}

        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <div>
              <h2 className={styles.panelTitle}>예약 발송 설정</h2>
              <p className={styles.panelDescription}>
                콘텐츠 등록 시 바로 메일을 보내지 않고, 아래 스케줄에 맞춰 최신 글만 자동 발송합니다.
              </p>
            </div>
          </div>
          <div className={styles.panelBody}>
            <div className={styles.scheduleGrid}>
              {MANAGED_CONTENT_TYPES.map(contentType => {
                const form = forms[contentType];
                const currentSchedule = overview.schedules.find(
                  schedule => schedule.contentType === contentType
                );
                const currentContent = latestContentByType.get(contentType);

                return (
                  <article key={contentType} className={styles.card}>
                    <div className={styles.cardHeader}>
                      <div>
                        <h3 className={styles.cardTitle}>뉴스레터 발송</h3>
                        <p className={styles.cardDescription}>
                          {currentSchedule?.nextDispatchAt
                            ? `다음 발송 예정: ${currentSchedule.nextDispatchAt}`
                            : '아직 저장된 예약 발송 규칙이 없습니다.'}
                        </p>
                      </div>
                    </div>
                    <div className={styles.cardBody}>
                      <div className={styles.fieldGrid}>
                        <div className={styles.field}>
                          <label className={styles.label}>반복 주기</label>
                          <select
                            className={styles.select}
                            value={form.frequencyType}
                            onChange={event =>
                              handleFieldChange(contentType, {
                                frequencyType: event.target.value as SubscriptionScheduleFrequency,
                                dayOfWeek:
                                  event.target.value === 'WEEKLY'
                                    ? form.dayOfWeek ?? 'MONDAY'
                                    : null,
                              })
                            }
                          >
                            <option value="DAILY">매일</option>
                            <option value="WEEKLY">매주</option>
                          </select>
                        </div>

                        <div className={styles.field}>
                          <label className={styles.label}>발송 시간</label>
                          <input
                            type="time"
                            className={styles.input}
                            value={form.dispatchTime}
                            onChange={event =>
                              handleFieldChange(contentType, { dispatchTime: event.target.value })
                            }
                          />
                        </div>

                        {form.frequencyType === 'WEEKLY' ? (
                          <div className={styles.fullField}>
                            <label className={styles.label}>발송 요일</label>
                            <select
                              className={styles.select}
                              value={form.dayOfWeek ?? 'MONDAY'}
                              onChange={event =>
                                handleFieldChange(contentType, {
                                  dayOfWeek: event.target.value as SubscriptionScheduleDayOfWeek,
                                })
                              }
                            >
                              {DAY_OPTIONS.map(option => (
                                <option key={option.value} value={option.value}>
                                  {option.label}
                                </option>
                              ))}
                            </select>
                          </div>
                        ) : null}

                        <div className={styles.fullField}>
                          <label className={styles.label}>예약 활성화</label>
                          <label className={styles.toggleRow}>
                            <input
                              type="checkbox"
                              checked={form.active}
                              onChange={event =>
                                handleFieldChange(contentType, { active: event.target.checked })
                              }
                            />
                            <span>
                              {form.active ? '예약 발송을 사용합니다.' : '예약 발송을 잠시 중지합니다.'}
                            </span>
                          </label>
                        </div>
                      </div>

                      <div className={styles.chipRow}>
                        <span className={styles.chip}>
                          대상 구독자{' '}
                          {contentType === 'NEWSLETTER'
                            ? `${overview.newsletterSubscriberCount}명`
                            : `${overview.issueSubscriberCount}명`}
                        </span>
                        <span className={styles.chip}>
                          최신 콘텐츠 {currentContent ? '연결됨' : '없음'}
                        </span>
                      </div>

                      <div className={styles.buttonRow}>
                        <button
                          type="button"
                          className={styles.primaryButton}
                          onClick={() => void handleSaveSchedule(contentType)}
                          disabled={savingType === contentType}
                        >
                          {savingType === contentType ? '저장 중...' : '예약 저장'}
                        </button>
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>
          </div>
        </section>

        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <div>
              <h2 className={styles.panelTitle}>최신 발송 대상 콘텐츠</h2>
              <p className={styles.panelDescription}>
                관리자 스케줄이 실행되면 아래 최신 콘텐츠 중 아직 발송되지 않은 글이 자동으로 메일에 사용됩니다.
              </p>
            </div>
          </div>
          <div className={styles.panelBody}>
            <div className={styles.contentGrid}>
              {MANAGED_CONTENT_TYPES.map(contentType => {
                const content = latestContentByType.get(contentType);

                return (
                  <article key={contentType} className={styles.card}>
                    <div className={styles.cardHeader}>
                      <div>
                        <h3 className={styles.cardTitle}>뉴스레터 최신 글</h3>
                        <p className={styles.cardDescription}>
                          {content?.publishedAt
                            ? `게시일 ${content.publishedAt}`
                            : '아직 연결된 콘텐츠가 없습니다.'}
                        </p>
                      </div>
                    </div>
                    <div className={styles.cardBody}>
                      {content ? (
                        <>
                          <div className={styles.itemTitle}>{content.title}</div>
                          <div className={styles.itemSub}>slug: {content.slug}</div>
                          <p className={styles.meta}>
                            {content.summary ?? '요약 정보가 없는 콘텐츠입니다.'}
                          </p>
                        </>
                      ) : (
                        <div className={styles.empty}>아직 등록된 콘텐츠가 없습니다.</div>
                      )}
                    </div>
                  </article>
                );
              })}
            </div>
          </div>
        </section>

        <div className={styles.doubleGrid}>
          <section className={styles.panel}>
            <div className={styles.panelHeader}>
              <div>
                <h2 className={styles.panelTitle}>최근 구독자</h2>
                <p className={styles.panelDescription}>
                  회원가입과 공개 구독 모달에서 들어온 구독 데이터를 함께 보여줍니다.
                </p>
              </div>
            </div>
            <div className={styles.panelBody}>
              {loading ? (
                <div className={styles.empty}>구독자 목록을 불러오는 중입니다.</div>
              ) : overview.subscribers.length === 0 ? (
                <div className={styles.empty}>등록된 구독자가 없습니다.</div>
              ) : (
                <div className={styles.list}>
                  {overview.subscribers.map((subscriber: AdminSubscriptionSubscriber) => (
                    <article key={subscriber.id} className={styles.listItem}>
                      <div className={styles.itemTop}>
                        <div>
                          <div className={styles.itemTitle}>{subscriber.email}</div>
                          <div className={styles.itemSub}>유입 경로 {subscriber.source}</div>
                        </div>
                        <span className={styles.chip}>
                          {subscriber.active ? '활성 구독' : '중지됨'}
                        </span>
                      </div>
                      <div className={styles.chipRow}>
                        {subscriber.subscribedTargets.map(target => (
                          <span key={`${subscriber.id}-${target}`} className={styles.chip}>
                            {target}
                          </span>
                        ))}
                      </div>
                      <p className={styles.meta}>
                        동의일 {subscriber.consentedAt}
                        <br />
                        구독일 {subscriber.subscribedAt}
                      </p>
                    </article>
                  ))}
                </div>
              )}
            </div>
          </section>

          <section className={styles.panel}>
            <div className={styles.panelHeader}>
              <div>
                <h2 className={styles.panelTitle}>최근 발송 이력</h2>
                <p className={styles.panelDescription}>
                  예약 배치나 기존 시드 데이터로 기록된 메일 발송 결과를 확인할 수 있습니다.
                </p>
              </div>
            </div>
            <div className={styles.panelBody}>
              {loading ? (
                <div className={styles.empty}>발송 이력을 불러오는 중입니다.</div>
              ) : overview.dispatches.length === 0 ? (
                <div className={styles.empty}>아직 발송 이력이 없습니다.</div>
              ) : (
                <div className={styles.list}>
                  {overview.dispatches.map((dispatch: AdminSubscriptionDispatch) => (
                    <article key={dispatch.id} className={styles.listItem}>
                      <div className={styles.itemTop}>
                        <div>
                          <div className={styles.itemTitle}>{dispatch.contentTitle}</div>
                          <div className={styles.itemSub}>
                            {dispatch.contentTypeLabel} / {dispatch.dispatchedAt}
                          </div>
                        </div>
                        <span className={styles.chip}>{dispatch.dispatchStatusLabel}</span>
                      </div>
                      <p className={styles.meta}>
                        채널 {dispatch.dispatchChannelLabel} · 수신자 {dispatch.recipientCount}명
                        <br />
                        slug {dispatch.contentSlug}
                      </p>
                    </article>
                  ))}
                </div>
              )}
            </div>
          </section>
        </div>
      </div>
    </AdminMarketingShell>
  );
}

export default AdminSubscriptionsPage;
