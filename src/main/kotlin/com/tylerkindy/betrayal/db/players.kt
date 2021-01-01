package com.tylerkindy.betrayal.db

import com.tylerkindy.betrayal.GridLoc
import com.tylerkindy.betrayal.Player
import com.tylerkindy.betrayal.defs.CharacterDefinition
import com.tylerkindy.betrayal.defs.characters
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

fun getPlayers(gameId: String): List<Player> {
    return transaction {
        Players.select { Players.gameId eq gameId }
            .map { it.toPlayer() }
    }
}

fun getPlayer(gameId: String, playerId: Int): Player {
    return transaction {
        Players.select { (Players.gameId eq gameId) and (Players.id eq playerId) }
            .firstOrNull()
            ?.run(ResultRow::toPlayer)
            ?: throw IllegalArgumentException("Player $playerId is not in game $gameId")
    }
}

private fun ResultRow.toPlayer(): Player {
    val id = this[Players.id]
    val characterId = this[Players.characterId]
    val characterDef = characters[characterId]
        ?: error("No character defined with ID $characterId")

    return Player(
        id = id,
        characterName = characterDef.name,
        color = characterDef.color,
        loc = GridLoc(this[Players.gridX], this[Players.gridY]),
        speed = characterDef.speed.toTrait(this[Players.speedIndex].toInt()),
        might = characterDef.might.toTrait(this[Players.mightIndex].toInt()),
        sanity = characterDef.sanity.toTrait(this[Players.sanityIndex].toInt()),
        knowledge = characterDef.knowledge.toTrait(this[Players.knowledgeIndex].toInt()),
        cards = getPlayerInventory(this[Players.gameId], id)
    )
}

fun insertStartingPlayers(gameId: String, numPlayers: Int) {
    val characters = getRandomCharacters(numPlayers)
    transaction {
        Players.batchInsert(characters) { character ->
            this[Players.gameId] = gameId
            this[Players.characterId] = character.id
            this[Players.gridX] = entranceHallLoc.gridX
            this[Players.gridY] = entranceHallLoc.gridY
            this[Players.speedIndex] = character.speed.startingIndex.toShort()
            this[Players.mightIndex] = character.might.startingIndex.toShort()
            this[Players.sanityIndex] = character.sanity.startingIndex.toShort()
            this[Players.knowledgeIndex] = character.knowledge.startingIndex.toShort()
        }
    }
}

private fun getRandomCharacters(numPlayers: Int): List<CharacterDefinition> {
    return characters.values
        .groupBy { it.color }.values
        .map { it.random() } // randomly pick one of each color
        .shuffled()
        .subList(0, numPlayers)
}

fun movePlayer(gameId: String, playerId: Int, loc: GridLoc) {
    getRoomAtLoc(gameId, loc)
        ?: throw IllegalArgumentException("Can't place player in empty space")

    val numUpdated = transaction {
        Players.update(where = { (Players.gameId eq gameId) and (Players.id eq playerId) }) {
            it[gridX] = loc.gridX
            it[gridY] = loc.gridY
        }
    }

    if (numUpdated == 0) {
        throw IllegalArgumentException("No player $playerId in game $gameId")
    }
}
