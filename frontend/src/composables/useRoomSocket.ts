import { onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import type { RoomViewDto, WsClientMessage, WsServerMessage } from '@/types/models';
import { useRoomStore } from '@/stores/room';

function wsUrl(roomCode: string): string {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${location.host}/ws/rooms/${roomCode}`;
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
  let ws: WebSocket | null = null;
  let reconnectTimer: ReturnType<typeof setTimeout> | undefined;
  let leaveSent = false;

  function send(payload: WsClientMessage) {
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(payload));
    }
  }

  function connect() {
    if (leaveSent || typeof window === 'undefined') return;
    ws = new WebSocket(wsUrl(roomCode));
    ws.onmessage = (event) => {
      const msg = JSON.parse(event.data) as WsServerMessage;
      if ('view' in msg && msg.view) {
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
        alert(msg.message);
      }
    };
    ws.onclose = (event) => {
      if (leaveSent || event.code === 1008) return;
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

  return { send };
}
