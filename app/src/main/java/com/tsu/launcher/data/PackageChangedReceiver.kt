package com.tsu.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tsu.launcher.util.IconCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用安装 / 卸载 / 更新后刷新应用列表并通知界面。
 */
class PackageChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val changedPackage = intent.data?.schemeSpecificPart
        if (!changedPackage.isNullOrEmpty()) {
            IconCache.removePackage(changedPackage)
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            try {
                AppRepository.reload(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
