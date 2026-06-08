import { beforeEach, describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import Controls from '@/components/Controls.vue';
import { useLocaleStore } from '@/stores/locale';
import { localeMessages, makeGameView, makePlayer, makeRoomView } from '@/test/fixtures';

describe('Controls', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    useLocaleStore().messages = localeMessages;
  });

  it('emits giveClue when spymaster submits clue form', async () => {
    const view = makeRoomView({
      canGiveClue: true,
      viewerId: 'spy-id',
      players: [makePlayer({ id: 'spy-id', role: 'SPYMASTER' })],
    });
    const game = makeGameView();

    const wrapper = mount(Controls, { props: { view, game } });
    await wrapper.find('input[type="text"]').setValue('ocean');
    await wrapper.find('input[type="number"]').setValue('2');
    await wrapper.find('button').trigger('click');

    expect(wrapper.emitted('giveClue')).toEqual([['ocean', 2]]);
  });

  it('emits endTurn when operative can end turn', async () => {
    const view = makeRoomView({
      canGiveClue: false,
      canEndTurn: true,
      viewerId: 'op-id',
      players: [makePlayer({ id: 'op-id', role: 'OPERATIVE' })],
    });

    const wrapper = mount(Controls, {
      props: { view, game: makeGameView({ phase: 'GUESSING' }) },
    });
    await wrapper.find('button').trigger('click');

    expect(wrapper.emitted('endTurn')).toEqual([[]]);
  });

  it('hides controls for operative while waiting', () => {
    const view = makeRoomView({
      canGiveClue: false,
      canEndTurn: false,
      viewerId: 'op-id',
      players: [makePlayer({ id: 'op-id', role: 'OPERATIVE' })],
    });

    const wrapper = mount(Controls, { props: { view, game: makeGameView() } });

    expect(wrapper.find('.controls').classes()).toContain('hidden');
  });

  it('shows disabled clue form for spymaster while waiting', () => {
    const view = makeRoomView({
      canGiveClue: false,
      canEndTurn: false,
      viewerId: 'spy-id',
      players: [makePlayer({ id: 'spy-id', role: 'SPYMASTER' })],
    });

    const wrapper = mount(Controls, { props: { view, game: makeGameView() } });

    expect(wrapper.find('.controls').classes()).toContain('controls--disabled');
    expect(wrapper.find('button').attributes('disabled')).toBeDefined();
  });

  it('renders nothing when game has a winner', () => {
    const view = makeRoomView({
      canGiveClue: true,
      players: [makePlayer({ role: 'SPYMASTER' })],
    });

    const wrapper = mount(Controls, {
      props: { view, game: makeGameView({ winner: 'RED' }) },
    });

    expect(wrapper.find('.controls').exists()).toBe(false);
  });
});
