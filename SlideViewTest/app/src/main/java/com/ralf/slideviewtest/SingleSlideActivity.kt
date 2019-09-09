package com.ralf.slideviewtest

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.bumptech.glide.Glide
import com.ralf.slideviewtest.view.SlideScrollListener
import kotlinx.android.synthetic.main.activity_single_slide.*

class SingleSlideActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_slide)
        window.decorView.setBackgroundColor(Color.BLACK)
        image_view.setOnTouchListener { _, motionEvent ->
            var result = false
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                Log.e("image_view", "click")
                result = true
            }
            result
        }

        slide_view.setBackgroundColor(Color.BLACK)
        slide_view.mEnableX = true
        slide_view.mEnableInterceptX = true
        slide_view.mEnable = true
        slide_view.yThreshold = 1f
        slide_view.xThreshold = 0.8f
        slide_view.mSlideScrollListener = object : SlideScrollListener {
            override fun showHeaderAndFooter() {

            }

            override fun onEndDrag(dy: Float) {

            }

            override fun hideHeaderAndFooter() {

            }

            override fun onLayoutClosed() {
                finish()
                // 防止退出时闪一下问题
                overridePendingTransition(0, 0)
            }

            override fun onScroll(dy: Float) {
                window.decorView.background.alpha = (255 * (1.0f - dy)).toInt()
            }
        }
        Glide.with(this)
            .load("https://ws1.sinaimg.cn/large/0065oQSqly1fw8wzdua6rj30sg0yc7gp.jpg")
            .into(image_view)
    }
}
