@file:Suppress("unused", "UNUSED_PARAMETER")

package com.u1f4f1.betterbottomsheet.bottomsheet

import android.content.Context
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.view.ViewCompat
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.view.ViewTreeObserver
import com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors.AnchorPointBottomSheetBehavior
import inkapplicaitons.android.logger.ConsoleLogger
import inkapplicaitons.android.logger.Logger
import java.util.*

abstract class BottomSheet : NestedScrollView {
    var recyclerView: RecyclerView? = null
    protected var bottomSheetBehavior: AnchorPointBottomSheetBehavior<*>? = null

    private val postOnStableStateRunnables = SparseArray<MutableList<Runnable>>()

    private var activatedListener: OnSheetActivatedListener? = null
    private var bottomSheetAdapter: BottomSheetAdapter? = null

    open var isActive: Boolean = false
        set(isActive) {
            field = isActive
            activatedListener?.isActivated(isActive)
        }

    internal var logger: Logger = ConsoleLogger("ANDROIDISBAD")

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    interface OnSheetActivatedListener {
        fun isActivated(isActive: Boolean)
    }

    fun setActivatedListener(activatedListener: OnSheetActivatedListener) {
        this.activatedListener = activatedListener
    }

    open fun removeAdapter() {
        this.recyclerView?.adapter = null
    }

    open fun setState(state: BottomSheetState) {
        this.bottomSheetBehavior?.state = state
    }

    open fun addBottomSheetStateCallback(callback: AnchorPointBottomSheetBehavior.BottomSheetStateCallback) {
        this.bottomSheetBehavior?.addBottomSheetStateCallback(callback)
    }

    open fun addBottomSheetSlideCallback(callback: AnchorPointBottomSheetBehavior.BottomSheetSlideCallback) {
        this.bottomSheetBehavior?.addBottomSheetSlideCallback(callback)
    }

    open fun setupRecyclerview(adapter: BottomSheetAdapter) {
        if (recyclerView == null) {
            throw RuntimeException("You must provide the BottomSheet with a RecyclerView before attaching an Adapter")
        }

        bottomSheetAdapter = adapter
        bottomSheetBehavior = AnchorPointBottomSheetBehavior.from(this)

        recyclerView?.let {
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = bottomSheetAdapter
            it.isNestedScrollingEnabled = false

            it.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                // this has to be so gross so we can remove this listener
                override fun onGlobalLayout() {
                    bottomSheetBehavior!!.state = BottomSheetState.STATE_COLLAPSED
                    it.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        }

        this.isNestedScrollingEnabled = true
        this.overScrollMode = View.OVER_SCROLL_ALWAYS

        val onBottomSheetStateChanged = object : AnchorPointBottomSheetBehavior.BottomSheetStateCallback {
            override fun onStateChanged(bottomSheet: View, newState: BottomSheetState) {
                if (postOnStableStateRunnables.get(newState.ordinal) != null && !postOnStableStateRunnables.get(newState.ordinal).isEmpty()) {
                    for (runnable in postOnStableStateRunnables.get(newState.ordinal)) {
                        runnable.run()
                        postOnStableStateRunnables.get(newState.ordinal).remove(runnable)
                    }
                }

                @Suppress("NON_EXHAUSTIVE_WHEN")
                when (newState) {
                    BottomSheetState.STATE_ANCHOR_POINT -> reset()
                    BottomSheetState.STATE_COLLAPSED -> {
                        isActive = false
                        activatedListener?.isActivated(false)
                        reset()
                    }
                }
            }
        }
        bottomSheetBehavior!!.addBottomSheetStateCallback(onBottomSheetStateChanged)
    }

    open fun reset() {
        ViewCompat.postOnAnimation(this) {
            smoothScrollTo(0, 0)
            logger.trace("bottomsheet settling")
        }
    }

    fun addOnStateChangedListener(stateCallback: AnchorPointBottomSheetBehavior.BottomSheetStateCallback) {
        bottomSheetBehavior?.addBottomSheetStateCallback(stateCallback)
    }

    fun postOnStateChange(state: BottomSheetState, runnable: Runnable) {
        if (bottomSheetBehavior == null) {
            throw RuntimeException("No BottomSheetBehavior attached")
        }

        if (state == bottomSheetBehavior!!.state) {
            runnable.run()
            return
        }

        if (postOnStableStateRunnables.get(state.ordinal) != null) {
            postOnStableStateRunnables.get(state.ordinal).add(runnable)
        } else {
            postOnStableStateRunnables.put(state.ordinal, object : ArrayList<Runnable>() {
                init {
                    add(runnable)
                }
            })
        }
    }
}
