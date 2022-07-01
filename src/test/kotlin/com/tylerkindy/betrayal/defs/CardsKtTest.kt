package com.tylerkindy.betrayal.defs

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CardsKtTest : StringSpec({
    "parsing of RangeRollTargets" {
        val target = RollTarget.parse("3-5")

        target shouldBe RollTarget.RangeRollTarget(3..5)
        target.matches(2) shouldBe false
        target.matches(3) shouldBe true
        target.matches(4) shouldBe true
        target.matches(5) shouldBe true
        target.matches(6) shouldBe false
    }

    "serialization of ExactRollTargets" {
        val target: RollTarget = RollTarget.ExactRollTarget(7)

        Json.encodeToString(target) shouldBe
                """
                {"type":"EXACT","target":7}
            """.trimIndent()
    }

    "serialization of RangeRollTargets" {
        val target: RollTarget = RollTarget.RangeRollTarget(3..5)

        Json.encodeToString(target) shouldBe
                """
                {"type":"RANGE","start":3,"end":5}
            """.trimIndent()
    }

    "serialization of MinRollTargets" {
        val target: RollTarget = RollTarget.MinRollTarget(3)

        Json.encodeToString(target) shouldBe
                """
                {"type":"MIN","minimum":3}
            """.trimIndent()
    }

    "parsing of events" {
        events[3] shouldBe EventDefinition(
            id = 3,
            name = "Phone Call",
            condition = null,
            flavorText = "A phone rings in the room. You feel compelled to answer it.",
            description = "Roll 2 dice. A sweet little granny voice says:\n<rollTable>",
            rollTable = listOf(
                RollTableRow(
                    RollTarget.ExactRollTarget(4),
                    "“Tea and cakes! Tea and cakes! You always were my favorite!” Gain 1 Sanity."
                ),
                RollTableRow(
                    RollTarget.ExactRollTarget(3),
                    "“I’m always here for you, Pattycakes. Watching…” Gain 1 Knowledge."
                ),
                RollTableRow(
                    RollTarget.RangeRollTarget(1..2),
                    "“I’m here, Sweetums! Give us a kiss!” Take 1 die of mental damage."
                ),
                RollTableRow(
                    RollTarget.ExactRollTarget(0),
                    "“Bad little children must be punished!” Take 2 dice of physical damage."
                )
            )
        )
    }
})
