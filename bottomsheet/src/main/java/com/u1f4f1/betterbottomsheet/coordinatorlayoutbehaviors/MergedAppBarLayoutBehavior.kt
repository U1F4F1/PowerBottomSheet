package com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.ColorRes
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.v4.content.ContextCompat
import android.support.v4.widget.NestedScrollView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewPropertyAnimator
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.u1f4f1.betterbottomsheet.R

import java.lang.ref.WeakReference

/**
 * ~ Licensed under the Apache License, Version 2.0 (the "License");
 * ~ you may not use this file except in compliance with the License.
 * ~ You may obtain a copy of the License at
 * ~
 * ~      http://www.apache.org/licenses/LICENSE-2.0
 * ~
 * ~ Unless required by applicable law or agreed to in writing, software
 * ~ distributed under the License is distributed on an "AS IS" BASIS,
 * ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * ~ See the License for the specific language governing permissions and
 * ~ limitations under the License.
 * ~
 * ~ https://github.com/miguelhincapie/CustomBottomSheetBehavior
 */

/**
 * This behavior should be applied on an AppBarLayout... More Explanations coming soon
 */
class MergedAppBarLayoutBehavior(private val mContext: Context, attrs: AttributeSet) : AppBarLayout.ScrollingViewBehavior(mContext, attrs) {

    private var initialized = false

    private var backGroundLayoutParams: FrameLayout.LayoutParams? = null
    /**
     * To avoid using multiple "peekheight=" in XML and looking flexibility allowing [AnchorPointBottomSheetBehavior.peekHeight]
     * get changed dynamically we get the [NestedScrollView] that has
     * "app:layout_behavior=" [AnchorPointBottomSheetBehavior] inside the [CoordinatorLayout]
     */
    private var bottomSheetBehaviorRef: WeakReference<AnchorPointBottomSheetBehavior<*>>? = null
    private var initialY: Float = 0.toFloat()
    private var visible = false

    private var toolbarTitle: String? = null

    private var toolbar: Toolbar? = null
    private var titleTextView: TextView? = null
    private var background: View? = null
    private var onNavigationClickListener: View.OnClickListener? = null

    private var titleAlphaValueAnimator: ValueAnimator? = null
    private var currentTitleAlpha = 0

    override fun layoutDependsOn(parent: CoordinatorLayout?, child: View?, dependency: View?): Boolean {
        if (dependency is NestedScrollView) {
            if (AnchorPointBottomSheetBehavior.from(dependency) != null) {
                return true
            }
        }
        return false
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout?, child: View?, dependency: View): Boolean {

        if (!initialized) {
            init(parent!!, child!!)
        }
        /**
         * Following docs we should return true if the Behavior changed the child view's size or position, false otherwise
         */
        var childMoved = false

        if (isDependencyYBelowAnchorPoint(parent!!, dependency)) {

            childMoved = setToolbarVisible(false, child!!)

        } else if (isDependencyYBetweenAnchorPointAndToolbar(parent, child!!, dependency)) {

            childMoved = setToolbarVisible(true, child)
            setFullBackGroundColor(android.R.color.transparent)
            setPartialBackGroundHeight(0)

        } else if (isDependencyYBelowToolbar(child, dependency) && !isDependencyYReachTop(dependency)) {

            childMoved = setToolbarVisible(true, child)
            if (isStatusBarVisible)
                setStatusBarBackgroundVisible(false)
            if (isTitleVisible)
                isTitleVisible = false
            setFullBackGroundColor(android.R.color.transparent)
            setPartialBackGroundHeight((child.height + child.y - dependency.y).toInt())

        } else if (isDependencyYBelowStatusToolbar(child, dependency) || isDependencyYReachTop(dependency)) {

            childMoved = setToolbarVisible(true, child)
            if (!isStatusBarVisible)
                setStatusBarBackgroundVisible(true)
            if (!isTitleVisible)
                isTitleVisible = true
            setFullBackGroundColor(R.color.colorPrimary)
            setPartialBackGroundHeight(0)
        }
        return childMoved
    }

    private fun init(parent: CoordinatorLayout, child: View) {

        val appBarLayout = child as AppBarLayout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appBarLayout.outlineProvider = ViewOutlineProvider.BACKGROUND
        }

        toolbar = appBarLayout.findViewById(R.id.expanded_toolbar) as Toolbar
        background = appBarLayout.findViewById(R.id.background)
        backGroundLayoutParams = background!!.layoutParams as FrameLayout.LayoutParams
        getBottomSheetBehavior(parent)

        titleTextView = findTitleTextView(toolbar!!)
        if (titleTextView == null)
            return

        initialY = child.getY()

        child.setVisibility(if (visible) View.VISIBLE else View.INVISIBLE)
        //        setStatusBarBackgroundVisible(isVisible);

        setFullBackGroundColor(if (visible && currentTitleAlpha == 1) R.color.colorPrimary else android.R.color.transparent)
        setPartialBackGroundHeight(0)
        titleTextView!!.text = toolbarTitle
        titleTextView!!.alpha = currentTitleAlpha.toFloat()
        initialized = true
        setToolbarVisible(false, child)
    }

    /**
     * Look into the CoordiantorLayout for the [AnchorPointBottomSheetBehavior]
     * @param coordinatorLayout with app:layout_behavior= [AnchorPointBottomSheetBehavior]
     */
    private fun getBottomSheetBehavior(coordinatorLayout: CoordinatorLayout) {

        for (i in 0..coordinatorLayout.childCount - 1) {
            val child = coordinatorLayout.getChildAt(i)

            if (child is NestedScrollView) {

                try {
                    val temp = AnchorPointBottomSheetBehavior.from(child)
                    bottomSheetBehaviorRef = WeakReference<AnchorPointBottomSheetBehavior<*>>(temp)
                    break
                } catch (e: IllegalArgumentException) {
                }

            }
        }
    }

    private fun isDependencyYBelowAnchorPoint(parent: CoordinatorLayout, dependency: View): Boolean {
        if (bottomSheetBehaviorRef == null || bottomSheetBehaviorRef!!.get() == null)
            getBottomSheetBehavior(parent)
        return dependency.y > bottomSheetBehaviorRef!!.get()!!.anchorPoint
    }

    private fun isDependencyYBetweenAnchorPointAndToolbar(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        if (bottomSheetBehaviorRef == null || bottomSheetBehaviorRef!!.get() == null)
            getBottomSheetBehavior(parent)
        return dependency.y <= bottomSheetBehaviorRef!!.get()!!.anchorPoint && dependency.y > child.y + child.height
    }

    private fun isDependencyYBelowToolbar(child: View, dependency: View): Boolean {
        return dependency.y <= child.y + child.height && dependency.y > child.y
    }

    private fun isDependencyYBelowStatusToolbar(child: View, dependency: View): Boolean {
        return dependency.y <= child.y
    }

    private fun isDependencyYReachTop(dependency: View): Boolean {
        return dependency.y == 0f
    }

    private fun setPartialBackGroundHeight(height: Int) {
        backGroundLayoutParams!!.height = height
        background!!.layoutParams = backGroundLayoutParams
    }

    private fun setFullBackGroundColor(@ColorRes colorRes: Int) {
        toolbar!!.setBackgroundColor(ContextCompat.getColor(mContext, colorRes))
    }

    private fun findTitleTextView(toolbar: Toolbar): TextView? {
        for (i in 0..toolbar.childCount - 1) {
            val toolBarChild = toolbar.getChildAt(i)
            if (toolBarChild is TextView &&
                    toolBarChild.text != null &&
                    toolBarChild.text.toString().contentEquals(mContext.resources.getString(R.string.key_binding_default_toolbar_name))) {
                return toolBarChild
            }
        }
        return null
    }

    private fun setToolbarVisible(visible: Boolean, child: View): Boolean {
        val mAppBarLayoutAnimation: ViewPropertyAnimator
        var childMoved = false
        if (visible && !this.visible) {
            childMoved = true
            child.y = (-child.height / 3).toFloat()
            mAppBarLayoutAnimation = child.animate().setDuration(mContext.resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
            mAppBarLayoutAnimation.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    child.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    (mContext as AppCompatActivity).setSupportActionBar(toolbar)
                    toolbar!!.setNavigationOnClickListener(onNavigationClickListener)
                    val actionBar = mContext.supportActionBar
                    actionBar?.setDisplayHomeAsUpEnabled(true)
                    this@MergedAppBarLayoutBehavior.visible = true
                }
            })
            mAppBarLayoutAnimation.alpha(1f).y(initialY).start()
        } else if (!visible && this.visible) {
            mAppBarLayoutAnimation = child.animate().setDuration(mContext.resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
            mAppBarLayoutAnimation.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    child.visibility = View.INVISIBLE
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    (mContext as AppCompatActivity).setSupportActionBar(null)
                    this@MergedAppBarLayoutBehavior.visible = false
                }
            })
            mAppBarLayoutAnimation.alpha(0f).start()
        }

        return childMoved
    }

    private var isTitleVisible: Boolean
        get() = titleTextView!!.alpha == 1f
        set(visible) {

            if (visible && titleTextView!!.alpha == 1f || !visible && titleTextView!!.alpha == 0f)
                return

            if (titleAlphaValueAnimator == null || !titleAlphaValueAnimator!!.isRunning) {
                toolbar!!.title = toolbarTitle
                val startAlpha = if (visible) 0 else 1
                currentTitleAlpha = if (visible) 1 else 0
                val endAlpha = currentTitleAlpha

                titleAlphaValueAnimator = ValueAnimator.ofFloat(startAlpha.toFloat(), endAlpha.toFloat())
                titleAlphaValueAnimator!!.duration = mContext.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
                titleAlphaValueAnimator!!.addUpdateListener { animation -> titleTextView!!.alpha = animation.animatedValue as Float }
                titleAlphaValueAnimator!!.start()
            }
        }

    private val isStatusBarVisible: Boolean
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return (mContext as Activity).window.statusBarColor == ContextCompat.getColor(mContext, R.color.colorPrimaryDark)
            }
            return true
        }

    private fun setStatusBarBackgroundVisible(visible: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (visible) {
                val window = (mContext as Activity).window
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(mContext, R.color.colorPrimaryDark)
            } else {
                val window = (mContext as Activity).window
                window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.statusBarColor = ContextCompat.getColor(mContext, android.R.color.transparent)
            }
        }
    }

    fun setNavigationOnClickListener(listener: View.OnClickListener) {
        this.onNavigationClickListener = listener
    }

    fun setToolbarTitle(title: String) {
        this.toolbarTitle = title
        if (this.toolbar != null)
            this.toolbar!!.title = title
    }

    override fun onSaveInstanceState(parent: CoordinatorLayout?, child: View?): Parcelable {
        return SavedState(super.onSaveInstanceState(parent, child),
                visible,
                toolbarTitle!!,
                currentTitleAlpha)
    }

    override fun onRestoreInstanceState(parent: CoordinatorLayout?, child: View?, state: Parcelable?) {
        val ss = state as SavedState?
        super.onRestoreInstanceState(parent, child, ss!!.superState)
        this.visible = ss.mVisible
        this.toolbarTitle = ss.mToolbarTitle
        this.currentTitleAlpha = ss.mTitleAlpha
    }

    protected class SavedState : View.BaseSavedState {

        internal val mVisible: Boolean
        internal val mToolbarTitle: String
        internal val mTitleAlpha: Int

        constructor(source: Parcel) : super(source) {
            mVisible = source.readByte().toInt() != 0
            mToolbarTitle = source.readString()
            mTitleAlpha = source.readInt()
        }

        constructor(superState: Parcelable, visible: Boolean, toolBarTitle: String, titleAlpha: Int) : super(superState) {
            this.mVisible = visible
            this.mToolbarTitle = toolBarTitle
            this.mTitleAlpha = titleAlpha
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeByte((if (mVisible) 1 else 0).toByte())
            out.writeString(mToolbarTitle)
            out.writeInt(mTitleAlpha)
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
        private val TAG = MergedAppBarLayoutBehavior::class.java.simpleName

        fun <V : View> from(view: V): MergedAppBarLayoutBehavior {
            val params = view.layoutParams as? CoordinatorLayout.LayoutParams ?: throw IllegalArgumentException("The view is not a child of CoordinatorLayout")
            val behavior = params
                    .behavior as? MergedAppBarLayoutBehavior ?: throw IllegalArgumentException("The view is not associated with " + "MergedAppBarLayoutBehavior")
            return behavior
        }
    }
}