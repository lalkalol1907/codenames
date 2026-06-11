<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, RouterView, useRoute } from 'vue-router';
import LocaleSwitcher from '@/components/LocaleSwitcher.vue';
import ThemeSwitcher from '@/components/ThemeSwitcher.vue';
import { useLocaleStore } from '@/stores/locale';

const localeStore = useLocaleStore();
const route = useRoute();
const isWide = computed(() => route.name === 'game');
</script>

<template>
  <div class="bg-glow" aria-hidden="true" />
  <header class="site-header">
    <RouterLink to="/" class="logo">{{ localeStore.t('app.title') }}</RouterLink>
    <div class="header-controls">
      <ThemeSwitcher />
      <form class="locale-form" @submit.prevent>
        <LocaleSwitcher />
      </form>
    </div>
  </header>
  <main class="container" :class="{ 'container--wide': isWide }">
    <RouterView />
  </main>
</template>
