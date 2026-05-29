package com.lalkalol.game.rules

import com.lalkalol.game.model.Card
import com.lalkalol.game.model.CardType
import com.lalkalol.game.model.Team
import com.lalkalol.words.WordRepository
import com.lalkalol.game.model.Language
import java.util.UUID
import kotlin.random.Random

data class GeneratedBoard(
    val cards: List<Card>,
    val startingTeam: Team,
)

class BoardGenerator(
    private val wordRepository: WordRepository,
) {
    suspend fun generate(language: Language, gameId: UUID): GeneratedBoard {
        val words = wordRepository.pickRandomWords(language, BOARD_SIZE)
        require(words.size == BOARD_SIZE) { "Not enough words in dictionary for language ${language.code}" }

        val startingTeam = if (Random.nextBoolean()) Team.RED else Team.BLUE
        val types = buildCardTypes(startingTeam)

        val cards = words.indices.map { index ->
            Card(
                id = UUID.randomUUID(),
                position = index,
                word = words[index],
                type = types[index],
                revealed = false,
            )
        }
        return GeneratedBoard(cards, startingTeam)
    }

    private fun buildCardTypes(startingTeam: Team): List<CardType> {
        val startingCount = 9
        val otherCount = 8
        val (startingType, otherType) = when (startingTeam) {
            Team.RED -> CardType.RED to CardType.BLUE
            Team.BLUE -> CardType.BLUE to CardType.RED
        }
        val types = mutableListOf<CardType>()
        repeat(startingCount) { types.add(startingType) }
        repeat(otherCount) { types.add(otherType) }
        repeat(7) { types.add(CardType.NEUTRAL) }
        types.add(CardType.ASSASSIN)
        types.shuffle()
        return types
    }

    companion object {
        const val BOARD_SIZE = 25
    }
}

object WinChecker {
    fun checkWinner(cards: List<Card>, lastRevealed: CardType, guessingTeam: Team): Team? {
        if (lastRevealed == CardType.ASSASSIN) {
            return TurnLogic.nextTeam(guessingTeam)
        }
        val redRemaining = cards.count { it.type == CardType.RED && !it.revealed }
        val blueRemaining = cards.count { it.type == CardType.BLUE && !it.revealed }
        return when {
            redRemaining == 0 -> Team.RED
            blueRemaining == 0 -> Team.BLUE
            else -> null
        }
    }
}

object TurnLogic {
    fun shouldEndTurnAfterGuess(cardType: CardType, currentTeam: Team): Boolean {
        val teamType = when (currentTeam) {
            Team.RED -> CardType.RED
            Team.BLUE -> CardType.BLUE
        }
        return cardType != teamType
    }

    fun nextTeam(current: Team): Team = when (current) {
        Team.RED -> Team.BLUE
        Team.BLUE -> Team.RED
    }
}
