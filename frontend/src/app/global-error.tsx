"use client";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <html lang="ko">
      <body>
        <h2>문제가 발생했습니다.</h2>
        {process.env.NODE_ENV === "development" && error.message ? (
          <pre style={{ fontSize: 12, opacity: 0.8 }}>{error.message}</pre>
        ) : null}
        <button type="button" onClick={() => reset()}>
          다시 시도
        </button>
      </body>
    </html>
  );
}
