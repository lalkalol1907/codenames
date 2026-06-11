import { defineStore } from 'pinia';
import { ref } from 'vue';

export type UiTheme = 'retro' | 'midnight' | 'light';

function readTheme(): UiTheme {
  if (typeof localStorage === 'undefined') return 'retro';
  const val = localStorage.getItem('ui_theme');
  return val === 'midnight' || val === 'light' ? val : 'retro';
}

export const useThemeStore = defineStore('theme', () => {
  const theme = ref<UiTheme>(readTheme());

  function applyTheme(t: UiTheme) {
    if (typeof document === 'undefined') return;
    if (t === 'retro') {
      document.documentElement.removeAttribute('data-theme');
    } else {
      document.documentElement.dataset.theme = t;
    }
  }

  function setTheme(next: UiTheme) {
    theme.value = next;
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('ui_theme', next);
    }
    applyTheme(next);
  }

  return { theme, setTheme, applyTheme };
});
