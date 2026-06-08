<script setup lang="ts">
import { computed } from 'vue';
import { useLocaleStore } from '@/stores/locale';

const localeStore = useLocaleStore();

const current = computed({
  get: () => localeStore.locale,
  set: (value) => {
    void localeStore.setLocale(value as 'en' | 'ru');
  },
});
</script>

<template>
  <label class="locale-label">
    <span class="locale-label-text">{{ localeStore.t('locale.label') }}</span>
    <select v-model="current">
      <option value="ru">{{ localeStore.t('lang.ru') }}</option>
      <option value="en">{{ localeStore.t('lang.en') }}</option>
    </select>
  </label>
</template>
