package org.breezyweather.main.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast

object TToast {
    private var sToast: Toast? = null

    @JvmOverloads
    fun show(context: Context?, msg: String, duration: Int = Toast.LENGTH_SHORT) {
        val toast = getToast(context)
        if (toast != null) {
            toast.duration = duration
            toast.setText(msg.toString())
            toast.show()
        } else {
            Log.i("TToast", "toast msg: $msg")
        }
    }

    @SuppressLint("ShowToast")
    private fun getToast(context: Context?): Toast? {
        if (context == null) {
            return sToast
        }
        //        if (sToast == null) {
//            synchronized (TToast.class) {
//                if (sToast == null) {
        sToast = Toast.makeText(context.applicationContext, "", Toast.LENGTH_SHORT)
        //                }
//            }
//        }
        return sToast
    }

    fun reset() {
        sToast = null
    }
}