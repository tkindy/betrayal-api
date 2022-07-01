package com.tylerkindy.betrayal.defs

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CharacterKtTest : DescribeSpec({
    describe("parseTraitDefinition") {
        it("works") {
            parseTraitDefinition("2,2,3,5*,5,5,6,6") shouldBe TraitDefinition(
                scale = listOf(2, 2, 3, 5, 5, 5, 6, 6),
                startingIndex = 3
            )
        }

        it("catches missing starting value") {
            shouldThrow<IllegalArgumentException> { parseTraitDefinition("2,3,4,4,4,6,6") }
        }

        it("catches duplicate starting values") {
            shouldThrow<IllegalArgumentException> { parseTraitDefinition("2,3*,4,4,4*,6,6") }
        }

        it("catches empty value") {
            shouldThrow<IllegalArgumentException> { parseTraitDefinition("") }
        }
    }
})
