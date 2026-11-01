package com.ledgerly.app.util

/**
 * Parsing and comparison for the GitHub release feed. Pure, so the rules around "is this
 * actually newer" are unit tested rather than eyeballed in the UI.
 */
data class ReleaseInfo(
    /** Normalised version, e.g. `1.3.0` (the `v` is stripped). */
    val version: String,
    val apkUrl: String,
    val releaseUrl: String,
    val notes: String,
    val apkSize: Long
) {
    val apkName: String get() = apkUrl.substringAfterLast('/')
}

object UpdateCheck {

    /** Result of the last check, for the Settings row. */
    data class Status(
        val checking: Boolean = false,
        val lastChecked: Long = 0L,
        val failed: Boolean = false
    )

    const val API_URL = "https://api.github.com/repos/darrenphilip/ledgerly/releases/latest"

    /** Returns null for drafts, pre-releases, and anything without an APK attached. */
    fun parse(json: String): ReleaseInfo? {
        val root = runCatching { Json.parse(json) }.getOrNull() as? Map<*, *> ?: return null
        if (root["draft"] as? Boolean == true) return null
        if (root["prerelease"] as? Boolean == true) return null

        val tag = root["tag_name"] as? String ?: return null
        val assets = root["assets"] as? List<*> ?: return null
        val apk = assets.asSequence()
            .mapNotNull { it as? Map<*, *> }
            .firstOrNull { (it["name"] as? String)?.endsWith(".apk", ignoreCase = true) == true }
            ?: return null

        val url = apk["browser_download_url"] as? String ?: return null
        return ReleaseInfo(
            version = versionOf(tag),
            apkUrl = url,
            releaseUrl = root["html_url"] as? String ?: "",
            notes = root["body"] as? String ?: "",
            apkSize = (apk["size"] as? Number)?.toLong() ?: 0L
        )
    }

    fun versionOf(tag: String): String = tag.trim().removePrefix("v").removePrefix("V")

    /**
     * True when [remote] is a higher version than [local]. Build suffixes like
     * `1.3.0-debug` are ignored, so a debug build of the same version is up to date.
     */
    fun isNewer(remote: String, local: String): Boolean {
        val a = parts(remote)
        val b = parts(local)
        if (a.isEmpty() || b.isEmpty()) return false
        for (index in 0 until maxOf(a.size, b.size)) {
            val remotePart = a.getOrElse(index) { 0 }
            val localPart = b.getOrElse(index) { 0 }
            if (remotePart != localPart) return remotePart > localPart
        }
        return false
    }

    private fun parts(version: String): List<Int> = version.trim()
        .removePrefix("v")
        .removePrefix("V")
        .split('.', '-', '+', ' ')
        .mapNotNull { chunk -> chunk.takeWhile { it.isDigit() }.toIntOrNull() }

    fun sizeLabel(bytes: Long): String = when {
        bytes <= 0L -> ""
        bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%d KB".format(bytes / 1024L)
    }
}