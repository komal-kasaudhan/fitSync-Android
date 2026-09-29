package com.esha.fitsync

import android.app.Application
import com.esha.fitsync.api.RetrofitClient

class FitSyncApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
    }
}
