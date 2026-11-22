import http from 'k6/http';
import { Counter } from 'k6/metrics';

const created = new Counter('reserve_created');
const rejected = new Counter('reserve_rejected');
const serverError = new Counter('reserve_server_error');

const PRODUCT_ID = __ENV.PRODUCT_ID || '2';
const SLOT_ID = __ENV.SLOT_ID || '30';
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    burst: {
      executor: 'shared-iterations',
      vus: 10,
      //vus: 200000,
      iterations: 10,
      //iterations: 200000,
      maxDuration: '30s',
      //maxDuration: '5m',
    },
  },
};

export function setup() {
  if (!__ENV.EMAIL || !__ENV.PASSWORD) {
    if (!__ENV.TOKEN) throw new Error('EMAIL·PASSWORD 또는 TOKEN 환경 변수가 필요합니다.');
    return { token: __ENV.TOKEN };
  }
  const res = http.post(
    `${BASE_URL}/auth/password/login`,
    JSON.stringify({ email: __ENV.EMAIL, password: __ENV.PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  if (res.status !== 200) throw new Error(`로그인 실패 status=${res.status}`);
  return { token: res.json('accessToken') };
}

export default function (data) {
  const res = http.post(
    `${BASE_URL}/products/${PRODUCT_ID}/reservations`,
    //JSON.stringify({ reservationSlotId: Number(SLOT_ID) }),
    JSON.stringify({ reservationSlotId: Number(SLOT_ID), totalPrice: Number(__ENV.PRICE || 59000) }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${data.token}`,
      },
    }
  );
  if (res.status === 201) created.add(1);
  else if (res.status >= 500) serverError.add(1);
  else rejected.add(1);
  console.log(`status=${res.status} body=${res.body.substring(0, 120)}`);
}