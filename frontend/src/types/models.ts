export type RoomStatus = 'LOBBY' | 'PLAYING' | 'FINISHED';
export type GamePhase = 'CLUE' | 'GUESSING';
export type TeamId = 'RED' | 'BLUE';
export type RoleId = 'SPYMASTER' | 'OPERATIVE' | 'SPECTATOR';
export type CardTypeId = 'RED' | 'BLUE' | 'NEUTRAL' | 'ASSASSIN';

export interface CardView {
  position: number;
  word: string;
  type: CardTypeId | null;
  revealed: boolean;
}

export interface PlayerViewDto {
  id: string;
  name: string;
  team: TeamId | null;
  role: RoleId | null;
  isHost: boolean;
  avatarUrl?: string | null;
}

export interface GameViewDto {
  currentTeam: TeamId;
  phase: GamePhase;
  clueWord: string | null;
  clueCount: number | null;
  guessesRemaining: number;
  cards: CardView[];
  winner: TeamId | null;
  redRemaining: number;
  blueRemaining: number;
}

export interface RoomViewDto {
  status: RoomStatus;
  hostPlayerId: string;
  players: PlayerViewDto[];
  game: GameViewDto | null;
  viewerId: string;
  canGiveClue: boolean;
  canGuess: boolean;
  canEndTurn: boolean;
  canStart: boolean;
  canRandomizeTeams: boolean;
}

export interface RoomBootstrapDto {
  code: string;
  language: string;
  needJoin: boolean;
  view?: RoomViewDto | null;
}

export interface RoomActionResponse {
  code: string;
  viewerId: string;
}

export interface ErrorResponse {
  error: string;
  needJoin?: boolean;
}

export interface EnumOption {
  value: string;
  label: string;
}

export interface RoomOptionsDto {
  teams: EnumOption[];
  roles: EnumOption[];
}

export interface DiscordBootstrapResponse {
  appToken: string;
  discordAccessToken: string;
  roomCode: string;
  view: RoomViewDto;
}

export type WsServerMessage =
  | { type?: 'state'; view: RoomViewDto }
  | { type: 'error'; message: string };

export type WsClientMessage =
  | { type: 'give_clue'; word: string; count: number }
  | { type: 'guess'; index: number }
  | { type: 'end_turn' };
