package one.mixin.android.ui.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_pin_bottom_sheet.view.*
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.ui.common.PinBottomSheetDialogFragment
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Account
import one.mixin.android.widget.PinView

class PinBiometricsBottomSheetDialogFragment : PinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "PinBiometricsBottomSheetDialogFragment"
        const val FROM_WALLET_SETTING = "from_wallet_setting"

        fun newInstance(fromWalletSetting: Boolean) = PinBiometricsBottomSheetDialogFragment().apply {
            arguments = bundleOf(FROM_WALLET_SETTING to fromWalletSetting)
        }
    }

    private val fromWalletSetting by lazy { arguments!!.getBoolean(FROM_WALLET_SETTING) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.pin.setListener(object : PinView.OnPinListener {
            override fun onUpdate(index: Int) {
                if (index != contentView.pin.getCount()) return

                contentView.pin_va?.displayedChild = PinBottomSheetDialogFragment.POS_PB
                bottomViewModel.verifyPin(contentView.pin.code()).autoDisposable(scopeProvider).subscribe({ r: MixinResponse<Account> ->
                    dialog.dismiss()
                    if (r.isSuccess) {
                        context?.updatePinCheck()
                        r.data?.let {
                            if (fromWalletSetting) {
                                val success = BiometricUtil.savePin(requireContext(), contentView.pin.code(),
                                    this@PinBiometricsBottomSheetDialogFragment)
                                if (success) callback?.onSuccess() else dismiss()
                            } else {
                                callback?.onSuccess()
                            }
                        }
                        dismiss()
                    } else {
                        contentView.pin_va?.displayedChild = PinBottomSheetDialogFragment.POS_PIN
                        contentView.pin.clear()
                        ErrorHandler.handleMixinError(r.errorCode)
                    }
                }, { t ->
                    dialog.dismiss()
                    contentView.pin_va?.displayedChild = PinBottomSheetDialogFragment.POS_PIN
                    contentView.pin.clear()
                    ErrorHandler.handleError(t)
                })
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (fromWalletSetting && requestCode == BiometricUtil.REQUEST_CODE_CREDENTIALS && resultCode == Activity.RESULT_OK) {
            BiometricUtil.savePin(requireContext(), contentView.pin.code(), this@PinBiometricsBottomSheetDialogFragment)
        }
    }

    override fun getTipTextRes(): Int =
        if (fromWalletSetting) R.string.wallet_pin_open_biometrics else R.string.wallet_pin_modify_biometrics
}