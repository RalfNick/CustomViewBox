package com.ralf.slideviewtest.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout

/**
 * @author Ralf(wanglixin)
 * DESCRIPTION
 * @name SlideLayout
 * @email -
 * @date 2019/09/01 11:39
 **/
class SlideLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    /**
     * 滑动方向
     */
    companion object {
        const val VERTICAL = 1
        const val HORIZONTAL = 2
        const val NONE = 0
    }

    /**
     * 是否使用该布局的滑动事件 true - 使用   false - 不使用
     */
    var mEnable = true
    private var mDirection = NONE
    private var originX = 0f
    private var originY = 0f
    /**
     * x,y方向是否可以滑动,默认情况下y 可以滑动，x 不可以滑动
     */
    var mEnableX = false

    /**
     * 是否允许水平方向上拦截
     */
    var mEnableInterceptX = false

    /**
     * y 轴向上还是向下 true - 向上  false - 向下
     * x 轴向左还是向右 true - 向右  false - 向左
     */
    private var isScrollingUp = true
    private var isScrollingRight = true
    /**
     * 滑动的阈值，达到一定值后自动向前执行，未达到则恢复原位
     */
    var yThreshold = 0f
    var xThreshold = 0f

    /**
     * 布局滑动监听
     */
    var mSlideScrollListener: SlideScrollListener? = null


    /**
     * 竖直方向上拦截
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // 开关关闭，不启用滑动效果
        if (!mEnable) {
            return super.onInterceptTouchEvent(ev)
        }
        val x = ev.rawX
        val y = ev.rawY
        var intercepted = false
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                originX = ev.rawX
                originY = ev.rawY
                intercepted = false
            }
            MotionEvent.ACTION_MOVE -> {
                val delX = Math.abs(x - originX)
                val delY = Math.abs(y - originY)
                val result = delY - delX
                mDirection = if (result > 0) VERTICAL else if (result < 0) HORIZONTAL else NONE
                intercepted = when (mDirection) {
                    VERTICAL -> true
                    HORIZONTAL -> mEnableInterceptX
                    else -> false
                }
            }
            MotionEvent.ACTION_UP -> {
                intercepted = false
            }
        }
        return intercepted
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!mEnable) {
            return super.onTouchEvent(ev)
        }
        val x = ev.rawX
        val y = ev.rawY
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val delY = y - originY
                val delX = x - originX
                isScrollingUp = delY > 0
                isScrollingRight = delX > 0
                translationY = delY
                translationX = if (mEnableX) delX else 0.0f
                setBackgroundAlpha()
                mSlideScrollListener?.hideHeaderAndFooter()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                when (mDirection) {
                    VERTICAL -> {
                        if (Math.abs(translationY) > height * yThreshold) {
                            mSlideScrollListener?.onEndDrag(translationY / height)
                            var end = if (isScrollingUp) height else -height
                            if (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                                end = if (isScrollingUp) getScreenHeight() else -getScreenHeight()
                            }
                            animation(translationX, end.toFloat())
                        } else {
                            animation(0f, 0f, isSetEnd = false)
                            mSlideScrollListener?.showHeaderAndFooter()
                        }
                    }
                    HORIZONTAL -> {
                        if (Math.abs(translationX) > width * xThreshold) {
                            mSlideScrollListener?.onEndDrag(translationX / width)
                            var end = if (isScrollingRight) width else -width
                            if (layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                                end = if (isScrollingRight) getScreenWidth() else -getScreenWidth()
                            }
                            animation(end.toFloat(), translationY)
                        } else {
                            animation(0f, 0f, isSetEnd = false)
                            mSlideScrollListener?.showHeaderAndFooter()
                        }
                    }
                }
                mDirection = NONE
            }
        }
        return true
    }

    private fun animation(
        endX: Float,
        endY: Float,
        isSetEnd: Boolean = true
    ) {
        val animatorX = ObjectAnimator.ofFloat(this, "translationX", translationX, endX)
        val animatorY = ObjectAnimator.ofFloat(this, "translationY", translationY, endY)
        when (mDirection) {
            VERTICAL -> {
                animatorY.addUpdateListener {
                    mSlideScrollListener?.onScroll(Math.min(Math.abs(it.animatedValue as Float) / getScreenHeight(), 1f)) }
            }
            HORIZONTAL -> {
                animatorX.addUpdateListener {
                    mSlideScrollListener?.onScroll(Math.min(Math.abs(it.animatedValue as Float) / getScreenWidth(), 1f)) }
            }
        }
        val animatorSet = AnimatorSet()
        animatorSet.apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                    if (isSetEnd) mSlideScrollListener?.onLayoutClosed()
                }
            })
            playTogether(animatorX, animatorY)
            start()
        }
    }

    private fun setBackgroundAlpha() {
        mSlideScrollListener?.apply {
            if (mDirection == VERTICAL) {
                onScroll(Math.min(Math.abs(translationY) / getScreenHeight(), 1f))
            } else {
                onScroll(Math.min(Math.abs(translationX) / getScreenWidth(), 1f))
            }
        }
    }

    private fun getScreenHeight() =
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.height

    private fun getScreenWidth() =
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.width
}