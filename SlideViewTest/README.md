# SlideLayout

### 1.View 事件分发初体验

View 事件在 Android 中也是很重要的一块，通过对事件的处理可以完成一系列的交互操作，使界面有更好的交互效果，本篇文章就来体验一下事件处理带来的效果，本文不会重点分析事件分发的过程以及原理，主要通过本文先来对事件的分发有一个初步的概念，后面的文章会单独总结事件分发的各种处理情况以及源码分析。

先来看一下效果。

![view_event](https://github.com/RalfNick/PicRepository/raw/master/view_event/view_event.gif)

从这个效果中分析一下需要实现的需求。

![view_event_content](https://github.com/RalfNick/PicRepository/raw/master/view_event/view_event_content.png)

### 2.实现思路

(1) 有了需求，就根据需求来分析，找出最主要的功能，由于主要的功能就是 View 的滑动，但是滑动的这个 View 是一个单一 View (继承自 View) 还是 ViewGroup 呢？如果是一个单一 View，那么就无法实现里面有多张图片左右滑动的情况，里面有多张图片，可以通过放置一个 RecyclerView 或者 ViewPager 之类的组件，所以要自定义的这个 View 也就定来了，选择 ViewGroup。

(2) 选择了 ViewGroup，是否可以使用已有的 ViewGroup 呢？这样就可以简化我们自己自定义 ViewGroup 的步骤，如果继承 ViewGroup，还需要自己完成测量和布局过程。因为我们只需要控制 View 的滑动，不需要额外的一些操作，如对内部子 View 的位置控制等，本文 Demo 中选择的是 FrameLayout。

(3) 选择了需要继承的 ViewGroup - FrameLayout，然后需要在里面完成事件的控制，手指移动时，我们自定义的 View 跟着移动，也就是说我们需要拦截到 Move 事件，来实现 View 的滑动，由于上面也提到了，里面有两种子 View，一种是一个单一 View，如 ImageView；另一种是 ViewGroup，如 RecyclerView 或者 ViewPager，对应的拦截情况如何处理，这是需要考虑的细节，下面实现过程中会分析。此外，View 的滑动如何实现呢？View 的滑动实现方式有多重，如视图动画、属性动画、View 的 scrollTo，ScrollBy、Scroller、Transition 等，这里本文 Demo 中采用的是 Transition

(4) 上面分析的主要的功能滑动，那么附加功能呢？滑动时 View 的透明度、滑动结束时根据滑动距离来完成剩下的动作、还有滑动监听，提供给使用者

基本的功能以及实现的思路差不多整理清楚了，下面就可以动手实现了，当然实现过程中也是有坑的，细节部分，在实现过程中逐步调整和和优化。

### 3.实现过程

下来看看定义的变量

```java
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

override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
...
}
override fun onTouchEvent(ev: MotionEvent): Boolean {
...
}
```

上面几个变量中主要记录滑动的方向和坐标，重点说下 mEnableX 和 mEnableInterceptX，mEnableX 其实可有可无，相当于一个总开关，打开可以滑动，关闭不能滑动；mEnableInterceptX 用于控制 SlideLayout 水平上是否拦截 Move 的动作，如果单一 View 可以拦截，如果是 RecyclerView 或者 ViewPager 之类的组件就不能拦截了，这个时候如果拦截那就会滑动冲突了，得到的可能就不是我们需要的结果了。

对于事件分发，我们知道有 3 个方法，一般情况下主要重写 onInterceptTouchEvent() 和 onTouchEvent() 方法，dispatchTouchEvent() 方法很少去重写。

还有一个滑动监听接口 SlideScrollListener

```java
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
```

接着来看 onInterceptTouchEvent() 方法的具体实现,简要说明一下 onInterceptTouchEvent() 的作用，当事件过来时，如果 onInterceptTouchEvent() 返回 true 时进行拦截，然后交给自己的 onTouchEvent() 处理，后面的事件都不需要在询问 onInterceptTouchEvent()，直接交给 onTouchEvent() 处理;如果 onInterceptTouchEvent() 返回 false 或者 super 时不拦截，那么事件将流转到子 View，如果没有子 View 或者子 View 没有进行处理，事件会返回来交给自己的 onTouchEvent() 处理。对于刚刚学习事件分发的童鞋，这部分不太理解，也没有关系，本文也不打算详细分析这块，先留一下一个初步的印象。

```java
override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
// (1)开关关闭，不启用滑动效果
if (!mEnable) {
return super.onInterceptTouchEvent(ev)
}
// (2) 获得绝对坐标，用于方向判断
val x = ev.rawX
val y = ev.rawY
var intercepted = false
when (ev.action) {
// (3) 事件的开始，DOWN 事件
MotionEvent.ACTION_DOWN -> {
originX = ev.rawX
originY = ev.rawY
intercepted = false
}
// (4) MOVE 事件中做逻辑判断，何时拦截处理
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
// (5) UP 事件不做处理
MotionEvent.ACTION_UP -> {
intercepted = false
}
}
return intercepted
}
```
因为 Demo 的效果要求的事件梳理并不复杂，所以 onInterceptTouchEvent() 的处理也很简单：

(1) 处不用说，想要有滑动效果，就打来开关，返回false 和 super 都是可以的，都是不拦截事件；

(2) 获得绝对坐标，用于方向判断，当然用相对坐标 getX、getY 也可以，因为在 onTouchEvent 中使用的 绝对坐标便于滑动，这里也就使用了绝对坐标。

(3) 事件的开始，DOWN 事件过来，仅仅记录一下坐标，并不做拦截处理，如果做了拦截，后续事件都会由 SlideLayout 来处理，后面事件过来时，onInterceptTouchEvent 中接收不到 UP 事件，里面的逻辑也就不会执行；还有一点，，DOWN 事件拦截后，SlideLayout 中子 View 就不会受到后续事件，对于 RecyclerView 或者 ViewPager 之类的组件，左右滑动事件就不会执行，不符合需求。

(4) MOVE 事件中做逻辑判断，判断何时拦截处理。首先如果是垂直方向上的滑动，一定是拦截的，如果是水平方向上的滑动，看具体的内部子 View 的需求，如果子 View 需要水平滑动，那就不做拦截只拦截垂直方向上的事件。所以 (4) 中的逻辑也就是这个逻辑。

(5) UP 事件不做处理，拦截也没有意义，因为 UP 事件是事件流的最后一项。

接着看 onTouchEvent 中的事件处理，onTouchEvent 中处理逻辑稍微多一些，主要是要完成 SlideLayout 的滑动，滑动监听以及滑动结束时的结束处理或者复位处理。

```java
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
// (1) MOVE 事件中处理滑动
MotionEvent.ACTION_MOVE -> {
val delY = y - originY
val delX = x - originX
isScrollingUp = delY > 0
isScrollingRight = delX > 0
translationY = delY
translationX = if (mEnableX) delX else 0.0f
// (2) 滑动监听，滑动过程中返回滑动的百分比,随着滑动隐藏界面上的 header 和 footer
setBackgroundAlpha()
hideHeaderAndFooter()
}
}
// (3) UP 事件处理，判断执行结束动作还是复位动作
MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
when (mDirection) {
VERTICAL -> {
// 滑动值 > 阈值，SlideLayout 移除屏幕外
if (Math.abs(translationY) > height * yThreshold) {
// 结束时的百分比传递给客户端
mSlideScrollListener?.onEndDrag(translationY / height)
// 根方向设置移除的值
var end = if (isScrollingUp) height else -height
// 这部分需要注意下，当 SlideLayout 的布局不是
// MATCH_PARENT,需要移动整个屏幕的高度，确保 SlideLayout 能够移除屏幕外
if (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
end = if (isScrollingUp) getScreenHeight() else -getScreenHeight()
}
// 执行移动动画
animation(translationX, end.toFloat())
} else {
// (4) 滑动值 < 阈值，SlideLayout 复位
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
```

onTouchEvent 方法中同样不需要拦截 DOWN 事件，如果拦截后，内部子 View 就接收不到事件了。

(1) MOVE 事件中处理滑动，判断滑动的方向以及设置滑动的距离，上下方向和左右方向上判断，为后面滑动结束后 SlideLayout 的消失时朝哪个方向移动作为判断的变量。然后通过 setTranslationY(float translationY) 和 setTranslationX(float translationX) 设置 SlideLayout 移动的距离，其中 x 方向是有开挂设置的。

(2) 滑动监听，滑动过程中返回滑动的百分比,随着滑动隐藏界面上的 header 和 footer

(3) UP 事件处理，判断执行结束动作还是复位动作。UP 事件是一系列操作最后的一个动作，当手指抬起时，当滑动的距离超过设定的阈值时，使 SlideLayout 继续滑动，直到移除屏幕外。未超过阈值时，需要恢复原位。这里有一点需要注意下，当 SlideLayout 的布局不是 MATCH_PARENT,需要移动整个屏幕的高度，确保 SlideLayout 能够移除屏幕外，文章开头动画中展示的单一 View 的效果中， 是一个 宽和高设置大小的 ImageView，SlideLayout 的尺寸是 WRAP_CONTENT，此时意味着 SlideLayout 并没有充满屏幕，要想将 SlideLayout 移除屏幕外，需要设置更大的距离，这里设置的是屏幕的高度。

(4) 未超过阈值时，需要恢复原位，使用的是属性动画将 SlideLayout 移动到原位置，同时传递移动的百分比给调用端，调用端根据这个百分比来进行控制背景的透明度和大小缩放等效果。

```java

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

```

### 3.SlideLayout 使用

(1) 内部 View 可以左右滑动

布局中使用 SlideLayout 作为外层布局，包裹内部需要使用该手势的布局

```java
<com.ralf.slideviewtest.view.SlideLayout
android:id="@+id/slide_layout"
android:layout_width="match_parent"
android:layout_height="match_parent">

<android.support.v7.widget.RecyclerView
android:id="@+id/recycler_view"
android:layout_width="match_parent"
android:layout_height="match_parent">

</android.support.v7.widget.RecyclerView>
</com.ralf.slideviewtest.view.SlideLayout>
```

代码设置部分

```java
// 左右禁止拦截
slide_layout.mEnableX = false
slide_layout.mEnable = true
// 设置 y 轴移动的阈值
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
// 这里设置了大小缩放的效果
if (slide_layout != null) {
slide_layout.scaleX = 1f - Math.abs(dy)
slide_layout.scaleY = 1f - Math.abs(dy)
}
}
}
```

(2) 内部 View 左右不滑动，内部单一 View

布局很简单，内部就放置了一个 ImageView

```java
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
xmlns:android="http://schemas.android.com/apk/res/android"
xmlns:tools="http://schemas.android.com/tools"
xmlns:app="http://schemas.android.com/apk/res-auto"
android:layout_width="match_parent"
android:layout_height="match_parent"
tools:context=".SingleSlideActivity">

<com.ralf.slideviewtest.view.SlideLayout
android:id="@+id/slide_view"
android:layout_width="wrap_content"
android:layout_gravity="center"
android:layout_height="wrap_content">

<ImageView android:layout_width="300dp"
android:layout_height="300dp"
android:id="@+id/image_view"
android:layout_gravity="center"
android:src="@mipmap/ic_launcher_round"
android:scaleType="centerCrop"/>

</com.ralf.slideviewtest.view.SlideLayout>

</FrameLayout>
```
```java

override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
setContentView(R.layout.activity_single_slide)
window.decorView.setBackgroundColor(Color.BLACK)
// 这个位置需要注意
image_view.setOnClickListener {
Log.e("image_view", "click")
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

```

在写代码的过程中，本以为单一 View 相对简单，但是也遇到了坑，代码中对 ImageView 设置了单击监听。这么做，是因为 SlideLayout 在 onInterceptTouchEvent 中对 DOWN 事件不做拦截，正常情况下事件会流向子 View，但是 ImageView  不可点击，并不接受事件，所以事件不会向下传递，传递到 SlideLayout 的 onTouchEvent 就结束了，最后由 SlideLayout 消耗掉事件，因为在下一次 Move 事件到来时，直接交给 SlideLayout 的 onTouchEvent 处理，略过了 onInterceptTouchEvent 中对 Move 处理的逻辑，最终效果并不符合需求。

解决的方式就是让 ImageView 可点击能够接收事件，这样 DOWN 事件就会流向 ImageView，下次 MOVE 事件过来时，就会经过 SlideLayout 的 onInterceptTouchEvent 和 onTouchEvent 完成正常的逻辑。

这样一个效果就实现了，其实总体的逻辑还是相对清晰的，理清事件的接收者和走向，处理起来就不会凌乱了，本篇是对事件分发的初体验，后面会有两篇文章来对事件分发进行总结以及源码分析，敬请期待！
