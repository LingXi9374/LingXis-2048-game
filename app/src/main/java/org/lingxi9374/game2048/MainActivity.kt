package org.lingxi9374.game2048

import android.content.Context
import android.content.ContextParams
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.lingxi9374.game2048.ui.*
import org.lingxi9374.game2048.ui.theme.LingXis2048Theme
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

/**
 * 专用于处理文件导入/导出 ActivityResultLaunchers 和 LaunchedEffect 的 Composable。
 * 提取到独立函数中可以确保正确的 ActivityResultRegistryOwner 被访问，从而解决 IllegalStateException。
 */
@Composable
private fun FileTransferHandlers(historyViewModel: HistoryViewModel) {
    val context = LocalContext.current

    // --- 导出文件启动器 ---
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            handleExportResult(context, it, historyViewModel)
        }
    }

    // --- 导入文件启动器 ---
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            handleImportResult(context, it, historyViewModel)
        }
    }

    // --- 事件监听 ---
    LaunchedEffect(historyViewModel) {
        launch {
            historyViewModel.exportEvent.collectLatest { suggestedFileName ->
                exportLauncher.launch(suggestedFileName.toString())
            }
        }

        launch {
            historyViewModel.importEvent.collectLatest {
                importLauncher.launch(arrayOf("text/plain"))
            }
        }
    }
}

/**
 * 将文件 I/O 逻辑提取到顶层私有函数中，以简化 Composable 回调。
 */
private fun handleExportResult(context: Context, uri: Uri, historyViewModel: HistoryViewModel) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            historyViewModel.exportData?.let { data ->
                outputStream.write(data.toByteArray())
            }
        }
    } catch (e: IOException) {
        Log.e("FileExport", "Error writing data to URI: $uri", e)
        // 可以在这里添加 Toast 或 SnackBar 来通知用户导出失败
    }
}

/**
 * 将文件 I/O 逻辑提取到顶层私有函数中，以简化 Composable 回调。
 */
private fun handleImportResult(context: Context, uri: Uri, historyViewModel: HistoryViewModel) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                val text = reader.readText()
                historyViewModel.importHistoryData(text)
            }
        }
    } catch (e: IOException) {
        Log.e("FileImport", "Error reading data from URI: $uri", e)
        // 可以在这里添加 Toast 或 SnackBar 来通知用户导入失败
    }
}

fun Context.wrapContextWithLocale(locale: Locale): ContextWrapper {
    val config = android.content.res.Configuration(this.resources.configuration)
    config.setLocale(locale)

    return ContextWrapper(this.createConfigurationContext(config))

}

/**
 * Context 扩展函数：查找并返回宿主 Activity。
 * 这是在 Compose 中获取 Activity 实例的推荐方式。
 * 它通过遍历 ContextWrapper 链条来找到 ComponentActivity 实例。
 */
fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}

class MainActivity : ComponentActivity() {
    // 核心代码：在 Activity 启动前注入本地化的 Context
    override fun attachBaseContext(newBase: Context) {
        val config = android.content.res.Configuration(newBase.resources.configuration)

        // **修正: 直接从 newBase.applicationContext 中安全获取 SettingsManager，并提供更详细的错误信息。**
        // 在 Activity 生命周期早期，applicationContext 应该指向 Application 实例。
        val applicationInstance = newBase.applicationContext

        val settingsManager =
            (applicationInstance as? LingXi2048Application)?.settingsManager

        if (settingsManager == null) {
            val actualClassName = applicationInstance?.javaClass?.name ?: "null context"
            // 抛出更详细的 IllegalStateException，指导用户检查 Manifest
            throw IllegalStateException("Failed to retrieve SettingsManager. Expected Application type 'LingXi2048Application', but found '$actualClassName'. Please verify your AndroidManifest.xml <application android:name=\"...\">.")
        }

        val targetLocale = settingsManager.getLocale()

        config.setLocale(targetLocale)
        val localizedContext = newBase.wrapContextWithLocale(targetLocale)

        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Hide the status bar persistently
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())

        val settingsManager = SettingsManager(this)

        setContent {
            ProvideLocale(settingsManager) {
                LingXis2048Theme {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        val context = LocalContext.current
                        val historyViewModel: HistoryViewModel = viewModel(
                            factory = HistoryViewModelFactory(context)
                        )

                        // <--- NEW: 调用提取出的 Composable 来处理文件 I/O 启动器和事件监听 --->
                        FileTransferHandlers(historyViewModel)

                        // --- 导航 ---
                        NavHost(
                            navController = navController,
                            startDestination = "game",
                            enterTransition = { fadeIn(animationSpec = tween(300)) },
                            exitTransition = { fadeOut(animationSpec = tween(300)) },
                            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                            popExitTransition = { fadeOut(animationSpec = tween(300)) }
                        ) {
                            composable("game") { GameScreen(navController) }
                            composable("settings") { SettingsScreen(navController) }
                            composable("about") { AboutScreen(navController) }
                            composable("history") { HistoryScreen(navController, historyViewModel) }
                        }
                    }
                }
            }
        }
    }
}
