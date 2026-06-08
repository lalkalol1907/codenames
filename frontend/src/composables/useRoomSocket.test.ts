import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent } from 'vue';
import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { useRoomSocket } from '@/composables/useRoomSocket';
import { useRoomStore } from '@/stores/room';
import { makePlayer, makeRoomView } from '@/test/fixtures';

const push = vi.fn();

vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
}));

const sockets: MockWebSocket[] = [];

class MockWebSocket {
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSED = 3;

  readyState = MockWebSocket.CONNECTING;
  onopen: (() => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  sent: string[] = [];

  constructor(public url: string) {
    sockets.push(this);
  }

  send(data: string) {
    this.sent.push(data);
  }

  close() {
    this.simulateClose();
  }

  simulateOpen() {
    this.readyState = MockWebSocket.OPEN;
    this.onopen?.();
  }

  simulateMessage(data: unknown) {
    this.onmessage?.({ data: JSON.stringify(data) } as MessageEvent);
  }

  simulateClose(code = 1000) {
    this.readyState = MockWebSocket.CLOSED;
    this.onclose?.({ code } as CloseEvent);
  }
}

function mountSocket(
  code = 'ABCD',
  options: { redirectOnPlaying?: boolean; redirectOnKicked?: boolean } = {},
) {
  const Host = defineComponent({
    props: {
      code: { type: String, required: true },
      options: { type: Object, default: () => ({}) },
    },
    setup(props) {
      return useRoomSocket(props.code, props.options as typeof options);
    },
    template: '<div />',
  });

  return mount(Host, { props: { code, options } });
}

function latestSocket(): MockWebSocket {
  const socket = sockets.at(-1);
  if (!socket) throw new Error('WebSocket was not created');
  return socket;
}

describe('useRoomSocket', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    sockets.length = 0;
    push.mockReset();
    vi.stubGlobal('WebSocket', MockWebSocket);
    Object.assign(globalThis.WebSocket, {
      CONNECTING: MockWebSocket.CONNECTING,
      OPEN: MockWebSocket.OPEN,
      CLOSED: MockWebSocket.CLOSED,
    });
    vi.useRealTimers();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('connects to room websocket and tracks connection state', async () => {
    const wrapper = mountSocket('ABCD');

    expect(wrapper.vm.wsConnection).toBe('connecting');
    expect(latestSocket().url).toBe('ws://localhost:3000/ws/rooms/ABCD');

    latestSocket().simulateOpen();
    await flushPromises();

    expect(wrapper.vm.wsConnection).toBe('connected');
  });

  it('send posts JSON payload when socket is open', async () => {
    const wrapper = mountSocket();
    latestSocket().simulateOpen();
    await flushPromises();

    wrapper.vm.send({ type: 'guess', index: 4 });
    await flushPromises();

    expect(latestSocket().sent).toEqual([JSON.stringify({ type: 'guess', index: 4 })]);
  });

  it('stores server error messages', async () => {
    const wrapper = mountSocket();
    latestSocket().simulateOpen();
    latestSocket().simulateMessage({ type: 'error', message: 'Not your turn' });
    await flushPromises();

    expect(wrapper.vm.wsError).toBe('Not your turn');
  });

  it('updates room store from state messages', async () => {
    const store = useRoomStore();
    const view = makeRoomView({
      status: 'LOBBY',
      players: [makePlayer({ id: 'viewer-id' })],
    });
    const wrapper = mountSocket();
    latestSocket().simulateOpen();
    latestSocket().simulateMessage({ type: 'state', view });
    await flushPromises();

    expect(wrapper.vm.wsError).toBeNull();
    expect(store.view).toEqual(view);
    expect(store.roomCode).toBe('ABCD');
  });

  it('redirects to game when playing state arrives', async () => {
    mountSocket('ABCD', { redirectOnPlaying: true });
    latestSocket().simulateOpen();
    latestSocket().simulateMessage({
      type: 'state',
      view: makeRoomView({ status: 'PLAYING', players: [makePlayer()] }),
    });
    await flushPromises();

    expect(push).toHaveBeenCalledWith('/rooms/ABCD/game');
  });

  it('redirects home when viewer is kicked from room', async () => {
    mountSocket('ABCD', { redirectOnKicked: true });
    latestSocket().simulateOpen();
    latestSocket().simulateMessage({
      type: 'state',
      view: makeRoomView({
        viewerId: 'viewer-id',
        players: [makePlayer({ id: 'other-id' })],
      }),
    });
    await flushPromises();

    expect(push).toHaveBeenCalledWith('/');
  });

  it('marks connection as reconnecting after unexpected close', async () => {
    vi.useFakeTimers();
    const wrapper = mountSocket();
    latestSocket().simulateOpen();
    latestSocket().simulateClose();
    await flushPromises();

    expect(wrapper.vm.wsConnection).toBe('reconnecting');

    vi.advanceTimersByTime(2000);
    await flushPromises();

    expect(sockets).toHaveLength(2);
    expect(wrapper.vm.wsConnection).toBe('reconnecting');
  });
});
