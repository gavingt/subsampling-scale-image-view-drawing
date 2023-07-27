package com.davemorrissey.labs.subscaleview.test.extension.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.View.OnTouchListener
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.io.Serializable

data class PaintOptions(var color: Int = Color.parseColor("#660000FF"), var alpha: Int = 50) : Serializable


class FreehandView @JvmOverloads constructor(context: Context?, attr: AttributeSet? = null) : SubsamplingScaleImageView(context, attr), OnTouchListener {

    private var paint = Paint()
    private var paintOptions = PaintOptions()

    private val vPath = Path()
    private val vPoint = PointF()
    private var vPrev = PointF()
    private var vPrevious: PointF? = null
    private var vStart: PointF? = null
    private var sPoints: MutableList<PointF?>? = null


    init {
        setOnTouchListener(this)

        paint.apply {
            color = paintOptions.color
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            // Base strokeWidth on density or hard-code it?
            val density = resources.displayMetrics.densityDpi.toFloat()
            strokeWidth = density / 20f
            isAntiAlias = true
        }
    }


    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        return false
    }


    fun reset() {
        sPoints = null
        invalidate()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Don't draw anything before image is ready.
        if (!isReady) return

        if (sPoints != null && sPoints!!.size >= 2) {
            vPath.reset()
            sourceToViewCoord(sPoints!![0]!!.x, sPoints!![0]!!.y, vPrev)
            vPath.moveTo(vPrev.x, vPrev.y)
            for (i in 1 until sPoints!!.size) {
                sourceToViewCoord(sPoints!![i]!!.x, sPoints!![i]!!.y, vPoint)
                vPath.quadTo(vPrev.x, vPrev.y, (vPoint.x + vPrev.x) / 2, (vPoint.y + vPrev.y) / 2)
                vPrev = vPoint
            }
            canvas.drawPath(vPath, paint)
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        var consumed = false
        val touchCount = event.pointerCount
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> if (event.actionIndex == 0) {
                vStart = PointF(event.x, event.y)
                vPrevious = PointF(event.x, event.y)
            } else {
                vStart = null
                vPrevious = null
            }

            MotionEvent.ACTION_MOVE -> {
                val sCurrentF = viewToSourceCoord(event.x, event.y)
                val sCurrent = PointF(sCurrentF!!.x, sCurrentF.y)
                val sStart = if (vStart == null) null else PointF(viewToSourceCoord(vStart)!!.x, viewToSourceCoord(vStart)!!.y)
                if (touchCount == 1 && vStart != null) {
                    val vDX = Math.abs(event.x - vPrevious!!.x)
                    val vDY = Math.abs(event.y - vPrevious!!.y)
                    if (vDX >= paint.strokeWidth * 2 || vDY >= paint.strokeWidth * 2) {
                        if (sPoints == null) {
                            sPoints = mutableListOf()
                            sPoints!!.add(sStart)
                        }
                        sPoints!!.add(sCurrent)
                        vPrevious!!.x = event.x
                        vPrevious!!.y = event.y
                    }
                    consumed = true
                    invalidate()
                } else if (touchCount == 1) {
                    // Consume all one touch drags to prevent odd panning effects handled by the superclass.
                    consumed = true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                invalidate()
                vPrevious = null
                vStart = null
            }
        }
        // Use parent to handle pinch and two-finger pan.
        return consumed || super.onTouchEvent(event)
    }
}