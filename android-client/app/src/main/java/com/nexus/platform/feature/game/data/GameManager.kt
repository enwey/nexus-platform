package com.nexus.platform.feature.game.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.core.network.BackendConfig
import com.nexus.platform.utils.ZipUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class GameManager(private val context: Context) {
    private companion object {
        const val TAG = "GameManager"
    }

    data class UpdateCheckResult(
        val hasUpdate: Boolean,
        val forceUpdate: Boolean,
        val ready: Boolean,
        val latestVersion: String?,
        val downloadUrl: String?,
        val md5: String?,
        val errMsg: String
    )

    data class PreparedGame(
        val rootDir: File,
        val entryRelativePath: String,
        val fromCache: Boolean
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
    private val gson = Gson()
    private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val updateStateMap = ConcurrentHashMap<String, UpdateState>()

    private data class UpdateState(
        var hasUpdate: Boolean = false,
        var forceUpdate: Boolean = false,
        var ready: Boolean = false,
        var latestVersion: String? = null,
        var downloadUrl: String? = null,
        var md5: String? = null,
        var inProgress: Boolean = false,
        var failed: Boolean = false
    )

    fun getGameDir(gameId: String): File {
        val activeVersion = getActiveVersion(gameId) ?: return getLegacyGameDir(gameId)
        return getVersionGameDir(gameId, activeVersion)
    }

    fun isGameDownloaded(gameId: String): Boolean {
        return resolveEntryPath(getGameDir(gameId)) != null
    }

    suspend fun checkForUpdate(game: GameItem, blockOnForce: Boolean = false): UpdateCheckResult = withContext(Dispatchers.IO) {
        val state = updateStateMap.getOrPut(game.id) { UpdateState() }
        if (state.ready) {
            return@withContext UpdateCheckResult(
                hasUpdate = true,
                forceUpdate = state.forceUpdate,
                ready = true,
                latestVersion = state.latestVersion,
                downloadUrl = state.downloadUrl,
                md5 = state.md5,
                errMsg = "update.check:ok"
            )
        }
        if (state.inProgress) {
            return@withContext UpdateCheckResult(
                hasUpdate = true,
                forceUpdate = state.forceUpdate,
                ready = false,
                latestVersion = state.latestVersion,
                downloadUrl = state.downloadUrl,
                md5 = state.md5,
                errMsg = "update.check:ok"
            )
        }

        val appId = game.id
        val localVersion = getActiveVersion(game.id) ?: game.version.ifBlank { "0.0.0" }
        val checkUrl = "${BackendConfig.apiBaseUrl}/game/check-update?appId=$appId&localVersion=$localVersion"
        val request = Request.Builder().url(checkUrl).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult(false, false, false, null, null, null, "update.check:fail")
                }
                val body = response.body?.string().orEmpty()
                val payload = gson.fromJson(body, JsonObject::class.java)
                if (payload?.get("code")?.asInt != 0) {
                    return@withContext UpdateCheckResult(false, false, false, null, null, null, "update.check:fail")
                }
                val data = payload.getAsJsonObject("data")
                if (data == null) {
                    state.hasUpdate = false
                    return@withContext UpdateCheckResult(false, false, false, null, null, null, "update.check:ok")
                }
                val hasUpdate = data.get("hasUpdate")?.asBoolean ?: false
                val forceUpdate = data.get("forceUpdate")?.asBoolean ?: false
                val latestVersion = data.get("latestVersion")?.asString
                val downloadUrl = normalizeDownloadUrl(data.get("downloadUrl")?.asString.orEmpty())
                val md5 = data.get("md5")?.asString.orEmpty()
                if (!hasUpdate || latestVersion.isNullOrBlank() || downloadUrl.isBlank()) {
                    state.hasUpdate = false
                    return@withContext UpdateCheckResult(false, false, false, latestVersion, null, md5, "update.check:ok")
                }

                state.hasUpdate = true
                state.forceUpdate = forceUpdate
                state.ready = false
                state.latestVersion = latestVersion
                state.downloadUrl = downloadUrl
                state.md5 = md5
                state.inProgress = true
                state.failed = false

                if (forceUpdate && blockOnForce) {
                    try {
                        downloadUpdateToStaging(game.id, latestVersion, downloadUrl, md5)
                        val applied = applyUpdate(game.id, latestVersion)
                        state.ready = applied
                        state.inProgress = false
                        return@withContext UpdateCheckResult(
                            hasUpdate = true,
                            forceUpdate = true,
                            ready = applied,
                            latestVersion = latestVersion,
                            downloadUrl = downloadUrl,
                            md5 = md5,
                            errMsg = if (applied) "update.check:ok" else "update.check:fail"
                        )
                    } catch (e: Exception) {
                        state.failed = true
                        state.inProgress = false
                        return@withContext UpdateCheckResult(
                            hasUpdate = true,
                            forceUpdate = true,
                            ready = false,
                            latestVersion = latestVersion,
                            downloadUrl = downloadUrl,
                            md5 = md5,
                            errMsg = "update.check:fail"
                        )
                    }
                }

                updateScope.launch {
                    try {
                        downloadUpdateToStaging(game.id, latestVersion, downloadUrl, md5)
                        state.ready = true
                    } catch (e: Exception) {
                        state.failed = true
                        Log.e(TAG, "update download failed id=${game.id} msg=${e.message}")
                    } finally {
                        state.inProgress = false
                    }
                }

                return@withContext UpdateCheckResult(true, forceUpdate, false, latestVersion, downloadUrl, md5, "update.check:ok")
            }
        } catch (e: Exception) {
            return@withContext UpdateCheckResult(false, false, false, null, null, null, "update.check:fail")
        }
    }

    suspend fun applyUpdate(gameId: String, targetVersion: String? = null): Boolean = withContext(Dispatchers.IO) {
        val state = updateStateMap[gameId] ?: return@withContext false
        if (!state.ready && targetVersion == null) {
            return@withContext false
        }
        val version = targetVersion ?: state.latestVersion ?: return@withContext false
        val gameDir = getVersionGameDir(gameId, version)
        val stagingDir = getStagingGameDir(gameId, version)
        if (!stagingDir.exists()) {
            return@withContext false
        }
        val backupDir = File(gameDir.parentFile, "${version}_backup")
        deleteDirectory(backupDir)
        if (gameDir.exists()) {
            gameDir.renameTo(backupDir)
        }
        if (!stagingDir.renameTo(gameDir)) {
            deleteDirectory(gameDir)
            stagingDir.copyRecursively(gameDir, overwrite = true)
            deleteDirectory(stagingDir)
        }
        deleteDirectory(backupDir)
        setActiveVersion(gameId, version)
        cleanupOldVersions(gameId, version)
        updateStateMap.remove(gameId)
        true
    }

    suspend fun prepareGame(game: GameItem, forceRefresh: Boolean = false): PreparedGame {
        return withContext(Dispatchers.IO) {
            val targetVersion = game.version.ifBlank { "0.0.0" }
            val gameDir = getVersionGameDir(game.id, targetVersion)
            val cachedEntry = resolveEntryPath(gameDir)
            if (!forceRefresh && cachedEntry != null) {
                setActiveVersion(game.id, targetVersion)
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
                val fallbackDir = getGameDir(game.id)
                val fallbackEntry = resolveEntryPath(fallbackDir)
                if (!forceRefresh && fallbackEntry != null) {
                    return@withContext PreparedGame(
                        rootDir = fallbackDir,
                        entryRelativePath = fallbackEntry,
                        fromCache = true
                    )
                }
                throw e
            }

            val entryPath = resolveEntryPath(gameDir)
                ?: throw IOException("Game entry not found: index.html")
            setActiveVersion(game.id, targetVersion)
            cleanupOldVersions(game.id, targetVersion)
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
            Log.d(TAG, "downloadGame start id=${game.id} url=$normalizedUrl")

            deleteDirectory(targetDir)
            targetDir.mkdirs()

            val zipFile = downloadPackageWithRedirects(game.id, normalizedUrl)

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

    private fun downloadPackageWithRedirects(gameId: String, initialUrl: String): File {
        var currentUrl = initialUrl
        var redirectCount = 0
        while (redirectCount < 5) {
            Log.d(TAG, "download try id=$gameId redirect=$redirectCount url=$currentUrl")
            val request = Request.Builder().url(currentUrl).build()
            client.newCall(request).execute().use { response ->
                if (response.isRedirect) {
                    val location = response.header("Location").orEmpty()
                    if (location.isBlank()) {
                        throw IOException("Game download redirect missing location")
                    }
                    currentUrl = normalizeDownloadUrl(location).trim()
                    Log.d(TAG, "download redirect id=$gameId location=$location normalized=$currentUrl")
                    redirectCount++
                    return@use
                }

                if (!response.isSuccessful) {
                    Log.e(TAG, "download failed id=$gameId http=${response.code} url=$currentUrl")
                    throw IOException("Game download failed: ${response.code}")
                }

                val zipFile = File(context.cacheDir, "${gameId}_${System.currentTimeMillis()}.zip")
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(zipFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IOException("Game package is empty")
                Log.d(TAG, "download success id=$gameId file=${zipFile.absolutePath}")
                return zipFile
            }
        }
        Log.e(TAG, "download failed id=$gameId reason=too_many_redirects")
        throw IOException("Game download failed: too many redirects")
    }

    private fun downloadUpdateToStaging(gameId: String, version: String, downloadUrl: String, md5: String) {
        val stagingDir = getStagingGameDir(gameId, version)
        deleteDirectory(stagingDir)
        stagingDir.mkdirs()
        val zipFile = downloadPackageWithRedirects("${gameId}_ota", downloadUrl)
        val actualMd5 = calculateMD5(zipFile)
        if (md5.isNotBlank() && !actualMd5.equals(md5, ignoreCase = true)) {
            zipFile.delete()
            throw IOException("Update package checksum mismatch")
        }
        ZipUtils.unzip(zipFile, stagingDir)
        zipFile.delete()
        normalizeExtractedStructure(stagingDir)
    }

    private fun getStagingGameDir(gameId: String, version: String): File {
        val root = getGameRootDir(gameId)
        if (!root.exists()) {
            root.mkdirs()
        }
        return File(root, "staging/${safeVersion(version)}")
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

            ""
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
              function createUpdateManager() {
                var checkedCallbacks = [];
                var readyCallbacks = [];
                var failedCallbacks = [];
                var state = { hasUpdate: false, ready: false };
                function emit(list, payload) {
                  list.forEach(function (fn) {
                    try { fn(payload); } catch (e) {}
                  });
                }
                function pollReady() {
                  if (!state.hasUpdate || state.ready) return;
                  setTimeout(function () {
                    call('wx.update.check', {}).then(function (res) {
                      state = {
                        hasUpdate: !!(res && res.hasUpdate),
                        ready: !!(res && res.ready)
                      };
                      if (state.ready) {
                        emit(readyCallbacks, {});
                      } else if (state.hasUpdate) {
                        pollReady();
                      }
                    }).catch(function () {
                      emit(failedCallbacks, {});
                    });
                  }, 1500);
                }
                setTimeout(function () {
                  call('wx.update.check', {}).then(function (res) {
                    state = {
                      hasUpdate: !!(res && res.hasUpdate),
                      ready: !!(res && res.ready)
                    };
                    emit(checkedCallbacks, { hasUpdate: state.hasUpdate });
                    if (state.ready) {
                      emit(readyCallbacks, {});
                    } else if (state.hasUpdate) {
                      pollReady();
                    }
                  }).catch(function () {
                    emit(checkedCallbacks, { hasUpdate: false });
                    emit(failedCallbacks, {});
                  });
                }, 0);

                return {
                  onCheckForUpdate: function (cb) { if (typeof cb === 'function') checkedCallbacks.push(cb); },
                  onUpdateReady: function (cb) { if (typeof cb === 'function') readyCallbacks.push(cb); },
                  onUpdateFailed: function (cb) { if (typeof cb === 'function') failedCallbacks.push(cb); },
                  applyUpdate: function () { return call('wx.update.apply', {}); }
                };
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
                },
                getMenuButtonBoundingClientRect: function () {
                  if (window.AndroidAppSync && window.AndroidAppSync.invokeSync) {
                    try {
                      var raw = window.AndroidAppSync.invokeSync(JSON.stringify({
                        api: 'wx.getMenuButtonBoundingClientRect',
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
                },
                getUpdateManager: function () { return createUpdateManager(); }
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
        if (rawUrl.startsWith("/")) {
            return BackendConfig.localHttpHost + rawUrl
        }
        val isPresigned = rawUrl.contains("X-Amz-Signature=", ignoreCase = true)
        return runCatching {
            val url = rawUrl.toHttpUrlOrNull() ?: return@runCatching rawUrl
            val host = url.host.lowercase()
            if (host == "localhost" || host == "127.0.0.1" || host == "::1") {
                if (isPresigned) {
                    return@runCatching rawUrl
                }
                url.newBuilder()
                    .host(BackendConfig.localHost)
                    .build()
                    .toString()
            } else {
                rawUrl
            }
        }.getOrElse {
            rawUrl
                .replace("http://localhost", "http://${BackendConfig.localHost}")
                .replace("https://localhost", "https://${BackendConfig.localHost}")
                .replace("http://127.0.0.1", "http://${BackendConfig.localHost}")
                .replace("https://127.0.0.1", "https://${BackendConfig.localHost}")
        }
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

    private fun compareVersion(left: String, right: String): Int {
        val l = left.split(".")
        val r = right.split(".")
        val size = maxOf(l.size, r.size)
        for (i in 0 until size) {
            val lv = l.getOrNull(i)?.toIntOrNull() ?: 0
            val rv = r.getOrNull(i)?.toIntOrNull() ?: 0
            if (lv != rv) {
                return lv - rv
            }
        }
        return 0
    }

    private fun getGameRootDir(gameId: String): File {
        val gamesDir = File(context.filesDir, "games")
        if (!gamesDir.exists()) {
            gamesDir.mkdirs()
        }
        return File(gamesDir, gameId)
    }

    private fun getLegacyGameDir(gameId: String): File {
        return File(File(context.filesDir, "games"), gameId)
    }

    private fun getVersionsRootDir(gameId: String): File {
        val versionsRoot = File(getGameRootDir(gameId), "versions")
        if (!versionsRoot.exists()) {
            versionsRoot.mkdirs()
        }
        return versionsRoot
    }

    private fun getVersionGameDir(gameId: String, version: String): File {
        return File(getVersionsRootDir(gameId), safeVersion(version))
    }

    private fun getActiveVersionFile(gameId: String): File {
        return File(getGameRootDir(gameId), "active_version.txt")
    }

    private fun getActiveVersion(gameId: String): String? {
        val activeFile = getActiveVersionFile(gameId)
        if (!activeFile.exists()) return null
        return activeFile.readText().trim().ifBlank { null }
    }

    private fun setActiveVersion(gameId: String, version: String) {
        val activeFile = getActiveVersionFile(gameId)
        val parent = activeFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        activeFile.writeText(version)
    }

    private fun cleanupOldVersions(gameId: String, keepVersion: String) {
        val versionsRoot = getVersionsRootDir(gameId)
        versionsRoot.listFiles()
            ?.filter { it.isDirectory && !it.name.equals(safeVersion(keepVersion), ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(1)
            ?.forEach { deleteDirectory(it) }
    }

    private fun safeVersion(version: String): String {
        return version.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}
