package com.example.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionHelper {
    private const val PREFS_NAME = "aurora_permissions_pref"
    private const val KEY_PERMISSION_PROMPTED = "has_prompted_audio_permission"

    fun getRequiredAudioPermission(): String {
        return if (Build.VERSION.SDK_INT >= 33) {
            "android.permission.READ_MEDIA_AUDIO"
        } else {
            "android.permission.READ_EXTERNAL_STORAGE"
        }
    }

    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            getRequiredAudioPermission()
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBeenPrompted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PERMISSION_PROMPTED, false)
    }

    fun markPrompted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PERMISSION_PROMPTED, true)
            .apply()
    }
}
