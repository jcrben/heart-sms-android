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

package xyz.heart.sms.view

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.database.SQLException
import android.os.Handler
import android.provider.MediaStore
import android.provider.MediaStore.Images
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import xyz.heart.sms.R
import xyz.heart.sms.adapter.AttachImageListAdapter
import xyz.heart.sms.shared.util.ColorUtils
import xyz.heart.sms.shared.util.CursorUtil
import xyz.heart.sms.shared.util.listener.ImageSelectedListener

/**
 * View that displays a list of images that are currently on your device and allows you to choose
 * one to attach to a message.
 */
@SuppressLint("ViewConstructor")
class AttachImageView(context: Context, private val callback: ImageSelectedListener, color: Int) : RecyclerView(context) {

    private var images: Cursor? = null

    init {
        ColorUtils.changeRecyclerOverscrollColors(this, color)
        val handler = Handler()

        Thread {
            val select = arrayOf(Images.Media._ID, Images.Media.MIME_TYPE)
            val cr = context.contentResolver
            
            images = try {
                cr.query(Images.Media.EXTERNAL_CONTENT_URI,
                        select, null, null, MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC")
            } catch (e: SQLException) {
                MatrixCursor(emptyArray())
            }

            if (images == null) {
                return@Thread
            }

            handler.post {
                layoutManager = GridLayoutManager(context, resources.getInteger(R.integer.images_column_count))
                adapter = AttachImageListAdapter(images!!, callback, color)
            }
        }.start()
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        CursorUtil.closeSilent(images)
    }

}
