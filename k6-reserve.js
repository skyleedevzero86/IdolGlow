import http from 'k6/http';
import { Counter } from 'k6/metrics';

const created = new Counter('reserve_created');
const rejected = new Counter('reserve_rejected');
const serverError = new Counter('reserve_server_error');

const PRODUCT_ID = __ENV.PRODUCT_ID || '2';
const SLOT_ID = __ENV.SLOT_ID || '30';

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

export default function () {
  const res = http.post(
    `http://59.24.204.101:8080/products/${PRODUCT_ID}/reservations`,
    //JSON.stringify({ reservationSlotId: Number(SLOT_ID) }),
    JSON.stringify({ reservationSlotId: Number(SLOT_ID), totalPrice: Number(__ENV.PRICE || 59000) }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${__ENV.TOKEN}`,
      },
    }
  );
  if (res.status === 201) created.add(1);
  else if (res.status >= 500) serverError.add(1);
  else rejected.add(1);
  console.log(`status=${res.status} body=${res.body.substring(0, 120)}`);
}