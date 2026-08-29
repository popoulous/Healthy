package hu.galambos.healthy.domain.scale

/** What the formula needs beyond the scale's own two numbers. */
data class BodyProfile(
    val heightCm: Int,
    val ageYears: Int,
    val female: Boolean,
) {
    val isUsable: Boolean get() = heightCm in 100..250 && ageYears in 5..120
}

/**
 * Everything derived from one weigh-in. Every field is in the unit the rest of
 * the app carries: masses in kilograms, the rate in kilocalories per day.
 */
data class BodyComposition(
    val fatPercent: Double,
    val waterMassKg: Double,
    val boneMassKg: Double,
    val muscleMassKg: Double,
    val basalRateKcal: Double,
)

/**
 * The body composition arithmetic the scale does not do.
 *
 * The scale measures two things — weight, and the impedance of a current
 * passed up one leg and down the other. Everything else is inference, done in
 * software. Zepp Life infers it too; it simply does not share the result.
 *
 * These coefficients come from the Mi Fit algorithm as reconstructed by the
 * open-source community, which is the only published description of it. They
 * will not always agree to the decimal with what Zepp Life shows, and no
 * amount of care here can fix that: the current formula is not published.
 *
 * The clamps are part of the original, not defensive padding. Impedance
 * readings from a foot that was not quite bare produce numbers that are not
 * bodies, and the bounds are where the formula itself gives up.
 */
object BodyCompositionCalculator {

    fun of(profile: BodyProfile, weightKg: Double, impedanceOhms: Int): BodyComposition? {
        if (!profile.isUsable || weightKg <= 0) return null

        val lbm = leanBodyMass(profile, weightKg, impedanceOhms)
        val fat = fatPercent(profile, weightKg, lbm)
        val water = waterPercent(fat)
        val bone = boneMass(profile, lbm)

        return BodyComposition(
            fatPercent = fat,
            waterMassKg = weightKg * water / 100.0,
            boneMassKg = bone,
            muscleMassKg = muscleMass(profile, weightKg, fat, bone),
            basalRateKcal = basalRate(profile, weightKg),
        )
    }

    private fun leanBodyMass(profile: BodyProfile, weightKg: Double, impedance: Int): Double {
        val height = profile.heightCm.toDouble()
        var lbm = (height * 9.058 / 100.0) * (height / 100.0)
        lbm += weightKg * 0.32 + 12.226
        lbm -= impedance * 0.0068
        lbm -= profile.ageYears * 0.0542
        return lbm
    }

    private fun fatPercent(profile: BodyProfile, weightKg: Double, lbm: Double): Double {
        val constant = when {
            profile.female && profile.ageYears <= 49 -> 9.25
            profile.female -> 7.25
            else -> 0.8
        }

        var coefficient = when {
            !profile.female && weightKg < 61 -> 0.98
            profile.female && weightKg > 60 -> 0.96
            profile.female && weightKg < 50 -> 1.02
            else -> 1.0
        }
        if (profile.female && weightKg !in 50.0..60.0 && profile.heightCm > 160) {
            coefficient *= 1.03
        }

        val fat = (1.0 - (((lbm - constant) * coefficient) / weightKg)) * 100.0
        return clamp(if (fat > 63) 75.0 else fat, 5.0, 75.0)
    }

    private fun waterPercent(fatPercent: Double): Double {
        val base = (100.0 - fatPercent) * 0.7
        val adjusted = base * (if (base <= 50) 1.02 else 0.98)
        return clamp(if (adjusted >= 65) 75.0 else adjusted, 35.0, 75.0)
    }

    private fun boneMass(profile: BodyProfile, lbm: Double): Double {
        val base = if (profile.female) 0.245691014 else 0.18016894
        var bone = (base - (lbm * 0.05158)) * -1
        bone += if (bone > 2.2) 0.1 else -0.1

        val ceiling = if (profile.female) 5.1 else 5.2
        if (bone > ceiling) bone = 8.0
        return clamp(bone, 0.5, 8.0)
    }

    private fun muscleMass(
        profile: BodyProfile,
        weightKg: Double,
        fatPercent: Double,
        boneMassKg: Double,
    ): Double {
        var muscle = weightKg - (fatPercent * 0.01 * weightKg) - boneMassKg
        val ceiling = if (profile.female) 84.0 else 93.5
        if (muscle >= ceiling) muscle = 120.0
        return clamp(muscle, 10.0, 120.0)
    }

    private fun basalRate(profile: BodyProfile, weightKg: Double): Double {
        val height = profile.heightCm.toDouble()
        val age = profile.ageYears.toDouble()
        var bmr = if (profile.female) {
            864.6 + (weightKg * 10.2036) - (height * 0.39336) - (age * 6.204)
        } else {
            877.8 + (weightKg * 14.916) - (height * 0.726) - (age * 8.976)
        }
        val ceiling = if (profile.female) 2996.0 else 2322.0
        if (bmr > ceiling) bmr = 5000.0
        return clamp(bmr, 500.0, 10000.0)
    }

    private fun clamp(value: Double, min: Double, max: Double) = value.coerceIn(min, max)
}
