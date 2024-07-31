package org.breezyweather.main.utils

import android.view.View
import android.view.ViewGroup

object ADUIUtils {
    fun removeFromParent(view: View?) {
        view?.parent?.let {
            (it as ViewGroup).removeView(view)
        }
    }
}