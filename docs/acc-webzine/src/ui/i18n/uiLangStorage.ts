export const UI_LANG_STORAGE_KEY = 'idolglow-ui-lang';

export type UiLang = 'ko' | 'en';

export function readUiLang(): UiLang {
  if (typeof window === 'undefined') return 'ko';
  return window.localStorage.getItem(UI_LANG_STORAGE_KEY) === 'en' ? 'en' : 'ko';
}

export function writeUiLang(lang: UiLang): void {
  try {
    window.localStorage.setItem(UI_LANG_STORAGE_KEY, lang);
  } catch {
    /* ignore quota / private mode */
  }
}

export function acceptLanguageHeader(): string {
  return readUiLang() === 'en' ? 'en-US,en;q=0.9,ko-KR;q=0.8' : 'ko-KR,ko;q=0.9,en;q=0.8';
}
