package xyz.heart.sms.shared.util.media.parsers

import android.content.Context
import xyz.heart.sms.api.implementation.ApiUtils
import xyz.heart.sms.shared.data.ArticlePreview
import xyz.heart.sms.shared.data.MimeType
import xyz.heart.sms.shared.util.Regex
import xyz.heart.sms.shared.util.media.MediaParser
import java.util.regex.Pattern

class ArticleParser(context: Context?) : MediaParser(context) {

    override val patternMatcher: Pattern
        get() = Regex.WEB_URL

    override val ignoreMatcher: String?
        get() = null

    public override val mimeType: String
        get() = MimeType.MEDIA_ARTICLE

    override fun buildBody(matchedText: String?): String? {
        val article = ApiUtils.parseArticle(matchedText)

        val preview = ArticlePreview.build(article)
        return if (preview != null && article != null &&
                article.title != null && !article.title.isEmpty() &&
                article.description != null && !article.description.isEmpty() &&
                preview.title != null && !preview.title!!.isEmpty() &&
                preview.description != null && !preview.description!!.isEmpty())

            preview.toString()
        else
            null
    }
}
