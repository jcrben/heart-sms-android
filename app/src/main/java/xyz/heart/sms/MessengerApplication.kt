/*
 * Copyright (C) 2020 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.heart.sms

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import com.sensortower.events.EventHandler
import xyz.heart.sms.api.implementation.Account
import xyz.heart.sms.api.implementation.AccountInvalidator
import xyz.heart.sms.api.implementation.firebase.FirebaseApplication
import xyz.heart.sms.api.implementation.firebase.FirebaseMessageHandler
import xyz.heart.sms.api.implementation.retrofit.ApiErrorPersister
import xyz.heart.sms.shared.data.DataSource
import xyz.heart.sms.shared.data.Settings
import xyz.heart.sms.shared.data.model.RetryableRequest
import xyz.heart.sms.shared.service.FirebaseHandlerService
import xyz.heart.sms.shared.service.FirebaseResetService
import xyz.heart.sms.shared.service.QuickComposeNotificationService
import xyz.heart.sms.shared.service.notification.ShortcutUpdater
import xyz.heart.sms.shared.util.*
import xyz.heart.sms.shared.util.UpdateUtils

/**
 * Base application that will serve as any intro for any context in the rest of the app. Main
 * function is to enable night mode so that colors change depending on time of day.
 */
class MessengerApplication : FirebaseApplication(), ApiErrorPersister, AccountInvalidator, EventHandler.Provider, ShortcutUpdater {

    override fun onCreate() {
        super.onCreate()

        KotlinObjectInitializers.initializeObjects(this)
        FirstRunInitializer.applyDefaultSettings(this)
        UpdateUtils.rescheduleWork(this)

        enableSecurity()

        TimeUtils.setupNightTheme()
        NotificationUtils.createNotificationChannels(this)

        if (Settings.quickCompose) {
            QuickComposeNotificationService.start(this)
        }
    }

    override fun refreshDynamicShortcuts(delay: Long) {
        if ("robolectric" != Build.FINGERPRINT && !Settings.firstStart) {
            val update = {
                val conversations = DataSource.getUnarchivedConversationsAsList(this)
                DynamicShortcutUtils(this@MessengerApplication).buildDynamicShortcuts(conversations)
            }

            if (delay == 0L) try {
                update()
                return
            } catch (e: Exception) {
            }

            Thread {
                try {
                    Thread.sleep(delay)
                    update()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    override fun getFirebaseMessageHandler(): FirebaseMessageHandler {
        return object : FirebaseMessageHandler {
            override fun handleMessage(application: Application, operation: String, data: String) {
                Thread { FirebaseHandlerService.process(application, operation, data) }.start()
            }

            override fun handleDelete(application: Application) {
                val handleMessage = Intent(application, FirebaseResetService::class.java)
                if (AndroidVersionUtil.isAndroidO) {
                    startForegroundService(handleMessage)
                } else {
                    startService(handleMessage)
                }
            }
        }
    }

    override fun onAddConversationError(conversationId: Long) {
        if (!Account.exists() || !Account.primary) {
            return
        }

        Thread {
            DataSource.insertRetryableRequest(this,
                    RetryableRequest(RetryableRequest.TYPE_ADD_CONVERSATION, conversationId, TimeUtils.now))
        }.start()
    }

    override fun onAddMessageError(messageId: Long) {
        if (!Account.exists() || !Account.primary) {
            return
        }

        Thread {
            DataSource.insertRetryableRequest(this,
                    RetryableRequest(RetryableRequest.TYPE_ADD_MESSAGE, messageId, TimeUtils.now))
        }.start()
    }

    override fun onAccountInvalidated(account: Account) {
        DataSource.invalidateAccountDetails()
    }

    companion object {

        /**
         * By default, java does not allow for strong security schemes due to export laws in other
         * countries. This gets around that. Might not be necessary on Android, but we'll put it here
         * anyways just in case.
         */
        private fun enableSecurity() {
            try {
                val field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted")
                field.isAccessible = true
                field.set(null, java.lang.Boolean.FALSE)
            } catch (e: Exception) {

            }

        }
    }

    override val eventHandler: EventHandler
        get() = object : EventHandler {
            override fun onAnalyticsEvent(type: String, message: String?) {
                Log.v(type, "skipped firebase analytics")
            }
        }
}
