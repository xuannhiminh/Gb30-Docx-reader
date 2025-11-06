package com.ezteam.baseproject.utils

import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd

object TemporaryStorage {
     /**
      * Check if in this time user has seen the dialog to request to set default reader or not
      */
     @JvmStatic var isObtainConsent: Boolean = false
     @JvmStatic @Volatile var isSavingFileNotNoti = false
     @JvmStatic var isShowedDefaultReaderRequestDialogInThisSession: Boolean = false
     @JvmStatic var isShowedFullScreenRequestDialogInThisSession: Boolean = false
     @JvmStatic var isShowSatisfiedDialogInThisSession: Boolean = false
     @JvmStatic var isShowedAddToHoneDialog: Boolean = false
     @JvmStatic var timeEnterPdfDetail = 0;
     @JvmStatic var shouldLoadAdsLanguageScreen = true;
     @JvmStatic var keepScreenOn: Boolean = false
     @JvmStatic var enableNotification: Boolean = true
     @JvmStatic var isShowedReloadGuideInThisSession: Boolean = false
     @JvmStatic var isNightMode: Boolean = false
     @JvmStatic var isLoadAds: Boolean = false
     @JvmStatic var isRateFullStar: Boolean = false
     @JvmStatic var nativeAdPreload : NativeAd? = null
     @JvmStatic var interAdPreloaded : InterstitialAd? = null
     @JvmStatic var isLoadingNativeAdsLanguage = false
     @JvmStatic var callbackNativeAdsLanguage: (NativeAd?) -> Unit = { _ -> }
     @JvmStatic
     fun reset() {
        isShowedDefaultReaderRequestDialogInThisSession = false
         isShowedFullScreenRequestDialogInThisSession = false
        isShowSatisfiedDialogInThisSession = false
        isShowedAddToHoneDialog = false
        timeEnterPdfDetail = 0
    }
}