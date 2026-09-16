package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ApkExportHelper {

    const val PROJECT_APK_PATH = "app/build/outputs/apk/debug/app-debug.apk"

    /**
     * Shares the current application APK file via Android's share intent so the user can
     * save or export it to Google Drive, WhatsApp, Files, or download it.
     */
    fun shareAppApk(context: Context) {
        try {
            val sourceApk = File(context.applicationInfo.sourceDir)
            if (!sourceApk.exists()) {
                Toast.makeText(context, "APK source not found on device", Toast.LENGTH_LONG).show()
                return
            }

            val targetDir = File(context.cacheDir, "apk_export")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val targetApk = File(targetDir, "BiteMart_Admin_Debug.apk")

            FileInputStream(sourceApk).use { input ->
                FileOutputStream(targetApk).use { output ->
                    input.copyTo(output)
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetApk
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                putExtra(Intent.EXTRA_SUBJECT, "BiteMart Admin & Operations APK")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "BiteMart Android APK containing Central Admin Portal, Delivery Partner, Restaurant Partner, and Customer apps."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Download / Export BiteMart APK")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error exporting APK: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
