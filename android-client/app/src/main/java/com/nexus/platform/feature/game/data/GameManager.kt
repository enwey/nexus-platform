package com.nexus.platform.feature.game.data

import android.content.Context
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.utils.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class GameManager(private val context: Context) {

    data class PreparedGame(
        val rootDir: File,
        val entryRelativePath: String,
        val fromCache: Boolean
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getGameDir(gameId: String): File {
        val gamesDir = File(context.filesDir, "games")
        if (!gamesDir.exists()) {
            gamesDir.mkdirs()
        }
        return File(gamesDir, gameId)
    }

    fun isGameDownloaded(gameId: String): Boolean {
        return resolveEntryPath(getGameDir(gameId)) != null
    }

    suspend fun prepareGame(game: GameItem, forceRefresh: Boolean = false): PreparedGame {
        return withContext(Dispatchers.IO) {
            val gameDir = getGameDir(game.id)
            val cachedEntry = resolveEntryPath(gameDir)
            if (!forceRefresh && cachedEntry != null) {
                return@withContext PreparedGame(
                    rootDir = gameDir,
                    entryRelativePath = cachedEntry,
                    fromCache = true
                )
            }

            try {
                if (game.downloadUrl.isBlank()) {
                    createLocalDemoGame(game, gameDir)
                } else {
                    downloadGame(game, gameDir)
                }
            } catch (e: Exception) {
                if (!forceRefresh && cachedEntry != null) {
                    return@withContext PreparedGame(
                        rootDir = gameDir,
                        entryRelativePath = cachedEntry,
                        fromCache = true
                    )
                }
                throw e
            }

            val entryPath = resolveEntryPath(gameDir)
                ?: throw IOException("Game entry not found: index.html")
            PreparedGame(
                rootDir = gameDir,
                entryRelativePath = entryPath,
                fromCache = false
            )
        }
    }

    suspend fun downloadGame(game: GameItem, targetDir: File) {
        withContext(Dispatchers.IO) {
            val normalizedUrl = normalizeDownloadUrl(game.downloadUrl).trim()
            if (normalizedUrl.isBlank()) {
                throw IOException("Game download url is empty")
            }

            deleteDirectory(targetDir)
            targetDir.mkdirs()

            val request = Request.Builder()
                .url(normalizedUrl)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Game download failed: ${response.code}")
                }

                val zipFile = File(context.cacheDir, "${game.id}_${System.currentTimeMillis()}.zip")
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(zipFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IOException("Game package is empty")

                val actualMd5 = calculateMD5(zipFile)
                if (game.md5.isNotBlank() && !actualMd5.equals(game.md5, ignoreCase = true)) {
                    zipFile.delete()
                    throw IOException("Game package checksum mismatch")
                }

                ZipUtils.unzip(zipFile, targetDir)
                zipFile.delete()
                normalizeExtractedStructure(targetDir)
            }
        }
    }

    suspend fun readSDKContent(): String {
        return withContext(Dispatchers.IO) {
            val directFile = File(context.filesDir, "wx-mock-sdk.js")
            if (directFile.exists()) {
                return@withContext directFile.readText()
            }

            val bundleFile = File(context.filesDir, "wx-mock-sdk.iife.js")
            if (bundleFile.exists()) {
                return@withContext bundleFile.readText()
            }

            val assetScript = runCatching {
                context.assets.open("wx-mock-sdk.iife.js").bufferedReader().use { it.readText() }
            }.getOrNull()
            if (!assetScript.isNullOrBlank()) {
                return@withContext assetScript
            }

            defaultSDKStub()
        }
    }

    private fun normalizeExtractedStructure(targetDir: File) {
        if (File(targetDir, "index.html").exists()) {
            return
        }
        val children = targetDir.listFiles()
            ?.filterNot { it.name.equals("__MACOSX", ignoreCase = true) }
            .orEmpty()
        if (children.size != 1 || !children[0].isDirectory) {
            return
        }

        val singleRoot = children[0]
        singleRoot.listFiles().orEmpty().forEach { child ->
            child.renameTo(File(targetDir, child.name))
        }
        singleRoot.deleteRecursively()
    }

    private fun resolveEntryPath(gameDir: File): String? {
        if (!gameDir.exists() || !gameDir.isDirectory) {
            return null
        }
        val rootIndex = File(gameDir, "index.html")
        if (rootIndex.exists()) {
            return "index.html"
        }

        val entry = gameDir.walkTopDown()
            .maxDepth(5)
            .filter { file ->
                file.isFile &&
                    file.name.equals("index.html", ignoreCase = true) &&
                    !file.path.contains("__MACOSX")
            }
            .minByOrNull { file ->
                file.relativeTo(gameDir).path.count { it == '/' || it == '\\' }
            }
            ?: return null
        return entry.relativeTo(gameDir).path.replace(File.separatorChar, '/')
    }

    private fun createLocalDemoGame(game: GameItem, targetDir: File) {
        deleteDirectory(targetDir)
        targetDir.mkdirs()

        val safeName = escapeHtml(game.name.ifBlank { "Nexus Demo Game" })
        val safeDesc = escapeHtml(
            game.description.ifBlank { "Offline demo game package. No remote API required." }
        )
        val html = """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1" />
              <title>$safeName</title>
              <style>
                * { box-sizing: border-box; }
                html, body { margin: 0; width: 100%; height: 100%; background: #090a0f; color: #fff; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
                .page {
                  width: 100%;
                  height: 100%;
                  display: flex;
                  justify-content: center;
                  align-items: center;
                  padding: 24px;
                  background:
                    radial-gradient(circle at 20% 20%, rgba(92, 103, 255, 0.55), transparent 48%),
                    radial-gradient(circle at 85% 10%, rgba(203, 99, 255, 0.42), transparent 52%),
                    #090a0f;
                }
                .card {
                  width: min(560px, 100%);
                  border-radius: 24px;
                  padding: 28px;
                  background: rgba(21, 22, 29, 0.86);
                  border: 1px solid rgba(255, 255, 255, 0.1);
                  backdrop-filter: blur(6px);
                }
                h1 { margin: 0; font-size: 28px; line-height: 1.25; }
                p { margin: 12px 0 0; color: #c6c8d5; line-height: 1.6; }
                button {
                  margin-top: 24px;
                  border: 0;
                  border-radius: 14px;
                  padding: 12px 18px;
                  font-size: 15px;
                  font-weight: 700;
                  color: #ffffff;
                  background: linear-gradient(135deg, #5C67FF, #CB63FF);
                }
                #status {
                  margin-top: 14px;
                  color: #8b8d99;
                  font-size: 13px;
                }
              </style>
            </head>
            <body>
              <div class="page">
                <div class="card">
                  <h1>$safeName</h1>
                  <p>$safeDesc</p>
                  <button id="playBtn">Start Demo Runtime</button>
                  <div id="status">Local demo package has been prepared successfully.</div>
                </div>
              </div>
              <script>
                (function () {
                  var status = document.getElementById('status');
                  var playBtn = document.getElementById('playBtn');
                  playBtn.addEventListener('click', function () {
                    status.textContent = 'Demo runtime is running.';
                    if (window.wx && typeof window.wx.showToast === 'function') {
                      window.wx.showToast({ title: 'Runtime Ready', icon: 'none' });
                    }
                  });
                })();
              </script>
            </body>
            </html>
        """.trimIndent()
        File(targetDir, "index.html").writeText(html, Charsets.UTF_8)
    }

    private fun defaultSDKStub(): String {
        return """
            ;(function () {
              if (window.wx) return;
              var callbacks = {};
              window.NexusBridgeCallback = window.NexusBridgeCallback || function (response) {
                if (!response || !response.callbackId) return;
                var cb = callbacks[response.callbackId];
                if (!cb) return;
                delete callbacks[response.callbackId];
                if (response.error) {
                  cb.reject(response.error);
                } else {
                  cb.resolve(response.data || {});
                }
              };
              function call(api, params) {
                return new Promise(function (resolve, reject) {
                  if (!window.AndroidApp || !window.AndroidApp.postMessage) {
                    resolve({ errMsg: api + ':ok' });
                    return;
                  }
                  var callbackId = 'cb_' + Date.now() + '_' + Math.random().toString(16).slice(2);
                  callbacks[callbackId] = { resolve: resolve, reject: reject };
                  window.AndroidApp.postMessage(JSON.stringify({
                    api: api,
                    params: params || {},
                    callbackId: callbackId
                  }));
                });
              }
              function ok(api) {
                return Promise.resolve({ errMsg: api + ':ok' });
              }
              window.wx = {
                request: function (params) { return call('wx.request', params); },
                login: function (params) { return call('wx.login', params); },
                showToast: function (params) { return call('wx.showToast', params); },
                showModal: function (params) { return call('wx.showModal', params); },
                setStorage: function (params) { return call('wx.setStorage', params); },
                getStorage: function (params) { return call('wx.getStorage', params); },
                removeStorage: function (params) { return call('wx.removeStorage', params); },
                clearStorage: function () { return call('wx.clearStorage', {}); },
                vibrateShort: function () { return call('wx.vibrateShort', {}); },
                vibrateLong: function () { return call('wx.vibrateLong', {}); },
                navigateToMiniProgram: function () { return ok('wx.navigateToMiniProgram'); },
                getSystemInfoSync: function () {
                  if (window.AndroidAppSync && window.AndroidAppSync.invokeSync) {
                    try {
                      var raw = window.AndroidAppSync.invokeSync(JSON.stringify({
                        api: 'wx.getSystemInfoSync',
                        params: {},
                        callbackId: 'sync'
                      }));
                      var parsed = JSON.parse(raw || '{}');
                      return parsed.data || {};
                    } catch (e) {
                      return {};
                    }
                  }
                  return {};
                }
              };
            })();
        """.trimIndent()
    }

    private fun deleteDirectory(file: File) {
        if (file.exists()) {
            file.deleteRecursively()
        }
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun normalizeDownloadUrl(rawUrl: String): String {
        return rawUrl
            .replace("http://localhost", "http://10.0.2.2")
            .replace("https://localhost", "https://10.0.2.2")
            .replace("http://127.0.0.1", "http://10.0.2.2")
            .replace("https://127.0.0.1", "https://10.0.2.2")
    }

    private fun calculateMD5(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        val digest = md.digest()
        return digest.joinToString("") { "%02x".format(it) }
    }
}
