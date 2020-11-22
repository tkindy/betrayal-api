package com.tylerkindy.betrayal

import org.jetbrains.exposed.sql.Table

object Games : Table() {
    val id = varchar("id", 6)
    val name = varchar("name", 32)

    override val primaryKey = PrimaryKey(id)
}

object Players : Table() {
    val id = integer("id")
    val gameId = varchar("gameId", 6)
    val characterId = short("characterId")
    val speedIndex = short("speedIndex")
    val mightIndex = short("mightIndex")
    val sanityIndex = short("sanityIndex")
    val knowledgeIndex = short("knowledgeIndex")

    override val primaryKey = PrimaryKey(id)
}
