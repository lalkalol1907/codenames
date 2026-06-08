import { defineStore } from 'pinia';
import { ref } from 'vue';
import { api } from '@/api/client';

export type UiLocale = 'en' | 'ru';

function readLocaleCookie(): UiLocale | null {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.match(/(?:^|; )ui_locale=([^;]*)/);
  const value = match?.[1];
  return value === 'ru' || value === 'en' ? value : null;
}

export const useLocaleStore = defineStore('locale', () => {
  const locale = ref<UiLocale>(readLocaleCookie() ?? 'en');
  const messages = ref<Record<string, string>>({});
  const ready = ref(false);

  async function loadMessages() {
    if (import.meta.env.SSR) {
      const bundled = await import(`@/i18n/bundled/${locale.value}.json`);
      messages.value = bundled.default as Record<string, string>;
      ready.value = true;
      return;
    }

    try {
      messages.value = await api.getI18n(locale.value);
    } catch {
      const bundled = await import(`@/i18n/bundled/${locale.value}.json`);
      messages.value = bundled.default as Record<string, string>;
    }
    ready.value = true;
  }

  async function setLocale(next: UiLocale) {
    locale.value = next;
    if (!import.meta.env.SSR) {
      await api.setLocale(next);
    }
    await loadMessages();
  }

  function t(key: string, ...args: (string | number)[]): string {
    let text = messages.value[key] ?? key;
    args.forEach((arg, index) => {
      text = text.replace(`{${index}}`, String(arg));
    });
    return text;
  }

  return { locale, messages, ready, loadMessages, setLocale, t };
});
