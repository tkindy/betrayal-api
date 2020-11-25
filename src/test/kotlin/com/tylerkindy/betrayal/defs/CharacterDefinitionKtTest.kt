package com.tylerkindy.betrayal.defs

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CharacterDefinitionKtTest : StringSpec({
    "parseTraitDefinition works" {
        parseTraitDefinition("2,2,3,5*,5,5,6,6") shouldBe TraitDefinition(
            scale = listOf(2, 2, 3, 5, 5, 5, 6, 6),
            startingIndex = 3
        )
    }

    "parseTraitDefinition catches missing starting value" {
        shouldThrow<IllegalArgumentException> { parseTraitDefinition("2,3,4,4,4,6,6") }
    }

    "parseTraitDefinition catches duplicate starting values" {
        shouldThrow<IllegalArgumentException> { parseTraitDefinition("2,3*,4,4,4*,6,6") }
    }

    "parseTraitDefinition catches empty value" {
        shouldThrow<IllegalArgumentException> { parseTraitDefinition("") }
    }
})
