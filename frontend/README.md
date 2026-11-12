# Idolglow 프론트화면 · 로컬 인증·API

구글 로그인 요청은 **백엔드**에서 처리된다.<br/> 브라우저 주소에 보이는 포트는 그때 띄운 백엔드의 `server.port` 이다.

## Google OAuth (`ERR_TOO_MANY_REDIRECTS` at `/auth/callback`)

OAuth 성공 후 백엔드가 `http://localhost:포트번호/auth/callback` 으로 보냅니다.  
Vite `server.proxy` 에 **`/auth` 전체**를 넣으면 이 URL도 백엔드로 넘어가 무한 리디렉션이 납니다.  
→ `/auth/callback` 은 프록시하지 말고, [`AuthCallbackPage`](src/ui/pages/AuthCallbackPage/AuthCallbackPage.tsx) 라우트로 처리합니다.

## API 포트

| 실행 방식           | 백엔드 포트 | 프론트 `.env.development`                 |
| ------------------- | ----------- | ----------------------------------------- |
| `./gradlew bootRun` | **8080**    | `VITE_API_BASE_URL=http://localhost:8080` |
| Docker `app` 프로필 | **9090**    | `VITE_API_BASE_URL=http://localhost:9090` |

Network 탭의 호스트·포트가 백엔드와 다르면 404 또는 `ERR_CONNECTION_REFUSED` 가 난다.  
→ [`samples/env.development.example`](samples/env.development.example) · [`samples/apiBase.ts`](samples/apiBase.ts) 참고.

## 헤더 메뉴

- **GLOW 추천** → `/glow`
- **나의 아카이브** → `/my-archive` (로그인 필요, 하위: 전체 · 저장 모음 · 최근 본)
- **아카이브** → `/archive` (동일 형식의 하위 메뉴: 전체 · 테마 모음 · 기획 모음)
