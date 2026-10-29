package com.dynamicisland.app.features.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkSize: Long,
    val isUpdateAvailable: Boolean
)

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Downloading(val progressPercent: Int) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

class UpdateManager(private val context: Context) {
    private val tag = "UpdateManager"
    private val repoOwner = "VIJNESH200"
    private val repoName = "Dynamic-Island"

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    fun resetStatus() {
        _status.value = UpdateStatus.Idle
    }

    suspend fun checkForUpdates(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        _status.value = UpdateStatus.Checking
        try {
            val endpoint = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "DynamicIsland-AndroidApp")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                _status.value = UpdateStatus.Error("GitHub API returned HTTP $responseCode")
                return@withContext null
            }

            val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)

            val tagName = json.optString("tag_name", "")
            val title = json.optString("name", tagName)
            val body = json.optString("body", "No release notes provided.")

            // Find APK asset
            val assets = json.optJSONArray("assets")
            var downloadUrl = ""
            var apkSize = 0L

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            val currentVersion = getCurrentVersionName()
            val isAvailable = isNewerVersion(tagName, currentVersion) && downloadUrl.isNotBlank()

            val info = AppUpdateInfo(
                tagName = tagName,
                versionName = tagName.removePrefix("v").removePrefix("V"),
                releaseTitle = title,
                releaseNotes = body,
                downloadUrl = downloadUrl,
                apkSize = apkSize,
                isUpdateAvailable = isAvailable
            )

            if (isAvailable) {
                _status.value = UpdateStatus.UpdateAvailable(info)
            } else {
                _status.value = UpdateStatus.UpToDate
            }
            return@withContext info
        } catch (e: Exception) {
            Log.e(tag, "Error checking for updates: ${e.message}", e)
            _status.value = UpdateStatus.Error("Check failed: ${e.localizedMessage}")
            return@withContext null
        }
    }

    suspend fun downloadAndInstall(info: AppUpdateInfo) = withContext(Dispatchers.IO) {
        if (info.downloadUrl.isBlank()) return@withContext

        try {
            _status.value = UpdateStatus.Downloading(0)

            val updateDir = File(context.cacheDir, "updates")
            if (!updateDir.exists()) updateDir.mkdirs()

            val targetFile = File(updateDir, "dynamic-island-${info.tagName}.apk")
            if (targetFile.exists()) targetFile.delete()

            var currentUrl = info.downloadUrl
            var redirectCount = 0
            var finalStream: java.io.InputStream? = null
            var totalLength = info.apkSize

            while (redirectCount < 5) {
                val conn = URL(currentUrl).openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 15000
                conn.readTimeout = 20000
                conn.setRequestProperty("User-Agent", "DynamicIsland-AndroidApp")

                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == 307 || code == 308
                ) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    currentUrl = location
                    redirectCount++
                    continue
                } else if (code == HttpURLConnection.HTTP_OK) {
                    val len = conn.contentLengthLong
                    if (len > 0) totalLength = len
                    finalStream = conn.inputStream
                    break
                } else {
                    throw RuntimeException("HTTP download failed with code $code")
                }
            }

            val input = finalStream ?: throw RuntimeException("Failed to open download stream")

            input.use { inStream ->
                FileOutputStream(targetFile).use { outStream ->
                    val buffer = ByteArray(16384)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastPercent = -1

                    while (inStream.read(buffer).also { bytesRead = it } != -1) {
                        outStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalLength > 0) {
                            val percent = ((totalRead * 100) / totalLength).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                _status.value = UpdateStatus.Downloading(percent)
                            }
                        }
                    }
                }
            }

            _status.value = UpdateStatus.ReadyToInstall(targetFile)

            withContext(Dispatchers.Main) {
                installApk(context, targetFile)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error downloading update: ${e.message}", e)
            _status.value = UpdateStatus.Error("Download failed: ${e.localizedMessage}")
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(context, "APK file not found", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Toast.makeText(context, "Please enable 'Allow from this source' to update Dynamic Island", Toast.LENGTH_LONG).show()
                return
            }
        }

        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(tag, "Error triggering package installer: ${e.message}", e)
            Toast.makeText(context, "Failed to launch installer: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "0.3.0"
        } catch (_: Exception) {
            "0.3.0"
        }
    }

    private fun isNewerVersion(latestTag: String, currentVersion: String): Boolean {
        val cleanLatest = latestTag.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")
        if (cleanLatest == cleanCurrent) return false

        val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until length) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
