import { useState } from "react";
import { useAuth } from "../../../auth/AuthContext";
import { apiErrorMessage } from "../../../shared/data/apiClient";
import {
  ADVERTISING_DISPATCH_HOURS,
  type AdminSubscriptionOverviewResponse,
  type AdminSubscriptionSchedule,
  type SubscriptionContentType,
  type SubscriptionScheduleDayOfWeek,
  type SubscriptionScheduleFrequency,
  upsertAdminSubscriptionSchedule,
} from "../../../shared/data/subscriptionAdminApi";
import styles from "./AdminSubscriptionsPage.module.css";

const DAY_OPTIONS: ReadonlyArray<{
  value: SubscriptionScheduleDayOfWeek;
  label: string;
}> = [
  { value: "MONDAY", label: "월요일" },
  { value: "TUESDAY", label: "화요일" },
  { value: "WEDNESDAY", label: "수요일" },
  { value: "THURSDAY", label: "목요일" },
  { value: "FRIDAY", label: "금요일" },
  { value: "SATURDAY", label: "토요일" },
  { value: "SUNDAY", label: "일요일" },
];

type ScheduleFormState = {
  readonly frequencyType: SubscriptionScheduleFrequency;
  readonly dayOfWeek: SubscriptionScheduleDayOfWeek | null;
  readonly dispatchTime: string;
  readonly active: boolean;
};

const MANAGED_CONTENT_TYPES = ["NEWSLETTER", "PRODUCT_RECOMMENDATION"] as const;
type ManagedContentType = (typeof MANAGED_CONTENT_TYPES)[number];

const DEFAULT_SCHEDULES: Record<ManagedContentType, ScheduleFormState> = {
  NEWSLETTER: {
    frequencyType: "WEEKLY",
    dayOfWeek: "MONDAY",
    dispatchTime: "09:00",
    active: true,
  },
  PRODUCT_RECOMMENDATION: {
    frequencyType: "WEEKLY",
    dayOfWeek: "MONDAY",
    dispatchTime: "10:00",
    active: false,
  },
};

const CONTENT_TYPE_COPY: Record<
  ManagedContentType,
  { readonly title: string; readonly label: string }
> = {
  NEWSLETTER: { title: "뉴스레터 발송", label: "뉴스레터" },
  PRODUCT_RECOMMENDATION: { title: "상품 추천 메일 발송", label: "상품 추천" },
};

const MINUTE_OPTIONS: readonly number[] = Array.from(
  { length: 60 },
  (_, index) => index,
);

const pad2 = (value: number): string => String(value).padStart(2, "0");

const splitDispatchTime = (
  value: string,
): { readonly hour: number; readonly minute: number } => {
  const [hourText, minuteText] = value.split(":");
  const hour = Number(hourText);
  const minute = Number(minuteText);
  return {
    hour: Number.isInteger(hour) ? hour : 0,
    minute: Number.isInteger(minute) ? minute : 0,
  };
};

const isAdvertisingHour = (dispatchTime: string): boolean =>
  ADVERTISING_DISPATCH_HOURS.includes(splitDispatchTime(dispatchTime).hour);

const toFormState = (
  contentType: ManagedContentType,
  schedule?: AdminSubscriptionSchedule | null,
): ScheduleFormState =>
  schedule
    ? {
        frequencyType: schedule.frequencyType,
        dayOfWeek: schedule.dayOfWeek,
        dispatchTime: schedule.dispatchTime,
        active: schedule.active,
      }
    : DEFAULT_SCHEDULES[contentType];

const buildForms = (
  schedules: readonly AdminSubscriptionSchedule[],
): Record<ManagedContentType, ScheduleFormState> => ({
  NEWSLETTER: toFormState(
    "NEWSLETTER",
    schedules.find((schedule) => schedule.contentType === "NEWSLETTER"),
  ),
  PRODUCT_RECOMMENDATION: toFormState(
    "PRODUCT_RECOMMENDATION",
    schedules.find(
      (schedule) => schedule.contentType === "PRODUCT_RECOMMENDATION",
    ),
  ),
});

function AdvertisingTimeField({
  value,
  onChange,
}: {
  readonly value: string;
  readonly onChange: (dispatchTime: string) => void;
}) {
  const { hour, minute } = splitDispatchTime(value);
  const hourInRange = ADVERTISING_DISPATCH_HOURS.includes(hour);

  return (
    <div className={styles.field}>
      <span className={styles.fieldLabel}>발송 시간 (08~20시)</span>
      <div className={styles.timeSelectRow}>
        <select
          className={styles.fieldControl}
          aria-label="발송 시"
          value={hourInRange ? String(hour) : ""}
          onChange={(event) =>
            onChange(`${pad2(Number(event.target.value))}:${pad2(minute)}`)
          }
        >
          {hourInRange ? null : (
            <option value="" disabled>
              시 선택
            </option>
          )}
          {ADVERTISING_DISPATCH_HOURS.map((option) => (
            <option key={option} value={option}>
              {pad2(option)}시
            </option>
          ))}
        </select>
        <select
          className={styles.fieldControl}
          aria-label="발송 분"
          value={String(minute)}
          onChange={(event) =>
            onChange(`${pad2(hour)}:${pad2(Number(event.target.value))}`)
          }
        >
          {MINUTE_OPTIONS.map((option) => (
            <option key={option} value={option}>
              {pad2(option)}분
            </option>
          ))}
        </select>
      </div>
      {hourInRange ? null : (
        <span className={styles.fieldWarning}>
          저장된 시각 {value}은 허용 범위 밖입니다. 8~20시 중에서 골라 주세요.
        </span>
      )}
    </div>
  );
}

type SchedulesTabProps = {
  readonly overview: AdminSubscriptionOverviewResponse | null;
  readonly loading: boolean;
  readonly error: string | null;
  readonly onSaved: (schedule: AdminSubscriptionSchedule) => void;
};

export function SchedulesTab({
  overview,
  loading,
  error,
  onSaved,
}: SchedulesTabProps) {
  const { accessToken } = useAuth();
  const [forms, setForms] = useState(() =>
    buildForms(overview?.schedules ?? []),
  );
  const [savingType, setSavingType] = useState<ManagedContentType | null>(null);
  const [feedback, setFeedback] = useState<{
    readonly type: "ok" | "error";
    readonly text: string;
  } | null>(null);

  const patchForm = (
    contentType: ManagedContentType,
    patch: Partial<ScheduleFormState>,
  ) => {
    setForms((current) => ({
      ...current,
      [contentType]: { ...current[contentType], ...patch },
    }));
  };

  const handleSave = async (contentType: ManagedContentType) => {
    if (!accessToken) return;
    const current = forms[contentType];
    if (
      contentType === "PRODUCT_RECOMMENDATION" &&
      !isAdvertisingHour(current.dispatchTime)
    ) {
      setFeedback({
        type: "error",
        text: "상품 추천 메일은 8시부터 20시 사이에만 발송할 수 있습니다.",
      });
      return;
    }
    setSavingType(contentType);
    setFeedback(null);
    try {
      const saved = await upsertAdminSubscriptionSchedule(
        accessToken,
        contentType,
        {
          frequencyType: current.frequencyType,
          dayOfWeek:
            current.frequencyType === "WEEKLY" ? current.dayOfWeek : null,
          dispatchTime: current.dispatchTime,
          active: current.active,
        },
      );
      onSaved(saved);
      patchForm(contentType, {
        frequencyType: saved.frequencyType,
        dayOfWeek: saved.dayOfWeek,
        dispatchTime: saved.dispatchTime,
        active: saved.active,
      });
      setFeedback({
        type: "ok",
        text: `${CONTENT_TYPE_COPY[contentType].label} 예약 발송 설정을 저장했습니다.`,
      });
    } catch (saveError) {
      setFeedback({
        type: "error",
        text: apiErrorMessage(saveError, "예약 발송 설정 저장에 실패했습니다."),
      });
    } finally {
      setSavingType(null);
    }
  };

  const latestNewsletter = overview?.latestContents.find(
    (content) => content.contentType === "NEWSLETTER",
  );

  return (
    <div className={styles.tabBody}>
      <p className={styles.helperText}>
        콘텐츠를 등록해도 바로 메일을 보내지 않고, 아래 스케줄에 맞춰 아직
        보내지 않은 최신 글만 자동 발송합니다.
      </p>
      {error ? (
        <p className={styles.inlineError} role="alert">
          {error}
        </p>
      ) : null}
      {feedback ? (
        <p
          className={
            feedback.type === "ok" ? styles.inlineSuccess : styles.inlineError
          }
          role={feedback.type === "error" ? "alert" : "status"}
        >
          {feedback.text}
        </p>
      ) : null}

      <div className={styles.scheduleList}>
        {MANAGED_CONTENT_TYPES.map((contentType) => {
          const form = forms[contentType];
          const schedule = overview?.schedules.find(
            (item) => item.contentType === contentType,
          );
          const saving = savingType === contentType;

          return (
            <section key={contentType} className={styles.scheduleRow}>
              <header className={styles.scheduleHead}>
                <div>
                  <h3 className={styles.scheduleTitle}>
                    {CONTENT_TYPE_COPY[contentType].title}
                  </h3>
                  <p className={styles.scheduleMeta}>
                    {loading && !overview
                      ? "설정을 불러오는 중입니다."
                      : schedule?.nextDispatchAt
                        ? `다음 발송 예정 ${schedule.nextDispatchAt}`
                        : "아직 저장된 예약 발송 규칙이 없습니다."}
                  </p>
                </div>
                <span
                  className={`${styles.badge} ${schedule?.active ? styles.badgeOk : styles.badgeMuted}`}
                >
                  {schedule?.active ? "사용 중" : "중지"}
                </span>
              </header>

              <div className={styles.scheduleFields}>
                <div className={styles.field}>
                  <span className={styles.fieldLabel}>반복 주기</span>
                  <select
                    className={styles.fieldControl}
                    aria-label="반복 주기"
                    value={form.frequencyType}
                    onChange={(event) => {
                      const frequencyType = event.target
                        .value as SubscriptionScheduleFrequency;
                      patchForm(contentType, {
                        frequencyType,
                        dayOfWeek:
                          frequencyType === "WEEKLY"
                            ? (form.dayOfWeek ?? "MONDAY")
                            : null,
                      });
                    }}
                  >
                    <option value="DAILY">매일</option>
                    <option value="WEEKLY">매주</option>
                  </select>
                </div>

                {form.frequencyType === "WEEKLY" ? (
                  <div className={styles.field}>
                    <span className={styles.fieldLabel}>발송 요일</span>
                    <select
                      className={styles.fieldControl}
                      aria-label="발송 요일"
                      value={form.dayOfWeek ?? "MONDAY"}
                      onChange={(event) =>
                        patchForm(contentType, {
                          dayOfWeek: event.target
                            .value as SubscriptionScheduleDayOfWeek,
                        })
                      }
                    >
                      {DAY_OPTIONS.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>
                ) : null}

                {contentType === "PRODUCT_RECOMMENDATION" ? (
                  <AdvertisingTimeField
                    value={form.dispatchTime}
                    onChange={(dispatchTime) =>
                      patchForm(contentType, { dispatchTime })
                    }
                  />
                ) : (
                  <div className={styles.field}>
                    <span className={styles.fieldLabel}>발송 시간</span>
                    <input
                      type="time"
                      className={styles.fieldControl}
                      aria-label="발송 시간"
                      value={form.dispatchTime}
                      onChange={(event) =>
                        patchForm(contentType, {
                          dispatchTime: event.target.value,
                        })
                      }
                    />
                  </div>
                )}

                <label className={styles.toggleField}>
                  <input
                    type="checkbox"
                    checked={form.active}
                    onChange={(event) =>
                      patchForm(contentType, { active: event.target.checked })
                    }
                  />
                  <span>예약 발송 사용</span>
                </label>
              </div>

              <footer className={styles.scheduleFoot}>
                <p className={styles.scheduleNote}>
                  {contentType === "PRODUCT_RECOMMENDATION"
                    ? "광고성 정보 수신에 동의한 회원에게만 08:00~20:59 사이에 발송합니다."
                    : `대상 구독자 ${overview?.newsletterSubscriberCount ?? 0}명 · 최신 글 ${
                        latestNewsletter
                          ? `「${latestNewsletter.title}」${latestNewsletter.publishedAt ? ` (${latestNewsletter.publishedAt})` : ""}`
                          : "없음"
                      }`}
                </p>
                <button
                  type="button"
                  className={styles.primaryButton}
                  onClick={() => void handleSave(contentType)}
                  disabled={saving || !overview}
                >
                  {saving ? "저장 중..." : "저장"}
                </button>
              </footer>
            </section>
          );
        })}
      </div>
    </div>
  );
}
