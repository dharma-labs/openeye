package io.github.dharma_labs.openeye.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Detection is catalog-driven now, so the catalog itself is the thing that can be wrong. These
 * checks are what stop a new app being added as dead configuration.
 */
class PlatformCatalogTest {

    @Test
    fun everyPackageIdMapsToExactlyOnePlatform() {
        val mapped = PlatformCatalog.all.flatMap { it.packageIds }
        assertEquals("duplicate package id", mapped.size, mapped.toSet().size)
        assertEquals(mapped.toSet(), PlatformCatalog.packages.keys)
        mapped.forEach { pkg ->
            val platform = requireNotNull(PlatformCatalog.byId(PlatformCatalog.packages.getValue(pkg)))
            assertTrue("$pkg not owned by ${platform.id}", pkg in platform.packageIds)
        }
    }

    @Test
    fun everySurfaceCanProveItIsOpen() {
        PlatformCatalog.all.forEach { platform ->
            assertTrue("${platform.id} has no surfaces", platform.surfaces.isNotEmpty())
            platform.surfaces.forEach { surface ->
                val detectable = surface.classHints.isNotEmpty() ||
                    surface.titleTokens.isNotEmpty() ||
                    surface.idHints.isNotEmpty() ||
                    surface.navTokens.isNotEmpty()
                assertTrue("${platform.id}/${surface.id} can never be detected", detectable)
                // A hint of "" would match every node in the tree.
                (surface.classHints + surface.titleTokens + surface.idHints +
                    surface.navIdHints + surface.navTokens).forEach { hint ->
                    assertTrue("${platform.id}/${surface.id} has an empty hint", hint.isNotBlank())
                    assertEquals(hint, hint.lowercase())
                }
            }
        }
    }

    @Test
    fun anythingAdvertisedAsHideableHasAHint() {
        PlatformCatalog.all.forEach { platform ->
            platform.surfaces.filter { it.hideNav }.forEach { surface ->
                val hideable = surface.navTokens.isNotEmpty() || surface.navIdHints.isNotEmpty()
                assertTrue("${platform.id}/${surface.id} cannot be hidden", hideable)
            }
        }
    }

    @Test
    fun theTikTokHomeTabIsNeverCovered() {
        val foryou = requireNotNull(PlatformCatalog.surface(PlatformCatalog.TIKTOK, "foryou"))
        assertTrue(!foryou.hideNav)
    }

    @Test
    fun theFourMainShortVideoAppsArePresent() {
        assertEquals(
            setOf("instagram", "youtube", "facebook", "tiktok", "snapchat"),
            PlatformCatalog.all.map { it.id }.toSet(),
        )
        assertTrue(PlatformCatalog.packages.containsKey("com.instagram.android"))
        assertTrue(PlatformCatalog.packages.containsKey("com.instagram.lite"))
        assertTrue(PlatformCatalog.packages.containsKey("com.facebook.lite"))
        assertTrue(PlatformCatalog.packages.containsKey("com.ss.android.ugc.aweme"))
        assertEquals("youtube", PlatformCatalog.packages["com.google.android.youtube"])
    }

    @Test
    fun defaultsShieldInstagramYouTubeAndFacebook() {
        val defaults = defaultPlatforms()
        assertEquals(PlatformCatalog.all.map { it.id }.toSet(), defaults.keys)
        assertTrue(checkNotNull(defaults[PlatformCatalog.INSTAGRAM]).enabled)
        assertTrue(checkNotNull(defaults[PlatformCatalog.YOUTUBE]).enabled)
        assertTrue(checkNotNull(defaults[PlatformCatalog.FACEBOOK]).enabled)
        assertFalse(defaults[PlatformCatalog.TIKTOK]?.enabled ?: true)
        assertFalse(defaults[PlatformCatalog.SNAPCHAT]?.enabled ?: true)
        assertEquals(BlockMode.INTENTIONAL, defaults[PlatformCatalog.INSTAGRAM]?.surfaces?.get("reels")?.mode)
    }

    @Test
    fun anOlderInstallGainsPlatformsAddedSince() {
        val stored = Settings(platforms = mapOf("instagram" to PlatformConfig(enabled = true)))
        val merged = stored.withCatalogDefaults()
        assertEquals(PlatformCatalog.all.map { it.id }.toSet(), merged.platforms.keys)
    }

    @Test
    fun storedChoicesBeatNewDefaults() {
        val strict = PlatformConfig(enabled = true, surfaces = mapOf("reels" to SurfaceConfig(BlockMode.STRICT)))
        val stored = Settings(platforms = mapOf("instagram" to strict))
        val merged = stored.withCatalogDefaults()
        assertEquals(strict, merged.platforms["instagram"])
    }

    @Test
    fun unknownPlatformsAreDropped() {
        val stored = Settings(platforms = mapOf("vimeo" to PlatformConfig(enabled = true)))
        assertNull(stored.withCatalogDefaults().platforms["vimeo"])
    }

    @Test
    fun reminderCadenceDefaultsToFiveMinutesAndAcceptsOff() {
        assertEquals(5, Settings().nudgeMinutes)
        assertTrue(!Settings().countLongVideos)
    }
}
