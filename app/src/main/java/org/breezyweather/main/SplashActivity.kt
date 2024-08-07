/**
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.CSJAdError
import com.bytedance.sdk.openadsdk.CSJSplashAd
import com.bytedance.sdk.openadsdk.TTAdConfig
import com.bytedance.sdk.openadsdk.TTAdConstant
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTAppDownloadListener
import com.bytedance.sdk.openadsdk.TTCustomController
import com.bytedance.sdk.openadsdk.TTSplashAd
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.breezyweather.Migrations
import org.breezyweather.R
import org.breezyweather.common.basic.GeoActivity
import org.breezyweather.common.bus.EventBus
import org.breezyweather.common.extensions.hasPermission
import org.breezyweather.common.extensions.isDarkMode
import org.breezyweather.common.snackbar.SnackbarContainer
import org.breezyweather.common.ui.composables.AlertDialogNoPadding
import org.breezyweather.common.ui.composables.LocationPreference
import org.breezyweather.common.utils.helpers.IntentHelper
import org.breezyweather.common.utils.helpers.SnackbarHelper
import org.breezyweather.databinding.ActivityMainBinding
import org.breezyweather.main.fragments.HomeFragment
import org.breezyweather.main.fragments.ManagementFragment
import org.breezyweather.main.fragments.ModifyMainSystemBarMessage
import org.breezyweather.main.fragments.PushedManagementFragment
import org.breezyweather.main.utils.ADUIUtils
import org.breezyweather.main.utils.MainThemeColorProvider
import org.breezyweather.main.utils.TToast.show
import org.breezyweather.search.SearchActivity
import org.breezyweather.settings.SettingsChangedMessage
import org.breezyweather.sources.SourceManager
import org.breezyweather.theme.compose.BreezyWeatherTheme
import org.breezyweather.theme.compose.DayNightTheme
import javax.inject.Inject


//import com.bytedance.sdk.openadsdk.MediationPrivacyConfig
@AndroidEntryPoint
class SplashActivity : GeoActivity(),
    HomeFragment.Callback,
    ManagementFragment.Callback {

    @Inject
    lateinit var sourceManager: SourceManager

    private lateinit var binding: ActivityMainBinding
//    private lateinit var viewModel: MainActivityViewModel

    private val _dialogPerLocationSettingsOpen = MutableStateFlow(false)
    val dialogPerLocationSettingsOpen = _dialogPerLocationSettingsOpen.asStateFlow()
    override fun onManageIconClicked() {
        // 实现你的逻辑
        // 例如，打开管理界面
    }

    companion object {
        const val SEARCH_ACTIVITY = 4

        private val mTTAdNative: TTAdNative? = null
        private val mSplashContainer: FrameLayout? = null

        const val AD_TIME_OUT: Int = 3000
        private const val mCodeId = "103046686" //开屏广告代码位id
        private const val mIsExpress = false //是否请求模板广告
        private const val mIsHalfSize = false

        const val ACTION_MAIN = "org.breezyweather.Main"
        const val KEY_MAIN_ACTIVITY_LOCATION_FORMATTED_ID = "MAIN_ACTIVITY_LOCATION_FORMATTED_ID"
        const val KEY_MAIN_ACTIVITY_ALERT_ID = "MAIN_ACTIVITY_ALERT_ID"

        const val ACTION_MANAGEMENT = "org.breezyweather.ACTION_MANAGEMENT"
        const val ACTION_SHOW_ALERTS = "org.breezyweather.ACTION_SHOW_ALERTS"

        const val ACTION_SHOW_DAILY_FORECAST = "org.breezyweather.ACTION_SHOW_DAILY_FORECAST"
        const val KEY_DAILY_INDEX = "DAILY_INDEX"

        private const val TAG_FRAGMENT_HOME = "fragment_main"
        private const val TAG_FRAGMENT_MANAGEMENT = "fragment_management"
    }


    private lateinit var mSplashContainer: FrameLayout
    private lateinit var adView: AdView
    override fun onCreate(savedInstanceState: Bundle?) {
        val isLaunch = savedInstanceState == null

        super.onCreate(savedInstanceState)

        initMediationAdSdk(this)

        if (isLaunch) {
            Migrations.upgrade(applicationContext)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // 初始化 mSplashContainer
        mSplashContainer = findViewById(R.id.splash_container)


        // 初始化Mobile Ads SDK
        MobileAds.initialize(this) { }
        // 初始化广告视图
        adView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }



    override fun onActivityReenter(resultCode: Int, data: Intent) {
        super.onActivityReenter(resultCode, data)
        if (resultCode == SEARCH_ACTIVITY) {
            val f = findManagementFragment()
            f?.prepareReenterTransition()
        }
    }



    //初始化聚合sdk
    private fun initMediationAdSdk(context: Context) {
        TTAdSdk.init(context, buildConfig(context))
        TTAdSdk.start(object : TTAdSdk.Callback {
            override fun success() {
                //初始化成功
                //在初始化成功回调之后进行广告加载
                loadSplashAd(context)
            }
            override fun fail(code: Int, msg: String?) {
                //初始化失败
                Log.d("初始化失败", "Splash  UNsuccessfully."+msg)
            }
        })
    }

    // 构造TTAdConfig
    private fun buildConfig(context: Context): TTAdConfig {
        return TTAdConfig.Builder()
            .appId("5585238") //APP ID
            .appName("彩云天气Pro") //APP Name
            .useMediation(true)  //开启聚合功能
            .debug(false)  //关闭debug开关
            .themeStatus(0)  //正常模式  0是正常模式；1是夜间模式；
            /**
             * 多进程增加注释说明：V>=5.1.6.0支持多进程，如需开启可在初始化时设置.supportMultiProcess(true) ，默认false；
             * 注意：开启多进程开关时需要将ADN的多进程也开启，否则广告展示异常，影响收益。
             * CSJ、gdt无需额外设置，KS、baidu、Sigmob、Mintegral需要在清单文件中配置各家ADN激励全屏xxxActivity属性android:multiprocess="true"
             */
            .supportMultiProcess(false)  //不支持
            .customController(getTTCustomController())  //设置隐私权
            .build()
    }
    //设置隐私合规
    private fun getTTCustomController(): TTCustomController? {
        return object : TTCustomController() {
            override fun isCanUseLocation(): Boolean {  //是否授权位置权限
                return true
            }

            override fun isCanUsePhoneState(): Boolean {  //是否授权手机信息权限
                return true
            }

            override fun isCanUseWifiState(): Boolean {  //是否授权wifi state权限
                return true
            }

            override fun isCanUseWriteExternal(): Boolean {  //是否授权写外部存储权限
                return true
            }

            override fun isCanUseAndroidId(): Boolean {  //是否授权Android Id权限
                return true
            }

//            override fun getMediationPrivacyConfig(): MediationPrivacyConfig? {
//                return object : MediationPrivacyConfig() {
//                    override fun isLimitPersonalAds(): Boolean {  //是否限制个性化广告
//                        return false
//                    }
//
//                    override fun isProgrammaticRecommend(): Boolean {  //是否开启程序化广告推荐
//                        return true
//                    }
//                }
//            }
        }
    }


    /**
     * 跳转到主页面
     */
    private fun goToMainActivity() {
        val intent = Intent(
            this@SplashActivity,
            MainActivity::class.java
        )
        startActivity(intent)
        mSplashContainer.removeAllViews() //移除所有视图
        this.finish()
    }

    private fun showToast(msg: String) {
        show(this, msg)
    }

    /**
     * 隐藏虚拟按键，并且全屏
     */
    protected fun hideBottomUIMenu() {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            val v = this.window.decorView
            v.systemUiVisibility = View.GONE
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            val decorView = window.decorView
            val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN)
            decorView.systemUiVisibility = uiOptions
        }
    }
    fun buildSplashAdslot(): AdSlot {
        var adSlot: AdSlot? = null
        val splashWidthDp: Float = ADUIUtils.getScreenWidthDp(this)
        val splashWidthPx: Int = ADUIUtils.getScreenWidthInPx(this)
        val screenHeightPx: Int = ADUIUtils.getScreenHeight(this)
        val screenHeightDp: Float = ADUIUtils.px2dip(this, screenHeightPx.toFloat()).toFloat()
        val splashHeightDp: Float
        val splashHeightPx: Int
        if (mIsHalfSize) {
            // 开屏高度 = 屏幕高度 - 下方预留的高度，demo中是预留了屏幕高度的1/5，因此开屏高度传入 屏幕高度*4/5
            splashHeightDp = screenHeightDp * 4 / 5f
            splashHeightPx = (screenHeightPx * 4 / 5f).toInt()
        } else {
            splashHeightDp = screenHeightDp
            splashHeightPx = screenHeightPx
        }
        adSlot = if (mIsExpress) {
            //个性化模板广告需要传入期望广告view的宽、高，单位dp，请传入实际需要的大小，
            //比如：广告下方拼接logo、适配刘海屏等，需要考虑实际广告大小
            //            float expressViewWidth = UIUtils.getScreenWidthDp(this);
            //            float expressViewHeight = UIUtils.getHeight(this);
            return AdSlot.Builder()
                .setCodeId(mCodeId)
                .setSupportDeepLink(true)
                .setImageAcceptedSize(
                    splashWidthPx,
                    splashHeightPx
                ) //模板广告需要设置期望个性化模板广告的大小,单位dp,代码位是否属于个性化模板广告，请在穿山甲平台查看
                .setExpressViewAcceptedSize(splashWidthDp, splashHeightDp)
                .build()
        } else {
            return AdSlot.Builder()
                .setCodeId(mCodeId)
                .setSupportDeepLink(true)
                .setImageAcceptedSize(splashWidthPx, splashHeightPx)
                .build()
        }
    }
    private fun loadSplashAd(context: Context) {
        val adNativeLoader = TTAdSdk.getAdManager().createAdNative(this)
        adNativeLoader.loadSplashAd(buildSplashAdslot(), object : TTAdNative.CSJSplashAdListener {
            override fun onSplashRenderSuccess(csjSplashAd: CSJSplashAd) {
                // 处理广告渲染成功
                Log.d("处理广告渲染成功", "Splash ad render successfully.")
            }
            override fun onSplashLoadSuccess(csjSplashAd: CSJSplashAd) {
                Log.d("处理广告渲染成功", "Splash ad loaded successfully.")
                showSplashAd(csjSplashAd)
            }

            override fun onSplashLoadFail(csjAdError: CSJAdError) {
                Log.e("处理广告渲染失败", "Splash ad load failed: ${csjAdError.getMsg()}")
            }
            override fun onSplashRenderFail(csjSplashAd: CSJSplashAd, csjAdError: CSJAdError) {
                // 处理广告渲染失败
                Log.e("处理广告渲染失败", "Splash ad render failed: ${csjAdError.getMsg()}")

            }
        }, AD_TIME_OUT)
    }

    private fun showSplashAd(csjSplashAd: CSJSplashAd) {
        // 展示广告的逻辑
        csjSplashAd.setSplashAdListener(object : CSJSplashAd.SplashAdListener {
            override fun onSplashAdShow(csjSplashAd: CSJSplashAd) {
                Log.d("AdLoad", "Splash ad is shown.")
            }

            override fun onSplashAdClick(csjSplashAd: CSJSplashAd) {
                Log.d("AdLoad", "Splash ad is clicked.")
            }

            override fun onSplashAdClose(csjSplashAd: CSJSplashAd, i: Int) {
                // 广告关闭后，销毁广告页面
//                finish()
//                goToMainActivity()
            }
        })
        // 获取广告视图并添加到容器中
        val splashView = csjSplashAd.splashView
        if (splashView != null) {
            // 确保容器已清空
            mSplashContainer.removeAllViews()
            // 将广告视图添加到容器中
            mSplashContainer.addView(splashView)
        } else {
            Log.e("AdLoad", "Splash ad view is null.")
        }
    }


    private fun getLocationId(intent: Intent?): String? {
        return intent?.getStringExtra(KEY_MAIN_ACTIVITY_LOCATION_FORMATTED_ID)
    }



    private fun findHomeFragment(): HomeFragment? {
        return if (binding.drawerLayout == null) {
            supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_HOME) as HomeFragment?
        } else {
            supportFragmentManager.findFragmentById(R.id.fragment_home) as HomeFragment?
        }
    }

    private fun findManagementFragment(): ManagementFragment? {
        return if (binding.drawerLayout == null) {
            supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_MANAGEMENT) as ManagementFragment?
        } else {
            supportFragmentManager.findFragmentById(R.id.fragment_drawer) as ManagementFragment?
        }
    }

    // interface.

    // main fragment callback.
    override fun onEditIconClicked() {
        _dialogPerLocationSettingsOpen.value = true
    }

    override fun onSettingsIconClicked() {
        IntentHelper.startSettingsActivity(this)
    }

    // management fragment callback.

    override fun onSearchBarClicked() {
        IntentHelper.startSearchActivityForResult(this, SEARCH_ACTIVITY)
    }
}
