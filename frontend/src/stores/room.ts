import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { RoomViewDto } from '@/types/models';

export const useRoomStore = defineStore('room', () => {
  const view = ref<RoomViewDto | null>(null);
  const roomCode = ref<string | null>(null);
  const language = ref<string>('en');

  function setView(next: RoomViewDto | null, code?: string) {
    view.value = next;
    if (code) roomCode.value = code;
  }

  function clear() {
    view.value = null;
    roomCode.value = null;
  }

  return { view, roomCode, language, setView, clear };
});
