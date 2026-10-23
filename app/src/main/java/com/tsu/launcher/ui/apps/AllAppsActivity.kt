package com.tsu.launcher.ui.apps

import android.content.IntentFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.tsu.launcher.R
import com.tsu.launcher.data.AppRepository
import com.tsu.launcher.data.PackageChangedReceiver
import com.tsu.launcher.databinding.ActivityAllAppsBinding
import com.tsu.launcher.model.AppInfo
import com.tsu.launcher.ui.common.AppAdapter
import com.tsu.launcher.ui.common.GridSpacingItemDecoration
import com.tsu.launcher.util.gridColumns
import com.tsu.launcher.util.launchApp
import com.tsu.launcher.util.openAppInfo
import com.tsu.launcher.util.uninstallApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val MENU_APP_INFO = 1
private const val MENU_UNINSTALL = 2

/**
 * 全部应用抽屉。
 */
class AllAppsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllAppsBinding
    private lateinit var adapter: AppAdapter

    private val scope = MainScope()
    private val appsChanged: () -> Unit = { loadApps() }
    private val packageReceiver = PackageChangedReceiver()

    private var allApps: List<AppInfo> = emptyList()
    private var keyword: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val columns = gridColumns()
        adapter = AppAdapter(
            onAppClick = { app ->
                launchApp(app)
                finish()
            },
            onAppLongClick = { app, view ->
                showAppMenu(app, view)
                true
            }
        )

        binding.apps.apply {
            layoutManager = GridLayoutManager(this@AllAppsActivity, columns)
            this.adapter = this@AllAppsActivity.adapter
            addItemDecoration(
                GridSpacingItemDecoration(
                    columns,
                    resources.getDimensionPixelSize(R.dimen.grid_spacing)
                )
            )
            setHasFixedSize(true)
        }

        binding.swipeContainer.onSwipeDown = { finish() }
        binding.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                keyword = s?.toString().orEmpty()
                applyFilter()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        loadApps()
    }

    override fun onStart() {
        super.onStart()
        AppRepository.addListener(appsChanged)
        ContextCompat.registerReceiver(
            this,
            packageReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        try {
            unregisterReceiver(packageReceiver)
        } catch (e: IllegalArgumentException) {
            // 已取消注册，忽略
        }
        AppRepository.removeListener(appsChanged)
        super.onStop()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out_down)
    }

    private fun loadApps() {
        scope.launch {
            allApps = AppRepository.loadApps(this@AllAppsActivity)
            applyFilter()
        }
    }

    private fun applyFilter() {
        val q = keyword.trim()
        val list = if (q.isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.label.contains(q, ignoreCase = true) ||
                    it.packageName.contains(q, ignoreCase = true)
            }
        }
        adapter.submitList(list)
        binding.empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAppMenu(app: AppInfo, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(Menu.NONE, MENU_APP_INFO, 0, R.string.action_app_info)
        popup.menu.add(Menu.NONE, MENU_UNINSTALL, 1, R.string.action_uninstall)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_APP_INFO -> openAppInfo(app.packageName)
                MENU_UNINSTALL -> uninstallApp(app.packageName)
            }
            true
        }
        popup.show()
    }
}
