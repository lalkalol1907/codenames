<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { CardView, RoomViewDto } from '@/types/models';
import { useSettingsStore } from '@/stores/settings';

const props = defineProps<{
  view: RoomViewDto;
}>();

const emit = defineEmits<{
  guess: [index: number];
}>();

const settingsStore = useSettingsStore();

const game = computed(() => props.view.game!);
const viewer = computed(() => props.view.players.find((p) => p.id === props.view.viewerId));
const isSpymaster = computed(() => viewer.value?.role === 'SPYMASTER');

const sortedCards = computed(() => [...game.value.cards].sort((a, b) => a.position - b.position));

// ── Hold-to-reveal ──
const holdingPosition = ref<number | null>(null);
let holdTimer: ReturnType<typeof setTimeout> | null = null;
const HOLD_MS = 600;

function onCardClick(card: CardView) {
  if (settingsStore.holdToReveal) return;
  if (props.view.canGuess && !card.revealed) {
    emit('guess', card.position);
  }
}

function onPointerDown(event: PointerEvent, card: CardView) {
  if (!settingsStore.holdToReveal) return;
  if (!props.view.canGuess || card.revealed) return;
  event.preventDefault();
  holdingPosition.value = card.position;
  holdTimer = setTimeout(() => {
    emit('guess', card.position);
    holdingPosition.value = null;
    holdTimer = null;
  }, HOLD_MS);
}

function cancelHold() {
  if (holdTimer !== null) {
    clearTimeout(holdTimer);
    holdTimer = null;
  }
  holdingPosition.value = null;
}

// ── Guess feedback ──
const justRevealedPositions = ref<Set<number>>(new Set());
const boardShaking = ref(false);

// Initialize tracking set without triggering animations on mount
let prevRevealedSet = new Set<number>(
  sortedCards.value.filter((c) => c.revealed).map((c) => c.position),
);

watch(sortedCards, (newCards) => {
  const currentRevealed = new Set(newCards.filter((c) => c.revealed).map((c) => c.position));
  const newlyRevealed = [...currentRevealed].filter((p) => !prevRevealedSet.has(p));

  if (newlyRevealed.length > 0) {
    // Add newly revealed positions — replace Set to trigger reactivity
    justRevealedPositions.value = new Set([...justRevealedPositions.value, ...newlyRevealed]);

    // Shake board if assassin was revealed
    const assassinHit = newCards.some(
      (c) => c.revealed && c.type === 'ASSASSIN' && newlyRevealed.includes(c.position),
    );
    if (assassinHit) {
      boardShaking.value = true;
      setTimeout(() => {
        boardShaking.value = false;
      }, 850);
    }

    // Remove glow class after animation completes
    setTimeout(() => {
      const updated = new Set(justRevealedPositions.value);
      newlyRevealed.forEach((p) => updated.delete(p));
      justRevealedPositions.value = updated;
    }, 750);
  }

  prevRevealedSet = currentRevealed;
});

// ── Card class helpers ──
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
</script>

<template>
  <div class="board" :class="{ 'board--shaking': boardShaking }">
    <div
      v-for="(card, index) in sortedCards"
      :key="card.position"
      class="card-slot"
      :class="{
        clickable: view.canGuess && !card.revealed,
        'card-slot--holding': holdingPosition === card.position,
        'card-slot--just-revealed': justRevealedPositions.has(card.position),
      }"
      :style="{ animationDelay: `${index * 45}ms` }"
      @click="onCardClick(card)"
      @pointerdown="onPointerDown($event, card)"
      @pointerup="cancelHold"
      @pointerleave="cancelHold"
      @pointercancel="cancelHold"
    >
      <div class="card-hold-progress" />
      <div class="card-flipper" :class="{ 'card-flipper--revealed': card.revealed }">
        <div class="card card-face card-front" :class="frontClasses(card)">{{ card.word }}</div>
        <div class="card card-face card-back" :class="backClasses(card)">{{ card.word }}</div>
      </div>
    </div>
  </div>
</template>
