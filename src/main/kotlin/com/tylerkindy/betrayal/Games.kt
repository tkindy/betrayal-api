package com.tylerkindy.betrayal

import org.jetbrains.exposed.sql.Table

object Games : Table() {
    val id = varchar("id", 6)
    val name = varchar("name", 32)
    override val primaryKey = PrimaryKey(id)
}
