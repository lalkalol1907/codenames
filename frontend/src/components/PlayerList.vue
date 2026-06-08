<script setup lang="ts">
import { computed } from 'vue';
import type { PlayerViewDto } from '@/types/models';
import { useLocaleStore } from '@/stores/locale';

const props = defineProps<{
  players: PlayerViewDto[];
  viewerId?: string;
  variant?: 'lobby' | 'game';
}>();

const localeStore = useLocaleStore();

const sortedPlayers = computed(() => {
  if (props.variant !== 'game') return props.players;
  const teamOrder: Record<string, number> = { RED: 0, BLUE: 1 };
  const roleOrder: Record<string, number> = { SPYMASTER: 0, OPERATIVE: 1 };
  return [...props.players].sort((a, b) => {
    const byTeam = (teamOrder[a.team ?? ''] ?? 2) - (teamOrder[b.team ?? ''] ?? 2);
    if (byTeam !== 0) return byTeam;
    return (roleOrder[a.role ?? ''] ?? 2) - (roleOrder[b.role ?? ''] ?? 2);
  });
});

function teamLabel(team: string | null) {
  if (!team) return '';
  return localeStore.t(`team.${team}`);
}

function roleLabel(role: string | null) {
  if (!role) return '';
  return localeStore.t(`role.${role}`);
}

function teamBadgeClass(team: string | null) {
  if (team === 'RED') return 'badge--red';
  if (team === 'BLUE') return 'badge--blue';
  return 'badge--muted';
}
</script>

<template>
  <ul class="player-list">
    <li
      v-for="player in sortedPlayers"
      :key="player.id"
      class="player-item"
      :class="[
        variant === 'game' && player.team ? `player-item--${player.team.toLowerCase()}` : '',
        variant === 'game' && player.id === viewerId ? 'player-item--self' : '',
      ]"
    >
      <span
        class="player-avatar"
        :class="
          variant === 'game' && player.team ? `player-avatar--${player.team.toLowerCase()}` : ''
        "
      >
        {{ (player.name || '?').charAt(0).toUpperCase() }}
      </span>
      <div class="player-info">
        <template v-if="variant === 'game'">
          <div class="player-name-row">
            <span class="player-name">{{ player.name }}</span>
            <span v-if="player.isHost" class="player-host">{{ localeStore.t('lobby.host') }}</span>
          </div>
          <span class="player-role">
            {{ player.team && player.role ? roleLabel(player.role) : '—' }}
          </span>
        </template>
        <template v-else>
          <div class="player-name">{{ player.name }}</div>
          <div class="player-meta">
            <span v-if="player.isHost" class="badge badge--host">{{
              localeStore.t('lobby.host')
            }}</span>
            <span
              v-if="player.team && player.role"
              class="badge"
              :class="teamBadgeClass(player.team)"
            >
              {{ teamLabel(player.team) }} · {{ roleLabel(player.role) }}
            </span>
            <span v-else class="badge badge--muted">{{
              localeStore.t('lobby.choosing_role')
            }}</span>
          </div>
        </template>
      </div>
    </li>
  </ul>
</template>
