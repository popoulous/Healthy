package hu.galambos.healthy.domain.scale

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyCompositionTest {

    private val man = BodyProfile(heightCm = 178, ageYears = 33, female = false)
    private val woman = BodyProfile(heightCm = 165, ageYears = 33, female = true)

    @Test
    fun `a plausible weigh-in gives plausible numbers`() {
        val result = BodyCompositionCalculator.of(man, weightKg = 79.2, impedanceOhms = 512)!!

        assertTrue("fat ${result.fatPercent}", result.fatPercent in 5.0..40.0)
        assertTrue("water ${result.waterMassKg}", result.waterMassKg in 20.0..70.0)
        assertTrue("bone ${result.boneMassKg}", result.boneMassKg in 1.0..5.0)
        assertTrue("muscle ${result.muscleMassKg}", result.muscleMassKg in 30.0..90.0)
        assertTrue("bmr ${result.basalRateKcal}", result.basalRateKcal in 1200.0..2400.0)
    }

    /**
     * The parts have to add up to roughly the person. If muscle, fat and bone
     * drift far from the weight, something has gone wrong in the arithmetic
     * rather than in the body.
     */
    @Test
    fun `fat, muscle and bone account for the weight`() {
        val weight = 79.2
        val result = BodyCompositionCalculator.of(man, weight, impedanceOhms = 512)!!

        val fatMass = weight * result.fatPercent / 100.0
        val total = fatMass + result.muscleMassKg + result.boneMassKg

        assertEquals(weight, total, 0.5)
    }

    @Test
    fun `the same body at a higher impedance reads as fatter`() {
        val lean = BodyCompositionCalculator.of(man, 79.2, impedanceOhms = 450)!!
        val less = BodyCompositionCalculator.of(man, 79.2, impedanceOhms = 650)!!

        assertTrue(less.fatPercent > lean.fatPercent)
    }

    @Test
    fun `sex changes the result for otherwise identical input`() {
        val asMan = BodyCompositionCalculator.of(
            BodyProfile(165, 33, female = false), 62.0, 520,
        )!!
        val asWoman = BodyCompositionCalculator.of(woman, 62.0, 520)!!

        assertTrue(asWoman.fatPercent > asMan.fatPercent)
    }

    @Test
    fun `basal rate falls with age`() {
        val young = BodyCompositionCalculator.of(man.copy(ageYears = 25), 79.2, 512)!!
        val older = BodyCompositionCalculator.of(man.copy(ageYears = 65), 79.2, 512)!!

        assertTrue(older.basalRateKcal < young.basalRateKcal)
    }

    /**
     * Without a height and an age there is no formula, only a weight. Better
     * to return nothing than a figure computed from a guess.
     */
    @Test
    fun `an unfilled profile yields nothing`() {
        assertNull(BodyCompositionCalculator.of(BodyProfile(0, 0, false), 79.2, 512))
        assertNull(BodyCompositionCalculator.of(man.copy(heightCm = 40), 79.2, 512))
    }

    @Test
    fun `a nonsense weight yields nothing`() {
        assertNull(BodyCompositionCalculator.of(man, weightKg = 0.0, impedanceOhms = 512))
    }

    @Test
    fun `extreme impedance is clamped rather than producing an impossible body`() {
        val result = BodyCompositionCalculator.of(man, 79.2, impedanceOhms = 3000)

        assertNotNull(result)
        assertTrue(result!!.fatPercent <= 75.0)
        assertTrue(result.boneMassKg >= 0.5)
        assertTrue(result.muscleMassKg >= 10.0)
    }
}
