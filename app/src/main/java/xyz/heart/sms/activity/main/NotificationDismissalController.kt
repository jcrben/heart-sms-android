package xyz.heart.sms.activity.main

import xyz.heart.sms.activity.MessengerActivity

class NotificationDismissalController(private val activity: MessengerActivity) {

    private val intent
        get() = activity.intent
}