package com.tsu.launcher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.tsu.launcher.model.AppInfo
import com.tsu.launcher.util.IconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 应用列表数据仓库：读取已安装应用、维护变更监听。
 */
object AppRepository {

    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    @Volatile
    private var cached: List<AppInfo> = emptyList()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun cachedApps(): List<AppInfo> = cached

    /** 读取应用列表（优先内存缓存），不通知监听器 */
    suspend fun loadApps(context: Context): List<AppInfo> {
        val memory = cached
        if (memory.isNotEmpty()) return memory
        val fresh = query(context)
        cached = fresh
        return fresh
    }

    /** 强制刷新并通知所有监听器 */
    suspend fun reload(context: Context) {
        val fresh = query(context)
        cached = fresh
        withContext(Dispatchers.Main) {
            listeners.forEach { it.invoke() }
        }
    }

    private fun query(context: Context): List<AppInfo> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val selfPackage = context.packageName
        val resolveInfos = packageManager.queryIntentActivities(launcherIntent, 0)
        val collator = Collator.getInstance()
        val seen = HashSet<String>()
        val apps = ArrayList<AppInfo>(resolveInfos.size)

        for (info in resolveInfos) {
            val activityInfo = info.activityInfo ?: continue
            val packageName = activityInfo.packageName ?: continue
            val className = activityInfo.name ?: continue
            if (packageName == selfPackage || !seen.add(packageName)) continue

            val label = info.loadLabel(packageManager).toString().ifEmpty { packageName }
            val cacheKey = "$packageName/$className"
            val icon = IconCache.get(cacheKey)
                ?: info.loadIcon(packageManager).also { IconCache.put(cacheKey, it) }

            apps.add(AppInfo(packageName, className, label, icon))
        }

        apps.sortWith(compareBy(collator) { it.label })
        return apps
    }

    /**
     * 首次使用时挑选一组系统常用应用作为默认 Dock，
     * 找不到则退化为应用列表前几个。
     */
    fun resolveDefaultDock(context: Context, apps: List<AppInfo>): List<String> {
        val packageManager = context.packageManager
        val probes = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(Intent.ACTION_VIEW, Uri.parse("http://")),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        )

        val result = LinkedHashSet<String>()
        probes.forEach { probe ->
            val pkg = packageManager.resolveActivity(probe, 0)?.activityInfo?.packageName
            if (pkg != null &&
                pkg != context.packageName &&
                apps.any { it.packageName == pkg }
            ) {
                result.add(pkg)
            }
        }

        apps.forEach { app ->
            if (result.size >= MAX_DOCK) return@forEach
            result.add(app.packageName)
        }

        return result.take(MAX_DOCK)
    }
}
