package com.tsu.launcher.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * 支持上下滑动手势的容器，用于「上滑打开抽屉」「下滑关闭抽屉」。
 * 只有判定为纵向手势时才会拦截事件，横向翻页与点击仍交给子 View。
 */
class SwipeGestureContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onSwipeUp: (() -> Unit)? = null
    var onSwipeDown: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null

    private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private val minDistance: Int = (48 * context.resources.displayMetrics.density).toInt()

    private val listener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onLongPress(e: MotionEvent) {
            onLongPress?.invoke()
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val dy = e2.y - e1.y
            val dx = e2.x - e1.x
            if (abs(dy) < abs(dx) || abs(dy) < minDistance) return false

            if (dy < 0) onSwipeUp?.invoke() else onSwipeDown?.invoke()
            return true
        }
    }

    private val detector = GestureDetector(context, listener)

    private var dragging = false
    private var startX = 0f
    private var startY = 0f

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
                dragging = false
                detector.onTouchEvent(ev)
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = startY - ev.y
                val dx = startX - ev.x
                if (!dragging && abs(dy) > touchSlop && abs(dy) > abs(dx)) {
                    dragging = true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                detector.onTouchEvent(ev)
            }
        }
        return dragging
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        detector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragging = false
        }
        return true
    }
}
