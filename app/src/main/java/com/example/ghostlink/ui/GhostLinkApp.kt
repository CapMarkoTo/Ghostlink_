package com.example.ghostlink // Это должно совпадать с твоим проектом

import android.app.Application
import com.google.android.material.color.DynamicColors

class GhostLinkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}