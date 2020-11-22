package com.tylerkindy.betrayal.defs

import com.tylerkindy.betrayal.CharacterColor
import org.apache.commons.csv.CSVFormat
import java.io.InputStreamReader

data class CharacterDefinition(
    val id: Short,
    val name: String,
    val color: CharacterColor,
    val speedScale: List<Int>,
    val mightScale: List<Int>,
    val sanityScale: List<Int>,
    val knowledgeScale: List<Int>
)

private val fileStream = CharacterDefinition::class.java.getResourceAsStream("characters.csv")
val characters = CSVFormat.DEFAULT.withFirstRecordAsHeader()
    .parse(InputStreamReader(fileStream))
    .map {
        CharacterDefinition(
            id = it["id"].toShort(),
            name = it["name"],
            color = CharacterColor.valueOf(it["color"]),
            speedScale = parseTraitScale(it["speed"]),
            mightScale = parseTraitScale(it["might"]),
            sanityScale = parseTraitScale(it["sanity"]),
            knowledgeScale = parseTraitScale(it["knowledge"])
        )
    }

private fun parseTraitScale(value: String): List<Int> =
    value.split(",").map { it.toInt() }
