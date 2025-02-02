package dev.kdrag0n.android12ext.core.xposed

import android.content.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import dev.kdrag0n.android12ext.BuildConfig
import dev.kdrag0n.android12ext.CustomApplication
import dev.kdrag0n.android12ext.core.BroadcastManager
import dev.kdrag0n.android12ext.core.xposed.hooks.FrameworkHooks
import dev.kdrag0n.android12ext.core.xposed.hooks.LauncherHooks
import dev.kdrag0n.android12ext.core.xposed.hooks.SystemUIHooks
import timber.log.Timber

private val FEATURE_FLAGS = mapOf(
    // DP2
    "isKeyguardLayoutEnabled" to "lockscreen",
    "isMonetEnabled" to "monet",
    //"isNewNotifPipelineEnabled" to "notification_shade", // crashes on DP2, does nothing on DP3
    //"isNewNotifPipelineRenderingEnabled" to "notification_shade", // breaks notifications
    "isShadeOpaque" to "notification_shade",
    "isToastStyleEnabled" to "toast",
    "useNewBrightnessSlider" to "notification_shade",
    "useNewLockscreenAnimations" to "lockscreen",

    // DP3
    "isQSLabelsEnabled" to "quick_settings", // crashes on DP2
    "isAlarmTileAvailable" to "global", // optional QS tile, no reason to keep disabled
    "isChargingRippleEnabled" to "charging_ripple", // only affects keyguard, so assign to lock screen
    "isNavigationBarOverlayEnabled" to "global", // for game dashboard, does nothing otherwise
    "isPMLiteEnabled" to "quick_settings", // doesn't work
    "isQuickAccessWalletEnabled" to "global", // optional QS tile, no reason to keep disabled
    //"isTwoColumnNotificationShadeEnabled" to "notification_shade", // landscape tablets only
)

class XposedHook(
    private val context: Context,
    private val lpparam: XC_LoadPackage.LoadPackageParam,
    private val prefs: SharedPreferences,
    private val broadcastManager: BroadcastManager,
) {
    private val sysuiHooks = SystemUIHooks(lpparam)
    private val frameworkHooks = FrameworkHooks(lpparam)
    private val launcherHooks = LauncherHooks(lpparam)

    init {
        CustomApplication.commonInit()
    }

    private fun isFeatureEnabled(feature: String, default: Boolean = true): Boolean {
        return prefs.getBoolean("${feature}_enabled", default)
    }

    private fun applySysUi() {
        broadcastManager.listenForPings()

        // Enable feature flags
        FEATURE_FLAGS.forEach { (flag, prefKey) ->
            if (isFeatureEnabled(prefKey)) {
                sysuiHooks.applyFeatureFlag(flag)
            }
        }

        // Enable privacy indicators
        if (isFeatureEnabled("privacy_indicators")) {
            sysuiHooks.applyPrivacyIndicators()
        }

        // Enable game dashboard
        if (isFeatureEnabled("game_dashboard")) {
            sysuiHooks.applyGameDashboard()
        }

        // Custom Monet engine
        if (isFeatureEnabled("custom_monet", false)) {
            sysuiHooks.applyThemeOverlayController()
        }

        // Disable Monet, if necessary
        if (!isFeatureEnabled("monet")) {
            disableMonetOverlays()
        }

        // Hide red background in rounded screenshots
        sysuiHooks.applyRoundedScreenshotBg()
    }

    private fun disableMonetOverlays() {
        try {
            context.disableOverlay(lpparam, "com.android.systemui:accent")
            context.disableOverlay(lpparam, "com.android.systemui:neutral")
        } catch (e: Exception) {
            Timber.e(e, "Failed to disable Monet overlays")
        }
    }

    private fun applyLauncher() {
        launcherHooks.applyFeatureFlags()
    }

    private fun applySystemServer() {
        frameworkHooks.applyMedianCutQuantizer()
    }

    fun applyAll() {
        // Global kill-switch
        if (!isFeatureEnabled("global")) {
            // Always register broadcast receiver in System UI
            if (lpparam.packageName == "com.android.systemui") {
                broadcastManager.listenForPings()
            }

            return
        }

        when (lpparam.packageName) {
            // Never hook our own app in case something goes wrong
            BuildConfig.APPLICATION_ID -> return
            // System UI
            "com.android.systemui" -> applySysUi()
            // Pixel Launcher
            "com.google.android.apps.nexuslauncher" -> applyLauncher()
            // System server
            "android" -> applySystemServer()
        }

        // All apps
        if (isFeatureEnabled("patterned_ripple")) {
            frameworkHooks.applyRipple()
        }

        if (isFeatureEnabled("haptic_touch", false)) {
            frameworkHooks.applyHapticTouch()
        }
    }
}