<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@unhead/vue';
import { api, ApiError } from '@/api/client';
import { loadPlayerName, savePlayerName } from '@/composables/usePlayerName';
import { useLocaleStore } from '@/stores/locale';
import { useDiscordStore } from '@/stores/discord';
import { SITE_URL } from '@/config/site';

const localeStore = useLocaleStore();
const discordStore = useDiscordStore();
const router = useRouter();

const pageTitle = () => `${localeStore.t('home.title')} — ${localeStore.t('app.title')}`;

useHead({
  title: pageTitle,
  htmlAttrs: { lang: () => localeStore.locale },
  meta: [
    { name: 'description', content: () => localeStore.t('meta.description') },
    { name: 'robots', content: 'index, follow' },
    { property: 'og:title', content: pageTitle },
    { property: 'og:description', content: () => localeStore.t('meta.description') },
    { property: 'og:type', content: 'website' },
    { property: 'og:site_name', content: () => localeStore.t('app.title') },
    { property: 'og:url', content: `${SITE_URL}/` },
    { property: 'og:locale', content: () => (localeStore.locale === 'ru' ? 'ru_RU' : 'en_US') },
    { name: 'twitter:card', content: 'summary' },
    { name: 'twitter:title', content: pageTitle },
    { name: 'twitter:description', content: () => localeStore.t('meta.description') },
  ],
  link: [
    { rel: 'canonical', href: `${SITE_URL}/` },
    { rel: 'alternate', hreflang: 'en', href: `${SITE_URL}/` },
    { rel: 'alternate', hreflang: 'ru', href: `${SITE_URL}/` },
    { rel: 'alternate', hreflang: 'x-default', href: `${SITE_URL}/` },
  ],
  script: [
    {
      type: 'application/ld+json',
      innerHTML: () =>
        JSON.stringify({
          '@context': 'https://schema.org',
          '@type': 'WebApplication',
          name: localeStore.t('app.title'),
          url: `${SITE_URL}/`,
          description: localeStore.t('meta.description'),
          applicationCategory: 'GameApplication',
          operatingSystem: 'Web browser',
          offers: { '@type': 'Offer', price: '0', priceCurrency: 'USD' },
          inLanguage: ['en', 'ru'],
        }),
    },
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
  'RED',
  'NEUTRAL',
  'BLUE',
  'RED',
  'NEUTRAL',
  'NEUTRAL',
  'RED',
  'NEUTRAL',
  'BLUE',
  'RED',
  'BLUE',
  'NEUTRAL',
  'ASSASSIN',
  'BLUE',
  'NEUTRAL',
  'RED',
  'BLUE',
  'NEUTRAL',
  'NEUTRAL',
  'RED',
  'NEUTRAL',
  'RED',
  'BLUE',
  'NEUTRAL',
  'BLUE',
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

    <section class="panel seo-section" aria-labelledby="seo-heading">
      <h2 id="seo-heading" class="section-title">{{ localeStore.t('home.seo_heading') }}</h2>
      <p class="seo-section__text">{{ localeStore.t('home.seo_p1') }}</p>
      <p class="seo-section__text">{{ localeStore.t('home.seo_p2') }}</p>
    </section>
  </template>
</template>
