package hu.galambos.healthy.domain.metric

/**
 * A stable key for a metric, independent of its record class name. UI state,
 * saved scroll positions and preferences key off this, so the entries must not
 * be renamed casually.
 */
enum class MetricId {
    // Headline six — the metrics the phone is known to actually carry.
    Steps,
    HeartRate,
    Sleep,
    OxygenSaturation,
    ActiveCalories,
    TotalCalories,
    Weight,

    // Everything else Health Connect can hold, added in F3.
    ActivityIntensity,
    BasalMetabolicRate,
    BloodGlucose,
    BloodPressure,
    BodyFat,
    BodyTemperature,
    BodyWaterMass,
    BoneMass,
    CyclingPedalingCadence,
    Distance,
    ElevationGained,
    Exercise,
    FloorsClimbed,
    HeartRateVariability,
    Height,
    Hydration,
    LeanBodyMass,
    Mindfulness,
    Nutrition,
    PlannedExercise,
    Power,
    RespiratoryRate,
    RestingHeartRate,
    SkinTemperature,
    Speed,
    StepsCadence,
    Vo2Max,
    WheelchairPushes,
}
