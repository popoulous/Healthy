package hu.galambos.healthy.domain.metric

/**
 * The unit a metric's value is carried in. Values travel as plain doubles in
 * this unit; converting to the display unit the user picked is the UI's job,
 * so the numbers stay comparable inside the app.
 */
enum class MetricUnit(val decimals: Int) {
    Count(0),
    Steps(0),
    Bpm(0),
    Percent(0),
    // The scale reports in five-gram steps, so a tenth would round away a
    // digit that was actually measured.
    Kilograms(2),
    Kilocalories(0),
    Hours(1),
    Meters(0),
    Kilometres(2),
    Litres(2),
    Celsius(1),
    MillimolesPerLitre(1),
    MillimetresOfMercury(0),
    Watts(0),
    MetresPerSecond(1),
    MillilitresPerMinuteKilogram(1),
    Minutes(0),
    BreathsPerMinute(0),
    Centimetres(0),
    Milliseconds(0),
    StepsPerMinute(0),
    RevolutionsPerMinute(0),
    CelsiusDelta(1),
}
