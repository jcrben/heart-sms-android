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

package xyz.heart.sms.api.implementation

import android.content.Context
import android.util.Log

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

import retrofit2.Response
import xyz.heart.sms.api.Api
import xyz.heart.sms.api.entity.*
import xyz.heart.sms.api.implementation.media.MediaDownloadCallback
import xyz.heart.sms.api.implementation.media.MediaUploadCallback
import xyz.heart.sms.api.implementation.retrofit.*
import xyz.heart.sms.encryption.EncryptionUtils

/**
 * Utility for easing access to APIs.
 */
object ApiUtils {
    const val RETRY_COUNT = 4

    private const val TAG = "ApiUtils"
    private const val MAX_SIZE = (1024 * 1024 * 5).toLong()

    fun isCallSuccessful(response: Response<*>): Boolean {
        val code = response.code()
        return code in 200..399
    }

    /**
     * Gets direct access to the apis for more advanced options.
     */
    var environment = "release"
    val api: Api by lazy { ApiAccessor.create(environment) }

    /**
     * Logs into the server.
     */
    fun login(email: String?, hashedPassword: String?): LoginResponse? {
        return try {
            val request = LoginRequest(email, hashedPassword)
            api.account().login(request).execute().body()
        } catch (e: IOException) {
            null
        }

    }

    /**
     * Signs up for the service.
     */
    fun signup(email: String?, password: String?, name: String?, phoneNumber: String?): SignupResponse? {
        return try {
            val request = SignupRequest(email, name, password, phoneNumber)
            api.account().signup(request).execute().body()
        } catch (e: IOException) {
            null
        }

    }

    /**
     * Removes the account from the server.
     */
    fun deleteAccount(accountId: String?) {
        val message = "removed account"
        val call = api.account().remove(accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Cleans all the database tables, for the account, on the server
     */
    fun cleanAccount(accountId: String?) {
        val message = "cleaned account"
        val call = api.account().clean(accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Registers your device as a new device on the server.
     */
    fun registerDevice(accountId: String?, info: String?, name: String?,
                       primary: Boolean, fcmToken: String?): Int? {
        val deviceBody = DeviceBody(info, name, primary, fcmToken)
        val request = AddDeviceRequest(accountId, deviceBody)

        try {
            val response = api.device().add(request).execute().body()
            if (response != null) {
                return response.id
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * Removes a device from the server.
     */
    fun removeDevice(accountId: String?, deviceId: Int) {
        val message = "remove device"
        val call = api.device().remove(deviceId, accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    fun updatePrimaryDevice(accountId: String?, newPrimaryDeviceId: String?) {
        if (accountId == null) {
            return
        }

        val message = "update primary device"
        val call = api.device().updatePrimary(newPrimaryDeviceId, accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Gets a list of all devices on the server.
     */
    fun getDevices(accountId: String?): Array<DeviceBody>? {
        return try {
            api.device().list(accountId).execute().body()
        } catch (e: IOException) {
            emptyArray()
        }

    }

    /**
     * Updates device info on the server.
     */
    fun updateDevice(accountId: String?, deviceId: Long, name: String?, fcmToken: String?) {
        val message = "update device"
        val call = api.device().update(deviceId, accountId, name, fcmToken)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Adds a new contact.
     */
    fun addContact(accountId: String?, id: Long, phoneNumber: String?, idMatcher: String?, name: String?, type: Int?,
                   color: Int, colorDark: Int, colorLight: Int, colorAccent: Int,
                   encryptionUtils: EncryptionUtils?) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        val body = if (type != null) {
            ContactBody(id, encryptionUtils.encrypt(phoneNumber), encryptionUtils.encrypt(idMatcher),
                    encryptionUtils.encrypt(name), type, color, colorDark, colorLight, colorAccent)
        } else {
            ContactBody(id, encryptionUtils.encrypt(phoneNumber), encryptionUtils.encrypt(idMatcher),
                    encryptionUtils.encrypt(name), color, colorDark, colorLight, colorAccent)
        }

        val request = AddContactRequest(accountId, body)

        addContact(request)
    }

    /**
     * Adds a new contact.
     */
    fun addContact(request: AddContactRequest) {
        val message = "add contact"
        val call = api.contact().add(request)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Deletes a contact
     */
    fun deleteContact(accountId: String?, id: Long, phoneNumber: String?,
                      encryptionUtils: EncryptionUtils?) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        val message = "delete contact"
        val call = api.contact().remove(encryptionUtils.encrypt(phoneNumber), id, accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Deletes a conversation and all of its messages.
     */
    fun clearContacts(accountId: String?) {
        if (accountId == null) {
            return
        }

        val message = "delete contact"
        val call = api.contact().clear(accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Updates a conversation with new settings or info.
     */
    fun updateContact(accountId: String?, id: Long, phoneNumber: String?, name: String?,
                      color: Int?, colorDark: Int?, colorLight: Int?,
                      colorAccent: Int?,
                      encryptionUtils: EncryptionUtils?) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        val request = UpdateContactRequest(
                encryptionUtils.encrypt(phoneNumber),
                encryptionUtils.encrypt(name), color, colorDark, colorLight, colorAccent)

        val message = "update contact"
        val call = api.contact().update(encryptionUtils.encrypt(phoneNumber), id, accountId, request)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Adds a new conversation.
     */
    fun addConversation(context: Context, accountId: String?, deviceId: Long, color: Int,
                        colorDark: Int, colorLight: Int, colorAccent: Int,
                        ledColor: Int, pinned: Boolean, read: Boolean, timestamp: Long,
                        title: String?, phoneNumbers: String?, snippet: String?,
                        ringtone: String?, idMatcher: String?, mute: Boolean,
                        archive: Boolean, privateNotifications: Boolean, folderId: Long?,
                        encryptionUtils: EncryptionUtils?, retryable: Boolean = true) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        val body = ConversationBody(
                deviceId, color, colorDark, colorLight, colorAccent, ledColor,
                pinned, read, timestamp, encryptionUtils.encrypt(title),
                encryptionUtils.encrypt(phoneNumbers), encryptionUtils.encrypt(snippet),
                encryptionUtils.encrypt(ringtone), null,
                encryptionUtils.encrypt(idMatcher), mute, archive, privateNotifications, folderId)
        val request = AddConversationRequest(accountId, body)
        val call = api.conversation().add(request)

        if (retryable) {
            // if the request errors out (no internet), we want to persist that issue and retry
            // it when connectivity is regained.
            call.enqueue(AddConversationRetryableCallback(context, call, RETRY_COUNT, deviceId))
        } else {
            val message = "add conversation"
            call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
        }
    }

    /**
     * Deletes a conversation and all of its messages.
     */
    fun deleteConversation(accountId: String?, deviceId: Long) {
        if (accountId == null) {
            return
        }

        val message = "delete conversation"
        val call = api.conversation().remove(deviceId, accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Add a conversation to a folder
     */
    fun addConversationToFolder(accountId: String?, deviceId: Long, folderId: Long) {
        if (accountId == null) {
            return
        }

        val message = "add conversation to folder"
        val call = api.conversation().addToFolder(deviceId, folderId, accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Remove a conversation from it's folder
     */
    fun removeConversationFromFolder(accountId: String?, deviceId: Long) {
        if (accountId == null) {
            return
        }

        val message = "remove conversation from folder"
        val call = api.conversation().removeFromFolder(deviceId, accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Archives a conversation.
     */
    fun archiveConversation(accountId: String?, deviceId: Long) {
        if (accountId == null) {
            return
        }

        val message = "archive conversation"
        val call = api.conversation().archive(deviceId, accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Moves a conversation back to the inbox.
     */
    fun unarchiveConversation(accountId: String?, deviceId: Long) {
        if (accountId == null) {
            return
        }

        val message = "unarchive conversation"
        val call = api.conversation().unarchive(deviceId, accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Updates a conversation with new settings or info.
     */
    fun updateConversation(accountId: String?, deviceId: Long, color: Int?,
                           colorDark: Int?, colorLight: Int?,
                           colorAccent: Int?, ledColor: Int?, pinned: Boolean?,
                           read: Boolean?, timestamp: Long?, title: String?,
                           snippet: String?, ringtone: String?, mute: Boolean?,
                           archive: Boolean?, privateNotifications: Boolean?,
                           encryptionUtils: EncryptionUtils?) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        val request = UpdateConversationRequest(color,
                colorDark, colorLight, colorAccent, ledColor, pinned, read, timestamp,
                encryptionUtils.encrypt(title), encryptionUtils.encrypt(snippet),
                encryptionUtils.encrypt(ringtone), mute, archive, privateNotifications)

        val message = "update conversation"
        val call = api.conversation().update(deviceId, accountId, request)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Updates a conversation with new snippet info
     */
    fun updateConversationSnippet(accountId: String?, deviceId: Long,
                                  read: Boolean?, archive: Boolean?,
                                  timestamp: Long?, snippet: String?,
                                  encryptionUtils: EncryptionUtils?) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        val request = UpdateConversationRequest(null, null, null, null, null, null, read, timestamp, null, encryptionUtils.encrypt(snippet), null, null, archive, null)

        val message = "update conversation snippet"
        val call = api.conversation().updateSnippet(deviceId, accountId, request)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Updates a conversation with a new title (usually when the name changes)
     */
    fun updateConversationTitle(accountId: String?, deviceId: Long,
                                title: String?, encryptionUtils: EncryptionUtils?) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        val message = "update conversation title"
        val call = api.conversation().updateTitle(deviceId, accountId, encryptionUtils.encrypt(title))

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Marks all messages in conversation as read.
     */
    fun readConversation(accountId: String?, androidDevice: String?, deviceId: Long) {
        if (accountId == null) {
            return
        }

        val message = "read conversation"
        val call = api.conversation().read(deviceId, androidDevice, accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Marks all messages in conversation as seen.
     */
    fun seenConversation(accountId: String?, deviceId: Long) {
        if (accountId == null) {
            return
        }

        val message = "seen conversation"
        val call = api.conversation().seen(deviceId, accountId)

        //call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Marks all messages as seen.
     */
    fun seenConversations(accountId: String?) {
        if (accountId == null) {
            return
        }

        val message = "seen all conversation"
        val call = api.conversation().seen(accountId)

        //call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Adds a new message to the server.
     */
    fun addMessage(context: Context, accountId: String?, deviceId: Long,
                   deviceConversationId: Long, messageType: Int,
                   data: String?, timestamp: Long, mimeType: String?,
                   read: Boolean, seen: Boolean, messageFrom: String?,
                   color: Int?, androidDeviceId: String?, simStamp: String?,
                   encryptionUtils: EncryptionUtils?, retryable: Boolean = true) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        if (mimeType == "text/plain" || messageType == 6 || mimeType == "media/map") {
            val body = MessageBody(deviceId,
                    deviceConversationId, messageType, encryptionUtils.encrypt(data),
                    timestamp, encryptionUtils.encrypt(mimeType), read, seen,
                    encryptionUtils.encrypt(messageFrom), color, androidDeviceId,
                    encryptionUtils.encrypt(simStamp))
            val request = AddMessagesRequest(accountId, body)
            val call = api.message().add(request)

            if (retryable) {
                // if the request errors out (no internet), we want to persist that issue and retry
                // it when connectivity is regained.
                call.enqueue(AddMessageRetryableCallback(context, call, RETRY_COUNT, deviceId))
            } else {
                val message = "added_message"
                call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
            }
        } else {
            val bytes = BinaryUtils.getMediaBytes(context, data, mimeType, true)
            addMedia(accountId, bytes, deviceId, encryptionUtils, MediaUploadCallback {
                val body = MessageBody(deviceId, deviceConversationId,
                        messageType, encryptionUtils.encrypt("firebase -1"),
                        timestamp, encryptionUtils.encrypt(mimeType), read, seen,
                        encryptionUtils.encrypt(messageFrom), color, androidDeviceId,
                        encryptionUtils.encrypt(simStamp))
                val request = AddMessagesRequest(accountId, body)
                val message = "add media message"
                val call = api.message().add(request)

                call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
            })
        }
    }

    /**
     * Updates a message with the given parameters.
     */
    fun updateMessage(accountId: String?, deviceId: Long, type: Int?,
                      read: Boolean?, seen: Boolean?, timestamp: Long?) {
        if (accountId == null) {
            return
        }

        val request = UpdateMessageRequest(type, read, seen, timestamp)
        val message = "update message"
        val call = api.message().update(deviceId, accountId, request)

        call.enqueue(LoggingRetryableCallback(call, 6, message))
    }

    /**
     * Updates a message with the given parameters.
     */
    fun updateMessageType(accountId: String?, deviceId: Long, type: Int) {
        if (accountId == null) {
            return
        }

        val message = "update message type"
        val call = api.message().updateType(deviceId, accountId, type)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Deletes the given message.
     */
    fun deleteMessage(accountId: String?, deviceId: Long) {
        if (accountId == null) {
            return
        }

        val message = "delete message"
        val call = api.message().remove(deviceId, accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Deletes messages older than the given timestamp.
     */
    fun cleanupMessages(accountId: String?, timestamp: Long) {
        if (accountId == null) {
            return
        }

        val message = "clean up messages"
        val call = api.message().cleanup(accountId, timestamp)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Deletes messages older than the given timestamp.
     */
    fun cleanupConversationMessages(accountId: String?, conversationId: Long, timestamp: Long) {
        if (accountId == null) {
            return
        }

        val message = "clean up conversation messages"
        val call = api.conversation().cleanup(accountId, conversationId, timestamp)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Adds a draft.
     */
    fun addDraft(accountId: String?, deviceId: Long,
                 deviceConversationId: Long, data: String?,
                 mimeType: String?, encryptionUtils: EncryptionUtils?) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        val body = DraftBody(deviceId, deviceConversationId,
                encryptionUtils.encrypt(data), encryptionUtils.encrypt(mimeType))
        val request = AddDraftRequest(accountId, body)

        val message = "add draft"
        val call = api.draft().add(request)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Deletes the given drafts.
     */
    fun deleteDrafts(accountId: String?, androidDeviceId: String?, deviceConversationId: Long) {
        if (accountId == null) {
            return
        }

        val message = "delete drafts"
        val call = api.draft().remove(deviceConversationId, androidDeviceId, accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Adds a blacklist.
     */
    fun addBlacklist(accountId: String?, deviceId: Long, phoneNumber: String?, phrase: String?,
                     encryptionUtils: EncryptionUtils?) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        val body = BlacklistBody(deviceId,
                encryptionUtils.encrypt(phoneNumber), encryptionUtils.encrypt(phrase))
        val request = AddBlacklistRequest(accountId, body)

        val message = "add blacklist"
        val call = api.blacklist().add(request)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Deletes the given blacklist.
     */
    fun deleteBlacklist(accountId: String?, deviceId: Long) {
        if (accountId == null) {
            return
        }

        val message = "delete blacklist"
        val call = api.blacklist().remove(deviceId, accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Adds a scheduled message.
     */
    fun addScheduledMessage(accountId: String?, deviceId: Long, title: String?,
                            to: String?, data: String?, mimeType: String?,
                            timestamp: Long, repeat: Int?, encryptionUtils: EncryptionUtils?) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        val body = ScheduledMessageBody(
                deviceId,
                encryptionUtils.encrypt(to),
                encryptionUtils.encrypt(data),
                encryptionUtils.encrypt(mimeType),
                timestamp,
                encryptionUtils.encrypt(title),
                repeat ?: 0)

        val request = AddScheduledMessageRequest(accountId, body)

        val message = "add scheduled message"
        val call = api.scheduled().add(request)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Update the text/date for a given scheduled message
     */
    fun updateScheduledMessage(accountId: String?, deviceId: Long, title: String?,
                               to: String?, data: String?, mimeType: String?,
                               timestamp: Long, repeat: Int?, encryptionUtils: EncryptionUtils?) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        val request = UpdateScheduledMessageRequest(
                encryptionUtils.encrypt(to), encryptionUtils.encrypt(data),
                encryptionUtils.encrypt(mimeType), timestamp,
                encryptionUtils.encrypt(title),
                repeat)

        val message = "update scheduled message"
        val call = api.scheduled().update(deviceId, accountId, request)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Deletes the given scheduled message.
     */
    fun deleteScheduledMessage(accountId: String?, deviceId: Long) {
        if (accountId == null) {
            return
        }

        val message = "delete scheduled message"
        val call = api.scheduled().remove(deviceId, accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Adds a template.
     */
    fun addTemplate(accountId: String?, deviceId: Long, text: String, encryptionUtils: EncryptionUtils?) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        val body = TemplateBody(deviceId, encryptionUtils.encrypt(text))
        val request = AddTemplateRequest(accountId, body)
        val message = "add template"

        val call = api.template().add(request)
        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Update the text for a given template.
     */
    fun updateTemplate(accountId: String?, deviceId: Long, text: String,
                       encryptionUtils: EncryptionUtils?) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        val request = UpdateTemplateRequest(encryptionUtils.encrypt(text))
        val message = "update template"

        val call = api.template().update(deviceId, accountId, request)
        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Deletes the given template.
     */
    fun deleteTemplate(accountId: String?, deviceId: Long) {
        if (accountId == null) {
            return
        }

        val message = "delete template"
        val call = api.template().remove(deviceId, accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Adds a template.
     */
    fun addAutoReply(accountId: String?, deviceId: Long, type: String, pattern: String, response: String,
                     encryptionUtils: EncryptionUtils?) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        val body = AutoReplyBody(deviceId, type, encryptionUtils.encrypt(pattern),
                encryptionUtils.encrypt(response))
        val request = AddAutoReplyRequest(accountId, body)
        val message = "add auto reply"

        val call = api.autoReply().add(request)
        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Update the text for a given template.
     */
    fun updateAutoReply(accountId: String?, deviceId: Long, type: String, pattern: String, response: String,
                       encryptionUtils: EncryptionUtils?) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        val request = UpdateAutoReplyRequest(type, encryptionUtils.encrypt(pattern),
                encryptionUtils.encrypt(response))
        val message = "update auto reply"

        val call = api.autoReply().update(deviceId, accountId, request)
        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Deletes the given auto reply.
     */
    fun deleteAutoReply(accountId: String?, deviceId: Long) {
        if (accountId == null) {
            return
        }

        val message = "delete auto reply"
        val call = api.autoReply().remove(deviceId, accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Adds a folder.
     */
    fun addFolder(accountId: String?, deviceId: Long, name: String, color: Int, colorDark: Int,
                  colorLight: Int, colorAccent: Int, encryptionUtils: EncryptionUtils?) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        val body = FolderBody(deviceId, encryptionUtils.encrypt(name), color, colorDark, colorLight, colorAccent)
        val request = AddFolderRequest(accountId, body)
        val message = "add folder"

        val call = api.folder().add(request)
        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Update the text for a given folder.
     */
    fun updateFolder(accountId: String?, deviceId: Long, name: String, color: Int, colorDark: Int,
                     colorLight: Int, colorAccent: Int, encryptionUtils: EncryptionUtils?) {
        if (accountId == null || encryptionUtils == null) {
            return
        }

        val request = UpdateFolderRequest(encryptionUtils.encrypt(name), color, colorDark, colorLight, colorAccent)
        val message = "update folder"

        val call = api.folder().update(deviceId, accountId, request)
        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Deletes the given template.
     */
    fun deleteFolder(accountId: String?, deviceId: Long) {
        if (accountId == null) {
            return
        }

        val message = "delete folder"
        val call = api.folder().remove(deviceId, accountId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Adds media associated with a given message
     */
    fun addMedia(accountId: String?, fileBytes: ByteArray, messageId: Long, encryptionUtils: EncryptionUtils?,
                 callback: MediaUploadCallback) {
        if (accountId == null || encryptionUtils == null) {
            callback.onUploadFinished()
            return
        }

        val request = AddMediaRequest(messageId, encryptionUtils.encrypt(fileBytes))
        val message = "add media"

        val call = api.media().add(accountId, request)
        call.enqueue(AddMediaRetryableCallback(call, RETRY_COUNT, messageId, callback))
    }

    /**
     * Downloads media associated with a given message
     */
    fun downloadMedia(accountId: String?, file: File, messageId: Long,
                      encryptionUtils: EncryptionUtils?,
                      callback: MediaDownloadCallback) {
        if (accountId == null || encryptionUtils == null) {
            callback.onDownloadComplete()
            return
        }

        val response = try {
            ApiUtils.api.media()
                    .download(messageId, accountId)
                    .execute().body()
        } catch (e: IOException) {
            null
        }

        if (response != null) {
            val bytes = encryptionUtils.decryptData(response.data);

            try {
                val bos = BufferedOutputStream(FileOutputStream(file))
                bos.write(bytes)
                bos.flush()
                bos.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Log.v(TAG, "finished downloading $messageId")
        callback.onDownloadComplete()
    }

    /**
     * Dismiss a notification across all devices.
     */
    fun dismissNotification(accountId: String?, deviceId: String?, conversationId: Long) {
        if (accountId == null) {
            return
        }

        val message = "dismiss notification"
        val call = api.account().dismissedNotification(accountId, deviceId, conversationId)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Update the subscription status on the server.
     */
    fun updateSubscription(accountId: String?, subscriptionType: Int?, expirationDate: Long?) {
        if (accountId == null) {
            return
        }

        val message = "update subscription"
        val call = api.account().updateSubscription(accountId, subscriptionType!!, expirationDate!!)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }

    /**
     * Update the snooze time setting.
     */
    fun updateSnooze(accountId: String?, snoozeTil: Long) {
        if (accountId != null) {
            updateSetting(accountId, "snooze", "long", snoozeTil)
        }
    }

    /**
     * Update the vibrate setting.
     */
    fun updateVibrate(accountId: String?, vibratePattern: String?) {
        if (accountId != null) {
            updateSetting(accountId, "vibrate_pattern_identifier", "string", vibratePattern)
        }
    }

    /**
     * Update the repeat notifications setting.
     */
    fun updateRepeatNotifications(accountId: String?, repeatString: String?) {
        if (accountId != null) {
            updateSetting(accountId, "repeat_notifications_interval", "string", repeatString)
        }
    }

    /**
     * Update the wake screen setting
     */
    fun updateWakeScreen(accountId: String?, wake: String?) {
        if (accountId != null) {
            updateSetting(accountId, "wake_screen", "string", wake)
        }
    }

    /**
     * Update the wake screen setting
     */
    fun updateHeadsUp(accountId: String?, headsUp: String?) {
        if (accountId != null) {
            updateSetting(accountId, "heads_up", "string", headsUp)
        }
    }

    /**
     * Update the delivery reports setting.
     */
    fun updateDeliveryReports(accountId: String?, deliveryReports: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "delivery_reports", "boolean", deliveryReports)
        }
    }

    /**
     * Update the delivery reports setting.
     */
    fun updateGiffgaffDeliveryReports(accountId: String?, deliveryReports: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "giffgaff_delivery", "boolean", deliveryReports)
        }
    }

    /**
     * Update the strip Unicode setting.
     */
    fun updateStripUnicode(accountId: String?, stripUnicode: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "strip_unicode", "boolean", stripUnicode)
        }
    }

    /**
     * Update the notification history option
     */
    fun updateShowHistoryInNotification(accountId: String?, showHistory: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "history_in_notifications", "boolean", showHistory)
        }
    }

    /**
     * Update the notification message content option
     */
    fun updateHideMessageContent(accountId: String?, hideContent: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "hide_message_content", "boolean", hideContent)
        }
    }

    /**
     * Update the notification dismissal option
     */
    fun updateDismissNotificationsAfterReply(accountId: String?, dismiss: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "dismiss_notifications_on_reply_android_p", "boolean", dismiss)
        }
    }

    /**
     * Update the rounder bubbles setting.
     */
    fun updateBubbleStyle(accountId: String?, style: String) {
        if (accountId != null) {
            updateSetting(accountId, "bubble_style", "string", style)
        }
    }

    /**
     * Update the notification actions setting.
     */
    fun updateNotificationActions(accountId: String?, stringified: String?) {
        if (accountId != null) {
            updateSetting(accountId, "notification_actions", "set", stringified)
        }
    }

    /**
     * Update the action for the left to right swipe.
     */
    fun updateLeftToRightSwipeAction(accountId: String?, identifier: String) {
        if (accountId != null) {
            updateSetting(accountId, "left_to_right_swipe", "string", identifier)
        }
    }

    /**
     * Update the action for the right to left swipe
     */
    fun updateRightToLeftSwipeAction(accountId: String?, identifier: String) {
        if (accountId != null) {
            updateSetting(accountId, "right_to_left_swipe", "string", identifier)
        }
    }

    /**
     * Update the convert to MMS setting, for long messages
     */
    fun updateConvertToMMS(accountId: String?, convert: String?) {
        if (accountId != null) {
            updateSetting(accountId, "sms_to_mms_message_conversion_count", "string", convert)
        }
    }

    /**
     * Update the MMS size limit setting.
     */
    fun updateMmsSize(accountId: String?, mmsSize: String?) {
        if (accountId != null) {
            updateSetting(accountId, "mms_size_limit", "string", mmsSize)
        }
    }

    /**
     * Update the group MMS setting.
     */
    fun updateGroupMMS(accountId: String?, groupMMS: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "group_mms", "boolean", groupMMS)
        }
    }

    /**
     * Update the read receipts setting.
     */
    fun updateMmsReadReceipts(accountId: String?, receipts: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "mms_read_receipts", "boolean", receipts)
        }
    }

    /**
     * Update the auto save media setting.
     */
    fun updateAutoSaveMedia(accountId: String?, save: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "auto_save_media", "boolean", save)
        }
    }

    /**
     * Update the override system apn setting.
     */
    fun updateOverrideSystemApn(accountId: String?, override: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "mms_override", "boolean", override)
        }
    }

    /**
     * Update the mmsc url for MMS.
     */
    fun updateMmscUrl(accountId: String?, mmsc: String?) {
        if (accountId != null) {
            updateSetting(accountId, "mmsc_url", "string", mmsc)
        }
    }

    /**
     * Update the MMS proxy setting.
     */
    fun updateMmsProxy(accountId: String?, proxy: String?) {
        if (accountId != null) {
            updateSetting(accountId, "mms_proxy", "string", proxy)
        }
    }

    /**
     * Update the MMS port setting.
     */
    fun updateMmsPort(accountId: String?, port: String?) {
        if (accountId != null) {
            updateSetting(accountId, "mms_port", "string", port)
        }
    }

    /**
     * Update the user agent setting.
     */
    fun updateUserAgent(accountId: String?, userAgent: String?) {
        if (accountId != null) {
            updateSetting(accountId, "user_agent", "string", userAgent)
        }
    }

    /**
     * Update the user agent profile url setting.
     */
    fun updateUserAgentProfileUrl(accountId: String?, userAgentProfileUrl: String?) {
        if (accountId != null) {
            updateSetting(accountId, "user_agent_profile_url", "string", userAgentProfileUrl)
        }
    }

    /**
     * Update the user agent tag name setting.
     */
    fun updateUserAgentProfileTagName(accountId: String?, tagName: String?) {
        if (accountId != null) {
            updateSetting(accountId, "user_agent_profile_tag_name", "string", tagName)
        }
    }

    /**
     * Update the secure private conversations setting.
     */
    fun updatePrivateConversationsPasscode(accountId: String?, passcode: String) {
        if (accountId != null) {
            updateSetting(accountId, "private_conversations_passcode", "string", Account.encryptor?.encrypt(passcode))
        }
    }

    /**
     * Update the smart replies setting.
     */
    fun updateSmartReplies(accountId: String?, useSmartReplies: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "smart_reply", "boolean", useSmartReplies)
        }
    }

    /**
     * Update the smart reply timeout setting.
     */
    fun updateSmartReplyTimeout(accountId: String?, useSmartReplyTimeout: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "smart_reply_timeout", "boolean", useSmartReplyTimeout)
        }
    }

    /**
     * Update the internal browser setting.
     */
    fun updateInternalBrowser(accountId: String?, useBrowser: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "internal_browser", "boolean", useBrowser)
        }
    }

    /**
     * Update the quick compose setting.
     */
    fun updateQuickCompose(accountId: String?, quickCompose: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "quick_compose", "boolean", quickCompose)
        }
    }

    /**
     * Update the signature setting.
     */
    fun updateSignature(accountId: String?, signature: String?) {
        if (accountId != null) {
            updateSetting(accountId, "signature", "string", signature)
        }
    }

    /**
     * Update the delayed sending setting.
     */
    fun updateDelayedSending(accountId: String?, delayedSending: String?) {
        if (accountId != null) {
            updateSetting(accountId, "delayed_sending", "string", delayedSending)
        }
    }

    /**
     * Update the cleanup old messages setting.
     */
    fun updateCleanupOldMessages(accountId: String?, cleanup: String?) {
        if (accountId != null) {
            updateSetting(accountId, "cleanup_old_messages", "string", cleanup)
        }
    }

    /**
     * Update the cleanup old messages setting.
     */
    fun updateUnknownNumberReception(accountId: String?, reception: String?) {
        if (accountId != null) {
            updateSetting(accountId, "unknown_number_reception", "string", reception)
        }
    }

    /**
     * Update the sound effects setting.
     */
    fun updateSoundEffects(accountId: String?, effects: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "sound_effects", "boolean", effects)
        }
    }

    /**
     * Update the mobile only setting
     */
    fun updateMobileOnly(accountId: String?, mobileOnly: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "mobile_only", "boolean", mobileOnly)
        }
    }

    /**
     * Update the font size setting
     */
    fun updateFontSize(accountId: String?, size: String?) {
        if (accountId != null) {
            updateSetting(accountId, "font_size", "string", size)
        }
    }

    /**
     * Update the emoji style setting
     */
    fun updateEmojiStyle(accountId: String?, style: String?) {
        if (accountId != null) {
            updateSetting(accountId, "emoji_style", "string", style)
        }
    }

    /**
     * Update the keyboard layout setting
     */
    fun updateKeyboardLayout(accountId: String?, layout: String?) {
        if (accountId != null) {
            updateSetting(accountId, "keyboard_layout", "string", layout)
        }
    }

    /**
     * Update the global theme color setting
     */
    fun updatePrimaryThemeColor(accountId: String?, color: Int) {
        if (accountId != null) {
            updateSetting(accountId, "global_primary_color", "int", color)
        }
    }

    /**
     * Update the global theme color setting
     */
    fun updatePrimaryDarkThemeColor(accountId: String?, color: Int) {
        if (accountId != null) {
            updateSetting(accountId, "global_primary_dark_color", "int", color)
        }
    }

    /**
     * Update the global theme color setting
     */
    fun updateAccentThemeColor(accountId: String?, color: Int) {
        if (accountId != null) {
            updateSetting(accountId, "global_accent_color", "int", color)
        }
    }

    /**
     * Update whether or not to apply the theme globally
     */
    fun updateUseGlobalTheme(accountId: String?, useGlobal: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "apply_theme_globally", "boolean", useGlobal)
        }
    }

    /**
     * Update whether or not to colorize the toolbar
     */
    fun updateApplyToolbarColor(accountId: String?, toolbarColor: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "apply_primary_color_toolbar", "boolean", toolbarColor)
        }
    }

    /**
     * Update whether or not to display the date categories in the conversation list
     */
    fun updateConversationCategories(accountId: String?, conversationCategories: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "conversation_categories", "boolean", conversationCategories)
        }
    }

    /**
     * Update whether or not to display the timestamp on every single message
     */
    fun updateMessageTimestamp(accountId: String?, messageTimestamp: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "message_timestamp", "boolean", messageTimestamp)
        }
    }

    /**
     * Update the base theme (day/night, always dark, always black)
     */
    fun updateBaseTheme(accountId: String?, themeString: String?) {
        if (accountId != null) {
            updateSetting(accountId, "base_theme", "string", themeString)
        }
    }

    /**
     * Update the favorite users for quick compose. Numbers should be a comma separated list
     */
    fun updateFavoriteUserNumbers(accountId: String?, numbersString: String?) {
        if (accountId != null) {
            updateSetting(accountId, "quick_compose_favorites", "string", numbersString)
        }
    }

    /**
     * Update the actions for the notifications. Actions should be a comma separated list
     */
    fun updateNotificationActionsSelectable(accountId: String?, actionsString: String?) {
        if (accountId != null) {
            updateSetting(accountId, "notification_actions_selection", "string", actionsString)
        }
    }

    /**
     * Enable or disable driving mode
     */
    fun enableDrivingMode(accountId: String?, enableDriving: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "driving_mode", "boolean", enableDriving)
        }
    }

    /**
     * Update the text of the driving mode preference
     */
    fun updateDrivingModeText(accountId: String?, text: String?) {
        if (accountId != null) {
            updateSetting(accountId, "driving_mode_edit", "string", text)
        }
    }

    /**
     * Enable or disable vacation mode
     */
    fun enableVacationMode(accountId: String?, enableVaction: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "vacation_mode", "boolean", enableVaction)
        }
    }

    /**
     * Update the text of the vacation mode preference
     */
    fun updateVacationModeText(accountId: String?, text: String?) {
        if (accountId != null) {
            updateSetting(accountId, "vacation_mode_edit", "string", text)
        }
    }

    /**
     * Heart does not tracks purchases. This is here for compatibility.
     */
    fun recordNewPurchase(type: String) {
        val message = "added a new purchase/install"
        val call = api.purchases().record(type)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))

    }

    /**
     * Update the blacklist phrase regex setting. This defines if blacklist phrases are treated as a Regex.
     */
    fun updateBlacklistPhraseRegex(accountId: String?, useBlacklistPhraseRegex: Boolean) {
        if (accountId != null) {
            updateSetting(accountId, "blacklist_phrase_regex", "boolean", useBlacklistPhraseRegex)
        }
    }

    /**
     * Dismiss a notification across all devices.
     */
    private fun updateSetting(accountId: String?, pref: String?, type: String?, value: Any?) {
        val message = "update $pref setting"
        val call = api.account().updateSetting(accountId, pref, type, value)

        call.enqueue(LoggingRetryableCallback(call, RETRY_COUNT, message))
    }
}
