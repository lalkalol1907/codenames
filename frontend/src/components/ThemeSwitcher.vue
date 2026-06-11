<script setup lang="ts">
import { computed } from 'vue';
import { useThemeStore, type UiTheme } from '@/stores/theme';
import { useLocaleStore } from '@/stores/locale';

const themeStore = useThemeStore();
const localeStore = useLocaleStore();

const current = computed({
  get: () => themeStore.theme,
  set: (value: UiTheme) => themeStore.setTheme(value),
});
</script>

<template>
  <label class="locale-label">
    <span class="locale-label-text">{{ localeStore.t('theme.label') }}</span>
    <select v-model="current">
      <option value="retro">{{ localeStore.t('theme.retro') }}</option>
      <option value="midnight">{{ localeStore.t('theme.midnight') }}</option>
      <option value="light">{{ localeStore.t('theme.light') }}</option>
    </select>
  </label>
</template>
