package com.tsu.launcher.ui.home

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.tsu.launcher.R
import com.tsu.launcher.data.AppRepository
import com.tsu.launcher.data.LauncherPreferences
import com.tsu.launcher.data.MAX_DOCK
import com.tsu.launcher.data.PackageChangedReceiver
import com.tsu.launcher.databinding.ActivityHomeBinding
import com.tsu.launcher.model.AppInfo
import com.tsu.launcher.ui.apps.AllAppsActivity
import com.tsu.launcher.ui.common.AppAdapter
import com.tsu.launcher.ui.common.GridSpacingItemDecoration
import com.tsu.launcher.util.dp
import com.tsu.launcher.util.gridColumns
import com.tsu.launcher.util.launchApp
import com.tsu.launcher.util.openAppInfo
import com.tsu.launcher.util.uninstallApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val MENU_APP_INFO = 1
private const val MENU_ADD_TO_DOCK = 2
private const val MENU_REMOVE_FROM_DOCK = 3
private const val MENU_UNINSTALL = 4

private const val ROWS_PER_PAGE = 4

/**
 * Launcher 主桌面。
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var workspaceAdapter: WorkspacePagerAdapter
    private lateinit var dockAdapter: AppAdapter

    private val scope = MainScope()
    private val appsChanged: () -> Unit = { loadApps() }
    private val packageReceiver = PackageChangedReceiver()

    private var dockPackages: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWorkspace()
        setupDock()

        binding.btnAllApps.setOnClickListener { openAllApps() }
        binding.swipeContainer.onSwipeUp = { openAllApps() }
        binding.swipeContainer.onLongPress = { chooseWallpaper() }

        loadApps()
    }

    override fun onStart() {
        super.onStart()
        AppRepository.addListener(appsChanged)
        ContextCompat.registerReceiver(
            this,
            packageReceiver,
            packageIntentFilter(),
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

    override fun onBackPressed() {
        // Launcher 主页不响应返回键，避免出现黑屏
        val pager = binding.pager
        if (pager.currentItem > 0) {
            pager.currentItem = 0
        }
    }

    private fun setupWorkspace() {
        val columns = gridColumns()
        workspaceAdapter = WorkspacePagerAdapter(
            columns = columns,
            rows = ROWS_PER_PAGE,
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app, view ->
                showAppMenu(app, view)
                true
            }
        )
        binding.pager.adapter = workspaceAdapter
        binding.pager.offscreenPageLimit = 1
        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
            }
        })
    }

    private fun setupDock() {
        dockAdapter = AppAdapter(
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app, view ->
                showAppMenu(app, view)
                true
            }
        )
        binding.dock.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, MAX_DOCK)
            adapter = dockAdapter
            isNestedScrollingEnabled = false
            addItemDecoration(GridSpacingItemDecoration(MAX_DOCK, 6.dp(this@HomeActivity), false))
        }
    }

    private fun loadApps() {
        scope.launch {
            val apps = AppRepository.loadApps(this@HomeActivity)
            dockPackages = resolveDockPackages(apps)

            val dockApps = dockPackages.mapNotNull { pkg ->
                apps.firstOrNull { it.packageName == pkg }
            }
            val homeApps = apps.filterNot { it.packageName in dockPackages }

            dockAdapter.submitList(dockApps)
            workspaceAdapter.submitPages(homeApps)

            buildDots(workspaceAdapter.pageCount)
            binding.emptyHome.visibility = if (homeApps.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun resolveDockPackages(apps: List<AppInfo>): List<String> {
        val saved = LauncherPreferences.dockPackages(this)
            .filter { pkg -> apps.any { it.packageName == pkg } }
        if (saved.isNotEmpty()) return saved

        val defaults = AppRepository.resolveDefaultDock(this, apps)
        if (defaults.isNotEmpty()) {
            LauncherPreferences.saveDockPackages(this, defaults)
        }
        return defaults
    }

    private fun buildDots(count: Int) {
        binding.indicator.removeAllViews()
        if (count <= 1) return

        val size = resources.getDimensionPixelSize(R.dimen.indicator_size)
        repeat(count) {
            val dot = ImageView(this).apply {
                setImageResource(R.drawable.dot_inactive)
            }
            val params = LinearLayout.LayoutParams(size, size).apply {
                leftMargin = size / 2
                rightMargin = size / 2
            }
            binding.indicator.addView(dot, params)
        }
        updateDots(binding.pager.currentItem)
    }

    private fun updateDots(selected: Int) {
        for (i in 0 until binding.indicator.childCount) {
            val dot = binding.indicator.getChildAt(i) as? ImageView ?: continue
            dot.setImageResource(
                if (i == selected) R.drawable.dot_active else R.drawable.dot_inactive
            )
        }
    }

    private fun showAppMenu(app: AppInfo, anchor: View) {
        val popup = PopupMenu(this, anchor)
        val inDock = app.packageName in dockPackages

        popup.menu.add(Menu.NONE, MENU_APP_INFO, 0, R.string.action_app_info)
        if (inDock) {
            popup.menu.add(Menu.NONE, MENU_REMOVE_FROM_DOCK, 1, R.string.action_remove_from_dock)
        } else {
            popup.menu.add(Menu.NONE, MENU_ADD_TO_DOCK, 1, R.string.action_add_to_dock)
        }
        popup.menu.add(Menu.NONE, MENU_UNINSTALL, 2, R.string.action_uninstall)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_APP_INFO -> openAppInfo(app.packageName)
                MENU_UNINSTALL -> uninstallApp(app.packageName)
                MENU_ADD_TO_DOCK -> addToDock(app)
                MENU_REMOVE_FROM_DOCK -> removeFromDock(app)
            }
            true
        }
        popup.show()
    }

    private fun addToDock(app: AppInfo) {
        val current = LauncherPreferences.dockPackages(this).toMutableList()
        if (current.size >= MAX_DOCK) {
            Toast.makeText(this, getString(R.string.dock_is_full, MAX_DOCK), Toast.LENGTH_SHORT).show()
            return
        }
        if (!current.contains(app.packageName)) {
            current.add(app.packageName)
            LauncherPreferences.saveDockPackages(this, current)
            loadApps()
        }
    }

    private fun removeFromDock(app: AppInfo) {
        val current = LauncherPreferences.dockPackages(this).toMutableList()
        if (current.remove(app.packageName)) {
            LauncherPreferences.saveDockPackages(this, current)
            loadApps()
        }
    }

    private fun openAllApps() {
        val options = ActivityOptionsCompat.makeCustomAnimation(
            this,
            R.anim.slide_in_up,
            R.anim.slide_out_down
        )
        startActivity(Intent(this, AllAppsActivity::class.java), options.toBundle())
    }

    private fun chooseWallpaper() {
        val intent = Intent(Intent.ACTION_SET_WALLPAPER)
        startActivity(Intent.createChooser(intent, getString(R.string.action_wallpaper)))
    }

    private fun packageIntentFilter(): IntentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addAction(Intent.ACTION_PACKAGE_REPLACED)
        addAction(Intent.ACTION_PACKAGE_CHANGED)
        addDataScheme("package")
    }
}
