package com.tsu.launcher.data

import android.content.Context

/**
 * Dock 最多放置的应用数量。
 */
internal const val MAX_DOCK = 5

/**
 * 底部 Dock 之类的简单偏好，使用 SharedPreferences 持久化。
 */
object LauncherPreferences {

    private const val NAME = "launcher_prefs"
    private const val KEY_DOCK = "dock_packages"

    private fun prefs(context: Context) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun dockPackages(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_DOCK, "").orEmpty()
        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(MAX_DOCK)
    }

    fun saveDockPackages(context: Context, packages: List<String>) {
        prefs(context)
            .edit()
            .putString(KEY_DOCK, packages.take(MAX_DOCK).joinToString(","))
            .apply()
    }
}
