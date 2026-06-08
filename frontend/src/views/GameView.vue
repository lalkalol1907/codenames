<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@unhead/vue';
import { trackEvent } from '@/analytics/umami';
import { api, ApiError } from '@/api/client';
import Board from '@/components/Board.vue';
import CluePanel from '@/components/CluePanel.vue';
import Controls from '@/components/Controls.vue';
import PlayerList from '@/components/PlayerList.vue';
import StatusBar from '@/components/StatusBar.vue';
import { useRoomSocket } from '@/composables/useRoomSocket';
import { useLocaleStore } from '@/stores/locale';
import { useRoomStore } from '@/stores/room';

const props = defineProps<{
  code: string;
}>();

const localeStore = useLocaleStore();
const roomStore = useRoomStore();
const router = useRouter();
const error = ref<string | null>(null);
const loading = ref(true);

useHead({
  title: () => `${localeStore.t('game.title', props.code)} — ${localeStore.t('app.title')}`,
});

const { send, wsError } = useRoomSocket(props.code);

const view = computed(() => roomStore.view);
const game = computed(() => view.value?.game ?? null);

watch(
  () => game.value?.winner,
  (winner, previous) => {
    if (winner && !previous) {
      trackEvent('game_finished', { winner });
    }
  },
);

onMounted(async () => {
  try {
    const bootstrap = await api.getRoom(props.code);
    if (bootstrap.needJoin) {
      await router.replace(`/rooms/${props.code}`);
      return;
    }
    if (bootstrap.view?.status === 'LOBBY') {
      await router.replace(`/rooms/${props.code}`);
      return;
    }
    if (bootstrap.view) {
      roomStore.setView(bootstrap.view, props.code);
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
});

function onGuess(index: number) {
  send({ type: 'guess', index });
}

function onGiveClue(word: string, count: number) {
  send({ type: 'give_clue', word, count });
}

function onEndTurn() {
  send({ type: 'end_turn' });
}
</script>

<template>
  <div v-if="loading" class="panel page-centered">
    <p class="page-subtitle">…</p>
  </div>

  <p v-else-if="error" class="alert-error" role="alert">{{ error }}</p>

  <div v-else-if="view && game" class="game-layout">
    <p v-if="wsError" class="alert-error" role="alert">{{ wsError }}</p>
    <aside class="panel game-sidebar">
      <h2 class="section-title">{{ localeStore.t('lobby.players') }}</h2>
      <PlayerList :players="view.players" :viewer-id="view.viewerId" variant="game" />
    </aside>

    <section class="panel game-panel">
      <StatusBar :game="game" />
      <CluePanel :view="view" :game="game" />
      <Board :view="view" @guess="onGuess" />
      <Controls :view="view" :game="game" @give-clue="onGiveClue" @end-turn="onEndTurn" />

      <div v-if="game.winner" class="game-over">
        {{ localeStore.t('game.team_wins', localeStore.t(`team.${game.winner}`)) }}
      </div>
    </section>
  </div>
</template>
