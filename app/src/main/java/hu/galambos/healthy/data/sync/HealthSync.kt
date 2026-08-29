package hu.galambos.healthy.data.sync

import hu.galambos.healthy.data.ChangePoll
import hu.galambos.healthy.data.HealthRepository
import hu.galambos.healthy.data.local.MetricStore
import hu.galambos.healthy.domain.metric.MetricDescriptor
import hu.galambos.healthy.domain.metric.MetricRegistry
import java.time.Instant
import java.time.LocalDate

/**
 * Keeps the local store level with Health Connect.
 *
 * The first pass reads a year and summarises it by day. After that the Changes
 * API says what moved, and only those days are read again — which is both far
 * faster and far kinder to Health Connect's rate limit than re-reading
 * everything whenever the app comes back to the foreground.
 */
class HealthSync(
    private val repository: HealthRepository,
    private val store: MetricStore,
    private val now: () -> Instant = Instant::now,
    private val today: () -> LocalDate = LocalDate::now,
) {

    suspend fun sync() {
        val state = store.syncState()
        val token = state?.changesToken

        if (token == null) {
            fullSync()
            return
        }

        when (val poll = repository.pollChanges(token)) {
            // A token unused for thirty days is gone, and the documented
            // recovery is to read again from the last known point rather than
            // from nothing.
            ChangePoll.TokenExpired -> resyncSince(
                Instant.ofEpochMilli(state.lastSyncEpochMillis),
            )

            // Health Connect is unreachable or refused. Leave the store as it
            // is: stale data beats a blank dashboard, and the next resume
            // tries again.
            ChangePoll.Unavailable -> Unit

            is ChangePoll.Changes -> applyChanges(poll)
        }
    }

    /** Everything the permission allows, summarised by day. */
    private suspend fun fullSync() {
        val end = today()
        val start = end.minusDays(INITIAL_DAYS)
        MetricRegistry.all.forEach { descriptor -> readInto(descriptor, start, end) }
        store.putSyncState(repository.newChangesToken(), now())
    }

    private suspend fun applyChanges(changes: ChangePoll.Changes) {
        val end = today()

        // A deletion carries only a record id, so which day lost data cannot
        // be worked out. The recent window is cleared and read again — crude,
        // but deletions are rare and correctness beats cleverness here.
        if (changes.deletions) {
            val from = end.minusDays(DELETION_REREAD_DAYS)
            MetricRegistry.all.forEach { descriptor ->
                store.clearFrom(descriptor.id, from)
                readInto(descriptor, from, end)
            }
        }

        changes.affected.forEach { (id, earliest) ->
            readInto(MetricRegistry[id], earliest, end)
        }

        store.putSyncState(changes.nextToken, now())
    }

    private suspend fun resyncSince(lastSync: Instant) {
        val end = today()
        val start = minOf(
            lastSync.atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
            end.minusDays(EXPIRY_REREAD_DAYS),
        )
        MetricRegistry.all.forEach { descriptor -> readInto(descriptor, start, end) }
        store.putSyncState(repository.newChangesToken(), now())
    }

    /**
     * Reads one metric over a range and files it. A metric with no permission
     * is skipped rather than recorded as empty — the card has to be able to
     * tell "you did not allow this" from "nothing wrote it".
     */
    private suspend fun readInto(descriptor: MetricDescriptor, from: LocalDate, to: LocalDate) {
        if (!repository.isGranted(descriptor)) return
        store.putDailyValues(descriptor.id, repository.readDailyValues(descriptor, from, to))
        store.putLatest(descriptor.id, repository.readLatest(descriptor))
    }

    private companion object {
        /**
         * A year on first run. Enough for a real trend, bounded enough that
         * the first sync finishes while the user is still looking at it.
         */
        const val INITIAL_DAYS = 365L
        const val DELETION_REREAD_DAYS = 30L
        const val EXPIRY_REREAD_DAYS = 30L
    }
}
