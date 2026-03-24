package com.sharefast.data.repository

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import com.sharefast.domain.model.InstalledApp
import com.sharefast.domain.repository.AppsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context

@Singleton
class AppsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppsRepository {

    override suspend fun loadInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val apps = if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        }

        apps
            .asSequence()
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .mapNotNull { info ->
                try {
                    val label = pm.getApplicationLabel(info).toString()
                    val apkPath = info.sourceDir
                    val size = runCatching { File(apkPath).length() }.getOrDefault(0L)
                    val drawable = pm.getApplicationIcon(info.packageName)
                    InstalledApp(
                        packageName = info.packageName,
                        label = label,
                        apkPath = apkPath,
                        sizeBytes = size,
                        launcherIcon = drawable.toBitmapSafe(),
                    )
                } catch (_: Exception) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun Drawable.toBitmapSafe(): Bitmap? = try {
        if (this is BitmapDrawable && bitmap != null && !bitmap.isRecycled) {
            bitmap
        } else {
            val w = intrinsicWidth.coerceAtLeast(1)
            val h = intrinsicHeight.coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
            bmp
        }
    } catch (_: Exception) {
        null
    }
}
