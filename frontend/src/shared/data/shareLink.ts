export type ShareLinkResult = "shared" | "copied" | "cancelled" | "failed";

const isAbort = (error: unknown): boolean =>
  error instanceof DOMException && error.name === "AbortError";

export const absoluteUrl = (path: string): string =>
  new URL(path, window.location.origin).toString();

export async function shareLink(
  title: string,
  path: string,
): Promise<ShareLinkResult> {
  const url = absoluteUrl(path);
  if (typeof navigator.share === "function") {
    try {
      await navigator.share({ title, url });
      return "shared";
    } catch (error) {
      if (isAbort(error)) return "cancelled";
    }
  }
  try {
    await navigator.clipboard.writeText(url);
    return "copied";
  } catch {
    return "failed";
  }
}
