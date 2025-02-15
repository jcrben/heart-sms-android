package xyz.heart.sms.shared.service

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import xyz.heart.sms.shared.util.*

class SmsReceivedService : JobIntentService() {

    companion object {
        private const val jobId = 100334
        fun start(context: Context, work: Intent) {
            enqueueWork(context, SmsReceivedService::class.java, jobId, work)
        }
    }

    override fun onHandleWork(intent: Intent) {
        SmsReceivedHandler(this).newSmsRecieved(intent)
    }

}