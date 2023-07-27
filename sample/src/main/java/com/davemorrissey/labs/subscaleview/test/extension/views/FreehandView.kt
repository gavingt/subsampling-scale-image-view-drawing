package com.davemorrissey.labs.subscaleview.test.extension.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.View.OnTouchListener
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.io.Serializable



class FreehandView @JvmOverloads constructor(context: Context?, attr: AttributeSet? = null) : SubsamplingScaleImageView(context, attr), OnTouchListener {

    private var paint = Paint()

    // Note that the "v" prefix indicates the measurement is wrt the ImageView (v is for view).
    // While the "s" prefix indicates the measurement is wrt the screen (s is for source).
    // We must convert measurements between "v" to "s" when moving between onDraw() and onTouchEvent().

    // Used during onDraw() to track what is being drawn. initialize it here to avoid allocations during onDraw().
    private var vPrev = PointF()
    private var vPath = Path()
    private val vPoint = PointF()

    // Used during onTouchEvent() to track what's being touched.
    private var vPrevious: PointF? = null
    private var vStart: PointF? = null

    // Cache each drawing as a separate MutableList<PointF?>?.
    private var sPointsList: MutableList<MutableList<PointF?>?> = mutableListOf()

    init {
        setOnTouchListener(this)

        paint.apply {
            color = Color.parseColor("#660000FF")
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
        sPointsList.clear()
        invalidate()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Don't draw anything before image is ready.
        if (!isReady) return

        // Draw based on a temp list, since we can't guarantee that sPointsList won't change while we're iterating through it.
        val sPointsTempList = mutableListOf<MutableList<PointF?>?>()
        sPointsTempList.addAll(sPointsList)

        // Iterate through each item in sPointsTempList and draw it.
        sPointsTempList.forEach { sPoints ->
            if (sPoints != null && sPoints.size >= 2) {
                vPath.reset()
                // vPrev is initialized here based on the value of sPoints[0].
                sourceToViewCoord(sPoints[0]!!.x, sPoints[0]!!.y, vPrev)
                vPath.moveTo(vPrev.x, vPrev.y)
                for (i in 1 until sPoints.size) {
                    sourceToViewCoord(sPoints[i]!!.x, sPoints[i]!!.y, vPoint)
                    vPath.quadTo(vPrev.x, vPrev.y, (vPoint.x + vPrev.x) / 2, (vPoint.y + vPrev.y) / 2)
                    vPrev = vPoint
                }
                canvas.drawPath(vPath, paint)
            }
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        var consumed = false
        val touchCount = event.pointerCount
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.actionIndex == 0) {
                    vStart = PointF(event.x, event.y)
                    vPrevious = PointF(event.x, event.y)
                } else {
                    vStart = null
                    vPrevious = null
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val sCurrentF = viewToSourceCoord(event.x, event.y)
                val sCurrent = PointF(sCurrentF!!.x, sCurrentF.y)
                val sStart = if (vStart == null) null else PointF(viewToSourceCoord(vStart)!!.x, viewToSourceCoord(vStart)!!.y)
                if (touchCount == 1 && vStart != null) {
                    val vDX = Math.abs(event.x - vPrevious!!.x)
                    val vDY = Math.abs(event.y - vPrevious!!.y)
                    if (vDX >= paint.strokeWidth / 2 || vDY >= paint.strokeWidth / 2) {
                        if (sPointsList.isEmpty()) {
                            sPointsList.add(mutableListOf(sStart))
                        }
                        sPointsList.last()!!.add(sCurrent)
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
                // Add a new sPoints object to represent the next drawn item.
                sPointsList.add(mutableListOf())
                invalidate()
                vPrevious = null
                vStart = null
            }
        }
        // Use parent to handle pinch and two-finger pan.
        return consumed || super.onTouchEvent(event)
    }
}