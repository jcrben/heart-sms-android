package xyz.heart.sms.activity.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.fragment.app.Fragment
import xyz.heart.sms.R
import xyz.heart.sms.activity.MessengerActivity
import xyz.heart.sms.activity.SettingsActivity
import xyz.heart.sms.activity.passcode.PasscodeVerificationActivity
import xyz.heart.sms.fragment.*
import xyz.heart.sms.fragment.conversation.ConversationListFragment
import xyz.heart.sms.fragment.settings.AboutFragment
import xyz.heart.sms.fragment.settings.HelpAndFeedbackFragment
import xyz.heart.sms.fragment.settings.MyAccountFragment
import xyz.heart.sms.shared.data.Settings
import xyz.heart.sms.shared.data.model.Folder
import xyz.heart.sms.shared.util.AnimationUtils
import xyz.heart.sms.shared.util.TimeUtils


class MainNavigationConversationListActionDelegate(private val activity: MessengerActivity) {

    private val navController
        get() = activity.navController
    private val intentHandler
        get() = activity.intentHandler

    fun displayConversations(): Boolean {
        return displayConversations(null)
    }

    fun displayConversations(savedInstanceState: Bundle?): Boolean {
        activity.fab.show()
        activity.invalidateOptionsMenu()
        navController.inSettings = false

        val (convoId, messageId) = intentHandler.displayConversation(savedInstanceState)

        var updateConversationListSize = false
        if (messageId != -1L && convoId != -1L) {
            navController.conversationListFragment = ConversationListFragment.newInstance(convoId, messageId)
            updateConversationListSize = true
        } else if (convoId != -1L && convoId != 0L) {
            navController.conversationListFragment = ConversationListFragment.newInstance(convoId)
            updateConversationListSize = true
        } else {
            navController.conversationListFragment = ConversationListFragment.newInstance()
        }

        if (updateConversationListSize) {
            val content = activity.findViewById<View>(R.id.content)
            content.post {
                AnimationUtils.conversationListSize = content.height
                AnimationUtils.toolbarSize = activity.toolbar.height
            }
        }

        navController.otherFragment = null

        val transaction = activity.supportFragmentManager.beginTransaction()

        if (navController.conversationListFragment != null) {
            transaction.replace(R.id.conversation_list_container, navController.conversationListFragment!!)
        }

        val messageList = activity.supportFragmentManager
                .findFragmentById(R.id.message_list_container)

        if (messageList != null) {
            transaction.remove(messageList)
        }

        try {
            transaction.commit()
        } catch (e: Exception) {
        }

        return true
    }

    internal fun displayArchived(): Boolean {
        return displayFragmentWithBackStack(ArchivedConversationListFragment())
    }

    internal fun displayPrivate(): Boolean {
        val showPrivateConversations: () -> Unit = {
            displayFragmentWithBackStack(PrivateConversationListFragment())
        }

        val lastEntry = Settings.privateConversationsLastPasscodeEntry
        return if (Settings.privateConversationsPasscode.isNullOrEmpty() || TimeUtils.now - lastEntry < TimeUtils.MINUTE) {
            showPrivateConversations()
            true
        } else {
            PasscodeVerificationActivity.show(activity, showPrivateConversations)
            false
        }
    }

    internal fun displayUnread(): Boolean {
        return displayFragmentWithBackStack(UnreadConversationListFragment())
    }

    internal fun displayFolder(folder: Folder): Boolean {
        return displayFragmentWithBackStack(FolderConversationListFragment.getInstance(folder))
    }

    internal fun displayScheduledMessages(): Boolean {
        return displayFragmentWithBackStack(ScheduledMessagesFragment())
    }

    internal fun displayBlacklist(): Boolean {
        return displayFragmentWithBackStack(BlacklistFragment.newInstance())
    }

    internal fun displayInviteFriends(): Boolean {
        return displayFragmentWithBackStack(InviteFriendsFragment())
    }

    internal fun displaySettings(): Boolean {
        SettingsActivity.startGlobalSettings(activity)
        return true
    }

    internal fun displayFeatureSettings(): Boolean {
        SettingsActivity.startFeatureSettings(activity)
        return true
    }

    internal fun displayMyAccount(): Boolean {
        return displayFragmentWithBackStack(MyAccountFragment())
    }

    internal fun displayHelpAndFeedback(): Boolean {
        return displayFragmentWithBackStack(HelpAndFeedbackFragment())
    }

    internal fun displayAbout(): Boolean {
        return displayFragmentWithBackStack(AboutFragment())
    }

    internal fun displayEditFolders(): Boolean {
        SettingsActivity.startFolderSettings(activity)
        return true
    }

    internal fun displayFragmentWithBackStack(fragment: Fragment): Boolean {
        activity.searchHelper.closeSearch()
        activity.fab.hide()
        activity.invalidateOptionsMenu()
        navController.inSettings = true

        navController.otherFragment = fragment
        Handler().postDelayed({
            try {
                activity.supportFragmentManager.beginTransaction()
                        .replace(R.id.conversation_list_container, fragment)
                        .commit()
            } catch (e: Exception) {
                activity.finish()
                activity.overridePendingTransition(0, 0)
                activity.startActivity(Intent(activity, MessengerActivity::class.java))
                activity.overridePendingTransition(0, 0)
            }
        }, 200)

        return true
    }
}