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
})
