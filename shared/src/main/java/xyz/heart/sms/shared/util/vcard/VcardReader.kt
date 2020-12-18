package xyz.heart.sms.shared.util.vcard

import android.content.Context
import android.net.Uri
import java.io.IOException

object VcardReader {

    @Throws(IOException::class)
    fun readCotactCard(context: Context, uri: String): String {
        return readCotactCard(context, Uri.parse(uri))
    }

    @Throws(IOException::class)
    fun readCotactCard(context: Context, uri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream!!.bufferedReader().use { it.readText() }
        } catch (e: Throwable) {
            ""
        }
    }
}