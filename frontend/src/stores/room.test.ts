import { beforeEach, describe, expect, it } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useRoomStore } from '@/stores/room';
import { makeRoomView } from '@/test/fixtures';

describe('useRoomStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('stores view and room code', () => {
    const store = useRoomStore();
    const view = makeRoomView({ status: 'LOBBY' });

    store.setView(view, 'ABCD');

    expect(store.view).toEqual(view);
    expect(store.roomCode).toBe('ABCD');
  });

  it('clears room state', () => {
    const store = useRoomStore();
    store.setView(makeRoomView(), 'ABCD');

    store.clear();

    expect(store.view).toBeNull();
    expect(store.roomCode).toBeNull();
  });
});
