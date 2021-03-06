package one.mixin.android.worker

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.WorkerParameters
import com.google.firebase.iid.FirebaseInstanceId
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.schedulers.Schedulers
import one.mixin.android.api.request.SessionRequest
import one.mixin.android.api.service.AccountService
import one.mixin.android.di.worker.ChildWorkerFactory

class RefreshFcmWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    val accountService: AccountService
) : BaseWork(context, parameters) {

    companion object {
        const val TOKEN = "token"
    }

    @SuppressLint("CheckResult")
    override fun onRun(): Result {
        val token = inputData.getString(TOKEN)
        if (token != null) {
            accountService.updateSession(SessionRequest(notificationToken = token))
                .observeOn(Schedulers.io()).subscribe({}, {})
        } else {
            FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { result ->
                accountService.updateSession(SessionRequest(notificationToken = result.token))
                    .observeOn(Schedulers.io()).subscribe({}, {})
            }
        }
        return Result.success()
    }

    @AssistedInject.Factory
    interface Factory : ChildWorkerFactory
}