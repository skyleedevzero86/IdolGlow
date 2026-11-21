import http from 'k6/http';
import { check } from 'k6';

export const options = {
  //vus: 200000000,
  vus: 200000,
  //duration: '30s',
  duration: '5m',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  const res = http.get('http://59.24.204.101:8080/products');
  check(res, { 'status 200': (r) => r.status === 200 });
}
