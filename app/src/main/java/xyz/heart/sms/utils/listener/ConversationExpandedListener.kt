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

package xyz.heart.sms.utils.listener

import xyz.heart.sms.adapter.view_holder.ConversationViewHolder

/**
 * Listener for notifying an object when a conversation has been expanded or contracted.
 */
interface ConversationExpandedListener {

    /**
     * Tells the listener to expand the conversation.
     *
     * @return true if the listener actually expanded, otherwise false.
     */
    fun onConversationExpanded(viewHolder: ConversationViewHolder): Boolean

    fun onConversationContracted(viewHolder: ConversationViewHolder)

}
