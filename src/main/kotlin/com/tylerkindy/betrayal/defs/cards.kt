package com.tylerkindy.betrayal.defs

import org.apache.commons.csv.CSVFormat
import java.io.InputStreamReader

sealed class RollTarget {
    data class ExactRollTarget(val target: Int) : RollTarget() {
        override fun matches(roll: Int): Boolean {
            return target == roll
        }
    }

    data class RangeRollTarget(val target: IntRange) : RollTarget() {
        override fun matches(roll: Int): Boolean {
            return target.contains(roll)
        }
    }

    data class MinRollTarget(val minimum: Int) : RollTarget() {
        override fun matches(roll: Int): Boolean {
            return roll >= minimum
        }
    }

    abstract fun matches(roll: Int): Boolean

    companion object {
        private val EXACT_REGEX = Regex("(\\d+)")
        private val RANGE_REGEX = Regex("(\\d+)-(\\d+)")
        private val MIN_REGEX = Regex("(\\d+)\\+")

        fun parse(s: String): RollTarget {
            return EXACT_REGEX.matchEntire(s)
                ?.let { ExactRollTarget(it.groupValues[1].toInt()) }
                ?: RANGE_REGEX.matchEntire(s)
                    ?.let {
                        RangeRollTarget(
                            IntRange(
                                it.groupValues[1].toInt(),
                                it.groupValues[2].toInt()
                            )
                        )
                    }
                ?: MIN_REGEX.matchEntire(s)
                    ?.let { MinRollTarget(it.groupValues[1].toInt()) }
                ?: throw IllegalArgumentException("Invalid roll target: '$s'")
        }
    }
}

data class RollTableRow(
    val target: RollTarget,
    val outcome: String
)
typealias RollTable = List<RollTableRow>

private val ROW_REGEX = Regex("([^\\s]+) {2}(.*)")
fun parseRollTable(s: String?): RollTable? {
    return s?.split('\n')
        ?.map { ROW_REGEX.matchEntire(it) ?: throw IllegalArgumentException("Invalid row '$it'") }
        ?.map {
            RollTableRow(
                target = RollTarget.parse(it.groupValues[1]),
                outcome = it.groupValues[2]
            )
        }
}

data class EventDefinition(
    val id: Short,
    val name: String,
    val condition: String?,
    val flavorText: String?,
    val description: String,
    val rollTable: RollTable?
)

enum class Subtype {
    COMPANION, WEAPON
}

data class ItemDefinition(
    val id: Short,
    val name: String,
    val subtype: Subtype?,
    val flavorText: String?,
    val description: String,
    val rollTable: RollTable?
)

data class OmenDefinition(
    val id: Short,
    val name: String,
    val subtype: Subtype?,
    val flavorText: String?,
    val description: String,
    val rollTable: RollTable?
)

val events = CSVFormat.DEFAULT.withNullString("").withFirstRecordAsHeader()
    .parse(InputStreamReader(EventDefinition::class.java.getResourceAsStream("/events.csv")))
    .map {
        EventDefinition(
            id = it["id"].toShort(),
            name = it["name"],
            condition = it["condition"],
            flavorText = it["flavorText"],
            description = it["description"],
            rollTable = parseRollTable(it["rollTable"])
        )
    }
val items = CSVFormat.DEFAULT.withNullString("").withFirstRecordAsHeader()
    .parse(InputStreamReader(ItemDefinition::class.java.getResourceAsStream("/items.csv")))
    .map {
        ItemDefinition(
            id = it["id"].toShort(),
            name = it["name"],
            subtype = it["subtype"]?.let(Subtype::valueOf),
            flavorText = it["flavorText"],
            description = it["description"],
            rollTable = parseRollTable(it["rollTable"])
        )
    }
val omens = CSVFormat.DEFAULT.withNullString("").withFirstRecordAsHeader()
    .parse(InputStreamReader(OmenDefinition::class.java.getResourceAsStream("/omens.csv")))
    .map {
        OmenDefinition(
            id = it["id"].toShort(),
            name = it["name"],
            subtype = it["subtype"]?.let(Subtype::valueOf),
            flavorText = it["flavorText"],
            description = it["description"],
            rollTable = parseRollTable(it["rollTable"])
        )
    }

enum class CardType(val id: Short) {
    EVENT(0),
    ITEM(1),
    OMEN(2)
}
