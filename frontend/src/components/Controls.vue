<script setup lang="ts">
import { ref } from 'vue';
import type { GameViewDto, RoomViewDto } from '@/types/models';
import { useLocaleStore } from '@/stores/locale';

defineProps<{
  view: RoomViewDto;
  game: GameViewDto;
}>();

const emit = defineEmits<{
  giveClue: [word: string, count: number];
  endTurn: [];
}>();

const localeStore = useLocaleStore();
const clueWord = ref('');
const clueCount = ref(1);

function submitClue() {
  const word = clueWord.value.trim();
  const count = Number(clueCount.value);
  if (word) emit('giveClue', word, count);
}
</script>

<template>
  <div
    v-if="!game.winner"
    class="controls"
    :class="{
      'controls--disabled':
        !view.canGiveClue &&
        !view.canEndTurn &&
        view.players.find((p) => p.id === view.viewerId)?.role === 'SPYMASTER',
      hidden:
        !view.canGiveClue &&
        !view.canEndTurn &&
        view.players.find((p) => p.id === view.viewerId)?.role !== 'SPYMASTER',
    }"
  >
    <template v-if="view.canGiveClue">
      <label class="field">
        <span class="field-label">{{ localeStore.t('game.clue_word') }}</span>
        <input v-model="clueWord" type="text" maxlength="64" />
      </label>
      <label class="field field--narrow">
        <span class="field-label">{{ localeStore.t('game.count') }}</span>
        <input v-model.number="clueCount" type="number" min="1" max="9" />
      </label>
      <button type="button" @click="submitClue">{{ localeStore.t('game.give_clue') }}</button>
    </template>
    <button v-else-if="view.canEndTurn" type="button" @click="emit('endTurn')">
      {{ localeStore.t('game.end_turn') }}
    </button>
    <template v-else-if="view.players.find((p) => p.id === view.viewerId)?.role === 'SPYMASTER'">
      <label class="field">
        <span class="field-label">{{ localeStore.t('game.clue_word') }}</span>
        <input type="text" maxlength="64" disabled />
      </label>
      <label class="field field--narrow">
        <span class="field-label">{{ localeStore.t('game.count') }}</span>
        <input type="number" min="1" max="9" value="1" disabled />
      </label>
      <button type="button" disabled>{{ localeStore.t('game.give_clue') }}</button>
    </template>
  </div>
</template>
