package com.example.pieviewtest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** 圆饼图 */
class PieView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet?,
    defStyle: Int = 0
) : View(context, attributes, defStyle) {

    // 颜色表 (注意: 此处定义颜色使用的是ARGB，带Alpha通道的)
    private val mColors = intArrayOf(
        -0x330100, -0x9b6a13, -0x1cd9ca, -0x800000, -0x7f8000, -0x7397, -0x7f7f80,
        -0x194800, -0x830400
    )

    private var mStartAngle = 0f
    private val mPaint = Paint()
    private var mWidth = 0
    private var mHeight = 0
    private var mPieDataList: List<PieData> = emptyList()
    private val mRect: RectF by lazy { RectF() }

    init {
        mPaint.style = Paint.Style.FILL
        mPaint.isAntiAlias = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (mPieDataList.isEmpty()) {
            return
        }
        canvas?.save()
        canvas?.translate(mWidth / 2f, mHeight / 2f)
        mRect.apply {
            val radius = mHeight.coerceAtMost(mWidth) / 2 * 0.8f
            left = -radius
            top = -radius
            right = radius
            bottom = radius
        }
        var currentAngle = mStartAngle
        mPieDataList.forEachIndexed { _, pieData ->
            mPaint.color = pieData.color
            canvas?.drawArc(mRect, currentAngle, pieData.angle, true, mPaint)
            currentAngle += pieData.angle
        }
        canvas?.restore()
    }

    fun setStartAngle(startAngle: Float) {
        mStartAngle = startAngle
        invalidate()
    }

    fun setPieData(pieData: List<PieData>) {
        mPieDataList = pieData
        initData()
        invalidate()
    }

    private fun initData() {
        if (mPieDataList.isEmpty()) {
            return
        }
        var sumValue = 0f
        mPieDataList.forEachIndexed { index, pieData ->
            pieData.color = mColors[index % mColors.size]
            sumValue += pieData.value
        }
        mPieDataList.forEachIndexed { _, pieData ->
            pieData.percentage = pieData.value / sumValue
            pieData.angle = pieData.percentage * 360
        }
    }
}