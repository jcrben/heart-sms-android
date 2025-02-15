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

package xyz.heart.sms.api.entity;

public class UpdateMessageRequest {

    public Integer message_type;
    public Boolean read;
    public Boolean seen;
    public Long timestamp;

    public UpdateMessageRequest(Integer message_type, Boolean read, Boolean seen, Long timestamp) {
        this.message_type = message_type;
        this.read = read;
        this.seen = seen;
        this.timestamp = timestamp;
    }

}
