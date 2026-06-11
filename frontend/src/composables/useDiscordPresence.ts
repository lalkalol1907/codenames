import { onUnmounted, watch } from 'vue';
import { useDiscordStore } from '@/stores/discord';
import { useRoomStore } from '@/stores/room';
import { getDiscordSdk } from '@/composables/useDiscord';

export function useDiscordPresence(roomCode: string) {
  const discordStore = useDiscordStore();
  const roomStore = useRoomStore();
  const startTime = Math.floor(Date.now() / 1000);

  async function updatePresence() {
    if (!discordStore.isDiscord) return;
    const sdk = getDiscordSdk();
    if (!sdk) return;
    const view = roomStore.view;
    if (!view) return;

    const activePlayers = view.players.filter(
      (p) => p.role !== 'SPECTATOR' && p.role !== null,
    ).length;

    let details: string;
    let state: string;

    if (view.status === 'LOBBY') {
      details = `Лобби · ${view.players.length} чел.`;
      state = activePlayers > 0 ? `${activePlayers} из 4 готовы` : 'Ожидание игроков';
    } else if (view.status === 'PLAYING') {
      details = 'Идёт игра';
      state = view.game?.winner
        ? `Победа ${view.game.winner === 'RED' ? 'красных' : 'синих'}`
        : `Ход ${view.game?.currentTeam === 'RED' ? 'красных' : 'синих'}`;
    } else {
      details = 'Игра завершена';
      state = view.game?.winner ? `Победа ${view.game.winner === 'RED' ? 'красных' : 'синих'}` : '';
    }

    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      await (sdk.commands as any).setActivity({
        activity: {
          details,
          state,
          party: {
            id: roomCode,
            size: [view.players.length, Math.max(view.players.length, 4)],
          },
          timestamps: { start: startTime },
        },
      });
    } catch {
      // setActivity is best-effort; not all Discord clients support it
    }
  }

  const stopWatch = watch(
    () =>
      [
        roomStore.view?.status,
        roomStore.view?.players.length,
        roomStore.view?.game?.currentTeam,
        roomStore.view?.game?.winner,
      ] as const,
    () => void updatePresence(),
    { immediate: true },
  );

  onUnmounted(stopWatch);
}
