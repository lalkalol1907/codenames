import { beforeEach, describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import StatusBar from '@/components/StatusBar.vue';
import { useLocaleStore } from '@/stores/locale';
import { localeMessages, makeGameView } from '@/test/fixtures';

describe('StatusBar', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    useLocaleStore().messages = localeMessages;
  });

  it('renders winner status when game is finished', () => {
    const wrapper = mount(StatusBar, {
      props: {
        game: makeGameView({ winner: 'RED' }),
      },
    });

    expect(wrapper.text()).toContain('Game over');
    expect(wrapper.text()).toContain('Red wins!');
  });

  it('renders turn and remaining counters during play', () => {
    const wrapper = mount(StatusBar, {
      props: {
        game: makeGameView({
          currentTeam: 'BLUE',
          phase: 'GUESSING',
          redRemaining: 5,
          blueRemaining: 4,
        }),
      },
    });

    expect(wrapper.text()).toContain('Turn: Blue');
    expect(wrapper.text()).toContain('Red left: 5');
    expect(wrapper.text()).toContain('Blue left: 4');
    expect(wrapper.text()).toContain('Phase: Guessing');
  });
});
