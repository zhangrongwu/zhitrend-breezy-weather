package org.breezyweather.main.utils
import android.app.Activity
import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.DisplayCutout
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import java.lang.reflect.InvocationTargetException


object ADUIUtils {
    fun getScreenWidthDp(context: Context): Float {
        val scale = context.resources.displayMetrics.density
        val width = context.resources.displayMetrics.widthPixels.toFloat()
        return width / (if (scale <= 0) 1f else scale) + 0.5f
    }

    //全面屏、刘海屏适配
    fun getHeight(activity: Activity): Float {
        hideBottomUIMenu(activity)
        val height: Float
        val realHeight = getRealHeight(activity)
        height = if (hasNotchScreen(activity)) {
            px2dip(activity, realHeight - getStatusBarHeight(activity)).toFloat()
        } else {
            px2dip(activity, realHeight.toFloat()).toFloat()
        }
        return height
    }

    fun hideBottomUIMenu(activity: Activity?) {
        if (activity == null) {
            return
        }
        try {
            //隐藏虚拟按键，并且全屏
            if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
                val v = activity.window.decorView
                v.systemUiVisibility = View.GONE
            } else if (Build.VERSION.SDK_INT >= 19) {
                //for new api versions.
                val decorView = activity.window.decorView
                val uiOptions = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        //                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
                decorView.systemUiVisibility = uiOptions
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //获取屏幕真实高度，不包含下方虚拟导航栏
    fun getRealHeight(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val dm = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealMetrics(dm)
        } else {
            display.getMetrics(dm)
        }
        val realHeight = dm.heightPixels
        return realHeight
    }

    //获取状态栏高度
    fun getStatusBarHeight(context: Context): Float {
        var height = 0f
        val resourceId = context.applicationContext.resources.getIdentifier(
            "status_bar_height",
            "dimen",
            "android"
        )
        if (resourceId > 0) {
            height =
                context.applicationContext.resources.getDimensionPixelSize(resourceId).toFloat()
        }
        return height
    }

    fun px2dip(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / (if (scale <= 0) 1f else scale) + 0.5f).toInt()
    }

    fun dp2px(context: Context, dp: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    /**
     * 判断是否是刘海屏
     *
     * @return
     */
    fun hasNotchScreen(activity: Activity): Boolean {
        return (isAndroidPHasNotch(activity) || getInt(
            "ro.miui.notch",
            activity
        ) == 1 || hasNotchAtHuawei(activity)
                || hasNotchAtOPPO(activity)
                || hasNotchAtVivo(activity))
    }

    /**
     * Android P 刘海屏判断
     *
     * @param activity
     * @return
     */
    fun isAndroidPHasNotch(activity: Activity): Boolean {
        var result = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            var displayCutout: DisplayCutout? = null
            try {
                val windowInsets = activity.window.decorView.rootWindowInsets
                if (windowInsets != null) {
                    displayCutout = windowInsets.displayCutout
                }
                if (displayCutout != null) {
                    result = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return result
    }

    /**
     * 小米刘海屏判断.
     *
     * @return 0 if it is not notch ; return 1 means notch
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    fun getInt(key: String?, activity: Activity): Int {
        var result = 0
        if (isMiui) {
            try {
                val classLoader = activity.classLoader
                val SystemProperties = classLoader.loadClass("android.os.SystemProperties")
                //参数类型
                val paramTypes = arrayOfNulls<Class<*>?>(2)
                paramTypes[0] = String::class.java
                paramTypes[1] = Int::class.javaPrimitiveType
                val getInt = SystemProperties.getMethod("getInt", *paramTypes)
                //参数
                val params = arrayOfNulls<Any>(2)
                params[0] = java.lang.String(key) as String
                params[1] = 0
                result = getInt.invoke(SystemProperties, *params) as Int
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            }
        }
        return result
    }

    /**
     * 华为刘海屏判断
     *
     * @return
     */
    fun hasNotchAtHuawei(context: Context): Boolean {
        var ret = false
        try {
            val classLoader = context.classLoader
            val HwNotchSizeUtil = classLoader.loadClass("com.huawei.android.util.HwNotchSizeUtil")
            val get = HwNotchSizeUtil.getMethod("hasNotchInScreen")
            ret = get.invoke(HwNotchSizeUtil) as Boolean
        } catch (e: ClassNotFoundException) {
        } catch (e: NoSuchMethodException) {
        } catch (e: Exception) {
        } finally {
            return ret
        }
    }

    const val VIVO_NOTCH: Int = 0x00000020 //是否有刘海
    const val VIVO_FILLET: Int = 0x00000008 //是否有圆角

    /**
     * VIVO刘海屏判断
     *
     * @return
     */
    fun hasNotchAtVivo(context: Context): Boolean {
        var ret = false
        try {
            val classLoader = context.classLoader
            val FtFeature = classLoader.loadClass("android.util.FtFeature")
            val method = FtFeature.getMethod("isFeatureSupport", Int::class.javaPrimitiveType)
            ret = method.invoke(FtFeature, VIVO_NOTCH) as Boolean
        } catch (e: ClassNotFoundException) {
        } catch (e: NoSuchMethodException) {
        } catch (e: Exception) {
        } finally {
            return ret
        }
    }

    /**
     * O-P-P-O刘海屏判断
     *
     * @return
     */
    fun hasNotchAtOPPO(context: Context): Boolean {
        val temp = "com.kllk.feature.screen.heteromorphism"
        val name = getKllkDecryptString(temp)
        return context.packageManager.hasSystemFeature(name)
    }

    val isMiui: Boolean
        get() {
            var sIsMiui = false
            try {
                val clz = Class.forName("miui.os.Build")
                if (clz != null) {
                    sIsMiui = true
                    return sIsMiui
                }
            } catch (e: Exception) {
                // ignore
            }
            return sIsMiui
        }

    /**
     * 用于o-p-p-o 版本隐私协议
     */
    fun getKllkDecryptString(encryptionString: String): String {
        if (TextUtils.isEmpty(encryptionString)) {
            return ""
        }
        var decryptTag = ""
        val decryptCapitalized = "O" + "P" + "P" + "O"
        val decrypt = "o" + "p" + "p" + "o"
        if (encryptionString.contains("KLLK")) {
            decryptTag = encryptionString.replace("KLLK", decryptCapitalized)
        } else if (encryptionString.contains("kllk")) {
            decryptTag = encryptionString.replace("kllk", decrypt)
        }
        return decryptTag
    }

    fun setViewSize(view: View, width: Int, height: Int) {
        if (view.parent is FrameLayout) {
            val lp = view.layoutParams as FrameLayout.LayoutParams
            lp.width = width
            lp.height = height
            view.layoutParams = lp
            view.requestLayout()
        } else if (view.parent is RelativeLayout) {
            val lp = view.layoutParams as RelativeLayout.LayoutParams
            lp.width = width
            lp.height = height
            view.layoutParams = lp
            view.requestLayout()
        } else if (view.parent is LinearLayout) {
            val lp = view.layoutParams as LinearLayout.LayoutParams
            lp.width = width
            lp.height = height
            view.layoutParams = lp
            view.requestLayout()
        }
    }

    fun getScreenWidthInPx(context: Context): Int {
        val dm = context.applicationContext.resources.displayMetrics
        return dm.widthPixels
    }

    fun getScreenHeightInPx(context: Context): Int {
        val dm = context.applicationContext.resources.displayMetrics
        return dm.heightPixels
    }

    fun getScreenHeight(context: Context): Int {
        return (getScreenHeightInPx(context) + getStatusBarHeight(context)).toInt()
    }

    fun removeFromParent(view: View?) {
        if (view != null) {
            val vp = view.parent
            if (vp is ViewGroup) {
                vp.removeView(view)
            }
        }
    }

    /**
     * 获取全面屏宽高
     *
     * @param context
     * @return
     */
    fun getScreenSize(context: Context?): IntArray {
        val size = intArrayOf(0, 0)
        if (context == null) {
            return size
        }
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val dm = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealMetrics(dm)
        } else {
            display.getMetrics(dm)
        }
        size[0] = dm.widthPixels
        size[1] = dm.heightPixels
        return size
    }
}