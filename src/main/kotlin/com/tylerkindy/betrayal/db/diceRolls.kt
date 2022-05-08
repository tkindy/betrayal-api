package com.tylerkindy.betrayal.db

import com.tylerkindy.betrayal.DiceRoll
import com.tylerkindy.betrayal.DiceRollType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random
import kotlin.random.nextInt

private val numDiceRange = 1..8

fun getLatestRoll(gameId: String): DiceRoll? {
    return transaction {
        DiceRolls.select { DiceRolls.gameId eq gameId }
            .orderBy(DiceRolls.id, SortOrder.DESC)
            .firstOrNull()
            ?.let { row ->
                DiceRoll(
                    values = row[DiceRolls.rolls]
                        .split("|")
                        .map(String::toInt),
                    type = row[DiceRolls.type]?.let { DiceRollType.valueOf(it) }
                        ?: DiceRollType.AD_HOC
                )
            }
    }
}

fun rollDice(gameId: String, numDice: Int, type: DiceRollType): DiceRoll {
    if (!numDiceRange.contains(numDice)) {
        throw IllegalArgumentException("Invalid number of dice $numDice")
    }

    val values = (0 until numDice).map { Random.nextInt(0..2) }

    transaction {
        DiceRolls.insert {
            it[this.gameId] = gameId
            it[this.rolls] = values.joinToString("|")
            it[this.type] = type.name
        }
    }

    return DiceRoll(values = values, type = type)
}
