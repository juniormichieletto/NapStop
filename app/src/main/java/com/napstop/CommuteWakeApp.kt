package com.napstop

import android.app.Application
import org.osmdroid.config.Configuration
import android.preference.PreferenceManager

class CommuteWakeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize DatabaseProvider
        DatabaseProvider.init(this)
        // Initialize OSMDroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        // Important: set the user agent to avoid getting blocked by OpenStreetMap
        Configuration.getInstance().userAgentValue = packageName
    }
}
