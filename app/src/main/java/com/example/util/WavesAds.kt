package com.example.util

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Central, isolated AdMob helper for the whole app.
 *
 * Monetization strategy (kept deliberately non-intrusive):
 *  - Adaptive BANNERS replace the existing BannerAdPlaceholder slots
 *    (Invoices, Clients, Products, Dashboard) - passive, never blocking.
 *  - One opt-in REWARDED ad: "Remove Ads for 24 hours" in Settings.
 *  - NO interstitials and NO app-open ads anywhere (they frustrate users).
 *
 * BEFORE RELEASE: replace the three TEST IDs below with the real ones from
 * your AdMob account (https://admob.google.com), and update the AdMob App ID
 * in AndroidManifest.xml to match. Google's test IDs earn nothing and must
 * not be shipped to production.
 */
object WavesAds {

    // ==================================================================
    // PRODUCTION AdMob IDs (WAVES: Invoice & Billing, admob.google.com).
    // App: ca-app-pub-5485098198270460~2002630118
    // Units: "Waves Banner" /9705860313, "Waves Rewarded" /5766615301
    // ==================================================================
    const val ADMOB_APP_ID = "ca-app-pub-5485098198270460~2002630118" // production
    const val BANNER_UNIT_ID = "ca-app-pub-5485098198270460/9705860313" // production "Waves Banner"
    const val REWARDED_UNIT_ID = "ca-app-pub-5485098198270460/5766615301" // production "Waves Rewarded"

    private const val PREFS_NAME = "waves_ads_prefs"
    private const val KEY_ADS_REMOVED_UNTIL = "ads_removed_until"
    private const val AD_FREE_DURATION_MS = 24L * 60 * 60 * 1000 // 24 hours

    private val initialized = AtomicBoolean(false)
    private val consentGathered = AtomicBoolean(false)

    /** Observable so every open screen hides its banner the moment a reward is earned. */
    val adsRemoved = mutableStateOf(false)

    fun adsRemovedUntil(context: Context): Long =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_ADS_REMOVED_UNTIL, 0L)

    /** Re-reads the persisted ad-free window (e.g. on app start / screen open). */
    fun refreshRemovedState(context: Context) {
        adsRemoved.value = System.currentTimeMillis() < adsRemovedUntil(context.applicationContext)
    }

    /** Initializes the Mobile Ads SDK once. Safe to call repeatedly. */
    fun initialize(context: Context, onReady: () -> Unit = {}) {
        refreshRemovedState(context)
        if (initialized.getAndSet(true)) {
            onReady()
            return
        }
        MobileAds.initialize(context.applicationContext) { onReady() }
    }

    /**
     * Google consent (UMP) - required for EEA/UK users. Shows Google's consent
     * form at most once per app session, before any ad is loaded. Failures are
     * non-blocking so ads still serve in regions without consent requirements.
     */
    fun gatherConsent(activity: Activity, onDone: () -> Unit = {}) {
        if (consentGathered.get()) {
            onDone()
            return
        }
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    consentGathered.set(true)
                    onDone()
                }
            },
            {
                // Consent update failed (offline etc.) - proceed without blocking.
                consentGathered.set(true)
                onDone()
            }
        )
    }

    /**
     * Real adaptive banner used by BannerAdPlaceholder. Renders nothing while
     * the user has an active ad-free window.
     */
    @Composable
    fun AdBanner(modifier: Modifier = Modifier) {
        val context = LocalContext.current

        // Re-check the persisted ad-free flag whenever this banner enters composition.
        remember(context) {
            refreshRemovedState(context)
            true
        }

        if (adsRemoved.value) return

        initialize(context)
        (context as? Activity)?.let { gatherConsent(it) }

        val adView = remember {
            AdView(context.applicationContext).apply {
                adUnitId = BANNER_UNIT_ID
                setAdSize(adaptiveBannerSize(context.applicationContext))
                loadAd(AdRequest.Builder().build())
            }
        }

        DisposableEffect(adView) {
            onDispose { adView.destroy() }
        }

        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { adView }
        )
    }

    private fun adaptiveBannerSize(context: Context): AdSize {
        val metrics = context.resources.displayMetrics
        val widthDp = (metrics.widthPixels / metrics.density).toInt().coerceAtLeast(320)
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
            ?: AdSize.BANNER
    }

    /**
     * Opt-in rewarded flow behind Settings > "Remove Ads".
     * Watching the full video grants 24 hours ad-free across the whole app.
     * [onResult] receives (success, user-facing message).
     */
    fun removeAdsFor24Hours(activity: Activity, onResult: (Boolean, String) -> Unit) {
        if (adsRemoved.value) {
            onResult(true, "Ads are already removed for you. Enjoy!")
            return
        }
        initialize(activity) {
            gatherConsent(activity) {
                RewardedAd.load(
                    activity,
                    REWARDED_UNIT_ID,
                    AdRequest.Builder().build(),
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            var rewardEarned = false
                            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    if (rewardEarned) {
                                        grantAdFree24Hours(activity)
                                        onResult(true, "Ads removed for 24 hours. Thank you for the support!")
                                    } else {
                                        onResult(false, "Watch the full video to remove ads for 24 hours.")
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    onResult(false, "Ad could not be shown. Please try again.")
                                }
                            }
                            ad.show(activity) { _ -> rewardEarned = true }
                        }

                        override fun onAdFailedToLoad(loadError: LoadAdError) {
                            onResult(false, "Ad not available right now. Please try again in a bit.")
                        }
                    }
                )
            }
        }
    }

    private fun grantAdFree24Hours(activity: Activity) {
        activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_ADS_REMOVED_UNTIL, System.currentTimeMillis() + AD_FREE_DURATION_MS)
            .apply()
        adsRemoved.value = true
    }
}
