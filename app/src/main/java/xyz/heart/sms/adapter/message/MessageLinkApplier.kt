package xyz.heart.sms.adapter.message

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.util.Patterns
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.FragmentActivity
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.klinker.android.link_builder.applyLinks
import xyz.heart.sms.R
import xyz.heart.sms.adapter.view_holder.MessageViewHolder
import xyz.heart.sms.fragment.message.MessageListFragment
import xyz.heart.sms.fragment.bottom_sheet.LinkLongClickFragment
import xyz.heart.sms.shared.data.Settings
import xyz.heart.sms.shared.data.model.Message
import xyz.heart.sms.shared.util.PhoneNumberUtils
import xyz.heart.sms.shared.util.Regex

@Suppress("DEPRECATION")
class MessageLinkApplier(private val fragment: MessageListFragment, private val accentColor: Int, private val receivedColor: Int) {

    private val activity: FragmentActivity? by lazy { fragment.activity }

    fun apply(holder: MessageViewHolder, message: Message, backgroundColor: Int) {
        val linkColor = if (message.type == Message.TYPE_RECEIVED) {
            holder.message!!.currentTextColor
        } else accentColor

        if (holder.message!!.context == null) {
            return
        }

        holder.message?.movementMethod = TouchableMovementMethod()
        holder.message?.applyLinks(
                buildEmailsLink(holder, linkColor),
                buildWebUrlsLink(holder, linkColor),
                buildPhoneNumbersLink(holder, linkColor)
        )
    }

    private fun buildEmailsLink(holder: MessageViewHolder, linkColor: Int): Link {
        val emails = Link(Patterns.EMAIL_ADDRESS)
        emails.textColor = linkColor
        emails.highlightAlpha = .4f
        emails.setOnClickListener { clickedText ->
            val email = arrayOf(clickedText)
            val uri = Uri.parse("mailto:$clickedText")

            val emailIntent = Intent(Intent.ACTION_SENDTO, uri)
            emailIntent.putExtra(Intent.EXTRA_EMAIL, email)
            try {
                holder.message!!.context.startActivity(emailIntent)
            } catch (e: ActivityNotFoundException) {
            }
        }

        return emails
    }

    private fun buildWebUrlsLink(holder: MessageViewHolder, linkColor: Int): Link {
        val urls = Link(Regex.WEB_URL)
        urls.textColor = linkColor
        urls.highlightAlpha = .4f

        urls.setOnLongClickListener { clickedText ->
            val link = if (!clickedText.startsWith("http")) {
                "https://$clickedText"
            } else clickedText

            val bottomSheet = LinkLongClickFragment()
            bottomSheet.setColors(receivedColor, accentColor)
            bottomSheet.setLink(link)
            bottomSheet.show(activity?.supportFragmentManager!!, "")
        }

        urls.setOnClickListener { clickedText ->
            if (fragment.multiSelect.isSelectable) {
                holder.messageHolder?.performClick()
                return@setOnClickListener
            }

            val link = if (!clickedText.startsWith("http")) {
                "https://$clickedText"
            } else clickedText

            if (skipInternalBrowser(link) || !Settings.internalBrowser) {
                val url = Intent(Intent.ACTION_VIEW)
                url.data = Uri.parse(link)
                try {
                    holder.itemView.context.startActivity(url)
                } catch (e: Exception) {
                    Log.e("force_close", "couldn't start link click: $clickedText", e)
                }
            } else {
                val builder = CustomTabsIntent.Builder()
                builder.setShowTitle(true)
                val customTabsIntent = builder.build()

                customTabsIntent.launchUrl(holder.itemView.context, Uri.parse(link))
            }
        }

        return urls
    }

    private fun buildPhoneNumbersLink(holder: MessageViewHolder, linkColor: Int): Link {
        return Link(Regex.PHONE)
                .setTextColor(linkColor)
                .setHighlightAlpha(.4f)
                .setOnClickListener {
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = Uri.parse("tel:" + PhoneNumberUtils.clearFormatting(it))

                    try {
                        holder.message!!.context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                    }
                }
    }

    private fun skipInternalBrowser(link: String): Boolean {
        val list = listOf("youtube", "maps.google", "photos.app.goo")
        for (item in list) {
            if (link.contains(item)) {
                return true
            }
        }

        return false
    }
}