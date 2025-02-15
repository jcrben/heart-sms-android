package xyz.heart.sms.shared.util.autoreply

import android.content.Context
import xyz.heart.sms.shared.data.DataSource
import xyz.heart.sms.shared.data.Settings
import xyz.heart.sms.shared.data.model.AutoReply
import xyz.heart.sms.shared.data.model.Conversation
import xyz.heart.sms.shared.data.model.Message
import xyz.heart.sms.shared.util.autoreply.parsers.ContactReplyParser
import xyz.heart.sms.shared.util.autoreply.parsers.DrivingReplyParser
import xyz.heart.sms.shared.util.autoreply.parsers.KeywordReplyParser
import xyz.heart.sms.shared.util.autoreply.parsers.VacationReplyParser

class AutoReplyParserFactory {

    fun getInstances(context: Context, conversation: Conversation, message: Message): List<AutoReplyParser> {
        return buildParsers(context).filter { it.canParse(conversation, message) }
    }

    private fun buildParsers(context: Context): List<AutoReplyParser> {
        val parsers = DataSource.getAutoRepliesAsList(context)
                .filter { it.response!!.isNotBlank() }
                .mapNotNull { mapToParser(context, it) }

        val driving = parsers.firstOrNull { it is DrivingReplyParser }
        if (driving != null && Settings.drivingMode) {
            return listOf(driving)
        }

        val vacation = parsers.firstOrNull { it is VacationReplyParser }
        if (vacation != null && Settings.vacationMode) {
            return listOf(vacation)
        }

        return parsers
    }

    private fun mapToParser(context: Context, reply: AutoReply): AutoReplyParser? {
        return when (reply.type) {
            AutoReply.TYPE_VACATION -> VacationReplyParser(context, reply)
            AutoReply.TYPE_DRIVING -> DrivingReplyParser(context, reply)
            AutoReply.TYPE_CONTACT -> ContactReplyParser(context, reply)
            AutoReply.TYPE_KEYWORD -> KeywordReplyParser(context, reply)
            else -> null
        }
    }
}