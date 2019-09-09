package com.ralf.slideviewtest.view

/**
 * @author Ralf(wanglixin)
 * DESCRIPTION
 * @name SlideScrollListener
 * @email -
 * @date 2019/09/01 14:15
 **/
interface SlideScrollListener {

    /**
     * 关闭布局
     */
    fun onLayoutClosed()

    /**
     * 隐藏头、脚工具栏
     */
    fun hideHeaderAndFooter()

    /**
     * 显示头、脚工具栏
     */
    fun showHeaderAndFooter()

    /**
     * 拖动y方向距离占Layout百分比
     *
     * @param dy dy
     */
    fun onScroll(dy: Float)

    /**
     * 结束拖动
     *
     * @param dy dy
     */
    fun onEndDrag(dy: Float)

}