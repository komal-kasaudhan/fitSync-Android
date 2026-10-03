package com.esha.fitsync.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.esha.fitsync.models.WeeklyNutritionDay
import kotlin.math.max

class WeeklyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var daysData: List<WeeklyNutritionDay> = emptyList()
    private var selectedIndex: Int = -1
    private var animationProgress: Float = 1f
    private var onDaySelectedListener: ((WeeklyNutritionDay) -> Unit)? = null

    private val barTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#122030")
        style = Paint.Style.FILL
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val targetLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#36A8FF")
        strokeWidth = 2.5f
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }
    private val targetTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#36A8FF")
        textSize = 24f
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B7280")
        textSize = 30f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val selectedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val emptyMessagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B7280")
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
    }

    private val barRect = RectF()
    private val trackRect = RectF()

    init {
        initDefaultDays()
    }

    private fun initDefaultDays() {
        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        daysData = labels.map { label ->
            WeeklyNutritionDay(
                date = "",
                dayLabel = label,
                caloriesConsumed = 0,
                calorieTarget = 2200,
                protein = 0,
                carbs = 0,
                fat = 0,
                water = 0,
                goalHit = false
            )
        }
        selectedIndex = daysData.size - 1
    }

    fun setOnDaySelectedListener(listener: (WeeklyNutritionDay) -> Unit) {
        this.onDaySelectedListener = listener
        if (selectedIndex in daysData.indices) {
            listener(daysData[selectedIndex])
        }
    }

    fun setData(days: List<WeeklyNutritionDay>, defaultSelectToday: Boolean = true) {
        val updateBlock = {
            if (days.isNotEmpty()) {
                this.daysData = days
            } else {
                initDefaultDays()
            }

            if (defaultSelectToday || selectedIndex !in daysData.indices) {
                selectedIndex = daysData.size - 1
            }

            // Animate entrance
            val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 600
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    animationProgress = it.animatedValue as Float
                    invalidate()
                }
            }
            animator.start()

            if (selectedIndex in daysData.indices) {
                onDaySelectedListener?.invoke(daysData[selectedIndex])
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            updateBlock()
        } else {
            post(updateBlock)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minHeight = (140 * resources.displayMetrics.density).toInt()
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val finalHeight = if (heightMode == MeasureSpec.EXACTLY) heightSize else minHeight
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (daysData.isEmpty()) initDefaultDays()

        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0 || height <= 0) return

        val bottomPadding = 55f
        val topPadding = 35f
        val chartHeight = height - bottomPadding - topPadding

        val count = daysData.size
        val step = width / count
        val barWidth = step * 0.48f

        // Max target and calories calculation
        val maxTarget = daysData.maxOfOrNull { it.calorieTarget } ?: 2200
        val maxCalories = daysData.maxOfOrNull { it.caloriesConsumed } ?: 0
        val scaleMax = max(maxTarget.toFloat() * 1.15f, max(maxCalories.toFloat() * 1.15f, 1200f))

        // Draw Target Line
        val targetY = topPadding + chartHeight * (1f - (maxTarget.toFloat() / scaleMax).coerceIn(0f, 1f))
        canvas.drawLine(step * 0.15f, targetY, width - step * 0.15f, targetY, targetLinePaint)
        canvas.drawText("TARGET ${maxTarget}", width - step * 0.2f, targetY - 8f, targetTextPaint)

        val hasAnyCalories = daysData.any { it.caloriesConsumed > 0 }

        // Draw each day bar
        for (i in daysData.indices) {
            val day = daysData[i]
            val centerX = i * step + step / 2f
            val isSelected = i == selectedIndex

            val left = centerX - barWidth / 2f
            val right = centerX + barWidth / 2f
            val bottom = height - bottomPadding
            val trackTop = topPadding

            // Draw full background track pill
            trackRect.set(left, trackTop, right, bottom)
            canvas.drawRoundRect(trackRect, 12f, 12f, barTrackPaint)

            // Calculate filled bar height
            val consumedRatio = (day.caloriesConsumed.toFloat() / scaleMax).coerceIn(0f, 1f)
            val barH = chartHeight * consumedRatio * animationProgress
            val minPillHeight = if (day.caloriesConsumed > 0) max(barH, 14f) else 10f
            val top = bottom - minPillHeight

            barRect.set(left, top, right, bottom)

            // Bar colors
            if (isSelected) {
                barPaint.color = if (day.goalHit) Color.parseColor("#10B981") else Color.parseColor("#00D4FF")
                barPaint.alpha = 255
            } else {
                barPaint.color = if (day.goalHit) Color.parseColor("#059669") else Color.parseColor("#162536")
                barPaint.alpha = if (hasAnyCalories) 120 else 180
            }

            canvas.drawRoundRect(barRect, 12f, 12f, barPaint)

            // Day label (Mon..Sun)
            val label = if (day.dayLabel.isNotEmpty()) day.dayLabel.take(3) else "D${i + 1}"
            val paint = if (isSelected) selectedTextPaint else textPaint
            canvas.drawText(label, centerX, height - 12f, paint)
        }

        // If no data logged yet, show subtle message
        if (!hasAnyCalories) {
            canvas.drawText("No calorie logs yet this week • Tap a bar for details", width / 2f, topPadding + chartHeight / 2f, emptyMessagePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> {
                if (daysData.isNotEmpty() && width > 0) {
                    val step = width.toFloat() / daysData.size
                    val tappedIndex = (event.x / step).toInt().coerceIn(0, daysData.size - 1)
                    if (selectedIndex != tappedIndex) {
                        selectedIndex = tappedIndex
                        invalidate()
                        onDaySelectedListener?.invoke(daysData[tappedIndex])
                    }
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
