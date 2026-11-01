package com.ledgerly.app

import com.ledgerly.app.util.UpdateCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckTest {

    private fun releaseJson(
        tag: String = "v1.3.0",
        body: String = "Notes",
        draft: Boolean = false,
        prerelease: Boolean = false,
        assets: String = """
            [{"name":"ledgerly-1.3.0.apk",
              "browser_download_url":"https://github.com/darrenphilip/ledgerly/releases/download/v1.3.0/ledgerly-1.3.0.apk",
              "size":10955293}]
        """.trimIndent()
    ) = """
        {
          "tag_name": "$tag",
          "html_url": "https://github.com/darrenphilip/ledgerly/releases/tag/$tag",
          "body": "$body",
          "draft": $draft,
          "prerelease": $prerelease,
          "assets": $assets
        }
    """.trimIndent()

    @Test
    fun `parses the APK asset out of a release`() {
        val info = UpdateCheck.parse(releaseJson())!!
        assertEquals("1.3.0", info.version)
        assertEquals("ledgerly-1.3.0.apk", info.apkName)
        assertEquals(10_955_293L, info.apkSize)
        assertEquals("https://github.com/darrenphilip/ledgerly/releases/tag/v1.3.0", info.releaseUrl)
        assertEquals("Notes", info.notes)
    }

    @Test
    fun `ignores drafts and pre-releases`() {
        assertNull(UpdateCheck.parse(releaseJson(draft = true)))
        assertNull(UpdateCheck.parse(releaseJson(prerelease = true)))
    }

    @Test
    fun `returns null when no APK is attached`() {
        assertNull(UpdateCheck.parse(releaseJson(assets = "[]")))
    }

    @Test
    fun `returns null for malformed or unexpected json`() {
        assertNull(UpdateCheck.parse("not json"))
        assertNull(UpdateCheck.parse("""{"message":"Not Found"}"""))
        assertNull(UpdateCheck.parse(""))
    }

    @Test
    fun `a higher patch version is newer`() {
        assertTrue(UpdateCheck.isNewer("1.3.0", "1.2.0"))
        assertFalse(UpdateCheck.isNewer("1.2.0", "1.2.0"))
        assertFalse(UpdateCheck.isNewer("1.2.0", "1.3.0"))
    }

    @Test
    fun `minor and major bumps are newer`() {
        assertTrue(UpdateCheck.isNewer("1.10.0", "1.9.9"))
        assertTrue(UpdateCheck.isNewer("2.0.0", "1.99.99"))
        assertFalse(UpdateCheck.isNewer("1.9.9", "1.10.0"))
    }

    @Test
    fun `missing parts count as zero`() {
        assertTrue(UpdateCheck.isNewer("1.3", "1.2.9"))
        assertFalse(UpdateCheck.isNewer("1.2", "1.2.0"))
        assertTrue(UpdateCheck.isNewer("1.2.1", "1.2"))
    }

    @Test
    fun `debug suffixes are ignored`() {
        assertFalse(UpdateCheck.isNewer("1.3.0", "1.3.0-debug"))
        assertTrue(UpdateCheck.isNewer("1.3.0", "1.2.0-debug"))
    }

    @Test
    fun `tags with a leading v are normalised`() {
        assertEquals("1.3.0", UpdateCheck.versionOf("v1.3.0"))
        assertTrue(UpdateCheck.isNewer("v1.3.0", "1.2.0"))
    }

    @Test
    fun `garbage versions never claim to be newer`() {
        assertFalse(UpdateCheck.isNewer("", "1.2.0"))
        assertFalse(UpdateCheck.isNewer("latest", "1.2.0"))
    }

    @Test
    fun `size labels are human readable`() {
        assertEquals("10.4 MB", UpdateCheck.sizeLabel(10_955_293L))
        assertEquals("512 KB", UpdateCheck.sizeLabel(524_288L))
        assertEquals("", UpdateCheck.sizeLabel(0L))
    }
}