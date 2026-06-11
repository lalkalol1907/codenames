import { beforeEach, describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import Board from '@/components/Board.vue';
import { useSettingsStore } from '@/stores/settings';
import { makeCard, makeGameView, makePlayer, makeRoomView } from '@/test/fixtures';

describe('Board', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    // Disable hold-to-reveal so existing click-based tests work as before
    useSettingsStore().setHoldToReveal(false);
  });

  it('renders cards sorted by position', () => {
    const view = makeRoomView({
      canGuess: false,
      players: [makePlayer()],
      game: makeGameView({
        cards: [
          makeCard({ position: 2, word: 'BANANA' }),
          makeCard({ position: 0, word: 'APPLE' }),
          makeCard({ position: 1, word: 'CHERRY' }),
        ],
      }),
    });

    const wrapper = mount(Board, { props: { view } });
    const words = wrapper.findAll('.card-front').map((node) => node.text());

    expect(words).toEqual(['APPLE', 'CHERRY', 'BANANA']);
  });

  it('emits guess when operative clicks an unrevealed card', async () => {
    const view = makeRoomView({
      canGuess: true,
      viewerId: 'viewer-id',
      players: [makePlayer({ id: 'viewer-id', role: 'OPERATIVE' })],
      game: makeGameView({
        cards: [makeCard({ position: 3, word: 'OCEAN', revealed: false })],
      }),
    });

    const wrapper = mount(Board, { props: { view } });
    await wrapper.find('.card-slot.clickable').trigger('click');

    expect(wrapper.emitted('guess')).toEqual([[3]]);
  });

  it('does not emit guess for revealed or non-clickable cards', async () => {
    const view = makeRoomView({
      canGuess: true,
      viewerId: 'viewer-id',
      players: [makePlayer({ id: 'viewer-id', role: 'OPERATIVE' })],
      game: makeGameView({
        cards: [makeCard({ position: 1, word: 'MOON', revealed: true })],
      }),
    });

    const wrapper = mount(Board, { props: { view } });
    await wrapper.find('.card-slot').trigger('click');

    expect(wrapper.emitted('guess')).toBeUndefined();
  });

  it('shows team colors to spymaster on hidden cards', () => {
    const view = makeRoomView({
      canGuess: false,
      viewerId: 'spy-id',
      players: [makePlayer({ id: 'spy-id', role: 'SPYMASTER' })],
      game: makeGameView({
        cards: [makeCard({ position: 0, word: 'RIVER', type: 'BLUE', revealed: false })],
      }),
    });

    const wrapper = mount(Board, { props: { view } });

    expect(wrapper.find('.card-front.type-BLUE').exists()).toBe(true);
  });

  it('hides team colors from operative on hidden cards', () => {
    const view = makeRoomView({
      canGuess: true,
      viewerId: 'viewer-id',
      players: [makePlayer({ id: 'viewer-id', role: 'OPERATIVE' })],
      game: makeGameView({
        cards: [makeCard({ position: 0, word: 'RIVER', type: 'BLUE', revealed: false })],
      }),
    });

    const wrapper = mount(Board, { props: { view } });

    expect(wrapper.find('.card-front.hidden-type').exists()).toBe(true);
    expect(wrapper.find('.card-front.type-BLUE').exists()).toBe(false);
  });
});
