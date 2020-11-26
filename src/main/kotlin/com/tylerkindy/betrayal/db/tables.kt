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

object RoomStacks : Table("\"roomStacks\"") {
    val id = integer("id").autoIncrement(idSeqName = "roomStacks_id_seq")
    val gameId = varchar("gameId", 6)
    val curIndex = short("curIndex").nullable()
    val flipped = bool("flipped")
    val rotation = short("rotation").nullable()
}

object RoomStackContents : Table("\"roomStackContents\"") {
    val id = integer("id").autoIncrement(idSeqName = "roomStackContents_id_seq")
    val stackId = integer("stackId")
    val index = short("index")
    val roomDefId = short("roomDefId")
}

object CardStacks : Table("\"cardStacks\"") {
    val id = integer("id").autoIncrement(idSeqName = "cardStacks_id_seq")
    val gameId = varchar("gameId", 6)
    val cardTypeId = short("cardTypeId")
    val curIndex = short("curIndex").nullable()
}

object CardStackContents : Table("\"cardStackContents\"") {
    val id = integer("id").autoIncrement(idSeqName = "cardStackContents_id_seq")
    val stackId = integer("stackId")
    val index = short("index")
    val cardDefId = short("cardDefId")
}
