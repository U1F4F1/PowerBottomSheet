package com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.v4.content.ContextCompat
import android.support.v4.widget.NestedScrollView
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.u1f4f1.betterbottomsheet.R

/**
 * [CoordinatorLayout.Behavior] that handles animating up an [AppBarLayout] if it is attached to the
 * [AppBarLayout] itself, or if it's attached to a parent [ViewGroup]
 */
class ScrollingAppBarLayoutBehavior(private val context: Context, attrs: AttributeSet) : AppBarLayout.ScrollingViewBehavior(context, attrs) {

    private var isInitialized = false
    private var isVisible = true
    /**
     * To avoid using multiple "peekheight=" in XML and looking flexibility allowing [AnchorPointBottomSheetBehavior.peekHeight]
     * get changed dynamically we get the [NestedScrollView] that has
     * "app:layout_behavior=" [AnchorPointBottomSheetBehavior] inside the [CoordinatorLayout]
     */
    private var anchorPointBottomSheetBehavior: AnchorPointBottomSheetBehavior<*>? = null

    private var appBarValueAnimator: ValueAnimator? = null

    override fun layoutDependsOn(parent: CoordinatorLayout?, child: View?, dependency: View?): Boolean {
        if (dependency is NestedScrollView) {
            if (AnchorPointBottomSheetBehavior.from<View>(dependency) != null) {
                return true
            }
        }
        return false
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        if (!isInitialized && !parent.isInEditMode) {
            return init(parent, child, dependency)
        }

        anchorPointBottomSheetBehavior?.let {
            getBottomSheetBehavior(parent)
        }

        child.getChildAppBarLayout()?.let {
            setAppBarVisible(it, dependency.y >= dependency.height - anchorPointBottomSheetBehavior!!.getPeekHeight())
        }

        return true
    }

    override fun onSaveInstanceState(parent: CoordinatorLayout?, child: View?): Parcelable {
        return SavedState(super.onSaveInstanceState(parent, child), isVisible)
    }

    override fun onRestoreInstanceState(parent: CoordinatorLayout?, child: View?, state: Parcelable?) {
        if (state !is SavedState) return

        val savedState = state
        super.onRestoreInstanceState(parent, child, savedState.superState)
        this.isVisible = savedState.isVisible
    }

    private fun init(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        /**
         * First we need to know if dependency view is upper or lower compared with
         * [AnchorPointBottomSheetBehavior] Y position to know if need to show the AppBar at beginning.
         */
        getBottomSheetBehavior(parent)
        anchorPointBottomSheetBehavior?.let { getBottomSheetBehavior(parent) }
        val mCollapsedY = dependency.height - anchorPointBottomSheetBehavior!!.getPeekHeight()
        isVisible = dependency.y >= mCollapsedY

        setStatusBarBackgroundVisible(isVisible)
        if (!isVisible) child.y = (child.y.toInt() - child.height - statusBarHeight).toFloat()
        isInitialized = true

        // We only need to move the view if we aren't already visible
        return !isVisible
    }

    fun setAppBarVisible(appBarLayout: AppBarLayout, visible: Boolean) {

        if (visible == isVisible) return

        if (appBarValueAnimator == null || !appBarValueAnimator!!.isRunning) {

            appBarValueAnimator = ValueAnimator.ofFloat(
                    appBarLayout.y,
                    if (visible) appBarLayout.y + appBarLayout.height + statusBarHeight
                    else appBarLayout.y - appBarLayout.height - statusBarHeight)

            appBarValueAnimator!!.duration = context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            appBarValueAnimator!!.addUpdateListener { animation -> appBarLayout.y = animation.animatedValue as Float }
            appBarValueAnimator!!.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    when { visible -> setStatusBarBackgroundVisible(true) }
                }

                override fun onAnimationEnd(animation: Animator) {
                    when { !visible -> setStatusBarBackgroundVisible(false) }
                    isVisible = visible
                    super.onAnimationEnd(animation)
                }
            })
            appBarValueAnimator!!.start()
        }
    }

    private val statusBarHeight: Int
        get() {
            var result: Int = 0
            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                result = context.resources.getDimensionPixelSize(resourceId)
            }
            return result
        }

    private fun setStatusBarBackgroundVisible(visible: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (visible) {
                val window = (context as Activity).window
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(context, R.color.colorPrimaryDark)
            } else {
                val window = (context as Activity).window
                window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.statusBarColor = ContextCompat.getColor(context, android.R.color.transparent)
            }
        }
    }

    /**
     * Look into the CoordiantorLayout for the [AnchorPointBottomSheetBehavior]

     * @param coordinatorLayout with app:layout_behavior= [AnchorPointBottomSheetBehavior]
     */
    private fun getBottomSheetBehavior(coordinatorLayout: CoordinatorLayout) {
        (0..coordinatorLayout.childCount - 1)
                .map { coordinatorLayout.getChildAt(it) }
                .filterIsInstance<NestedScrollView>()
                .forEach {
                    try {
                        anchorPointBottomSheetBehavior = AnchorPointBottomSheetBehavior.from<View>(it)
                    } catch (e: IllegalArgumentException) { }
                }
    }

    fun View.getChildAppBarLayout(): AppBarLayout? {
        if (this !is ViewGroup) return null

        (0..this.childCount - 1).forEach { i ->
            val child = this.getChildAt(i)

            if (child is AppBarLayout) {
                return child
            }

            if (child is ViewGroup) {
                return child.getChildAppBarLayout()
            }
        }

        return null
    }

    protected class SavedState : View.BaseSavedState {

        internal val isVisible: Boolean

        constructor(source: Parcel) : super(source) {
            isVisible = source.readByte().toInt() != 0
        }

        constructor(superState: Parcelable, visible: Boolean) : super(superState) {
            this.isVisible = visible
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeByte((if (isVisible) 1 else 0).toByte())
        }

        companion object {

            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {

        fun <V : View> from(view: V): ScrollingAppBarLayoutBehavior {
            val params = view.layoutParams as? CoordinatorLayout.LayoutParams
                    ?: throw IllegalArgumentException("The view is not a child of CoordinatorLayout")
            val behavior = params.behavior as? ScrollingAppBarLayoutBehavior
                    ?: throw IllegalArgumentException("The view is not associated with ScrollingAppBarLayoutBehavior")
            return behavior
        }
    }
}