<script setup lang="ts">
import { computed } from 'vue';
import type { GameViewDto } from '@/types/models';
import { useLocaleStore } from '@/stores/locale';

const props = defineProps<{
  game: GameViewDto;
}>();

const localeStore = useLocaleStore();

const redFound = computed(
  () => props.game.cards.filter((c) => c.revealed && c.type === 'RED').length,
);
const blueFound = computed(
  () => props.game.cards.filter((c) => c.revealed && c.type === 'BLUE').length,
);
const redTotal = computed(() => props.game.redRemaining + redFound.value);
const blueTotal = computed(() => props.game.blueRemaining + blueFound.value);

const isRedActive = computed(() => !props.game.winner && props.game.currentTeam === 'RED');
const isBlueActive = computed(() => !props.game.winner && props.game.currentTeam === 'BLUE');
</script>

<template>
  <div class="scoreboard">
    <!-- RED team block -->
    <div
      class="score score--red"
      :class="{
        'score--active': isRedActive,
        'score--winner': game.winner === 'RED',
      }"
    >
      <span class="score__label">{{ localeStore.t('team.RED') }}</span>
      <span class="score__num">{{ game.redRemaining }}</span>
      <div class="score__dots" aria-hidden="true">
        <span
          v-for="n in redTotal"
          :key="n"
          class="score__dot"
          :class="{ 'is-found': n <= redFound }"
        />
      </div>
      <span class="sr-only">{{ localeStore.t('game.red_left', game.redRemaining) }}</span>
    </div>

    <!-- Center: turn / phase / winner -->
    <div class="scoreboard-center">
      <template v-if="game.winner">
        <span class="scoreboard-phase">{{ localeStore.t('game.over') }}</span>
        <span class="scoreboard-turn" :class="`scoreboard-turn--${game.winner}`">
          {{ localeStore.t('game.wins', localeStore.t(`team.${game.winner}`)) }}
        </span>
      </template>
      <template v-else>
        <span class="scoreboard-turn" :class="`scoreboard-turn--${game.currentTeam}`">
          {{ localeStore.t('game.turn', localeStore.t(`team.${game.currentTeam}`)) }}
        </span>
        <span class="scoreboard-phase">
          {{ localeStore.t('game.phase', localeStore.t(`phase.${game.phase}`)) }}
        </span>
      </template>
    </div>

    <!-- BLUE team block -->
    <div
      class="score score--blue"
      :class="{
        'score--active': isBlueActive,
        'score--winner': game.winner === 'BLUE',
      }"
    >
      <span class="score__label">{{ localeStore.t('team.BLUE') }}</span>
      <span class="score__num">{{ game.blueRemaining }}</span>
      <div class="score__dots" aria-hidden="true">
        <span
          v-for="n in blueTotal"
          :key="n"
          class="score__dot"
          :class="{ 'is-found': n <= blueFound }"
        />
      </div>
      <span class="sr-only">{{ localeStore.t('game.blue_left', game.blueRemaining) }}</span>
    </div>
  </div>
</template>
