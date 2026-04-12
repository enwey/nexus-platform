package com.nexus.platform.feature.game.runtime

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler
import com.nexus.platform.R
import com.nexus.platform.core.bridge.NexusBridge
import com.nexus.platform.core.bridge.RuntimeMetricsProvider
import com.nexus.platform.core.i18n.AppLanguageManager
import com.nexus.platform.core.network.BackendConfig
import com.nexus.platform.data.local.GameEngagementStore
import com.nexus.platform.data.remote.PlatformBackendApi
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.domain.model.GameRuntimeProfile
import com.nexus.platform.feature.game.data.GameManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class GameRuntimeActivity : AppCompatActivity() {
    private enum class CapsuleTheme { WHITE, BLACK }

    private lateinit var webView: WebView
    private lateinit var capsuleMenu: LinearLayout
    private lateinit var capsuleMoreIcon: ImageView
    private lateinit var capsuleCloseIcon: ImageView
    private lateinit var capsuleDivider: TextView
    private lateinit var runtimeLoading: LinearLayout
    private lateinit var runtimeStatus: TextView
    private lateinit var runtimeRetry: TextView
    private lateinit var gameManager: GameManager
    private lateinit var engagementStore: GameEngagementStore
    private lateinit var backendApi: PlatformBackendApi
    private lateinit var nexusBridge: NexusBridge
    private var runtimeMenuDialog: BottomSheetDialog? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var loadJob: Job? = null
    private var currentGame: GameItem? = null
    private var runtimeProfile: GameRuntimeProfile? = null
    private var statusBarInsetPx: Int = 0
    private var navBarBottomInsetPx: Int = 0
    private var capsuleBottomInsetPx: Int = 0
    private val capsuleBaseTopPx by lazy {
        resources.getDimensionPixelSize(R.dimen.runtime_capsule_margin_top)
    }
    private val capsuleHeightPx by lazy {
        resources.getDimensionPixelSize(R.dimen.runtime_capsule_height)
    }

    companion object {
        private const val EXTRA_GAME = "game"
        private const val APP_ASSET_BASE = "https://appassets.androidplatform.net/assets/"

        fun start(context: Context, game: GameItem) {
            val intent = Intent(context, GameRuntimeActivity::class.java).apply {
                putExtra(EXTRA_GAME, game)
                if (context !is AppCompatActivity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = false
        setContentView(R.layout.activity_game)

        val game = readGameFromIntent()
        if (game == null) {
            finish()
            return
        }
        currentGame = game

        initViews()
        initWindowInsets()
        initWebView()
        initBridge()
        initBackDispatcher()
        loadRuntimeProfile(game.id)
        loadGame(game = game, forceRefresh = false)
    }

    private fun readGameFromIntent(): GameItem? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_GAME, GameItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_GAME) as? GameItem
        }
    }

    private fun initViews() {
        webView = findViewById(R.id.gameWebView)
        capsuleMenu = findViewById(R.id.capsuleMenu)
        capsuleMoreIcon = findViewById(R.id.capsuleMoreIcon)
        capsuleCloseIcon = findViewById(R.id.capsuleCloseIcon)
        capsuleDivider = findViewById(R.id.capsuleDivider)
        runtimeLoading = findViewById(R.id.runtimeLoading)
        runtimeStatus = findViewById(R.id.runtimeStatus)
        runtimeRetry = findViewById(R.id.runtimeRetry)
        findViewById<View>(R.id.capsuleClose).setOnClickListener { finish() }
        findViewById<View>(R.id.capsuleMore).setOnClickListener { showRuntimeMenu() }
        runtimeRetry.setOnClickListener {
            val game = currentGame ?: return@setOnClickListener
            loadGame(game = game, forceRefresh = true)
        }
        gameManager = GameManager(this)
        engagementStore = GameEngagementStore(this)
        backendApi = PlatformBackendApi(this)
        applyCapsuleTheme(CapsuleTheme.BLACK)
    }

    private fun initWindowInsets() {
        val baseTop = capsuleBaseTopPx
        ViewCompat.setOnApplyWindowInsetsListener(capsuleMenu) { view, insets ->
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            statusBarInsetPx = statusTop
            navBarBottomInsetPx = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val lp = view.layoutParams as FrameLayout.LayoutParams
            val targetTop = baseTop + statusTop
            if (lp.topMargin != targetTop) {
                lp.topMargin = targetTop
                view.layoutParams = lp
            }
            val capsuleHeight = if (view.height > 0) {
                view.height
            } else {
                capsuleHeightPx
            }
            capsuleBottomInsetPx = targetTop + capsuleHeight
            insets
        }
        ViewCompat.requestApplyInsets(capsuleMenu)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false

        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.webChromeClient = WebChromeClient()
    }

    private fun initBridge() {
        val metricsProvider = object : RuntimeMetricsProvider {
            override fun getSystemInfo(): Map<String, Any> {
                val density = resources.displayMetrics.density
                val windowWidthPx = if (webView.width > 0) webView.width else resources.displayMetrics.widthPixels
                val windowHeightPx = if (webView.height > 0) webView.height else resources.displayMetrics.heightPixels
                val windowWidth = windowWidthPx / density
                val windowHeight = windowHeightPx / density
                val statusBarHeight = statusBarInsetPx / density
                val defaultSafeTopPx = statusBarInsetPx + capsuleBaseTopPx + capsuleHeightPx
                val safeTopPx = maxOf(defaultSafeTopPx, capsuleBottomInsetPx)
                val safeTop = safeTopPx / density
                val safeBottom = windowHeight - (navBarBottomInsetPx / density)

                return mapOf(
                    "brand" to Build.BRAND,
                    "model" to Build.MODEL,
                    "pixelRatio" to density,
                    "screenWidth" to windowWidth,
                    "screenHeight" to windowHeight,
                    "windowWidth" to windowWidth,
                    "windowHeight" to windowHeight,
                    "statusBarHeight" to statusBarHeight,
                    "safeArea" to mapOf(
                        "left" to 0f,
                        "right" to windowWidth,
                        "top" to safeTop,
                        "bottom" to safeBottom
                    ),
                    "language" to java.util.Locale.getDefault().language,
                    "version" to "1.0.0",
                    "system" to "Android ${Build.VERSION.RELEASE}",
                    "platform" to "android",
                    "fontSizeSetting" to 16,
                    "SDKVersion" to "1.0.0",
                    "benchmarkLevel" to 1,
                    "albumAuthorized" to true,
                    "cameraAuthorized" to true,
                    "locationAuthorized" to true,
                    "microphoneAuthorized" to true,
                    "notificationAuthorized" to true,
                    "bluetoothAuthorized" to true
                )
            }

            override fun getMenuButtonRect(): Map<String, Any> {
                val density = resources.displayMetrics.density
                val location = IntArray(2)
                capsuleMenu.getLocationOnScreen(location)
                val left = location[0] / density
                val top = location[1] / density
                val width = capsuleMenu.width / density
                val height = capsuleMenu.height / density
                return mapOf(
                    "width" to width,
                    "height" to height,
                    "left" to left,
                    "top" to top,
                    "right" to left + width,
                    "bottom" to top + height
                )
            }

            override suspend fun checkForUpdate(): Map<String, Any> {
                val game = currentGame ?: return mapOf(
                    "hasUpdate" to false,
                    "forceUpdate" to false,
                    "ready" to false,
                    "errMsg" to "update.check:fail"
                )
                val result = gameManager.checkForUpdate(game)
                return mapOf(
                    "hasUpdate" to result.hasUpdate,
                    "forceUpdate" to result.forceUpdate,
                    "ready" to result.ready,
                    "version" to (result.latestVersion ?: ""),
                    "errMsg" to result.errMsg
                )
            }

            override suspend fun applyUpdate(): Map<String, Any> {
                val game = currentGame ?: return mapOf(
                    "success" to false,
                    "errMsg" to "update.apply:fail"
                )
                val success = gameManager.applyUpdate(game.id, null)
                if (success) {
                    runOnUiThread {
                        recreate()
                    }
                }
                return mapOf(
                    "success" to success,
                    "errMsg" to if (success) "update.apply:ok" else "update.apply:fail"
                )
            }
        }

        nexusBridge = NexusBridge(this, webView, metricsProvider)
        webView.addJavascriptInterface(nexusBridge, "AndroidApp")
        webView.addJavascriptInterface(nexusBridge.createSyncBridge(), "AndroidAppSync")
    }

    private fun initBackDispatcher() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (::webView.isInitialized && webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        finish()
                    }
                }
            }
        )
    }

    private fun loadGame(game: GameItem, forceRefresh: Boolean) {
        engagementStore.markPlayed(game.id)
        scope.launch(Dispatchers.IO) {
            runCatching { backendApi.markPlayed(game.id) }
        }
        loadJob?.cancel()
        loadJob = scope.launch {
            try {
                showLoading(message = getString(R.string.runtime_status_preparing), showRetry = false)

                if (game.requiresOnline) {
                    val backendReachable = withContext(Dispatchers.IO) {
                        gameManager.isBackendReachable()
                    }
                    if (!backendReachable) {
                        throw IOException("Game requires online service")
                    }
                }

                val hasLocalBundle = withContext(Dispatchers.IO) {
                    gameManager.isGameDownloaded(game.id)
                }

                // For offline-capable games with local package, avoid blocking startup on update checks.
                if (!game.requiresOnline && hasLocalBundle && !forceRefresh) {
                    withContext(Dispatchers.IO) {
                        runCatching { gameManager.checkForUpdate(game = game, blockOnForce = false) }
                    }
                } else {
                    val update = withContext(Dispatchers.IO) {
                        gameManager.checkForUpdate(game = game, blockOnForce = true)
                    }
                    if (update.forceUpdate && !update.ready) {
                        throw IOException("Game download failed: force update required")
                    }
                }

                val prepared = withContext(Dispatchers.IO) {
                    gameManager.prepareGame(game = game, forceRefresh = forceRefresh)
                }

                val status = if (prepared.fromCache) {
                    getString(R.string.runtime_status_cache)
                } else {
                    getString(R.string.runtime_status_loading_resource)
                }
                showLoading(message = status, showRetry = false)

                val assetLoader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", InternalStoragePathHandler(this@GameRuntimeActivity, prepared.rootDir))
                    .addPathHandler("/res/", AssetsPathHandler(this@GameRuntimeActivity))
                    .build()

                webView.webViewClient = buildWebViewClient(assetLoader)
                val launchUrl = APP_ASSET_BASE + prepared.entryRelativePath
                webView.loadUrl(launchUrl)
            } catch (e: Exception) {
                showFailure(error = e)
            }
        }
    }

    private fun loadRuntimeProfile(appId: String) {
        scope.launch(Dispatchers.IO) {
            val profile = runCatching { backendApi.getGameRuntimeProfile(appId) }.getOrNull()
            withContext(Dispatchers.Main) {
                runtimeProfile = profile
                applyCapsuleTheme(resolveCapsuleTheme(profile?.capsuleTheme))
            }
        }
    }

    private fun resolveCapsuleTheme(rawTheme: String?): CapsuleTheme {
        return when (rawTheme?.trim()?.lowercase()) {
            "white", "light" -> CapsuleTheme.WHITE
            "black", "dark" -> CapsuleTheme.BLACK
            else -> CapsuleTheme.BLACK
        }
    }

    private fun applyCapsuleTheme(theme: CapsuleTheme) {
        when (theme) {
            CapsuleTheme.WHITE -> {
                capsuleMenu.setBackgroundResource(R.drawable.bg_runtime_capsule_white)
                capsuleDivider.setBackgroundColor(Color.parseColor("#33000000"))
                val iconColor = Color.parseColor("#D9000000")
                capsuleMoreIcon.setColorFilter(iconColor)
                capsuleCloseIcon.setColorFilter(iconColor)
            }

            CapsuleTheme.BLACK -> {
                capsuleMenu.setBackgroundResource(R.drawable.bg_runtime_capsule)
                capsuleDivider.setBackgroundColor(Color.parseColor("#52FFFFFF"))
                val iconColor = Color.parseColor("#F2FFFFFF")
                capsuleMoreIcon.setColorFilter(iconColor)
                capsuleCloseIcon.setColorFilter(iconColor)
            }
        }
    }

    private fun showRuntimeMenu() {
        val game = currentGame ?: return
        runtimeMenuDialog?.dismiss()
        val dialog = BottomSheetDialog(this).also { sheet ->
            val root = LayoutInflater.from(this)
                .inflate(R.layout.layout_runtime_menu_sheet, null, false)
            val profile = runtimeProfile
            val shareLink = buildShareLandingUrl(game.id)
            val shareText = buildShareText(game, profile, shareLink)

            root.findViewById<TextView>(R.id.runtimeMenuTitle).text =
                profile?.gameName?.ifBlank { game.name } ?: game.name
            root.findViewById<TextView>(R.id.runtimeMenuPlayers).text =
                profile?.playerCountText?.ifBlank { getString(R.string.runtime_menu_game_players) }
                    ?: getString(R.string.runtime_menu_game_players)
            root.findViewById<TextView>(R.id.runtimeMenuStudio).text =
                profile?.studioName?.ifBlank { getString(R.string.runtime_menu_game_studio) }
                    ?: getString(R.string.runtime_menu_game_studio)

            val favoriteActionText = root.findViewById<TextView>(R.id.runtimeMenuActionFavoriteText)
            val headerFavorite = root.findViewById<ImageView>(R.id.runtimeMenuHeaderFavorite)
            val actionFavoriteIcon = root.findViewById<ImageView>(R.id.runtimeMenuActionFavoriteIcon)
            val refreshFavoriteLabel = {
                val favorite = engagementStore.isFavorite(game.id)
                favoriteActionText.text = if (favorite) {
                    getString(R.string.runtime_menu_remove_favorite)
                } else {
                    getString(R.string.runtime_menu_add_favorite)
                }
                val starRes = if (favorite) {
                    R.drawable.ic_menu_star_filled
                } else {
                    R.drawable.ic_menu_star_outline
                }
                headerFavorite.setImageResource(starRes)
                actionFavoriteIcon.setImageResource(starRes)
                headerFavorite.contentDescription = favoriteActionText.text
                actionFavoriteIcon.contentDescription = favoriteActionText.text
            }
            val toggleFavorite = {
                val nowFavorite = engagementStore.toggleFavorite(game.id)
                refreshFavoriteLabel()
                Toast.makeText(
                    this,
                    if (nowFavorite) getString(R.string.runtime_favorite_added) else getString(R.string.runtime_favorite_removed),
                    Toast.LENGTH_SHORT
                ).show()
                scope.launch(Dispatchers.IO) {
                    val synced = backendApi.setFavorite(game.id, nowFavorite)
                    if (!synced) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@GameRuntimeActivity,
                                getString(R.string.runtime_favorite_sync_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            refreshFavoriteLabel()
            headerFavorite.setOnClickListener { toggleFavorite() }
            root.findViewById<View>(R.id.runtimeMenuActionFavorite).setOnClickListener { toggleFavorite() }

            val shareAction = {
                scope.launch(Dispatchers.IO) {
                    backendApi.markShared(game.id)
                }
            }
            root.findViewById<View>(R.id.runtimeMenuShareWhatsapp).setOnClickListener {
                shareAction()
                shareToChannel(
                    packageName = "com.whatsapp",
                    chooserTitle = getString(R.string.runtime_menu_share_whatsapp),
                    text = shareText
                )
            }
            root.findViewById<View>(R.id.runtimeMenuShareFacebook).setOnClickListener {
                shareAction()
                shareToChannel(
                    packageName = "com.facebook.katana",
                    chooserTitle = getString(R.string.runtime_menu_share_facebook),
                    text = shareText
                )
            }
            root.findViewById<View>(R.id.runtimeMenuShareXiaohongshu).setOnClickListener {
                shareAction()
                shareToChannel(
                    packageName = "com.xingin.xhs",
                    chooserTitle = getString(R.string.runtime_menu_share_xiaohongshu),
                    text = shareText
                )
            }
            root.findViewById<View>(R.id.runtimeMenuShareLink).setOnClickListener {
                copyShareTextToClipboard(shareLink)
            }

            root.findViewById<View>(R.id.runtimeMenuActionRestart).setOnClickListener {
                sheet.dismiss()
                loadGame(game = game, forceRefresh = true)
            }
            root.findViewById<View>(R.id.runtimeMenuActionFeedback).setOnClickListener {
                val mailIntent = Intent(
                    Intent.ACTION_SENDTO,
                    Uri.parse("mailto:support@nexus.local")
                ).apply {
                    putExtra(Intent.EXTRA_SUBJECT, "Game feedback: ${game.name}")
                    putExtra(Intent.EXTRA_TEXT, "AppId=${game.id}\nVersion=${game.version}\n\nDescribe your issue:")
                }
                if (mailIntent.resolveActivity(packageManager) != null) {
                    startActivity(mailIntent)
                } else {
                    Toast.makeText(this, getString(R.string.runtime_feedback_todo), Toast.LENGTH_SHORT).show()
                }
            }
            root.findViewById<View>(R.id.runtimeMenuCancel).setOnClickListener { sheet.dismiss() }

            sheet.setContentView(root)
            sheet.window?.setBackgroundDrawableResource(android.R.color.transparent)
            sheet.setOnShowListener { dialogInterface ->
                val bottomSheetDialog = dialogInterface as BottomSheetDialog
                val bottomSheet =
                    bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
                bottomSheet?.let {
                    BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
            runtimeMenuDialog = sheet
        }.also { runtimeMenuDialog = it }
        dialog.show()
    }

    private fun buildShareText(game: GameItem, profile: GameRuntimeProfile?, shareLink: String): String {
        val title = profile?.shareTitle?.ifBlank { game.name } ?: game.name
        val subtitle = profile?.shareSubtitle?.ifBlank { game.description } ?: game.description
        return listOf(title, subtitle, shareLink)
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private fun buildShareLandingUrl(appId: String): String {
        val base = BackendConfig.apiBaseUrl.trimEnd('/')
        return "$base/public/share/game/$appId"
    }

    private fun shareToChannel(packageName: String, chooserTitle: String, text: String) {
        val targetIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage(packageName)
        }
        val targetResolved = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.resolveActivity(targetIntent, PackageManager.ResolveInfoFlags.of(0)) != null
            } else {
                @Suppress("DEPRECATION")
                packageManager.resolveActivity(targetIntent, 0) != null
            }
        }.getOrDefault(false)

        if (targetResolved) {
            startActivity(targetIntent)
            return
        }

        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(fallbackIntent, chooserTitle))
    }

    private fun copyShareTextToClipboard(text: String) {
        val clipboardManager = getSystemService(ClipboardManager::class.java)
        val clip = ClipData.newPlainText("runtime_share_text", text)
        clipboardManager?.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.runtime_share_copied), Toast.LENGTH_SHORT).show()
    }

    private fun buildWebViewClient(assetLoader: WebViewAssetLoader): WebViewClient {
        return object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val uri = request?.url ?: return null
                return assetLoader.shouldInterceptRequest(uri)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                showLoading(message = getString(R.string.runtime_status_starting), showRetry = false)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectSDK()
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                hideLoading()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString().orEmpty()
                return !(url.startsWith("https://appassets.androidplatform.net/") || url.startsWith("https://game.local/"))
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val desc = error?.description?.toString().orEmpty()
                    showFailure(IOException("WebView load failed: $desc"))
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true) {
                    val code = errorResponse?.statusCode ?: -1
                    showFailure(IOException("WebView http error: $code"))
                }
            }
        }
    }

    private fun injectSDK() {
        scope.launch(Dispatchers.IO) {
            val sdkContent = gameManager.readSDKContent().trim()
            if (sdkContent.isBlank()) {
                return@launch
            }
            withContext(Dispatchers.Main) {
                if (!::webView.isInitialized) {
                    return@withContext
                }
                webView.evaluateJavascript(sdkContent, null)
            }
        }
    }

    private fun showLoading(message: String, showRetry: Boolean) {
        if (!::runtimeLoading.isInitialized) {
            return
        }
        webView.visibility = View.INVISIBLE
        runtimeLoading.visibility = View.VISIBLE
        runtimeStatus.text = message
        runtimeRetry.visibility = if (showRetry) View.VISIBLE else View.GONE
    }

    private fun hideLoading() {
        if (!::runtimeLoading.isInitialized) {
            return
        }
        webView.visibility = View.VISIBLE
        runtimeLoading.visibility = View.GONE
        runtimeRetry.visibility = View.GONE
    }

    private fun showFailure(error: Throwable) {
        val message = userVisibleError(error)
        showLoading(message = message, showRetry = true)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun userVisibleError(error: Throwable): String {
        val msg = error.message.orEmpty()
        return when {
            msg.contains("requires online service", ignoreCase = true) -> getString(R.string.runtime_error_requires_online)
            msg.contains("checksum", ignoreCase = true) -> getString(R.string.runtime_error_checksum)
            msg.contains("download", ignoreCase = true) -> getString(R.string.runtime_error_download)
            msg.contains("entry", ignoreCase = true) -> getString(R.string.runtime_error_entry_missing)
            msg.contains("WebView", ignoreCase = true) -> getString(R.string.runtime_error_webview)
            else -> getString(R.string.runtime_error_startup)
        }
    }

    override fun onDestroy() {
        loadJob?.cancel()
        if (::nexusBridge.isInitialized) {
            nexusBridge.cleanup()
        }
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.removeJavascriptInterface("AndroidApp")
            webView.removeJavascriptInterface("AndroidAppSync")
            webView.webChromeClient = null
            webView.webViewClient = WebViewClient()
            webView.destroy()
        }
        runtimeMenuDialog?.dismiss()
        runtimeMenuDialog = null
        scope.cancel()
        super.onDestroy()
    }
}
