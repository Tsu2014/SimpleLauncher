package com.tsu.launcher.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * 手势容器：
 * - 上滑：由 onSwipeUp 回调（桌面 -> 抽屉）
 * - 下滑：由 onSwipeDown 回调（抽屉 -> 桌面）
 * - 空白处长按：由 onLongPress 回调（由框架自身的长按检测触发）
 *
 * 仅当判定为纵向手势且子 View 不需要纵向滚动时才拦截事件，
 * 因此 ViewPager2 横滑翻页、RecyclerView 纵向滚动、图标点击均不受影响。
 */
class SwipeGestureContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onSwipeUp: (() -> Unit)? = null
    var onSwipeDown: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null

    /** 子 View 是否还能继续滚动（true 时把手势让给子 View） */
    var childCanScrollVertically: (() -> Boolean)? = null

    private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private val minDistance: Int = (56 * context.resources.displayMetrics.density).toInt()

    private var dragging = false
    private var longPressed = false
    private var swipeHandled = false
    private var startX = 0f
    private var startY = 0f

    init {
        isLongClickable = true
        setOnLongClickListener {
            longPressed = true
            onLongPress?.invoke()
            true
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
                dragging = false
                longPressed = false
                swipeHandled = false
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = startY - ev.y
                val dx = startX - ev.x
                val verticalEnough = abs(dy) > touchSlop && abs(dy) > abs(dx)
                val childNeedsScroll = childCanScrollVertically?.invoke() == true
                if (!dragging && verticalEnough && !childNeedsScroll) {
                    dragging = true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                detectSwipe(ev)
                dragging = false
            }
        }
        return dragging
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 交给框架的触摸处理，这样才能触发长按回调
        val consumed = super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                detectSwipe(event)
                dragging = false
            }
        }
        return consumed || dragging
    }

    private fun detectSwipe(event: MotionEvent) {
        if (swipeHandled || longPressed) return
        swipeHandled = true

        val dy = startY - event.y
        val dx = startX - event.x
        if (abs(dy) < minDistance || abs(dy) < abs(dx)) return

        if (dy > 0) onSwipeUp?.invoke() else onSwipeDown?.invoke()
    }
}
