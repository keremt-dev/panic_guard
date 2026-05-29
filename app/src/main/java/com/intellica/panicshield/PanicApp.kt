package com.intellica.panicshield

import android.app.Application
import com.intellica.panicshield.block.ProtectedAppsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PanicApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            ProtectedAppsRepository(this@PanicApp).seedDefaultsIfFirstRun()
        }
    }
}
