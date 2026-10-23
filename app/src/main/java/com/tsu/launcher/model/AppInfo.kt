package com.tsu.launcher.model

import android.graphics.drawable.Drawable

/**
 * Launcher 中的一个可启动应用项。
 */
data class AppInfo(
    val packageName: String,
    val className: String,
    val label: String,
    val icon: Drawable
)
