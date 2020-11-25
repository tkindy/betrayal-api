package com.tylerkindy.betrayal.db

import org.jetbrains.exposed.sql.Table

object Games : Table() {
    val id = varchar("id", 6)
    val name = varchar("name", 32)

    override val primaryKey = PrimaryKey(id)
}

object Players : Table() {
    val id = integer("id").autoIncrement(idSeqName = "players_id_seq")
    val gameId = varchar("gameId", 6)
    val characterId = short("characterId")
    val gridX = integer("gridX")
    val gridY = integer("gridY")
    val speedIndex = short("speedIndex")
    val mightIndex = short("mightIndex")
    val sanityIndex = short("sanityIndex")
    val knowledgeIndex = short("knowledgeIndex")

    override val primaryKey = PrimaryKey(id)
}
