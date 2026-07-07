import { acceptLanguageHeader } from '../ui/i18n/uiLangStorage';
import { getApiBaseUrl } from './authConfig';
import { getCookie } from './cookieUtil';

const CSRF_HEADER = 'X-Refresh-CSRF';

function withAcceptLanguage(headers?: Record<string, string>): Record<string, string> {
  return { 'Accept-Language': acceptLanguageHeader(), ...headers };
}

export type UserRole = 'USER' | 'ADMIN';

export type UserLoginInfo = {
  id: number;
  email: string;
  nickname: string;
  name: string | null;
  picture: string | null;
  oauthLinked: boolean;
  oauthProviders?: readonly string[];
  linkedProviders?: readonly string[];
  authProvider?: string | null;
  provider?: string | null;
  loginProvider?: string | null;
  signupProvider?: string | null;
  /** 서버: password_hash 보유(이메일 가입 등). SNS 전용 계정은 false. 구버전 API 미전달 시 생략 가능 */
  hasPassword?: boolean;
  role: UserRole;
  lastLoginAt: string | null;
};

export function emailLocalPart(email: string): string {
  const i = email.indexOf('@');
  return i === -1 ? email : email.slice(0, i);
}

export function formatMyPageProfileLine(user: UserLoginInfo): string {
  const nick = user.nickname;
  if (user.oauthLinked) {
    const profileName = user.name?.trim();
    if (profileName) {
      return `${profileName} (${nick})`;
    }
  }
  return `${nick} (${emailLocalPart(user.email)})`;
}

type AccessTokenJson = {
  grantType: string;
  accessToken: string;
  accessTokenExpiresIn: number;
};

type PasswordLoginJson = AccessTokenJson & {
  requirePasswordChange: boolean;
};

export function getGoogleLoginUrl(): string {
  return getOAuthLoginUrl('google');
}

export function getOAuthLoginUrl(provider: 'google' | 'naver' | 'kakao'): string {
  return `${getApiBaseUrl()}/auth/login/${provider}`;
}

export async function reissueAccessToken(): Promise<string | null> {
  const csrf = getCookie('refreshCsrfToken');
  if (!csrf) return null;
  try {
    const res = await fetch(`${getApiBaseUrl()}/auth/reissue`, {
      method: 'POST',
      credentials: 'include',
      headers: withAcceptLanguage({ [CSRF_HEADER]: csrf }),
    });
    if (!res.ok) return null;
    const data = (await res.json()) as AccessTokenJson;
    return data.accessToken ?? null;
  } catch {
    return null;
  }
}

async function fetchLoginUserWithHeaders(headers?: Record<string, string>): Promise<UserLoginInfo | null> {
  try {
    const res = await fetch(`${getApiBaseUrl()}/mypage/user`, {
      cache: 'no-store',
      headers: withAcceptLanguage(headers),
      credentials: 'include',
    });
    if (!res.ok) return null;
    return (await res.json()) as UserLoginInfo;
  } catch {
    return null;
  }
}

async function requestAuthedBlob(accessToken: string, path: string): Promise<Blob> {
  const res = await fetch(`${getApiBaseUrl()}${path}`, {
    headers: withAcceptLanguage({ Authorization: `Bearer ${accessToken}` }),
    credentials: 'include',
  });
  if (!res.ok) {
    throw new Error(`다운로드에 실패했습니다. (${res.status})`);
  }
  return res.blob();
}

function triggerDownload(blob: Blob, filename: string): void {
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.append(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(url);
}

export async function fetchLoginUser(accessToken?: string | null): Promise<UserLoginInfo | null> {
  if (!accessToken) {
    return null;
  }
  const bearerHeaders = accessToken ? { Authorization: `Bearer ${accessToken}` } : undefined;
  return fetchLoginUserWithHeaders(bearerHeaders);
}

export type MypagePrimaryPromo = {
  variant: string;
  textBeforeHighlight: string;
  highlight: string;
  textAfterHighlight: string;
  href: string;
  ctaLabel: string;
};

export const MYPAGE_PRIMARY_PROMO_FALLBACK: MypagePrimaryPromo = {
  variant: 'DEFAULT',
  textBeforeHighlight: '아티클부터 이벤트까지, ',
  highlight: 'IDOL GLOW',
  textAfterHighlight: '와 함께하는 문화 큐레이션',
  href: '/articles',
  ctaLabel: '둘러보기 →',
};

export type MypageSecondaryPromo = {
  variant: string;
  textBeforeStrong: string;
  strong: string;
  textAfterStrong: string;
  metricValue: number;
  metricUnit: string;
  href: string;
};

export type MypagePromoStrip = {
  primary: MypagePrimaryPromo;
  secondary: MypageSecondaryPromo;
};

export const MYPAGE_PROMO_STRIP_FALLBACK: MypagePromoStrip = {
  primary: MYPAGE_PRIMARY_PROMO_FALLBACK,
  secondary: {
    variant: 'EMPTY',
    textBeforeStrong: '예약·이용 후 ',
    strong: '리뷰와 일정',
    textAfterStrong: ' 을 정리해 보세요.',
    metricValue: 0,
    metricUnit: '건 예약',
    href: '/mypage#recent-bookings',
  },
};

export async function fetchMypagePromoStrip(accessToken: string): Promise<MypagePromoStrip | null> {
  const res = await fetch(`${getApiBaseUrl()}/mypage/promo`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    credentials: 'include',
  });
  if (!res.ok) return null;
  return (await res.json()) as MypagePromoStrip;
}

export type ReservationSummary = {
  reservationId: number;
  status: 'PREBOOK' | 'PENDING' | 'BOOKED' | 'COMPLETED' | 'CANCELED';
  productId: number;
  productName: string;
  productDescription: string;
  totalPrice: number;
  visitDate: string;
  visitStartTime: string;
  visitEndTime: string;
  attractions: string[];
  expiresAt: string | null;
  confirmedAt: string | null;
  canceledAt: string | null;
  cancelReason: string | null;
};

export type MyPaymentSummary = {
  paymentId: number;
  reservationId: number;
  productId: number;
  productName: string;
  provider: string;
  paymentReference: string;
  amount: number;
  cancelAmount: number;
  status: 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'CANCELED' | 'EXPIRED' | 'REFUNDED' | 'PARTIAL_CANCELED';
  failureReason: string | null;
  approvedAt: string | null;
  failedAt: string | null;
  canceledAt: string | null;
  visitDate: string;
  visitStartTime: string;
  visitEndTime: string;
  canCancel: boolean;
  cancelDeadlineAt: string | null;
  receiptAvailable: boolean;
};

export async function fetchMyBookings(accessToken: string): Promise<ReservationSummary[] | null> {
  const res = await fetch(`${getApiBaseUrl()}/mypage/bookings`, {
    headers: withAcceptLanguage({ Authorization: `Bearer ${accessToken}` }),
    credentials: 'include',
  });
  if (!res.ok) return null;
  return (await res.json()) as ReservationSummary[];
}

export type MyProductReviewImage = {
  id: number;
  originalFilename: string;
  url: string;
  sortOrder: number;
};

export type MyProductReviewSummary = {
  reviewId: number;
  productId: number;
  userId: number;
  rating: number;
  content: string;
  createdAt: string;
  images: readonly MyProductReviewImage[];
  verifiedPurchase: boolean;
  helpfulCount: number;
  hidden: boolean;
};

export async function fetchMyReviews(accessToken: string): Promise<MyProductReviewSummary[] | null> {
  const res = await fetch(`${getApiBaseUrl()}/mypage/reviews`, {
    headers: withAcceptLanguage({ Authorization: `Bearer ${accessToken}` }),
    credentials: 'include',
  });
  if (!res.ok) return null;
  return (await res.json()) as MyProductReviewSummary[];
}

export async function fetchMyPayments(accessToken: string): Promise<MyPaymentSummary[] | null> {
  const res = await fetch(`${getApiBaseUrl()}/mypage/payments`, {
    headers: withAcceptLanguage({ Authorization: `Bearer ${accessToken}` }),
    credentials: 'include',
  });
  if (!res.ok) return null;
  return (await res.json()) as MyPaymentSummary[];
}

export async function cancelMyPayment(
  accessToken: string,
  paymentId: number,
  reason?: string
): Promise<MyPaymentSummary | null> {
  const res = await fetch(`${getApiBaseUrl()}/mypage/payments/${paymentId}/cancel`, {
    method: 'POST',
    headers: withAcceptLanguage({
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    }),
    credentials: 'include',
    body: JSON.stringify({ reason: reason?.trim() || null }),
  });
  if (!res.ok) return null;
  return (await res.json()) as MyPaymentSummary;
}

export async function downloadMyPaymentReceipt(accessToken: string, paymentId: number): Promise<void> {
  const blob = await requestAuthedBlob(accessToken, `/mypage/payments/${paymentId}/receipt.pdf`);
  triggerDownload(blob, `my-payment-${paymentId}-receipt.pdf`);
}

export type ScheduleResponse = {
  scheduleId: number;
  productId: number;
  title: string;
  startAt: string;
  endAt: string;
};

export type ScheduleSliceResponse = {
  schedules: ScheduleResponse[];
  hasNext: boolean;
  nextCursorId: number | null;
};

export async function fetchSchedulesSlice(
  accessToken: string,
  params?: { cursorId?: number; size?: number }
): Promise<ScheduleSliceResponse | null> {
  const sp = new URLSearchParams();
  if (params?.cursorId != null) sp.set('cursorId', String(params.cursorId));
  if (params?.size != null) sp.set('size', String(params.size));
  const q = sp.toString();
  const url = `${getApiBaseUrl()}/schedules${q ? `?${q}` : ''}`;
  const res = await fetch(url, {
    headers: withAcceptLanguage({ Authorization: `Bearer ${accessToken}` }),
    credentials: 'include',
  });
  if (!res.ok) return null;
  return (await res.json()) as ScheduleSliceResponse;
}

export type UpdateProfileBody = {
  nickname?: string;
  profileImageUrl?: string;
};

export async function updateProfile(
  accessToken: string,
  body: UpdateProfileBody
): Promise<UserLoginInfo | null> {
  const res = await fetch(`${getApiBaseUrl()}/users`, {
    method: 'PATCH',
    headers: withAcceptLanguage({
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    }),
    credentials: 'include',
    body: JSON.stringify(body),
  });
  if (!res.ok) return null;
  return (await res.json()) as UserLoginInfo;
}

export type ChangePasswordBody = {
  currentPassword: string;
  newPassword: string;
};

export async function changePassword(
  accessToken: string,
  body: ChangePasswordBody
): Promise<{ ok: true } | { ok: false; message: string }> {
  const res = await fetch(`${getApiBaseUrl()}/users/password`, {
    method: 'PATCH',
    headers: withAcceptLanguage({
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    }),
    credentials: 'include',
    body: JSON.stringify(body),
  });
  if (res.status === 204 || res.ok) {
    return { ok: true };
  }
  let message = '비밀번호 변경에 실패했습니다. 입력값을 확인해 주세요.';
  try {
    const errBody = (await res.json()) as { message?: string };
    if (typeof errBody?.message === 'string' && errBody.message.trim()) {
      message = errBody.message.trim();
    }
  } catch {
    /* ignore */
  }
  return { ok: false, message };
}

export type UploadProfileImageResult =
  | { ok: true; user: UserLoginInfo }
  | { ok: false; message: string };

export async function uploadProfileImage(
  accessToken: string,
  file: File
): Promise<UploadProfileImageResult> {
  const fd = new FormData();
  fd.append('file', file);
  const res = await fetch(`${getApiBaseUrl()}/users/profile-image`, {
    method: 'POST',
    headers: withAcceptLanguage({ Authorization: `Bearer ${accessToken}` }),
    credentials: 'include',
    body: fd,
  });
  if (!res.ok) {
    let message = '이미지 업로드에 실패했습니다. 잠시 후 다시 시도해 주세요.';
    try {
      const body = (await res.json()) as { message?: string };
      if (typeof body?.message === 'string' && body.message.trim()) {
        message = body.message.trim();
      }
    } catch {
      /* non-JSON body */
    }
    return { ok: false, message };
  }
  const user = (await res.json()) as UserLoginInfo;
  return { ok: true, user };
}

export type SignupCheckResponse = {
  available: boolean;
  code?: string | null;
};

export type SignupEmailVerificationRequestResponse = {
  sent: boolean;
  reason?: string;
  expiresInSeconds?: number;
};

export async function checkSignupEmail(email: string): Promise<SignupCheckResponse | null> {
  const params = new URLSearchParams();
  params.set('email', email.trim());
  const res = await fetch(`${getApiBaseUrl()}/auth/signup/check-email?${params}`, {
    headers: withAcceptLanguage(),
    credentials: 'include',
  });
  if (!res.ok) return null;
  return (await res.json()) as SignupCheckResponse;
}

export async function checkSignupNickname(nickname: string): Promise<SignupCheckResponse | null> {
  const params = new URLSearchParams();
  params.set('nickname', nickname.trim());
  const res = await fetch(`${getApiBaseUrl()}/auth/signup/check-nickname?${params}`, {
    headers: withAcceptLanguage(),
    credentials: 'include',
  });
  if (!res.ok) return null;
  return (await res.json()) as SignupCheckResponse;
}

export async function requestSignupEmailVerification(
  email: string
): Promise<SignupEmailVerificationRequestResponse | null> {
  const params = new URLSearchParams();
  params.set('email', email.trim());
  const res = await fetch(`${getApiBaseUrl()}/auth/signup/email-verification/request?${params}`, {
    method: 'POST',
    headers: withAcceptLanguage(),
    credentials: 'include',
  });
  if (!res.ok) return null;
  return (await res.json()) as SignupEmailVerificationRequestResponse;
}

export async function signupWithPassword(body: {
  email: string;
  nickname: string;
  password: string;
  subscribeToUpdates?: boolean;
}): Promise<{ ok: true; accessToken: string } | { ok: false; message: string }> {
  const res = await fetch(`${getApiBaseUrl()}/auth/signup`, {
    method: 'POST',
    headers: withAcceptLanguage({ 'Content-Type': 'application/json' }),
    credentials: 'include',
    body: JSON.stringify(body),
  });
  const text = await res.text();
  if (!res.ok) {
    try {
      const j = JSON.parse(text) as { message?: string };
      return { ok: false, message: j.message ?? '회원가입에 실패했습니다.' };
    } catch {
      return { ok: false, message: '회원가입에 실패했습니다.' };
    }
  }
  const data = JSON.parse(text) as AccessTokenJson;
  const token = data.accessToken ?? '';
  if (!token) return { ok: false, message: '응답에 토큰이 없습니다.' };
  return { ok: true, accessToken: token };
}

export async function loginWithPassword(body: {
  email: string;
  password: string;
}): Promise<{ ok: true; accessToken: string; requirePasswordChange: boolean } | { ok: false; message: string }> {
  const res = await fetch(`${getApiBaseUrl()}/auth/password/login`, {
    method: 'POST',
    headers: withAcceptLanguage({ 'Content-Type': 'application/json' }),
    credentials: 'include',
    body: JSON.stringify(body),
  });
  const text = await res.text();
  if (!res.ok) {
    try {
      const j = JSON.parse(text) as { message?: string };
      return { ok: false, message: j.message ?? '로그인에 실패했습니다.' };
    } catch {
      return { ok: false, message: '로그인에 실패했습니다.' };
    }
  }
  const data = JSON.parse(text) as PasswordLoginJson;
  if (!data.accessToken) {
    return { ok: false, message: '응답에 토큰이 없습니다.' };
  }
  return {
    ok: true,
    accessToken: data.accessToken,
    requirePasswordChange: Boolean(data.requirePasswordChange),
  };
}

export async function requestTemporaryPassword(email: string): Promise<{ sent: boolean; message: string } | null> {
  const res = await fetch(`${getApiBaseUrl()}/auth/password/temporary`, {
    method: 'POST',
    headers: withAcceptLanguage({ 'Content-Type': 'application/json' }),
    credentials: 'include',
    body: JSON.stringify({ email: email.trim() }),
  });
  if (!res.ok) return null;
  const data = (await res.json()) as { sent?: boolean; message?: string };
  return {
    sent: Boolean(data.sent),
    message: data.message?.trim() || '요청을 처리했습니다.',
  };
}

export async function requestAccountIdReminder(
  email: string
): Promise<{ sent: boolean; message: string } | null> {
  const res = await fetch(`${getApiBaseUrl()}/auth/account/find-id`, {
    method: 'POST',
    headers: withAcceptLanguage({ 'Content-Type': 'application/json' }),
    credentials: 'include',
    body: JSON.stringify({ email: email.trim() }),
  });
  if (!res.ok) return null;
  const data = (await res.json()) as { sent?: boolean; message?: string };
  return {
    sent: Boolean(data.sent),
    message: data.message?.trim() || '요청을 처리했습니다.',
  };
}

export async function logoutRequest(): Promise<void> {
  const csrf = getCookie('refreshCsrfToken');
  if (!csrf) return;
  await fetch(`${getApiBaseUrl()}/auth/logout`, {
    method: 'POST',
    credentials: 'include',
    headers: withAcceptLanguage({ [CSRF_HEADER]: csrf }),
  });
}
