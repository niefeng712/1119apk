package com.example.lotteryinfo.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String
)

object AppUpdater {
    private const val LATEST_RELEASE_URL =
        "https://" + "api" + ".github.com/repos/niefeng712/1119apk/releases/latest"

    private val client = OkHttpClient()

    suspend fun checkUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)

                val latestVersion = json.optString("tag_name", "")
                    .removePrefix("v")
                    .removePrefix("V")
                    .trim()
                if (latestVersion.isEmpty()) return@withContext null

                val releaseNotes = json.optString("body", "")
                val assets = json.optJSONArray("assets") ?: return@withContext null

                var downloadUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
                if (downloadUrl.isEmpty()) return@withContext null

                val currentVersion = try {
                    context.packageManager
                        .getPackageInfo(context.packageName, 0)
                        .versionName
                        ?: "0"
                } catch (_: Exception) {
                    "0"
                }

                if (isNewerVersion(latestVersion, currentVersion)) {
                    UpdateInfo(
                        latestVersion = latestVersion,
                        downloadUrl = downloadUrl,
                        releaseNotes = releaseNotes
                    )
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body ?: return@withContext false

                val cacheDir = context.externalCacheDir ?: context.cacheDir
                val apkFile = File(cacheDir, "update.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                var lastProgress = -1

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                val progress = ((downloadedBytes * 100) / totalBytes)
                                    .toInt()
                                    .coerceIn(0, 100)
                                if (progress != lastProgress) {
                                    lastProgress = progress
                                    withContext(Dispatchers.Main) {
                                        onProgress(progress)
                                    }
                                }
                            }
                        }
                        output.flush()
                    }
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                withContext(Dispatchers.Main) {
                    onProgress(100)
                    context.startActivity(installIntent)
                }
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteParts = remote.removePrefix("v").removePrefix("V")
            .split(".")
            .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
        val currentParts = current.removePrefix("v").removePrefix("V")
            .split(".")
            .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
