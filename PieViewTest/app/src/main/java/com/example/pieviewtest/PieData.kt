package com.example.pieviewtest

import java.io.Serializable

/** 圆饼图数据$ */
data class PieData(
    val value: Int,
    var percentage: Float,
    var color: Int,
    var angle: Float
) : Serializable