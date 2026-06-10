import { onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { RoomViewDto, WsClientMessage, WsServerMessage } from '@/types/models';
import { useRoomStore } from '@/stores/room';
import { useDiscordStore } from '@/stores/discord';

export type WsConnectionState = 'connecting' | 'connected' | 'reconnecting';

function wsUrl(roomCode: string): string {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
  const base = `${protocol}//${location.host}/ws/rooms/${roomCode}`;
  try {
    const discordStore = useDiscordStore();
    if (discordStore.appToken) {
      return `${base}?token=${encodeURIComponent(discordStore.appToken)}`;
    }
  } catch {
    // Pinia not ready
  }
  return base;
}

function isStillInRoom(view: RoomViewDto): boolean {
  const viewerId = view.viewerId.toLowerCase();
  return view.players.some((p) => p.id.toLowerCase() === viewerId);
}

export function useRoomSocket(
  roomCode: string,
  options: {
    redirectOnPlaying?: boolean;
    redirectOnKicked?: boolean;
  } = {},
) {
  const roomStore = useRoomStore();
  const router = useRouter();
  const wsError = ref<string | null>(null);
  const wsConnection = ref<WsConnectionState>('connecting');
  let ws: WebSocket | null = null;
  let reconnectTimer: ReturnType<typeof setTimeout> | undefined;
  let leaveSent = false;
  let hasConnectedOnce = false;

  function send(payload: WsClientMessage) {
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(payload));
    }
  }

  function connect() {
    if (leaveSent || typeof window === 'undefined') return;
    wsConnection.value = hasConnectedOnce ? 'reconnecting' : 'connecting';
    ws = new WebSocket(wsUrl(roomCode));
    ws.onopen = () => {
      hasConnectedOnce = true;
      wsConnection.value = 'connected';
    };
    ws.onmessage = (event) => {
      const msg = JSON.parse(event.data) as WsServerMessage;
      if ('view' in msg && msg.view) {
        wsError.value = null;
        roomStore.setView(msg.view, roomCode);
        if (
          options.redirectOnPlaying &&
          (msg.view.status === 'PLAYING' || msg.view.status === 'FINISHED')
        ) {
          router.push(`/rooms/${roomCode}/game`);
          return;
        }
        if (options.redirectOnKicked && !isStillInRoom(msg.view)) {
          router.push('/');
        }
      } else if ('message' in msg) {
        wsError.value = msg.message;
      }
    };
    ws.onclose = (event) => {
      if (leaveSent || event.code === 1008) return;
      wsConnection.value = 'reconnecting';
      clearTimeout(reconnectTimer);
      reconnectTimer = setTimeout(connect, 2000);
    };
  }

  onMounted(() => {
    connect();
    const onPageHide = () => {
      leaveSent = true;
      clearTimeout(reconnectTimer);
      ws?.close();
    };
    window.addEventListener('pagehide', onPageHide);
    onUnmounted(() => {
      window.removeEventListener('pagehide', onPageHide);
      leaveSent = true;
      clearTimeout(reconnectTimer);
      ws?.close();
    });
  });

  return { send, wsError, wsConnection };
}
