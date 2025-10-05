package org.lingxi9374.game2048

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

// File: ComposeLocalProviderKt.kt (或你的文件)

@Composable
fun ProvideLocale(settingsManager: SettingsManager, content: @Composable () -> Unit) {
    val localeManager = remember { LocaleManager(settingsManager) }

    // 1. 获取当前的原始 Context 和 Configuration
    val context = LocalContext.current
    val currentConfig = LocalConfiguration.current

    // 2. 创建一个临时的、本地化的 Configuration 对象
    val newConfig = remember(localeManager.locale, currentConfig) {
        Configuration(currentConfig).apply {
            setLocale(localeManager.locale)
        }
    }

    // **核心更改：不要覆盖 LocalContext。覆盖 LocalConfiguration 和你的 LocalLocaleManager**
    CompositionLocalProvider(
        LocalLocaleManager provides localeManager,
        LocalConfiguration provides newConfig, // ⬅️ 只提供新的配置
        content = content
    )

    // 注意：你可能还需要手动更改 Activity 的基础 Context（如在 Activity 的 attachBaseContext 中），
    // 以确保整个 Activity 的资源（如 ViewModelFactory 中的 Context）也被本地化。
}