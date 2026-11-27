import http from 'k6/http';
import { Counter, Trend } from 'k6/metrics';

const created = new Counter('reserve_created');
const rejected = new Counter('reserve_rejected');
const serverError = new Counter('reserve_server_error');
const rejectTime = new Trend('reserve_reject_ms', true);
const createTime = new Trend('reserve_create_ms', true);

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PRODUCT_ID = __ENV.PRODUCT_ID || '2';
const TOTAL_PRICE = Number(__ENV.TOTAL_PRICE || 59000);
const MODE = __ENV.MODE || 'burst';
const VUS = Number(__ENV.VUS || 100);
const SLOT_FROM = Number(__ENV.SLOT_FROM || 1);
const SLOT_COUNT = Number(__ENV.SLOT_COUNT || 1);
const RATE_MODES = ['warmup', 'stream'];

export const options = {
  summaryTrendStats: ['avg', 'p(50)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    run: RATE_MODES.includes(MODE)
      ? {
          executor: 'constant-arrival-rate',
          rate: Number(__ENV.RATE || 100),
          timeUnit: '1s',
          duration: __ENV.DURATION || '15s',
          preAllocatedVUs: 200,
          maxVUs: 400,
        }
      : { executor: 'shared-iterations', vus: VUS, iterations: VUS, maxDuration: '120s' },
  },
};

export function setup() {
  if (!__ENV.TOKEN) throw new Error('TOKEN 환경 변수에 로그인 토큰을 넣어 주세요.');
}

export default function () {
  const slot = MODE === 'burst' ? SLOT_FROM : SLOT_FROM + Math.floor(Math.random() * SLOT_COUNT);
  const res = http.post(
    `${BASE_URL}/products/${PRODUCT_ID}/reservations`,
    JSON.stringify({ reservationSlotId: slot, totalPrice: TOTAL_PRICE }),
    { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${__ENV.TOKEN}` } },
  );
  if (res.status === 201) {
    created.add(1);
    createTime.add(res.timings.duration);
  } else if (res.status >= 500 || res.status === 0) {
    serverError.add(1);
  } else {
    rejected.add(1);
    rejectTime.add(res.timings.duration);
  }
}
