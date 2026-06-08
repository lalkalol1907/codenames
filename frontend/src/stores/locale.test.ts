import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useLocaleStore } from '@/stores/locale';

vi.mock('@/api/client', () => ({
  api: {
    getI18n: vi.fn(),
    setLocale: vi.fn(),
  },
}));

describe('useLocaleStore', () => {
  beforeEach(() => {
    document.cookie = 'ui_locale=ru';
    setActivePinia(createPinia());
  });

  it('reads locale from cookie', () => {
    const store = useLocaleStore();
    expect(store.locale).toBe('ru');
  });

  it('interpolates placeholders in t()', () => {
    const store = useLocaleStore();
    store.messages = {
      'lobby.waiting_hint': 'Waiting ({0}/4)',
    };

    expect(store.t('lobby.waiting_hint', 2)).toBe('Waiting (2/4)');
  });

  it('falls back to key when message is missing', () => {
    const store = useLocaleStore();
    store.messages = {};

    expect(store.t('missing.key')).toBe('missing.key');
  });
});
