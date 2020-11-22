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

object Rooms : Table() {
    val id = integer("id").autoIncrement(idSeqName = "rooms_id_seq")
    val gameId = varchar("gameId", 6)
    val roomDefId = short("roomDefId")
    val gridX = integer("gridX")
    val gridY = integer("gridY")
    val rotation = short("rotation")
}

object Stacks : Table() {
    val id = integer("id").autoIncrement(idSeqName = "stacks_id_seq")
    val gameId = varchar("gameId", 6)
    val stackTypeId = short("stackTypeId")
    val curIndex = short("curIndex").nullable()
}

object StackContents : Table() {
    val id = integer("id").autoIncrement(idSeqName = "stackContents_id_seq")
    val stackId = integer("stackId")
    val index = short("index")
    val contentId = short("contentId")
}
