package com.davemorrissey.labs.subscaleview.test.extension.views

import android.graphics.Path
import com.davemorrissey.labs.subscaleview.test.extension.views.Action
import java.io.Writer

class Move(val x: Float, val y: Float) : Action {
    override fun getTargetX(): Float {
        return x
    }

    override fun getTargetY(): Float {
        return y
    }

    override fun perform(path: Path) {
        path.moveTo(x, y)
    }

    override fun perform(writer: Writer) {
        writer.write("M$x,$y")
    }
}