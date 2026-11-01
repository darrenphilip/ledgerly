package com.ledgerly.app.data

import com.ledgerly.app.util.ReleaseInfo
import com.ledgerly.app.util.UpdateCheck
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The only part of Ledgerly that touches the network: fetching the latest release and
 * downloading its APK. Every call is wrapped so a failure is a no-op, never an error state.
 */
class UpdateClient {

    fun fetchLatest(): ReleaseInfo? = runCatching {
        val body = withConnection(UpdateCheck.API_URL) { connection ->
            connection.inputStream.bufferedReader().use { it.readText() }
        }
        UpdateCheck.parse(body)
    }.getOrNull()

    /** Downloads [info] into [directory]; returns null when the download fails. */
    fun download(info: ReleaseInfo, directory: File): File? = runCatching {
        directory.mkdirs()
        val target = File(directory, "ledgerly-${info.version}.apk")
        val partial = File(directory, "ledgerly-${info.version}.apk.part")
        withConnection(info.apkUrl) { connection ->
            connection.inputStream.use { input ->
                partial.outputStream().use { output -> input.copyTo(output) }
            }
        }
        if (!partial.renameTo(target)) {
            partial.delete()
            error("Could not finish the download")
        }
        target
    }.getOrNull()

    private fun <T> withConnection(url: String, block: (HttpURLConnection) -> T): T {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Ledgerly-Android")
        }
        return try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            block(connection)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
    }
}