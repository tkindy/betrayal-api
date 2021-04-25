package com.tylerkindy.betrayal.db

import com.tylerkindy.betrayal.Direction
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.ShortShrinker
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.ints
import io.kotest.property.exhaustive.map
import io.kotest.property.forAll
import kotlin.random.nextInt

val directionSets = Arb.set(Arb.enum<Direction>(), 0..Direction.values().size)
val rotations = Exhaustive.ints(0..3).map { it.toShort() }
val invalidRotationValues = (Short.MIN_VALUE..Short.MAX_VALUE) - (0..3)
val invalidRotations = arbitrary(listOf(Short.MIN_VALUE, -1, 4, Short.MAX_VALUE), ShortShrinker) {
    it.random.nextInt(invalidRotationValues.indices).let { i -> invalidRotationValues[i] }.toShort()
}

class RoomsKtTest : DescribeSpec({

    describe("rotateDoors") {
        it("maintains number of directions") {
            forAll(directionSets, rotations) { dirs, rotation ->
                rotateDoors(dirs, rotation).size == dirs.size
            }
        }

        it("throws for invalid rotations") {
            checkAll(directionSets, invalidRotations) { dirs, rotation ->
                shouldThrow<IllegalArgumentException> { rotateDoors(dirs, rotation) }
            }
        }

        it("returns the same directions with no rotation") {
            forAll(directionSets) { dirs ->
                rotateDoors(dirs, 0) == dirs
            }
        }

        it("always returns all directions") {
            val allDirections = Direction.values().toSet()
            forAll(rotations) { rotation ->
                rotateDoors(allDirections, rotation) == allDirections
            }
        }

        it("rotates doors") {
            rotateDoors(
                setOf(Direction.NORTH, Direction.WEST),
                3
            ) shouldBe setOf(Direction.NORTH, Direction.EAST)
        }
    }
})
