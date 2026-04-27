package com.dredio.textraocr

import android.app.Application
import com.google.android.gms.ads.MobileAds

class TextraOcrApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDiagnostics.install(this)
        MobileAds.initialize(this)
        AppDiagnostics.logBreadcrumb(this, "Application created")
    }
}
