package xyz.heart.sms.shared.util.listener

import xyz.heart.sms.shared.data.MediaMessage

/**
 * Callback for easily notifying the caller when a media has been selected
 */
interface MediaSelectedListener {
    fun onSelected(messageList: List<MediaMessage>, selectedPosition: Int)
    fun onStartDrag(index: Int)
}
