<script setup lang="ts">
import { computed } from 'vue';
import type { PlayerViewDto } from '@/types/models';
import { useLocaleStore } from '@/stores/locale';

const props = defineProps<{
  players: PlayerViewDto[];
  viewerId?: string;
  variant?: 'lobby' | 'game';
  compact?: boolean;
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

function playerRoleText(player: PlayerViewDto) {
  if (player.role === 'SPECTATOR') return roleLabel(player.role);
  if (player.team && player.role) return roleLabel(player.role);
  return '—';
}

function avatarTitle(player: PlayerViewDto) {
  if (props.variant !== 'game') return undefined;
  const parts = [player.name];
  if (player.isHost) parts.push(localeStore.t('lobby.host'));
  parts.push(playerRoleText(player));
  return parts.join(' · ');
}
</script>

<template>
  <ul class="player-list" :class="{ 'player-list--compact': compact }">
    <li
      v-for="player in sortedPlayers"
      :key="player.id"
      class="player-item"
      :class="[
        variant === 'game' && player.team ? `player-item--${player.team.toLowerCase()}` : '',
        variant === 'game' && player.id === viewerId ? 'player-item--self' : '',
        compact ? 'player-item--avatar-only' : '',
      ]"
    >
      <span
        class="player-avatar"
        :class="
          variant === 'game' && player.team ? `player-avatar--${player.team.toLowerCase()}` : ''
        "
        :title="avatarTitle(player)"
      >
        <img
          v-if="player.avatarUrl"
          :src="player.avatarUrl"
          :alt="player.name"
          class="player-avatar__img"
          loading="lazy"
        />
        <template v-else>{{ (player.name || '?').charAt(0).toUpperCase() }}</template>
      </span>
      <div class="player-info">
        <template v-if="variant === 'game'">
          <div class="player-name-row">
            <span class="player-name">{{ player.name }}</span>
            <span v-if="player.isHost" class="player-host">{{ localeStore.t('lobby.host') }}</span>
          </div>
          <span class="player-role">{{ playerRoleText(player) }}</span>
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
            <span v-else-if="player.role === 'SPECTATOR'" class="badge badge--muted">{{
              roleLabel(player.role)
            }}</span>
            <span v-else class="badge badge--muted">{{
              localeStore.t('lobby.choosing_role')
            }}</span>
          </div>
        </template>
      </div>
    </li>
  </ul>
</template>
