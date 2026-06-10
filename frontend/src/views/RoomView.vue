<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@unhead/vue';
import { api, ApiError } from '@/api/client';
import { useLocaleStore } from '@/stores/locale';
import { useRoomStore } from '@/stores/room';
import { useDiscordStore } from '@/stores/discord';
import JoinView from '@/views/JoinView.vue';
import LobbyView from '@/views/LobbyView.vue';

const props = defineProps<{
  code: string;
}>();

const localeStore = useLocaleStore();
const roomStore = useRoomStore();
const discordStore = useDiscordStore();
const router = useRouter();

const loading = ref(!import.meta.env.SSR);
const error = ref<string | null>(null);
const needJoin = ref(true);
const view = ref<import('@/types/models').RoomViewDto | null>(null);

useHead({
  title: () => `${localeStore.t('join.title', props.code)} — ${localeStore.t('app.title')}`,
});

async function loadRoom() {
  loading.value = true;
  error.value = null;
  try {
    const bootstrap = await api.getRoom(props.code);
    roomStore.language = bootstrap.language;
    if (bootstrap.needJoin) {
      needJoin.value = true;
      view.value = null;
      roomStore.clear();
      return;
    }
    needJoin.value = false;
    view.value = bootstrap.view ?? null;
    if (view.value) {
      roomStore.setView(view.value, props.code);
      if (view.value.status === 'PLAYING' || view.value.status === 'FINISHED') {
        await router.replace(`/rooms/${props.code}/game`);
      }
    }
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      error.value = localeStore.t('error.room_not_found');
    } else {
      error.value = e instanceof ApiError ? e.message : localeStore.t('error.unexpected');
    }
  } finally {
    loading.value = false;
  }
}

let stopDiscordWatch: (() => void) | undefined;

onMounted(() => {
  if (import.meta.env.SSR) return;

  if (discordStore.isDiscord) {
    // Discord init is async; watch the store until the bootstrap view arrives,
    // then use it directly without a separate REST call.
    const stopWatch = watch(
      () => roomStore.view,
      (newView) => {
        if (newView && roomStore.roomCode === props.code) {
          view.value = newView;
          roomStore.language = roomStore.language || 'en';
          needJoin.value = false;
          loading.value = false;
          stopWatch();
        }
      },
      { immediate: true },
    );
    stopDiscordWatch = stopWatch;
    return;
  }

  void loadRoom();
});

onUnmounted(() => {
  stopDiscordWatch?.();
});

async function onJoined() {
  await loadRoom();
}

const lobbyView = computed(() => view.value);
</script>

<template>
  <div v-if="loading" class="panel page-centered">
    <p class="page-subtitle">…</p>
  </div>

  <p v-else-if="error" class="alert-error" role="alert">{{ error }}</p>

  <JoinView v-else-if="needJoin" :code="code" @joined="onJoined" />

  <LobbyView v-else-if="lobbyView" :code="code" :initial-view="lobbyView" />
</template>
