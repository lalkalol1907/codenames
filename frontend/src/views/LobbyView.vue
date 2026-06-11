<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useHead } from '@unhead/vue';
import { api, ApiError } from '@/api/client';
import { trackEvent } from '@/analytics/umami';
import { useRoomSocket } from '@/composables/useRoomSocket';
import { useLocaleStore } from '@/stores/locale';
import { useRoomStore } from '@/stores/room';
import { useDiscordStore } from '@/stores/discord';
import { getDiscordSdk } from '@/composables/useDiscord';
import { useDiscordPresence } from '@/composables/useDiscordPresence';
import type { EnumOption, RoomViewDto } from '@/types/models';
import PlayerList from '@/components/PlayerList.vue';

const props = defineProps<{
  code: string;
  initialView: RoomViewDto;
}>();

const localeStore = useLocaleStore();
const roomStore = useRoomStore();
const discordStore = useDiscordStore();
const error = ref<string | null>(null);
const loading = ref(false);
const team = ref('RED');
const role = ref('SPYMASTER');
const options = ref<{ teams: EnumOption[]; roles: EnumOption[] } | null>(null);
const linkCopied = ref(false);
let linkCopiedTimer: ReturnType<typeof setTimeout> | undefined;

roomStore.setView(props.initialView, props.code);
const { wsError } = useRoomSocket(props.code, { redirectOnPlaying: true, redirectOnKicked: true });
useDiscordPresence(props.code);

useHead({
  title: () => `${localeStore.t('lobby.title', props.code)} — ${localeStore.t('app.title')}`,
});

const view = computed(() => roomStore.view ?? props.initialView);
const isHost = computed(() => view.value.hostPlayerId === view.value.viewerId);
const isSpectator = computed(() => role.value === 'SPECTATOR');
const isDiscord = computed(() => discordStore.isDiscord);

onMounted(async () => {
  if (import.meta.env.SSR) return;
  try {
    options.value = await api.getRoomOptions(props.code);
  } catch {
    options.value = {
      teams: [
        { value: 'RED', label: localeStore.t('team.RED') },
        { value: 'BLUE', label: localeStore.t('team.BLUE') },
      ],
      roles: [
        { value: 'SPYMASTER', label: localeStore.t('role.SPYMASTER') },
        { value: 'OPERATIVE', label: localeStore.t('role.OPERATIVE') },
        { value: 'SPECTATOR', label: localeStore.t('role.SPECTATOR') },
      ],
    };
  }
});

// Discord: автостарт когда все роли заняты — без нажатия хостом
watch(
  () => view.value.canStart,
  (canStart) => {
    if (canStart && isDiscord.value && isHost.value) {
      void startGame();
    }
  },
);

async function openDiscordInvite() {
  const sdk = getDiscordSdk();
  if (!sdk) return;
  try {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    await (sdk.commands as any).openInviteDialog();
  } catch {
    // ignore — not all clients support it
  }
}

async function saveRole() {
  error.value = null;
  loading.value = true;
  try {
    await api.setRole(props.code, isSpectator.value ? null : team.value, role.value);
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : localeStore.t('error.unexpected');
  } finally {
    loading.value = false;
  }
}

async function randomizeTeams() {
  error.value = null;
  loading.value = true;
  try {
    await api.randomizeTeams(props.code);
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : localeStore.t('error.unexpected');
  } finally {
    loading.value = false;
  }
}

async function startGame() {
  error.value = null;
  loading.value = true;
  try {
    await api.startGame(props.code);
    trackEvent('game_started');
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : localeStore.t('error.unexpected');
  } finally {
    loading.value = false;
  }
}

function roomLink(): string {
  if (typeof window === 'undefined') return '';
  return `${window.location.origin}/rooms/${props.code}`;
}

async function copyRoomLink() {
  if (typeof window === 'undefined') return;
  error.value = null;
  try {
    await navigator.clipboard.writeText(roomLink());
    linkCopied.value = true;
    clearTimeout(linkCopiedTimer);
    linkCopiedTimer = setTimeout(() => {
      linkCopied.value = false;
    }, 2000);
  } catch {
    error.value = localeStore.t('lobby.copy_link_failed');
  }
}
</script>

<template>
  <div>
    <header style="margin-bottom: 1rem">
      <div class="room-header">
        <p class="room-badge">{{ code }}</p>
        <button
          v-if="isDiscord"
          type="button"
          class="btn--secondary room-copy-link"
          @click="openDiscordInvite"
        >
          Пригласить
        </button>
        <button v-else type="button" class="btn--secondary room-copy-link" @click="copyRoomLink">
          {{ linkCopied ? localeStore.t('lobby.link_copied') : localeStore.t('lobby.copy_link') }}
        </button>
      </div>
      <p class="room-meta" style="margin: 0.25rem 0 0">
        {{ localeStore.t('lobby.game_language') }}:
        <strong>{{ roomStore.language.toUpperCase() }}</strong>
        · {{ localeStore.t('lobby.share_code') }}
      </p>
    </header>

    <p v-if="error || wsError" class="alert-error" role="alert">{{ error || wsError }}</p>

    <Teleport to="body">
      <div v-if="linkCopied" class="toast" role="status" aria-live="polite">
        {{ localeStore.t('lobby.link_copied') }}
      </div>
    </Teleport>

    <div class="grid-lobby">
      <section class="panel">
        <h2 class="section-title">{{ localeStore.t('lobby.players') }}</h2>
        <PlayerList :players="view.players" />
      </section>

      <section class="panel">
        <h2 class="section-title">{{ localeStore.t('lobby.choose_team') }}</h2>
        <form class="form-stack" @submit.prevent="saveRole">
          <div class="form-row form-row--2">
            <label v-if="!isSpectator" class="field">
              <span class="field-label">{{ localeStore.t('lobby.team') }}</span>
              <select v-model="team" required>
                <option
                  v-for="option in options?.teams ?? []"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </option>
              </select>
            </label>
            <label class="field">
              <span class="field-label">{{ localeStore.t('lobby.role') }}</span>
              <select v-model="role" required>
                <option
                  v-for="option in options?.roles ?? []"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </option>
              </select>
            </label>
          </div>
          <button type="submit" :disabled="loading">{{ localeStore.t('lobby.save') }}</button>
        </form>

        <div v-if="isHost" class="roles-actions">
          <button
            type="button"
            class="btn--secondary"
            :disabled="!view.canRandomizeTeams || loading"
            @click="randomizeTeams"
          >
            {{ localeStore.t('lobby.randomize_teams') }}
          </button>
          <p class="hint">{{ localeStore.t('lobby.randomize_hint') }}</p>
        </div>

        <div v-if="isHost" class="start-section">
          <button
            type="button"
            class="primary"
            :disabled="!view.canStart || loading"
            @click="startGame"
          >
            {{ localeStore.t('lobby.start_game') }}
          </button>
          <p class="hint">
            {{
              view.canStart
                ? localeStore.t('lobby.start_hint')
                : localeStore.t('lobby.waiting_hint', view.players.length)
            }}
          </p>
        </div>
      </section>
    </div>
  </div>
</template>
