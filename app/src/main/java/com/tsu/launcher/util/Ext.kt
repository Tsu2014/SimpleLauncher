package com.tsu.launcher.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.tsu.launcher.R
import com.tsu.launcher.model.AppInfo

fun Int.dp(context: Context): Int =
    (this * context.resources.displayMetrics.density + 0.5f).toInt()

/** 依据屏幕宽度计算网格列数 */
fun Context.gridColumns(): Int =
    (resources.configuration.screenWidthDp / 90).coerceIn(4, 6)

/** 启动一个应用 */
fun Context.launchApp(app: AppInfo) {
    val intent = packageManager.getLaunchIntentForPackage(app.packageName)
        ?: Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(app.packageName, app.className)

    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, R.string.toast_cannot_open, Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        Toast.makeText(this, R.string.toast_cannot_open, Toast.LENGTH_SHORT).show()
    }
}

/** 打开系统「应用信息」页面 */
fun Context.openAppInfo(packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, R.string.toast_cannot_open, Toast.LENGTH_SHORT).show()
    }
}

/** 拉起系统卸载流程 */
fun Context.uninstallApp(packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE)
        .setData(Uri.parse("package:$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, R.string.toast_cannot_open, Toast.LENGTH_SHORT).show()
    }
}
