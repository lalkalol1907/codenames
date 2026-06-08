import type { GameViewDto, RoomViewDto } from '@/types/models';

export function makeRoomView(overrides: Partial<RoomViewDto> = {}): RoomViewDto {
  return {
    status: 'LOBBY',
    hostPlayerId: 'host-id',
    players: [],
    game: null,
    viewerId: 'viewer-id',
    canGiveClue: false,
    canGuess: false,
    canEndTurn: false,
    canStart: false,
    canRandomizeTeams: false,
    ...overrides,
  };
}

export function makeGameView(overrides: Partial<GameViewDto> = {}): GameViewDto {
  return {
    currentTeam: 'RED',
    phase: 'CLUE',
    clueWord: null,
    clueCount: null,
    guessesRemaining: 0,
    cards: [],
    winner: null,
    redRemaining: 8,
    blueRemaining: 7,
    ...overrides,
  };
}

export const localeMessages: Record<string, string> = {
  'game.over': 'Game over',
  'game.wins': '{0} wins!',
  'game.turn': 'Turn: {0}',
  'game.red_left': 'Red left: {0}',
  'game.blue_left': 'Blue left: {0}',
  'game.phase': 'Phase: {0}',
  'team.RED': 'Red',
  'team.BLUE': 'Blue',
  'phase.CLUE': 'Clue',
  'phase.GUESSING': 'Guessing',
};
