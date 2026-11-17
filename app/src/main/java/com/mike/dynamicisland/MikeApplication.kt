package com.mike.dynamicisland

import android.app.Application
import android.content.Context
import com.mike.dynamicisland.core.MikeEngine

class MikeApplication : Application() {

    val mikeEngine: MikeEngine by lazy { MikeEngine(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        // Initialise le moteur léger et le profil matériel au démarrage du processus
        mikeEngine
    }

    companion object {
        @Volatile
        private var fallbackEngine: MikeEngine? = null

        fun getEngine(context: Context): MikeEngine {
            val app = context.applicationContext as? MikeApplication
            if (app != null) return app.mikeEngine
            return fallbackEngine ?: synchronized(this) {
                fallbackEngine ?: MikeEngine(context.applicationContext).also { fallbackEngine = it }
            }
        }
    }
}
