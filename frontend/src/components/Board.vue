<script setup lang="ts">
import { computed } from 'vue';
import type { CardView, RoomViewDto } from '@/types/models';

const props = defineProps<{
  view: RoomViewDto;
}>();

const emit = defineEmits<{
  guess: [index: number];
}>();

const game = computed(() => props.view.game!);
const viewer = computed(() => props.view.players.find((p) => p.id === props.view.viewerId));
const isSpymaster = computed(() => viewer.value?.role === 'SPYMASTER');

function frontClasses(card: CardView) {
  if (card.revealed) return 'hidden-type';
  if (isSpymaster.value && card.type) return `type-${card.type}`;
  return 'hidden-type';
}

function backClasses(card: CardView) {
  const classes = ['revealed', card.type || 'NEUTRAL'];
  if (isSpymaster.value) classes.push('spymaster-revealed');
  return classes.join(' ');
}

function onCardClick(card: CardView) {
  if (props.view.canGuess && !card.revealed) {
    emit('guess', card.position);
  }
}

const sortedCards = computed(() => [...game.value.cards].sort((a, b) => a.position - b.position));
</script>

<template>
  <div class="board">
    <div
      v-for="(card, index) in sortedCards"
      :key="card.position"
      class="card-slot"
      :class="{ clickable: view.canGuess && !card.revealed }"
      :style="{ animationDelay: `${index * 45}ms` }"
      @click="onCardClick(card)"
    >
      <div class="card-flipper" :class="{ 'card-flipper--revealed': card.revealed }">
        <div class="card card-face card-front" :class="frontClasses(card)">{{ card.word }}</div>
        <div class="card card-face card-back" :class="backClasses(card)">{{ card.word }}</div>
      </div>
    </div>
  </div>
</template>
