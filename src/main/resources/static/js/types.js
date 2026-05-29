/**
 * @fileoverview Shared JSDoc types for Codenames client scripts.
 * Mirrors Kotlin DTOs in com.lalkalol.web.dto.ViewDto and PageModel.clientI18nJson.
 */

/** @typedef {'LOBBY'|'PLAYING'|'FINISHED'} RoomStatus */

/** @typedef {'CLUE'|'GUESSING'} GamePhase */

/** @typedef {'RED'|'BLUE'} TeamId */

/** @typedef {'SPYMASTER'|'OPERATIVE'} RoleId */

/** @typedef {'RED'|'BLUE'|'NEUTRAL'|'ASSASSIN'} CardTypeId */

/**
 * @typedef {Object} CardView
 * @property {number} position
 * @property {string} word
 * @property {CardTypeId|null} type
 * @property {boolean} revealed
 */

/**
 * @typedef {Object} PlayerViewDto
 * @property {string} id
 * @property {string} name
 * @property {TeamId|null} [team]
 * @property {RoleId|null} [role]
 * @property {boolean} isHost
 * @property {boolean} [host] Legacy alias used in lobby render
 */

/**
 * @typedef {Object} GameViewDto
 * @property {TeamId} currentTeam
 * @property {GamePhase} phase
 * @property {string|null} clueWord
 * @property {number|null} clueCount
 * @property {number} guessesRemaining
 * @property {CardView[]} cards
 * @property {TeamId|null} winner
 * @property {number} redRemaining
 * @property {number} blueRemaining
 */

/**
 * @typedef {Object} RoomViewDto
 * @property {RoomStatus} status
 * @property {string} hostPlayerId
 * @property {PlayerViewDto[]} players
 * @property {GameViewDto|null} game
 * @property {string} viewerId
 * @property {boolean} canGiveClue
 * @property {boolean} canGuess
 * @property {boolean} canEndTurn
 * @property {boolean} canStart
 * @property {boolean} canRandomizeTeams
 */

/**
 * @typedef {Object} WsStateMessage
 * @property {'state'} [type]
 * @property {RoomViewDto} view
 */

/**
 * @typedef {Object} WsErrorMessage
 * @property {'error'} [type]
 * @property {string} message
 */

/** @typedef {WsStateMessage|WsErrorMessage} WsServerMessage */

/**
 * @typedef {Object} GiveClueMessage
 * @property {'give_clue'} type
 * @property {string} word
 * @property {number} count
 */

/**
 * @typedef {Object} GuessMessage
 * @property {'guess'} type
 * @property {number} index
 */

/**
 * @typedef {Object} EndTurnMessage
 * @property {'end_turn'} type
 */

/** @typedef {GiveClueMessage|GuessMessage|EndTurnMessage} WsClientMessage */

/**
 * @typedef {Object} CardSnapshot
 * @property {number} position
 * @property {boolean} revealed
 */

/**
 * @typedef {Record<TeamId, string>} TeamLabels
 */

/**
 * @typedef {Record<RoleId, string>} RoleLabels
 */

/**
 * @typedef {Record<GamePhase, string>} PhaseLabels
 */

/**
 * @typedef {Object} I18n
 * @property {string} [host]
 * @property {string} [choosingRole]
 * @property {string} [startGame]
 * @property {string} [startHint]
 * @property {string} [waitingHint]
 * @property {string} [randomizeTeams]
 * @property {string} [randomizeHint]
 * @property {string} [gameOver]
 * @property {string} [wins]
 * @property {string} [teamWins]
 * @property {string} [turn]
 * @property {string} [redLeft]
 * @property {string} [blueLeft]
 * @property {string} [phase]
 * @property {string} [clue]
 * @property {string} [waitingClue]
 * @property {string} [waitingYourClue]
 * @property {string} [clueWord]
 * @property {string} [count]
 * @property {string} [giveClue]
 * @property {string} [endTurn]
 * @property {TeamLabels} [teams]
 * @property {RoleLabels} [roles]
 * @property {PhaseLabels} [phases]
 */

/**
 * Globals injected by _app-data.ftl.
 * @typedef {Window & { i18n: I18n, initialView?: RoomViewDto }} CodenamesWindow
 */
