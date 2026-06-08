<script setup lang="ts">
import { ref } from 'vue';
import { trackEvent } from '@/analytics/umami';
import { api, ApiError } from '@/api/client';
import { loadPlayerName, savePlayerName } from '@/composables/usePlayerName';
import { useLocaleStore } from '@/stores/locale';

const props = defineProps<{
  code: string;
}>();

const emit = defineEmits<{
  joined: [code: string];
}>();

const localeStore = useLocaleStore();
const name = ref(loadPlayerName());
const error = ref<string | null>(null);
const loading = ref(false);

async function submit() {
  error.value = null;
  loading.value = true;
  try {
    await api.joinRoomAt(props.code, name.value);
    savePlayerName(name.value);
    trackEvent('room_joined', { source: 'link' });
    emit('joined', props.code);
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : localeStore.t('error.unexpected');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="panel page-centered">
    <p class="page-subtitle page-centered__label">{{ localeStore.t('join.title', code) }}</p>
    <p class="room-badge">{{ code }}</p>

    <p v-if="error" class="alert-error" role="alert">{{ error }}</p>

    <form class="form-stack page-centered__form" @submit.prevent="submit">
      <label class="field">
        <span class="field-label">{{ localeStore.t('join.your_name') }}</span>
        <input
          v-model="name"
          type="text"
          required
          maxlength="64"
          autofocus
          autocomplete="nickname"
        />
      </label>
      <button type="submit" class="primary" :disabled="loading">
        {{ localeStore.t('join.enter_lobby') }}
      </button>
    </form>
  </section>
</template>
