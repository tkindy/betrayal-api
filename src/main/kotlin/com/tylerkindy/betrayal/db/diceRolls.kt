package com.tylerkindy.betrayal.db

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random
import kotlin.random.nextInt

private val numDiceRange = 1..8

fun getLatestRoll(gameId: String): List<Int>? {
    return transaction {
        DiceRolls.select { DiceRolls.gameId eq gameId }
            .orderBy(DiceRolls.id, SortOrder.DESC)
            .firstOrNull()
            ?.let {
                it[DiceRolls.rolls].split("|")
                    .map(String::toInt)
            }
    }
}

fun rollDice(gameId: String, numDice: Int): List<Int> {
    if (!numDiceRange.contains(numDice)) {
        throw IllegalArgumentException("Invalid number of dice $numDice")
    }

    val rolls = (0 until numDice).map { Random.nextInt(0..2) }

    transaction {
        DiceRolls.insert {
            it[this.gameId] = gameId
            it[this.rolls] = rolls.joinToString("|")
        }
    }

    return rolls
}
