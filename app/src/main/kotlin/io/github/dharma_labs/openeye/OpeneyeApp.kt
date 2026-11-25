package io.github.dharma_labs.openeye

import android.app.Application
import io.github.dharma_labs.openeye.data.AppRepository

class OpeneyeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppRepository.get(this)
    }
}