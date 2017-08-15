@file:Suppress("unused", "UNUSED_PARAMETER")

package com.u1f4f1.powerbottomsheet.bottomsheet

import android.content.Context
import android.support.v4.view.ViewCompat
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.view.ViewTreeObserver
import com.u1f4f1.powerbottomsheet.coordinatorlayoutbehaviors.AnchorPointBottomSheetBehavior
import com.u1f4f1.powerbottomsheet.humanReadableToString
import com.u1f4f1.powerbottomsheet.info
import com.u1f4f1.powerbottomsheet.trace
import java.util.*

abstract class BottomSheet : NestedScrollView {
    var recyclerView: RecyclerView? = null
    protected open var bottomSheetBehavior: AnchorPointBottomSheetBehavior<*>? = null
        get() = field
        set(value) {
            field = value

            if (stateCallbacks.isNotEmpty()) {
                stateCallbacks.forEach {
                    field!!.addBottomSheetStateCallback(it)
                    stateCallbacks.remove(it)
                }
            }

            if (slideCallbacks.isNotEmpty()) {
                slideCallbacks.forEach {
                    field!!.addBottomSheetSlideCallback(it)
                    slideCallbacks.remove(it)
                }
            }

            if (activatedCallback != null) {
                field!!.activeCallback = activatedCallback
            }
        }

    private val postOnStableStateRunnables = SparseArray<MutableList<Runnable>>()

    private var bottomSheetAdapter: BottomSheetAdapter? = null

    private var activatedCallback: AnchorPointBottomSheetBehavior.OnSheetActivatedListener? = null

    private var stateCallbacks: MutableSet<AnchorPointBottomSheetBehavior.BottomSheetStateCallback> = mutableSetOf()
    private var slideCallbacks: MutableSet<AnchorPointBottomSheetBehavior.BottomSheetSlideCallback> = mutableSetOf()

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        info("Created BottomSheet ${this.humanReadableToString()}")
    }

    fun setActivatedListener(activatedListener: AnchorPointBottomSheetBehavior.OnSheetActivatedListener) {
        if (bottomSheetAdapter == null) {
            activatedCallback = activatedListener
        } else {
            bottomSheetBehavior?.activeCallback = activatedListener
        }
    }

    open fun removeAdapter() {
        this.recyclerView?.adapter = null
    }

    open fun getState() : BottomSheetState? {
        return this.bottomSheetBehavior?.state
    }

    open fun setState(state: BottomSheetState) {
        this.bottomSheetBehavior!!.state = state
    }

    open fun addBottomSheetStateCallback(callback: AnchorPointBottomSheetBehavior.BottomSheetStateCallback) {
        if (this.bottomSheetBehavior == null) {
            this.stateCallbacks.add(callback)
        } else {
            this.bottomSheetBehavior?.addBottomSheetStateCallback(callback)
        }
    }

    open fun addBottomSheetSlideCallback(callback: AnchorPointBottomSheetBehavior.BottomSheetSlideCallback) {
        if (this.bottomSheetBehavior == null) {
            this.slideCallbacks.add(callback)
        } else {
            this.bottomSheetBehavior?.addBottomSheetSlideCallback(callback)
        }
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
                    BottomSheetState.STATE_ANCHOR_POINT,
                    BottomSheetState.STATE_COLLAPSED -> {
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
            trace("bottomsheet settling")
        }
    }

    fun addOnStateChangedListener(stateCallback: AnchorPointBottomSheetBehavior.BottomSheetStateCallback) {
        bottomSheetBehavior!!.addBottomSheetStateCallback(stateCallback)
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
