package org.lingxi9374.game2048

import android.app.Application
import java.util.Locale

/**
 * 这是你的自定义 Application 类。
 * 它持有应用生命周期内单例的 SettingsManager。
 */
class LingXi2048Application : Application() {

    lateinit var settingsManager: SettingsManager

    override fun onCreate() {
        super.onCreate()

        // 2. 在 onCreate() 中安全地初始化 Context 依赖项
        settingsManager = SettingsManager(this)
    }

}
