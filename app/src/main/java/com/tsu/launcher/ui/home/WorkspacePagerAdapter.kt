package com.tsu.launcher.ui.home

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tsu.launcher.model.AppInfo
import com.tsu.launcher.ui.common.AppAdapter
import com.tsu.launcher.ui.common.GridSpacingItemDecoration
import com.tsu.launcher.util.dp

/**
 * 桌面分页：每一页是一个不可滚动的应用网格。
 */
class WorkspacePagerAdapter(
    private val columns: Int,
    private val rows: Int,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo, View) -> Boolean
) : RecyclerView.Adapter<WorkspacePagerAdapter.PageViewHolder>() {

    private var pages: List<List<AppInfo>> = emptyList()

    val pageCount: Int
        get() = pages.size

    private val pageSize: Int
        get() = (columns * rows).coerceAtLeast(1)

    fun submitPages(apps: List<AppInfo>) {
        pages = if (apps.isEmpty()) emptyList() else apps.chunked(pageSize)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val context = parent.context
        val recyclerView = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, columns)
            isNestedScrollingEnabled = false
            clipToPadding = false
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            addItemDecoration(GridSpacingItemDecoration(columns, 8.dp(context)))
        }
        val adapter = AppAdapter(onAppClick, onAppLongClick)
        recyclerView.adapter = adapter
        return PageViewHolder(recyclerView, adapter)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.adapter.submitList(pages[position])
    }

    class PageViewHolder(
        val recyclerView: RecyclerView,
        val adapter: AppAdapter
    ) : RecyclerView.ViewHolder(recyclerView)
}
