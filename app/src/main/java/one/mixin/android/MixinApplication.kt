package one.mixin.android

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.work.Configuration
import androidx.work.WorkManager
import com.bugsnag.android.Bugsnag
import com.crashlytics.android.Crashlytics
import com.facebook.stetho.Stetho
import com.google.firebase.FirebaseApp
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import io.reactivex.plugins.RxJavaPlugins
import one.mixin.android.crypto.MixinSignalProtocolLogger
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.db.MixinDatabase
import one.mixin.android.di.AppComponent
import one.mixin.android.di.AppInjector
import one.mixin.android.di.worker.MixinWorkerFactory
import one.mixin.android.extension.clear
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.job.BlazeMessageService
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.landing.InitializeActivity
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.util.Session
import one.mixin.android.webrtc.CallService
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.uiThread
import org.whispersystems.libsignal.logging.SignalProtocolLoggerProvider
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class MixinApplication : Application(), HasActivityInjector, HasServiceInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var dispatchingServiceInjector: DispatchingAndroidInjector<Service>

    @Inject
    lateinit var mixinWorkerFactory: MixinWorkerFactory

    @Inject
    lateinit var jobManager: MixinJobManager

    lateinit var appComponent: AppComponent

    companion object {
        lateinit var appContext: Context
        @JvmField
        var conversationId: String? = null

        fun get(): MixinApplication = appContext as MixinApplication
    }

    override fun onCreate() {
        super.onCreate()
        init()
        FirebaseApp.initializeApp(this)
        SignalProtocolLoggerProvider.setProvider(MixinSignalProtocolLogger())
        MixinApplication.appContext = applicationContext
        AndroidThreeTen.init(this)
        appComponent = AppInjector.init(this)
        val wmConfig = Configuration.Builder().setWorkerFactory(mixinWorkerFactory).build()
        WorkManager.initialize(this, wmConfig)
        RxJavaPlugins.setErrorHandler {}
    }

    private fun init() {
        Bugsnag.init(this, BuildConfig.BUGSNAG_API_KEY)
        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
            Timber.plant(Timber.DebugTree())
        }
    }

    fun inject() {
        appComponent = AppInjector.inject(this)
    }

    override fun activityInjector(): DispatchingAndroidInjector<Activity>? = dispatchingAndroidInjector
    override fun serviceInjector(): DispatchingAndroidInjector<Service>? = dispatchingServiceInjector

    var onlining = AtomicBoolean(false)

    fun gotoTimeWrong(serverTime: Long) {
        if (onlining.compareAndSet(true, false)) {
            val ise = IllegalStateException("Time error: Server-Time $serverTime - Local-Time ${System.currentTimeMillis()}")
            Crashlytics.logException(ise)
            BlazeMessageService.stopService(this)
            CallService.disconnect(this)
            notificationManager.cancelAll()
            defaultSharedPreferences.putBoolean(Constants.Account.PREF_WRONG_TIME, true)
            InitializeActivity.showWongTimeTop(this)
        }
    }

    fun closeAndClear(toLanding: Boolean = true) {
        if (onlining.compareAndSet(true, false)) {
            BlazeMessageService.stopService(this)
            CallService.disconnect(this)
            notificationManager.cancelAll()
            Session.clearAccount()
            defaultSharedPreferences.clear()
            defaultSharedPreferences.putBoolean(Constants.Account.PREF_LOGOUT_COMPLETE, false)
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            if (toLanding) {
                doAsync {
                    clearData()

                    uiThread {
                        inject()
                        LandingActivity.show(this@MixinApplication)
                    }
                }
            } else {
                clearData()
                inject()
            }
        }
    }

    fun clearData() {
        jobManager.cancelAllJob()
        jobManager.clear()
        SignalDatabase.getDatabase(this).clearAllTables()
        MixinDatabase.getDatabase(this).clearAllTables()
        defaultSharedPreferences.putBoolean(Constants.Account.PREF_LOGOUT_COMPLETE, true)
    }
}