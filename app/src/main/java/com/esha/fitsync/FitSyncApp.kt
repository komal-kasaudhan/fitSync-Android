package com.esha.fitsync

import android.app.Application
import com.esha.fitsync.api.RetrofitClient
import org.osmdroid.config.Configuration
import java.io.File

class FitSyncApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
        com.esha.fitsync.data.AppCacheManager.init(this)

        // OpenStreetMap osmdroid setup
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = packageName
        val basePath = File(cacheDir, "osmdroid")
        val tileCache = File(basePath, "tiles")
        osmConfig.osmdroidBasePath = basePath
        osmConfig.osmdroidTileCache = tileCache
    }
}
