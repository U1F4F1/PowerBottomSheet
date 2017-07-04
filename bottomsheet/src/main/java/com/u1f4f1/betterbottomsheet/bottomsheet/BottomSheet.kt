@file:Suppress("unused", "UNUSED_PARAMETER")

package com.u1f4f1.betterbottomsheet.bottomsheet

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.view.ViewCompat
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.view.ViewTreeObserver

import com.u1f4f1.betterbottomsheet.coordinatorlayoutbehaviors.AnchorPointBottomSheetBehavior

import java.util.ArrayList

import inkapplicaitons.android.logger.ConsoleLogger
import inkapplicaitons.android.logger.Logger

abstract class BottomSheet : NestedScrollView {
    internal var recyclerView: RecyclerView? = null
    private var bottomSheetBehavior: AnchorPointBottomSheetBehavior<*>? = null

    private val postOnStableStateRunnables = SparseArray<MutableList<Runnable>>()

    private var activatedListener: OnSheetActivatedListener? = null
    private var bottomSheetAdapter: BottomSheetAdapter? = null

    var isActive: Boolean = false
        set(isActive) {
            field = isActive
            activatedListener?.isActivated(isActive)
        }

    internal var logger: Logger = ConsoleLogger("ANDROIDISBAD")

    interface OnSheetActivatedListener {
        fun isActivated(isActive: Boolean)
    }

    constructor(context: Context) : super(context) {
        inflateLayout(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        inflateLayout(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        inflateLayout(context, attrs)
    }

    internal abstract fun inflateLayout(context: Context, attrs: AttributeSet?)

    fun setActivatedListener(activatedListener: OnSheetActivatedListener) {
        this.activatedListener = activatedListener
    }

    fun removeAdapter() {
        this.recyclerView?.adapter = null
    }

    private fun setupRecyclerview(adapter: BottomSheetAdapter) {
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
                    bottomSheetBehavior!!.state = AnchorPointBottomSheetBehavior.STATE_COLLAPSED
                    it.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        }

        this.isNestedScrollingEnabled = true
        this.overScrollMode = View.OVER_SCROLL_ALWAYS

        bottomSheetBehavior?.addBottomSheetStateCallback(this::onBottomSheetStateChanged)
    }

    fun reset() {
        ViewCompat.postOnAnimation(this) {
            smoothScrollTo(0, 0)
            logger.trace("bottomsheet settling")
        }
    }

    fun addOnStateChangedListener(stateCallback: AnchorPointBottomSheetBehavior.BottomSheetStateCallback) {
        bottomSheetBehavior?.addBottomSheetStateCallback(stateCallback)
    }

    fun postOnStateChange(@AnchorPointBottomSheetBehavior.State state: Int, runnable: Runnable) {
        if (bottomSheetBehavior == null) {
            throw RuntimeException("No BottomSheetBehavior attached")
        }

        if (state == bottomSheetBehavior!!.state) {
            runnable.run()
            return
        }

        if (postOnStableStateRunnables.get(state) != null) {
            postOnStableStateRunnables.get(state).add(runnable)
        } else {
            postOnStableStateRunnables.put(state, object : ArrayList<Runnable>() {
                init {
                    add(runnable)
                }
            })
        }
    }

    @SuppressLint("SwitchIntDef")
    internal fun onBottomSheetStateChanged(bottomSheet: View, @AnchorPointBottomSheetBehavior.State newState: Int) {
        if (postOnStableStateRunnables.get(newState) != null && !postOnStableStateRunnables.get(newState).isEmpty()) {
            for (runnable in postOnStableStateRunnables.get(newState)) {
                runnable.run()
                postOnStableStateRunnables.get(newState).remove(runnable)
            }
        }

        when (newState) {
            AnchorPointBottomSheetBehavior.STATE_ANCHOR_POINT -> reset()
            AnchorPointBottomSheetBehavior.STATE_COLLAPSED -> {
                isActive = false
                activatedListener?.isActivated(false)
                reset()
            }
        }
    }
}
