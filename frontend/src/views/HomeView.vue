<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@unhead/vue';
import { trackEvent } from '@/analytics/umami';
import { api, ApiError } from '@/api/client';
import { loadPlayerName, savePlayerName } from '@/composables/usePlayerName';
import { useLocaleStore } from '@/stores/locale';
import { useDiscordStore } from '@/stores/discord';

const localeStore = useLocaleStore();
const discordStore = useDiscordStore();
const router = useRouter();

const publicUrl = import.meta.env.VITE_PUBLIC_URL || 'http://localhost:8080';

useHead({
  title: () => `${localeStore.t('home.title')} — ${localeStore.t('app.title')}`,
  htmlAttrs: { lang: () => localeStore.locale },
  meta: [
    { name: 'description', content: () => localeStore.t('meta.description') },
    {
      property: 'og:title',
      content: () => `${localeStore.t('home.title')} — ${localeStore.t('app.title')}`,
    },
    { property: 'og:description', content: () => localeStore.t('meta.description') },
    { property: 'og:type', content: 'website' },
    { property: 'og:url', content: publicUrl },
  ],
  link: [
    { rel: 'canonical', href: `${publicUrl}/` },
    { rel: 'alternate', hreflang: 'en', href: `${publicUrl}/` },
    { rel: 'alternate', hreflang: 'ru', href: `${publicUrl}/` },
  ],
});

const createName = ref('');
const createLanguage = ref('en');
const joinCode = ref('');
const joinName = ref('');
const error = ref<string | null>(null);
const loading = ref(false);

if (!import.meta.env.SSR) {
  const savedName = loadPlayerName();
  if (savedName) {
    createName.value = savedName;
    joinName.value = savedName;
  }
}

const heroPreviewColors = [
  'RED', 'NEUTRAL', 'BLUE', 'RED', 'NEUTRAL',
  'NEUTRAL', 'RED', 'NEUTRAL', 'BLUE', 'RED',
  'BLUE', 'NEUTRAL', 'ASSASSIN', 'BLUE', 'NEUTRAL',
  'RED', 'BLUE', 'NEUTRAL', 'NEUTRAL', 'RED',
  'NEUTRAL', 'RED', 'BLUE', 'NEUTRAL', 'BLUE',
] as const;

const gameLanguages = [
  { value: 'en', labelKey: 'lang.en' },
  { value: 'ru', labelKey: 'lang.ru' },
];

async function createRoom() {
  error.value = null;
  loading.value = true;
  try {
    const result = await api.createRoom(createName.value, createLanguage.value);
    savePlayerName(createName.value);
    trackEvent('room_created', { language: createLanguage.value });
    await router.push(`/rooms/${result.code}`);
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : localeStore.t('error.unexpected');
  } finally {
    loading.value = false;
  }
}

async function joinRoom() {
  error.value = null;
  loading.value = true;
  try {
    const result = await api.joinRoom(joinCode.value.toUpperCase(), joinName.value);
    savePlayerName(joinName.value);
    trackEvent('room_joined', { source: 'home' });
    await router.push(`/rooms/${result.code}`);
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : localeStore.t('error.unexpected');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <!-- Discord mode: SDK is initialising or already redirected to the room -->
  <div v-if="discordStore.isDiscord" class="panel page-centered loading-state">
    <p v-if="discordStore.error" class="alert-error" role="alert">{{ discordStore.error }}</p>
    <template v-else>
      <div class="spinner" aria-hidden="true" />
      <p class="page-subtitle">{{ localeStore.t('game.loading') }}</p>
    </template>
  </div>

  <template v-else>
    <div class="hero">
      <h1 class="page-title">{{ localeStore.t('home.title') }}</h1>
      <p class="hero-tagline">{{ localeStore.t('home.tagline') }}</p>
      <div class="hero-preview" aria-hidden="true">
        <span
          v-for="(type, i) in heroPreviewColors"
          :key="i"
          class="hero-preview__cell"
          :class="`hero-preview__cell--${type}`"
        />
      </div>
    </div>

    <p v-if="error" class="alert-error" role="alert">{{ error }}</p>

    <div class="grid-2">
      <section class="panel">
        <h2 class="section-title">
          <span class="section-icon" aria-hidden="true">✦</span>
          {{ localeStore.t('home.create_room') }}
        </h2>
        <form class="form-stack" @submit.prevent="createRoom">
          <label class="field">
            <span class="field-label">{{ localeStore.t('home.name') }}</span>
            <input
              v-model="createName"
              type="text"
              required
              maxlength="64"
              autocomplete="nickname"
            />
          </label>
          <label class="field">
            <span class="field-label">{{ localeStore.t('home.game_language') }}</span>
            <select v-model="createLanguage">
              <option v-for="lang in gameLanguages" :key="lang.value" :value="lang.value">
                {{ localeStore.t(lang.labelKey) }}
              </option>
            </select>
          </label>
          <button type="submit" class="primary" :disabled="loading">
            {{ localeStore.t('home.create') }}
          </button>
        </form>
      </section>

      <section class="panel">
        <h2 class="section-title">
          <span class="section-icon section-icon--join" aria-hidden="true">→</span>
          {{ localeStore.t('home.join_room') }}
        </h2>
        <form class="form-stack" @submit.prevent="joinRoom">
          <label class="field">
            <span class="field-label">{{ localeStore.t('home.room_code') }}</span>
            <input
              v-model="joinCode"
              type="text"
              required
              maxlength="4"
              pattern="[A-Za-z0-9]{4}"
              class="mono"
              placeholder="ABCD"
              autocomplete="off"
            />
          </label>
          <label class="field">
            <span class="field-label">{{ localeStore.t('home.name') }}</span>
            <input v-model="joinName" type="text" required maxlength="64" autocomplete="nickname" />
          </label>
          <button type="submit" class="primary" :disabled="loading">
            {{ localeStore.t('home.join') }}
          </button>
        </form>
      </section>
    </div>
  </template>
</template>
