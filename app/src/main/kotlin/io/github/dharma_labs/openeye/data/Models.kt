package io.github.dharma_labs.openeye.data

import kotlinx.serialization.Serializable

@Serializable
enum class BlockMode { STRICT, INTENTIONAL, OFF }

enum class BlockReason { NONE, FOCUS, SCHEDULE, SHIELD }

@Serializable
data class SurfaceConfig(
    val mode: BlockMode = BlockMode.INTENTIONAL,
)

@Serializable
data class PlatformConfig(
    val enabled: Boolean = false,
    val surfaces: Map<String, SurfaceConfig> = emptyMap(),
    val dailyLimitMinutes: Int = 0,
)

@Serializable
data class ScheduleConfig(
    val enabled: Boolean = false,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val startMinute: Int = 9 * 60,
    val endMinute: Int = 21 * 60,
    val mode: BlockMode = BlockMode.STRICT,
)

@Serializable
data class FocusConfig(
    val active: Boolean = false,
    val startEpochMs: Long = 0L,
    val endEpochMs: Long = 0L,
    val durationMinutes: Int = 25,
)

/**
 * What the person committed to, written while calm. Quoted back verbatim on the block screen, so
 * the objection comes from their own past self and not from the app.
 */
@Serializable
data class AttentionContract(
    val stopRuleMinutes: Int = 0,
    val signedEpochMs: Long = 0L,
    val why: String = "",
    /** From onboarding: how scrolling usually leaves them feeling. Their words, not a score. */
    val usualFeeling: String = "",
)

/** One if-then plan. Specific trigger plus specific action is what makes these work. */
@Serializable
data class ImplementationPlan(
    val id: String,
    val trigger: String,
    val action: String,
    val createdAtEpochMs: Long,
    val enabled: Boolean = true,
)

/**
 * A single affect label attached to a feed period. Naming the feeling is the intervention; the
 * aggregate is what exposes wanting without liking.
 */
@Serializable
data class FeelingCheck(
    val epochMs: Long,
    val epochDay: Long,
    val feeling: String,
    val minutes: Long = 0L,
    val source: String = "today",
)

@Serializable
data class Settings(
    val masterEnabled: Boolean = true,
    val strictMode: Boolean = false,
    val onboardingComplete: Boolean = false,
    val themeMode: String = "system",
    val trackingEnabled: Boolean = true,
    val platforms: Map<String, PlatformConfig> = defaultPlatforms(),
    val schedule: ScheduleConfig = ScheduleConfig(),
    val focus: FocusConfig = FocusConfig(),
    val contract: AttentionContract = AttentionContract(),
    val plans: List<ImplementationPlan> = emptyList(),
    val feelings: List<FeelingCheck> = emptyList(),
    /** Advances once per interrupt so rotating copy cannot repeat itself. */
    val promptCursor: Int = 0,
    /** A lapse is addressed once a day, then left alone. */
    val lapseHandledDay: Long = 0L,
    val planAskedInOnboarding: Boolean = false,
    /** Minutes of feed time between in-feed reminder popups. 0 turns them off. */
    val nudgeMinutes: Int = 5,
    /** Add whole-app screen time to the counter. Measured, never estimated. */
    val countLongVideos: Boolean = false,
    /** The corner timer that says how long the phone has been on today. Off inside feeds. */
    val ambientTimer: Boolean = true,
)

@Serializable
data class BlockEvent(
    val id: String,
    val epochMs: Long,
    val platformId: String,
    val surfaceId: String,
    val mode: String,
    val outcome: String,
    val intention: String? = null,
)

/** Aggregated watch time and reel/short count for one day and surface. */
@Serializable
data class WatchRecord(
    val epochDay: Long,
    val platformId: String,
    val surfaceId: String,
    val count: Int = 0,
    val millis: Long = 0,
    val sessions: Int = 0,
    val longestSessionMs: Long = 0,
    val touches: Int = 0,
    val hour: Int = -1,
)

fun defaultPlatforms(): Map<String, PlatformConfig> = PlatformCatalog.all.associate { platform ->
    platform.id to PlatformConfig(
        // The three the app is useful out of the box for. TikTok and Snapchat are opt-in because
        // their detection is the least certain and covering a wrong button is worse than missing one.
        enabled = platform.id in setOf(PlatformCatalog.INSTAGRAM, PlatformCatalog.YOUTUBE, PlatformCatalog.FACEBOOK),
        surfaces = platform.surfaces.associate { surface -> surface.id to SurfaceConfig() },
    )
}

/**
 * Everything the detection engine needs to know about one entry point, so supporting a new app is a
 * catalog edit and never a change to the accessibility service.
 */
data class Surface(
    val id: String,
    val label: String,
    val blurb: String,
    /** Bottom-navigation captions. Used to hide the button and to detect a selected tab. */
    val navTokens: List<String> = emptyList(),
    /** Cover the bottom-bar entry point. False where the feed is the app's home screen. */
    val hideNav: Boolean = true,
    /** Fragments of the activity class name that mean this surface is open. */
    val classHints: List<String> = emptyList(),
    /** Fragments of `viewIdResourceName` proving the surface fills the screen. Keep these strict. */
    val idHints: List<String> = emptyList(),
    /** Looser id fragments for the bottom-bar item; geometry keeps them out of the feed itself. */
    val navIdHints: List<String> = emptyList(),
    /** Header captions shown while the surface fills the screen. */
    val titleTokens: List<String> = emptyList(),
    /** A swipeable clip pager: an audio label and a missing bottom bar can prove it is open. */
    val immersive: Boolean = false,
    /** Also cover the horizontal shelf this app renders on its home feed. */
    val shelf: Boolean = false,
)

data class Platform(
    val id: String,
    val name: String,
    val packageName: String,
    /** Every install variant to watch; lite builds are separate packages with the same surfaces. */
    val packageIds: List<String> = listOf(packageName),
    val monogram: String,
    val tint: Long,
    val surfaces: List<Surface>,
)

/**
 * Stored settings from an older install can be missing platforms added since. Without a config
 * entry a toggle has nothing to write to, so the new app would look like it ignores the switch.
 */
fun Settings.withCatalogDefaults(): Settings {
    val merged = defaultPlatforms() + platforms
    val known = PlatformCatalog.all.map { it.id }.toSet()
    return copy(platforms = merged.filterKeys { it in known })
}

object PlatformCatalog {
    const val INSTAGRAM = "instagram"
    const val YOUTUBE = "youtube"
    const val FACEBOOK = "facebook"
    const val TIKTOK = "tiktok"
    const val SNAPCHAT = "snapchat"

    val all: List<Platform> = listOf(
        Platform(
            id = INSTAGRAM,
            name = "Instagram",
            packageName = "com.instagram.android",
            packageIds = listOf("com.instagram.android", "com.instagram.lite", "com.instagram.barcelo"),
            monogram = "Ig",
            tint = 0xFFE1306C,
            surfaces = listOf(
                Surface(
                    id = "reels",
                    label = "Reels",
                    blurb = "no bottom. on purpose.",
                    navTokens = listOf("reels"),
                    classHints = listOf("clips", "reels"),
                    idHints = listOf("clips", "reel_viewer", "reels_viewer"),
                    navIdHints = listOf("reel", "clips"),
                    titleTokens = listOf("reels"),
                    immersive = true,
                ),
                Surface(
                    id = "explore",
                    label = "Explore",
                    blurb = "the grid decides.",
                    navTokens = listOf("explore"),
                    classHints = listOf("explore"),
                    idHints = listOf("explore"),
                    navIdHints = listOf("explore"),
                ),
            ),
        ),
        Platform(
            id = YOUTUBE,
            name = "YouTube",
            packageName = "com.google.android.youtube",
            monogram = "Yt",
            tint = 0xFFFF4D4D,
            surfaces = listOf(
                Surface(
                    id = "shorts",
                    label = "Shorts",
                    blurb = "swipe up, evening gone.",
                    navTokens = listOf("shorts"),
                    classHints = listOf("shorts"),
                    idHints = listOf("shorts", "reel_recycler", "reel_shelf"),
                    navIdHints = listOf("short", "reel"),
                    titleTokens = listOf("shorts"),
                    immersive = true,
                    shelf = true,
                ),
            ),
        ),
        Platform(
            id = FACEBOOK,
            name = "Facebook",
            packageName = "com.facebook.katana",
            packageIds = listOf("com.facebook.katana", "com.facebook.lite"),
            monogram = "Fb",
            tint = 0xFF4C8BF5,
            surfaces = listOf(
                Surface(
                    id = "reels",
                    label = "Reels",
                    blurb = "same loop, new app.",
                    navTokens = listOf("reels"),
                    classHints = listOf("reels", "clips"),
                    idHints = listOf("reels_viewer", "reel_viewer", "clips_view"),
                    navIdHints = listOf("reel"),
                    titleTokens = listOf("reels"),
                    immersive = true,
                ),
            ),
        ),
        Platform(
            id = TIKTOK,
            name = "TikTok",
            packageName = "com.zhiliaoapp.musically",
            packageIds = listOf(
                "com.zhiliaoapp.musically",
                "com.zhiliaoapp.musically.go",
                "com.ss.android.ugc.aweme",
            ),
            monogram = "Tt",
            tint = 0xFF25F4EE,
            surfaces = listOf(
                Surface(
                    id = "foryou",
                    label = "For You",
                    blurb = "the whole app is the feed.",
                    navTokens = listOf("for you", "foryou"),
                    // For You is TikTok's home tab: hiding it leaves no way out of the app.
                    hideNav = false,
                    classHints = listOf("feed", "videodetail"),
                    idHints = listOf("hollywood", "aweme"),
                    titleTokens = listOf("for you"),
                    immersive = true,
                ),
            ),
        ),
        Platform(
            id = SNAPCHAT,
            name = "Snapchat",
            packageName = "com.snapchat.android",
            monogram = "Sc",
            tint = 0xFFFFE14D,
            surfaces = listOf(
                Surface(
                    id = "spotlight",
                    label = "Spotlight",
                    blurb = "vertical video tab.",
                    navTokens = listOf("spotlight"),
                    classHints = listOf("spotlight"),
                    idHints = listOf("spotlight"),
                    navIdHints = listOf("spotlight"),
                    titleTokens = listOf("spotlight"),
                    immersive = true,
                ),
            ),
        ),
    )

    fun byId(id: String): Platform? = all.firstOrNull { it.id == id }

    fun surface(platformId: String, surfaceId: String): Surface? =
        byId(platformId)?.surfaces?.firstOrNull { it.id == surfaceId }

    /** Every package the service should listen for, mapped to its platform id. */
    val packages: Map<String, String> = buildMap {
        all.forEach { platform -> platform.packageIds.forEach { put(it, platform.id) } }
    }

    /** Surfaces that can be shielded, in catalog order, for the shields screen. */
    fun blockableSurfaces(platformId: String): List<Surface> = byId(platformId)?.surfaces.orEmpty()
}