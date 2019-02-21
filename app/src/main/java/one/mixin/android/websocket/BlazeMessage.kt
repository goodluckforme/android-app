package one.mixin.android.websocket

import com.google.gson.JsonElement
import one.mixin.android.api.ResponseError
import one.mixin.android.vo.MessageStatus
import java.io.Serializable
import java.util.UUID

data class BlazeMessage(
    val id: String,
    val action: String,
    val params: BlazeMessageParam?,
    val data: JsonElement? = null,
    val error: ResponseError? = null
) : Serializable {

    companion object {
        private const val serialVersionUID: Long = -1138873694585349395
    }

    fun isReceiveMessageAction(): Boolean {
        return action == CREATE_MESSAGE || action == ACKNOWLEDGE_MESSAGE_RECEIPT || action == CREATE_CALL
    }
}

const val CREATE_MESSAGE = "CREATE_MESSAGE"
const val CREATE_SESSION_MESSAGE = "CREATE_SESSION_MESSAGE"
const val ACKNOWLEDGE_MESSAGE_RECEIPT = "ACKNOWLEDGE_MESSAGE_RECEIPT"
const val ACKNOWLEDGE_MESSAGE_RECEIPTS = "ACKNOWLEDGE_MESSAGE_RECEIPTS"
const val LIST_PENDING_MESSAGES = "LIST_PENDING_MESSAGES"
const val ERROR_ACTION = "ERROR"
const val COUNT_SIGNAL_KEYS = "COUNT_SIGNAL_KEYS"
const val CONSUME_SIGNAL_KEYS = "CONSUME_SIGNAL_KEYS"
const val CONSUME_SESSION_SIGNAL_KEYS = "CONSUME_SESSION_SIGNAL_KEYS"
const val SYNC_SIGNAL_KEYS = "SYNC_SIGNAL_KEYS"
const val CREATE_SIGNAL_KEY_MESSAGES = "CREATE_SIGNAL_KEY_MESSAGES"
const val CREATE_CALL = "CREATE_CALL"

fun createAckParamBlazeMessage(messageId: String, status: MessageStatus) =
    BlazeMessage(UUID.randomUUID().toString(), ACKNOWLEDGE_MESSAGE_RECEIPT, createAckParam(messageId, status.name))

fun createAckListParamBlazeMessage(messages: List<BlazeAckMessage>) =
    BlazeMessage(UUID.randomUUID().toString(), ACKNOWLEDGE_MESSAGE_RECEIPTS, createAckListParam(messages))

fun createParamBlazeMessage(param: BlazeMessageParam) =
    BlazeMessage(UUID.randomUUID().toString(), CREATE_MESSAGE, param)

fun createParamSessionMessage(param: BlazeMessageParam) =
        BlazeMessage(UUID.randomUUID().toString(), CREATE_SESSION_MESSAGE, param)

fun createListPendingMessage() =
    BlazeMessage(UUID.randomUUID().toString(), LIST_PENDING_MESSAGES, null)

fun createCountSignalKeys() =
    BlazeMessage(UUID.randomUUID().toString(), COUNT_SIGNAL_KEYS, null)

fun createConsumeSignalKeys(param: BlazeMessageParam) =
    BlazeMessage(UUID.randomUUID().toString(), CONSUME_SIGNAL_KEYS, param)

fun createConsumeSessionSignalKeys(param: BlazeMessageParam) =
    BlazeMessage(UUID.randomUUID().toString(), CONSUME_SESSION_SIGNAL_KEYS, param)

fun createSyncSignalKeys(param: BlazeMessageParam) =
    BlazeMessage(UUID.randomUUID().toString(), SYNC_SIGNAL_KEYS, param)

fun createSignalKeyMessage(param: BlazeMessageParam) =
    BlazeMessage(UUID.randomUUID().toString(), CREATE_SIGNAL_KEY_MESSAGES, param)

fun createCallMessage(param: BlazeMessageParam) =
    BlazeMessage(UUID.randomUUID().toString(), CREATE_CALL, param)