package eu.emufii.app.update

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The first of three locks, and the only one testable without a device and a real APK:
 * whether a compromised `latest.json` can send the app to fetch a binary elsewhere.
 */
class UpdateInstallerTest {

    private val base = "https://coordinator.example"

    @Test
    fun `with no published url, we pull from the coordinator`() {
        assertEquals("$base/download", UpdateInstaller.downloadUrl(null, base))
        assertEquals("$base/download", UpdateInstaller.downloadUrl("", base))
    }

    @Test
    fun `a url on the same host is followed`() {
        val published = "$base/releases/emufii-1.9.3.apk"

        assertEquals(published, UpdateInstaller.downloadUrl(published, base))
    }

    @Test
    fun `a url from elsewhere is not followed`() {
        assertEquals(
            "$base/download",
            UpdateInstaller.downloadUrl("https://ailleurs.example/emufii.apk", base)
        )
    }

    @Test
    fun `the same host in cleartext is not followed`() {
        assertEquals(
            "$base/download",
            UpdateInstaller.downloadUrl("http://coordinator.example/emufii.apk", base)
        )
    }

    @Test
    fun `an unreadable url falls back on the coordinator`() {
        assertEquals("$base/download", UpdateInstaller.downloadUrl("not a url", base))
    }
}
