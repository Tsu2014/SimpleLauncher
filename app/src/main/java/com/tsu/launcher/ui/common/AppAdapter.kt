package com.tsu.launcher.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tsu.launcher.databinding.ItemAppBinding
import com.tsu.launcher.model.AppInfo

/**
 * 桌面图标 / Dock / 抽屉共用的应用网格适配器。
 */
class AppAdapter(
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo, View) -> Boolean
) : ListAdapter<AppInfo, AppAdapter.AppViewHolder>(Diff) {

    private object Diff : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
            oldItem.label == newItem.label && oldItem.icon === newItem.icon
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position), onAppClick, onAppLongClick)
    }

    class AppViewHolder(
        private val binding: ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            app: AppInfo,
            onAppClick: (AppInfo) -> Unit,
            onAppLongClick: (AppInfo, View) -> Boolean
        ) {
            binding.appIcon.setImageDrawable(app.icon)
            binding.appLabel.text = app.label
            binding.root.setOnClickListener { onAppClick(app) }
            binding.root.setOnLongClickListener { onAppLongClick(app, it) }
        }
    }
}
