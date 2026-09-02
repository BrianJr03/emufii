package eu.emufii.app.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.SigningInfo
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import eu.emufii.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "UpdateInstaller"

/**
 * The largest APK we agree to pull: room above the current 32 MB, and a stop on a chatty
 * server filling the cache while we look away.
 * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Three outcomes, not two
 */
private const val MAX_APK_BYTES = 200L * 1024 * 1024

sealed interface UpdateOutcome {
    data object HandedToAndroid : UpdateOutcome

    /**
     * Not an error: a permission to grant once, and [settings] opens the exact screen.
     * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Two refusals that are not errors
     */
    data class NeedsPermission(val settings: Intent) : UpdateOutcome

    data object Unavailable : UpdateOutcome

    data object DownloadFailed : UpdateOutcome

    /** The downloaded file is not an Emufii update: what the check exists to catch. */
    data object Rejected : UpdateOutcome
}

/**
 * Reopens a path `docs/SECURITY_REVIEW.md` (S5) had ruled out, closed with three locks:
 * the URL is not followed as given (coordinator host over HTTPS only), the signature
 * decides rather than the provenance, and nothing starts without a tap. Lock 2 carries
 * the security.
 * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Why this is acceptable when the S5 review excluded it
 */
object UpdateInstaller {

    /**
     * Null if no link is acceptable; with no `url` published, the coordinator's `/download`.
     * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Two refusals that are not errors
     */
    fun downloadUrl(
        published: String?,
        baseUrl: String = BuildConfig.COORDINATOR_BASE_URL
    ): String? {
        val fallback = "$baseUrl/download"
        val candidate = published?.takeIf { it.isNotBlank() } ?: return fallback
        val base = runCatching { URL(baseUrl) }.getOrNull() ?: return null
        val target = runCatching { URL(candidate) }.getOrNull() ?: return fallback
        // A link elsewhere is not followed, and not treated as an attack either:
        // "View" opens it in the browser.
        // pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Two refusals that are not errors
        val sameOrigin = target.protocol == "https" && target.host.equals(base.host, ignoreCase = true)
        return if (sameOrigin) candidate else fallback
    }

    suspend fun downloadAndInstall(
        context: Context,
        latest: LatestVersion
    ): UpdateOutcome = withContext(Dispatchers.IO) {
        val app = context.applicationContext

        if (!app.packageManager.canRequestPackageInstalls()) {
            return@withContext UpdateOutcome.NeedsPermission(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    "package:${app.packageName}".toUri()
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        val url = downloadUrl(latest.url) ?: return@withContext UpdateOutcome.Unavailable
        // In the cache: an APK forgotten in the player's documents would be this
        // feature's only lasting trace.
        // pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Three outcomes, not two
        val target = File(app.cacheDir, "update.apk")
        when (download(url, target)) {
            Fetched.Ok -> Unit
            Fetched.Missing -> {
                target.delete()
                return@withContext UpdateOutcome.Unavailable
            }
            Fetched.Broken -> {
                target.delete()
                return@withContext UpdateOutcome.DownloadFailed
            }
        }

        if (!isGenuine(app, target, latest.versionCode)) {
            target.delete()
            return@withContext UpdateOutcome.Rejected
        }

        val handed = runCatching { hand(app, target) }
            .onFailure { Log.w(TAG, "handing over to Android failed", it) }
            .getOrDefault(false)
        if (!handed) {
            target.delete()
            return@withContext UpdateOutcome.DownloadFailed
        }
        UpdateOutcome.HandedToAndroid
    }

    /**
     * Three outcomes, not two: a boolean made a stalled transfer report "not
     * downloadable here yet".
     * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Three outcomes, not two
     */
    private enum class Fetched { Ok, Missing, Broken }

    private fun download(url: String, target: File): Fetched = runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            // 60 s: at 30 s a briefly stalled transfer was abandoned, measured on the Thor.
            // pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Three outcomes, not two
            readTimeout = 60_000
            // A redirect can leave the host checked above: following it undoes lock 1.
            instanceFollowRedirects = false
        }
        try {
            // 404 is the only answer meaning "there is nothing to take here";
            // everything else is a transport incident.
            if (conn.responseCode == 404) return Fetched.Missing
            if (conn.responseCode != 200) return Fetched.Broken
            val announced = conn.contentLengthLong
            if (announced > MAX_APK_BYTES) return Fetched.Broken
            var written = 0L
            conn.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        written += read
                        // The ceiling holds without `Content-Length`: a server is not
                        // obliged to announce one.
                        if (written > MAX_APK_BYTES) return Fetched.Broken
                        output.write(buffer, 0, read)
                    }
                }
            }
            // A cut download gives a truncated ZIP the check would reject anyway; report
            // it as the network problem it is.
            if (announced <= 0 || written == announced) Fetched.Ok else Fetched.Broken
        } finally {
            conn.disconnect()
        }
    }.onFailure { Log.w(TAG, "download failed", it) }.getOrDefault(Fetched.Broken)

    /**
     * The central lock: certificate and version, the second closing the rollback.
     * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § The central lock: two questions, and both must hold
     */
    private fun isGenuine(context: Context, apk: File, announcedVersion: Int): Boolean {
        val pm = context.packageManager
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val info = runCatching { pm.getPackageArchiveInfo(apk.path, flags) }.getOrNull()
            ?: return false.also { Log.w(TAG, "APK illisible") }

        if (info.packageName != context.packageName) {
            Log.w(TAG, "APK d'un autre paquet : ${info.packageName}")
            return false
        }
        val downloadedVersion = info.longVersionCode.toInt()
        if (downloadedVersion < announcedVersion || downloadedVersion <= BuildConfig.VERSION_CODE) {
            Log.w(TAG, "APK is version $downloadedVersion, refused")
            return false
        }

        val mine = runCatching {
            pm.getPackageInfo(context.packageName, flags).signingInfo
        }.getOrNull() ?: return false
        return sameCertificates(mine, info.signingInfo)
    }

    /**
     * Certificates rather than key pairs: reading the wrong array returns an empty list,
     * which would compare "equal" to another empty one.
     * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § The central lock: two questions, and both must hold
     */
    private fun sameCertificates(mine: SigningInfo?, theirs: SigningInfo?): Boolean {
        if (mine == null || theirs == null) return false
        fun certs(info: SigningInfo): Set<String> {
            val list =
                if (info.hasMultipleSigners()) info.apkContentsSigners
                else info.signingCertificateHistory
            return (list ?: emptyArray()).map { it.toCharsString() }.toSet()
        }
        val ours = certs(mine)
        val other = certs(theirs)
        if (ours.isEmpty() || other.isEmpty()) return false
        // Intersection, never equality: a key rotation would fail the one update that
        // must succeed that day.
        // pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § The central lock: two questions, and both must hold
        return ours.any { it in other }
    }

    /**
     * [PackageInstaller] rather than `ACTION_INSTALL_PACKAGE`, deprecated since Oreo.
     * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Two refusals that are not errors
     */
    private fun hand(context: Context, apk: File): Boolean {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        )
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("emufii", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val intent = Intent(context, UpdateInstallReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            session.commit(pending.intentSender)
        }
        return true
    }
}

/**
 * Only `STATUS_PENDING_USER_ACTION` demands anything; without this relay the button
 * passes for dead where it applies.
 * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § Why this is acceptable when the S5 review excluded it
 */
class UpdateInstallReceiver : android.content.BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, Int.MIN_VALUE)
        if (status != PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            Log.i(TAG, "installation : statut $status ${message.orEmpty()}")
            return
        }
        // minSdk 33: the typed overload is present, the deprecated one is not carried.
        val confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { confirm?.let(context::startActivity) }
            .onFailure { Log.w(TAG, "dialogue de confirmation non ouvert", it) }
    }
}
