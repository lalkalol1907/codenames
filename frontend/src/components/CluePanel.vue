<script setup lang="ts">
import { computed } from 'vue';
import type { GameViewDto, RoomViewDto } from '@/types/models';
import { useLocaleStore } from '@/stores/locale';

const props = defineProps<{
  view: RoomViewDto;
  game: GameViewDto;
}>();

const localeStore = useLocaleStore();

const panelClass = computed(() => ({
  'clue-panel--your-turn': props.game.phase === 'CLUE' && props.view.canGiveClue,
  hidden: !props.game.winner && props.game.phase !== 'CLUE' && !props.game.clueWord,
}));

const content = computed(() => {
  if (props.game.clueWord) {
    return localeStore.t(
      'game.clue',
      props.game.clueWord,
      props.game.clueCount ?? 0,
      props.game.guessesRemaining,
    );
  }
  if (props.game.phase === 'CLUE') {
    if (props.view.canGiveClue) {
      return localeStore.t('game.waiting_your_clue');
    }
    return localeStore.t('game.waiting_clue');
  }
  return '';
});
</script>

<template>
  <div v-if="!game.winner" class="clue-panel" :class="panelClass">
    <template v-if="game.clueWord">
      <span class="clue-word">{{ game.clueWord }}</span>
      · {{ content }}
    </template>
    <span
      v-else-if="game.phase === 'CLUE' && view.canGiveClue"
      class="clue-panel-wait clue-panel-wait--yours"
    >
      {{ content }}
    </span>
    <template v-else-if="game.phase === 'CLUE'">{{ content }}</template>
  </div>
</template>
