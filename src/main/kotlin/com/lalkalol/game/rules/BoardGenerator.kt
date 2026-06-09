package com.lalkalol.game.rules

import com.lalkalol.common.model.Language
import com.lalkalol.common.model.Team
import com.lalkalol.game.model.Card
import com.lalkalol.game.model.CardType
import com.lalkalol.words.WordRepository
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.random.Random

data class GeneratedBoard(
    val cards: List<Card>,
    val startingTeam: Team,
)

@Component
class BoardGenerator(
    private val wordRepository: WordRepository,
) {
    fun generate(language: Language, gameId: UUID): GeneratedBoard {
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
