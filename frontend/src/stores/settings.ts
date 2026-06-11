import { defineStore } from 'pinia';
import { ref } from 'vue';

function readHoldToReveal(): boolean {
  if (typeof localStorage === 'undefined') return true;
  const val = localStorage.getItem('ui_hold_to_reveal');
  return val === null ? true : val === 'true';
}

export const useSettingsStore = defineStore('settings', () => {
  const holdToReveal = ref(readHoldToReveal());

  function setHoldToReveal(val: boolean) {
    holdToReveal.value = val;
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('ui_hold_to_reveal', String(val));
    }
  }

  return { holdToReveal, setHoldToReveal };
});
