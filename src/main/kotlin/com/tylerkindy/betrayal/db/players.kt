package com.tylerkindy.betrayal.db

import com.tylerkindy.betrayal.Player
import com.tylerkindy.betrayal.defs.characters
import org.jetbrains.exposed.sql.select

fun getPlayers(gameId: String): List<Player> {
    return Players.select { Players.gameId eq gameId }
        .map {
            val characterId = it[Players.characterId]
            val characterDef = characters[characterId]
                ?: error("No character defined with ID $characterId")

            Player(
                id = it[Players.id],
                characterName = characterDef.name,
                color = characterDef.color,
                speed = characterDef.speed.toTrait(it[Players.speedIndex].toInt()),
                might = characterDef.might.toTrait(it[Players.mightIndex].toInt()),
                sanity = characterDef.sanity.toTrait(it[Players.sanityIndex].toInt()),
                knowledge = characterDef.knowledge.toTrait(it[Players.knowledgeIndex].toInt())
            )
        }
}
