export const UI_LANG_STORAGE_KEY = 'idolglow-ui-lang';

export const UI_LANGS = ['ko', 'en', 'ja', 'zh'] as const;

export type UiLang = (typeof UI_LANGS)[number];

const ACCEPT_LANGUAGE: Record<UiLang, string> = {
  ko: 'ko-KR,ko;q=0.9,en;q=0.8',
  en: 'en-US,en;q=0.9,ko;q=0.8',
  ja: 'ja-JP,ja;q=0.9,en;q=0.8,ko;q=0.7',
  zh: 'zh-CN,zh;q=0.9,en;q=0.8,ko;q=0.7',
};

const LANG_CODE: Record<UiLang, string> = {
  ko: 'KO',
  en: 'EN',
  ja: 'JA',
  zh: 'ZH',
};

const LANG_ARIA: Record<UiLang, string> = {
  ko: '한국어 사용 중. 영어로 전환',
  en: '영어 사용 중. 일본어로 전환',
  ja: '일본어 사용 중. 중국어로 전환',
  zh: '중국어 사용 중. 한국어로 전환',
};

export function isUiLang(value: string | null): value is UiLang {
  return value === 'ko' || value === 'en' || value === 'ja' || value === 'zh';
}

export function readUiLang(): UiLang {
  if (typeof window === 'undefined') return 'ko';
  const stored = window.localStorage.getItem(UI_LANG_STORAGE_KEY);
  return isUiLang(stored) ? stored : 'ko';
}

export function writeUiLang(lang: UiLang): void {
  try {
    window.localStorage.setItem(UI_LANG_STORAGE_KEY, lang);
  } catch {
    return;
  }
}

export function nextUiLang(current: UiLang): UiLang {
  const index = UI_LANGS.indexOf(current);
  return UI_LANGS[(index + 1) % UI_LANGS.length];
}

export function uiLangCode(lang: UiLang): string {
  return LANG_CODE[lang];
}

export function uiLangAria(lang: UiLang): string {
  return LANG_ARIA[lang];
}

export function acceptLanguageHeader(): string {
  return ACCEPT_LANGUAGE[readUiLang()];
}
