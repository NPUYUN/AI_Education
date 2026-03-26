package com.example.ai_education

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import java.io.File

@HiltAndroidApp
class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 预加载地图配置，提升首次进入地图的加载速度
        val config = Configuration.getInstance()
        config.load(this, getSharedPreferences("osmdroid", 0))
        config.userAgentValue = packageName
        val basePath = File(cacheDir, "osmdroid")
        val tilePath = File(basePath, "tiles")
        if (!basePath.exists()) basePath.mkdirs()
        if (!tilePath.exists()) tilePath.mkdirs()
        config.osmdroidBasePath = basePath
        config.osmdroidTileCache = tilePath
        config.tileFileSystemCacheMaxBytes = 100L * 1024 * 1024 // 100MB缓存
        config.tileFileSystemCacheTrimBytes = 80L * 1024 * 1024
    }
}
