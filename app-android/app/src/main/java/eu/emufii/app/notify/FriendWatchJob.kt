package eu.emufii.app.notify

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import eu.emufii.app.network.CoordinatorClient
import eu.emufii.app.profile.FriendStatus
import eu.emufii.app.profile.FriendStore
import eu.emufii.app.settings.SettingsStore
import eu.emufii.app.update.UpdateCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The watch that runs while Emufii is closed.
 *
 * ## What this can and cannot promise, stated plainly
 *
 * Emufii is sideloaded and has no push service behind it: nothing on a server
 * can wake this app up. The only honest mechanism left is to ask, now and then,
 * from the device itself. Android's floor for periodic work is fifteen minutes,
 * and Doze stretches it further on a phone in a pocket. So an alert about a
 * friend can arrive a quarter of an hour after they arrived, sometimes more,
 * and a friend who plays for ten minutes may never be announced at all.
 *
 * That is a real limit and it is written into the settings text rather than
 * hidden: a feature that quietly under-delivers teaches people to distrust every
 * notification the app ever sends. What this does deliver reliably is the slow
 * news, a new version and a friend who settles in for an evening.
 *
 * ## Why JobScheduler and not WorkManager
 *
 * WorkManager would bring a dependency, a database and a hundred kilobytes for
 * one periodic task with no chaining, no constraints beyond the network, and no
 * result to observe. The platform scheduler does this exact job, and the APK
 * stays where it is.
 */
class FriendWatchJob : JobService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var work: Job? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        work = scope.launch {
            runCatching { sweep(applicationContext) }
            // Never rescheduled on failure: the next tick is a quarter of an hour
            // away and it will ask the same question. Retrying a poll that failed
            // because the network was down only spends battery.
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        work?.cancel()
        return false
    }

    companion object {
        private const val JOB_ID = 7301
        private const val PERIOD_MS = 15 * 60 * 1000L

        /**
         * Starts or stops the watch to match what the player asked for.
         *
         * Idempotent, and deliberately not `setUpdateCurrent`-shy: scheduling the
         * same job again simply replaces it, which is what we want when a setting
         * changes. The job is dropped entirely when nothing is being watched, so
         * an app nobody has friends in costs nothing at all.
         */
        fun sync(context: Context, wantFriends: Boolean, wantUpdates: Boolean) {
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
            val hasFriends = FriendStore.get(context).friends.value.isNotEmpty()
            val wanted = (wantFriends && hasFriends) || wantUpdates

            if (!wanted || !Notifications.allowed(context)) {
                scheduler.cancel(JOB_ID)
                return
            }

            scheduler.schedule(
                JobInfo.Builder(JOB_ID, ComponentName(context, FriendWatchJob::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(PERIOD_MS)
                    // Survives a reboot: a watch that stops the first time the
                    // handheld is turned off and on is not a watch.
                    .setPersisted(true)
                    .build()
            )
        }

        /**
         * One pass: ask, compare, announce.
         *
         * Lives here rather than in the service so the app can run the very same
         * pass while it is open, against the very same memory. Two implementations
         * of "what is new" would drift, and the player would get told twice.
         */
        suspend fun sweep(context: Context) {
            val settings = SettingsStore.get(context)
            val state = WatchState(context)

            if (settings.notifyUpdates.value) {
                val latest = UpdateCheck.fetch()
                if (latest != null &&
                    UpdateCheck.isNewer(latest) &&
                    latest.versionCode > state.notifiedVersion()
                ) {
                    Notifications.update(context, latest.versionName)
                    state.setNotifiedVersion(latest.versionCode)
                }
            }

            if (!settings.notifyFriends.value) return

            val store = FriendStore.get(context)
            val friends = store.friends.value
            if (friends.isEmpty()) return

            val codes = friends.map { it.code }
            val fresh = CoordinatorClient().friendStatuses(codes).getOrNull() ?: return

            val current = codes.associateWith { code ->
                fresh[code]?.let {
                    FriendStatus(
                        online = true,
                        sessionCode = it.sessionCode,
                        romTitle = it.romTitle,
                        romTitleId = it.romTitleId,
                        players = it.players,
                        ready = it.ready
                    )
                } ?: FriendStatus.Offline
            }

            val names = friends.associate { it.code to (fresh[it.code]?.name ?: it.name) }
            friendEvents(state.seen(), current, names).forEach { Notifications.friendEvent(context, it) }

            state.setSeen(seenFrom(current))
            store.noteNames(fresh.mapNotNull { (c, p) -> p.name?.let { c to it } }.toMap())
        }
    }
}
