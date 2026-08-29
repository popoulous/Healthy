package hu.galambos.healthy.domain

/**
 * Health Connect is part of the platform from Android 14, and an installable
 * app before that. The app runs on both, so all three states are reachable.
 */
enum class HealthConnectAvailability {
    Available,

    /** Installed, but too old for the APIs this app uses. */
    UpdateRequired,

    /** Not installed. Only possible before Android 14. */
    NotInstalled,
}

/**
 * Everything this app shows was written by some other app, and other apps'
 * records are readable only 30 days back unless the history permission is
 * held. That permission does not exist on every device, which is a different
 * situation from having been refused it — the user can act on the second.
 */
enum class HistoryAccess {
    /** This device's Health Connect has no such permission; 30 days is the ceiling. */
    Unsupported,
    NotGranted,
    Granted,
}
