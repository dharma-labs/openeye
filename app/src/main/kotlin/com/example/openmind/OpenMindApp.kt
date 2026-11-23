package com.example.openmind

import android.app.Application
import com.example.openmind.data.AppRepository

class OpenMindApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppRepository.get(this)
    }
}