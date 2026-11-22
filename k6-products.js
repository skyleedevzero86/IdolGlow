import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const STAGE_SECONDS = __ENV.STAGE_SECONDS || '30s';

export const options = {
  summaryTrendStats: ['avg', 'p(50)', 'p(95)', 'p(99)', 'max'],
  stages: [
    { duration: '5s', target: 10 },
    { duration: STAGE_SECONDS, target: 10 },
    { duration: '5s', target: 100 },
    { duration: STAGE_SECONDS, target: 100 },
    { duration: '5s', target: 300 },
    { duration: STAGE_SECONDS, target: 300 },
    { duration: '5s', target: 500 },
    { duration: STAGE_SECONDS, target: 500 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/products`);
  check(res, { 'status 200': (r) => r.status === 200 });
}
