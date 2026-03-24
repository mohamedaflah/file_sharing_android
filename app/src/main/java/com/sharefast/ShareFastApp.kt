package com.sharefast

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ShareFastApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val loader = ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(180)
            .build()
        Coil.setImageLoader(loader)
    }
}
