package com.tsu.launcher.util

import android.graphics.drawable.Drawable
import android.util.LruCache

/**
 * 应用图标内存缓存，避免每次刷新都重新解码 bitmap。
 */
object IconCache {

    private const val MAX_ENTRIES = 300

    private val cache = object : LruCache<String, Drawable>(MAX_ENTRIES) {
        override fun sizeOf(key: String, value: Drawable): Int = 1
    }

    @Synchronized
    fun get(key: String): Drawable? = cache.get(key)

    @Synchronized
    fun put(key: String, drawable: Drawable) {
        cache.put(key, drawable)
    }

    @Synchronized
    fun removePackage(packageName: String) {
        val prefix = "$packageName/"
        cache.snapshot().keys
            .filter { it.startsWith(prefix) }
            .forEach { cache.remove(it) }
    }

    @Synchronized
    fun clear() {
        cache.evictAll()
    }
}
