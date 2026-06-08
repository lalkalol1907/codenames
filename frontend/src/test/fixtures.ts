import type { CardView, GameViewDto, PlayerViewDto, RoomViewDto } from '@/types/models';

export function makePlayer(overrides: Partial<PlayerViewDto> = {}): PlayerViewDto {
  return {
    id: 'viewer-id',
    name: 'Alice',
    team: 'RED',
    role: 'OPERATIVE',
    isHost: false,
    ...overrides,
  };
}

export function makeCard(overrides: Partial<CardView> = {}): CardView {
  return {
    position: 0,
    word: 'APPLE',
    type: 'RED',
    revealed: false,
    ...overrides,
  };
}

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
  'game.clue_word': 'Clue word',
  'game.count': 'Count',
  'game.give_clue': 'Give clue',
  'game.end_turn': 'End turn',
  'team.RED': 'Red',
  'team.BLUE': 'Blue',
  'phase.CLUE': 'Clue',
  'phase.GUESSING': 'Guessing',
};
