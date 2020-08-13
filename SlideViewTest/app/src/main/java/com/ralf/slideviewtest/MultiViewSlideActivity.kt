package com.ralf.slideviewtest

import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.ralf.slideviewtest.adapter.CommonAdapter
import com.ralf.slideviewtest.adapter.ViewHolder
import com.ralf.slideviewtest.entity.ImageEntity
import com.ralf.slideviewtest.view.SlideScrollListener
import kotlinx.android.synthetic.main.activity_multi_view_slide.*

class MultiViewSlideActivity : AppCompatActivity() {

    private var isBarShow: Boolean = true
    private var isBarShowing: Boolean = false

    private var mData = ArrayList<ImageEntity>()

    init {
        mData.add(ImageEntity("https://gank.io/images/d6bba8cf5b8c40f9ad229844475e9149", false))
        mData.add(ImageEntity("https://gank.io/images/d6bba8cf5b8c40f9ad229844475e9149", false))
        mData.add(ImageEntity("https://gank.io/images/d6bba8cf5b8c40f9ad229844475e9149", false))
        mData.add(ImageEntity("https://gank.io/images/d6bba8cf5b8c40f9ad229844475e9149", false))
        mData.add(ImageEntity("https://gank.io/images/d6bba8cf5b8c40f9ad229844475e9149", false))
    }

    /**
     * 获取状态栏高度
     *
     * @param context
     * @return
     */
    private fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    /**
     * 显示和隐藏状态栏
     *
     * @param show
     */
    private fun setStatusBarVisible(show: Boolean) {
        if (show) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_view_slide)
        //强制竖屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setStatusBarVisible(true)
        // 设置 top bar 高度
        val params = top_bar_rl.layoutParams as FrameLayout.LayoutParams
        params.topMargin = getStatusBarHeight(this)
        top_bar_rl.layoutParams = params
        recycler_view.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recycler_view.adapter = object : CommonAdapter<ImageEntity>(this, mData, R.layout.layout_adapter_item) {
            override fun bindData(holder: ViewHolder, data: ImageEntity, position: Int) {
                holder.setImagePath(R.id.image, object : ViewHolder.HolderImageLoader(mData[position].path) {
                    override fun loadImage(iv: ImageView, path: String) {
                        Glide.with(holder.itemView.context).load(path).into(iv)
                    }
                })
            }
        }
        window.decorView.setBackgroundColor(Color.WHITE)
        slide_layout.setBackgroundColor(Color.WHITE)
        slide_layout.mEnableX = true
        slide_layout.mEnable = true
        slide_layout.yThreshold = 0.4f
        slide_layout.mSlideScrollListener = object : SlideScrollListener {
            override fun showHeaderAndFooter() {
                if (isBarShow || isBarShowing) {
                    return
                }
                slide_layout.postDelayed({ changeBar() }, 200)

            }

            override fun onEndDrag(dy: Float) {

            }

            override fun hideHeaderAndFooter() {
                if (!isBarShow || isBarShowing) {
                    return
                }
                changeBar()
            }

            override fun onLayoutClosed() {
                finish()
                overridePendingTransition(0, 0)
            }

            override fun onScroll(dy: Float) {
                window.decorView.background.alpha = (255 * (1.0f - dy)).toInt()
                if (slide_layout != null) {
                    slide_layout.scaleX = 1f - Math.abs(dy)
                    slide_layout.scaleY = 1f - Math.abs(dy)
                }
            }
        }


    }

    private fun changeBar() {
        isBarShowing = true
        slide_layout.setBackgroundColor(if (!isBarShow) Color.WHITE else Color.BLACK)
        window.decorView.setBackgroundColor(if (!isBarShow) Color.WHITE else Color.BLACK)
        // 最好两个 Activity 的样式一致，不全屏都不全屏，全屏都全屏，否则通知栏显示隐藏会出现闪烁
        setStatusBarVisible(!isBarShow)
        if (!isBarShow) {
            ObjectAnimator.ofFloat(top_bar_rl, "translationY", top_bar_rl.translationY, 0f)
                .setDuration(200)
                .start()
            ObjectAnimator.ofFloat(bottom_bar_fl, "translationY", bottom_bar_fl.translationY, 0f)
                .setDuration(200)
                .start()
        } else {
            ObjectAnimator.ofFloat(
                top_bar_rl,
                "translationY",
                0f,
                -top_bar_rl.height.toFloat() - getStatusBarHeight(this)
            )
                .setDuration(200)
                .start()
            ObjectAnimator.ofFloat(bottom_bar_fl, "translationY", 0f, bottom_bar_fl.height.toFloat())
                .setDuration(200)
                .start()
        }
        isBarShow = !isBarShow
        isBarShowing = false
    }
}