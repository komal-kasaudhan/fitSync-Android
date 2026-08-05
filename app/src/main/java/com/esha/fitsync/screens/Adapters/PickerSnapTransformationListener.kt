package com.esha.fitsync.ui.custom

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class PickerSnapTransformationListener(
    private val onItemSelected: (position: Int) -> Unit
) : RecyclerView.OnScrollListener() {

    private val snapHelper = LinearSnapHelper()
    private var lastSnappedPosition = RecyclerView.NO_POSITION

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        val layoutManager = recyclerView.layoutManager ?: return
        val isHorizontal = layoutManager.canScrollHorizontally()
        val midLine = if (isHorizontal) recyclerView.width / 2 else recyclerView.height / 2

        for (i in 0 until layoutManager.childCount) {
            val child = layoutManager.getChildAt(i) ?: continue
            val childCenter = if (isHorizontal) {
                (layoutManager.getDecoratedLeft(child) + layoutManager.getDecoratedRight(child)) / 2
            } else {
                (layoutManager.getDecoratedTop(child) + layoutManager.getDecoratedBottom(child)) / 2
            }

            val absoluteDistance = abs(midLine - childCenter)

            if (!isHorizontal) {

                val containerHeight = recyclerView.height.toFloat()
                val progress = (absoluteDistance.toFloat() / (containerHeight / 2)).coerceIn(0.0f, 1.0f)


                child.scaleX = 1.25f - (progress * 0.25f)
                child.scaleY = 1.25f - (progress * 0.25f)
                child.alpha = 1.0f - (progress * 0.60f)
            }
        }
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            val layoutManager = recyclerView.layoutManager ?: return
            val snapView = snapHelper.findSnapView(layoutManager) ?: return
            val currentPos = layoutManager.getPosition(snapView)

            if (currentPos != RecyclerView.NO_POSITION && currentPos != lastSnappedPosition) {
                lastSnappedPosition = currentPos
                snapView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onItemSelected(currentPos)
            }
        }
    }

    fun attach(recyclerView: RecyclerView) {
        snapHelper.attachToRecyclerView(recyclerView)
        recyclerView.addOnScrollListener(this)
    }
}