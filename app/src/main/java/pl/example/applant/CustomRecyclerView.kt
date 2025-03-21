package pl.example.applant

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class CustomRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {

    var gestureDetector: GestureDetector? = null

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        gestureDetector?.onTouchEvent(e) // Przekaż zdarzenie do GestureDetector
        return super.onInterceptTouchEvent(e)
    }
}