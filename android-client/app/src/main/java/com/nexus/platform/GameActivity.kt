package com.nexus.platform

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler
import com.nexus.platform.bridge.NexusBridge
import com.nexus.platform.utils.GameManager
import com.nexus.platform.utils.ZipUtils
import kotlinx.coroutines.*
import java.io.File

class GameActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var gameManager: GameManager
    private lateinit var nexusBridge: NexusBridge
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private const val EXTRA_GAME = "game"
        
        fun start(context: Context, game: Game) {
            val intent = Intent(context, GameActivity::class.java).apply {
                putExtra(EXTRA_GAME, game)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val game = intent.getSerializableExtra(EXTRA_GAME) as? Game
        if (game == null) {
            finish()
            return
        }

        initViews()
        initWebView()
        initBridge()
        loadGame(game)
    }

    private fun initViews() {
        webView = findViewById(R.id.gameWebView)
        gameManager = GameManager(this)
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
        
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectSDK()
            }
            
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                return if (url.startsWith("https://game.local/") || url.startsWith("https://appassets.androidplatform.net/")) {
                    false
                } else {
                    true
                }
            }
        }
    }

    private fun initBridge() {
        nexusBridge = NexusBridge(this, webView)
        webView.addJavascriptInterface(nexusBridge, "AndroidApp")
    }

    private fun loadGame(game: Game) {
        scope.launch {
            try {
                showLoading(true)
                
                val unzipDir = gameManager.getGameDir(game.id)
                
                if (!gameManager.isGameDownloaded(game.id)) {
                    withContext(Dispatchers.IO) {
                        gameManager.downloadGame(game, unzipDir)
                    }
                }
                
                val assetLoader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", InternalStoragePathHandler(this@GameActivity, unzipDir))
                    .addPathHandler("/res/", AssetsPathHandler(this@GameActivity))
                    .build()
                
                webView.setWebViewClient(object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request?.url)
                    }
                })
                
                val gameUrl = "https://appassets.androidplatform.net/index.html"
                webView.loadUrl(gameUrl)
                
            } catch (e: Exception) {
                e.printStackTrace()
                showError("游戏加载失败: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun injectSDK() {
        scope.launch(Dispatchers.IO) {
            try {
                val sdkContent = gameManager.readSDKContent()
                withContext(Dispatchers.Main) {
                    webView.evaluateJavascript(sdkContent, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        
    }

    private fun showError(message: String) {
        
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
