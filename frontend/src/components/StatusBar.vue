<script setup lang="ts">
import { computed } from 'vue';
import type { GameViewDto } from '@/types/models';
import { useLocaleStore } from '@/stores/locale';

const props = defineProps<{
  game: GameViewDto;
}>();

const localeStore = useLocaleStore();

const pills = computed(() => {
  if (props.game.winner) {
    const winner = localeStore.t(`team.${props.game.winner}`);
    return [
      { text: localeStore.t('game.over'), className: '' },
      {
        text: localeStore.t('game.wins', winner),
        className: `stat-pill--turn team-${props.game.winner}`,
      },
    ];
  }
  return [
    {
      text: localeStore.t('game.turn', localeStore.t(`team.${props.game.currentTeam}`)),
      className: `stat-pill--turn team-${props.game.currentTeam}`,
    },
    {
      text: localeStore.t('game.red_left', props.game.redRemaining),
      className: 'team-RED',
    },
    {
      text: localeStore.t('game.blue_left', props.game.blueRemaining),
      className: 'team-BLUE',
    },
    {
      text: localeStore.t('game.phase', localeStore.t(`phase.${props.game.phase}`)),
      className: '',
    },
  ];
});
</script>

<template>
  <div class="status-bar">
    <span v-for="(pill, index) in pills" :key="index" class="stat-pill" :class="pill.className">
      {{ pill.text }}
    </span>
  </div>
</template>
