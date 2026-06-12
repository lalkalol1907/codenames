<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { useHead } from '@unhead/vue';
import { api, ApiError } from '@/api/client';
import Board from '@/components/Board.vue';
import CluePanel from '@/components/CluePanel.vue';
import Controls from '@/components/Controls.vue';
import PlayerList from '@/components/PlayerList.vue';
import StatusBar from '@/components/StatusBar.vue';
import { useRoomSocket } from '@/composables/useRoomSocket';
import { useDiscordPresence } from '@/composables/useDiscordPresence';
import { useLocaleStore } from '@/stores/locale';
import { useRoomStore } from '@/stores/room';
import { useSettingsStore } from '@/stores/settings';

const props = defineProps<{
  code: string;
}>();

const localeStore = useLocaleStore();
const roomStore = useRoomStore();
const settingsStore = useSettingsStore();
const router = useRouter();
const error = ref<string | null>(null);
const loading = ref(true);
const showReconnected = ref(false);
const overlayDismissed = ref(false);
let reconnectedTimer: ReturnType<typeof setTimeout> | undefined;

useHead({
  title: () => `${localeStore.t('game.title', props.code)} — ${localeStore.t('app.title')}`,
});

const { send, wsError, wsConnection } = useRoomSocket(props.code);
useDiscordPresence(props.code);

const view = computed(() => roomStore.view);
const game = computed(() => view.value?.game ?? null);
const showReconnectBanner = computed(
  () => wsConnection.value === 'connecting' || wsConnection.value === 'reconnecting',
);

watch(wsConnection, (state, previous) => {
  if (state === 'connected' && previous === 'reconnecting') {
    showReconnected.value = true;
    clearTimeout(reconnectedTimer);
    reconnectedTimer = setTimeout(() => {
      showReconnected.value = false;
    }, 2500);
  }
});

watch(
  () => game.value?.winner,
  (winner, previous) => {
    if (winner && !previous) {
      overlayDismissed.value = false;
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

// Confetti
const CONFETTI_COLORS = [
  'var(--red)',
  'var(--blue)',
  'var(--accent)',
  'var(--red-soft)',
  'var(--blue-soft)',
  'var(--text-muted)',
];

function confettiStyle(n: number) {
  return {
    left: `${((n - 1) / 22) * 100}%`,
    top: `-${8 + (n % 6) * 2}%`,
    width: `${7 + (n % 4) * 3}px`,
    height: `${7 + (n % 4) * 3}px`,
    background: CONFETTI_COLORS[(n - 1) % CONFETTI_COLORS.length],
    animationDelay: `${((n % 7) * 0.18).toFixed(2)}s`,
    animationDuration: `${2.4 + (n % 5) * 0.3}s`,
  };
}
</script>

<template>
  <div v-if="loading" class="panel page-centered loading-state">
    <div class="skeleton-board" aria-hidden="true">
      <div
        v-for="n in 25"
        :key="n"
        class="skeleton-board__cell"
        :style="{ animationDelay: `${(n % 5) * 0.08}s` }"
      />
    </div>
    <p class="sr-only">{{ localeStore.t('game.loading') }}</p>
  </div>

  <p v-else-if="error" class="alert-error" role="alert">{{ error }}</p>

  <div v-else-if="view && game" class="game-page">
    <p v-if="showReconnectBanner" class="alert-warn" role="status" aria-live="polite">
      {{ localeStore.t('game.reconnecting') }}
    </p>
    <p
      v-else-if="showReconnected"
      class="alert-warn alert-warn--ok"
      role="status"
      aria-live="polite"
    >
      {{ localeStore.t('game.reconnected') }}
    </p>
    <p v-if="wsError" class="alert-error" role="alert">{{ wsError }}</p>

    <div class="game-layout">
      <aside
        class="panel game-sidebar"
        :class="{ 'game-sidebar--collapsed': settingsStore.sidebarCollapsed }"
      >
        <div class="game-sidebar__header">
          <h2 class="section-title game-sidebar__title">
            {{ localeStore.t('lobby.players') }}
          </h2>
          <button
            type="button"
            class="game-sidebar__toggle"
            :aria-expanded="!settingsStore.sidebarCollapsed"
            :aria-label="
              settingsStore.sidebarCollapsed
                ? localeStore.t('sidebar.expand')
                : localeStore.t('sidebar.collapse')
            "
            @click="settingsStore.toggleSidebarCollapsed()"
          >
            <svg
              class="game-sidebar__chevron"
              viewBox="0 0 20 20"
              fill="currentColor"
              aria-hidden="true"
            >
              <path
                fill-rule="evenodd"
                d="M7.21 14.77a.75.75 0 01.02-1.06L11.168 10 7.23 6.29a.75.75 0 111.04-1.08l4.5 4.25a.75.75 0 010 1.08l-4.5 4.25a.75.75 0 01-1.06-.02z"
                clip-rule="evenodd"
              />
            </svg>
          </button>
        </div>
        <PlayerList
          :players="view.players"
          :viewer-id="view.viewerId"
          variant="game"
          :compact="settingsStore.sidebarCollapsed"
        />
      </aside>

      <section class="panel game-panel">
        <StatusBar :game="game" />
        <CluePanel :view="view" :game="game" />
        <Board :view="view" @guess="onGuess" />
        <Controls :view="view" :game="game" @give-clue="onGiveClue" @end-turn="onEndTurn" />

        <div v-if="game.winner && overlayDismissed" class="game-over">
          {{ localeStore.t('game.team_wins', localeStore.t(`team.${game.winner}`)) }}
        </div>

        <!-- Hold-to-reveal toggle -->
        <div class="game-settings">
          <label class="game-settings__toggle">
            <input
              type="checkbox"
              :checked="settingsStore.holdToReveal"
              @change="(e) => settingsStore.setHoldToReveal((e.target as HTMLInputElement).checked)"
            />
            {{ localeStore.t('settings.hold_to_reveal') }}
          </label>
        </div>
      </section>
    </div>
  </div>

  <!-- Game-over overlay -->
  <Teleport to="body">
    <div
      v-if="game && game.winner && !overlayDismissed"
      class="game-over-overlay"
      role="dialog"
      :aria-label="localeStore.t('game.team_wins', localeStore.t(`team.${game.winner}`))"
    >
      <div
        v-for="n in 22"
        :key="n"
        class="confetti-piece"
        :style="confettiStyle(n)"
        aria-hidden="true"
      />
      <div class="game-over-card">
        <div class="game-over-emoji" aria-hidden="true">
          {{ game.winner === 'RED' ? '🔴' : '🔵' }}
        </div>
        <h2 class="game-over-title" :class="`game-over-title--${game.winner}`">
          {{ localeStore.t('game.team_wins', localeStore.t(`team.${game.winner}`)) }}
        </h2>
        <div class="game-over-actions">
          <RouterLink to="/" class="btn btn--secondary">
            {{ localeStore.t('game.back_home') }}
          </RouterLink>
          <button type="button" @click="overlayDismissed = true">
            {{ localeStore.t('game.see_board') }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
