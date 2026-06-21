package com.firebase.loginauth

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MAHShagorEnterpriseApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize any global configurations here
        // The Hilt dependency injection will be automatically initialized
    }
}
