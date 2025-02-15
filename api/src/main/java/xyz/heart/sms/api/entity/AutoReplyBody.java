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

public class AutoReplyBody {

    public long deviceId;
    public String replyType;
    public String pattern;
    public String response;

    public AutoReplyBody(long deviceId, String replyType, String pattern, String response) {
        this.deviceId = deviceId;
        this.replyType = replyType;
        this.pattern = pattern;
        this.response = response;
    }

    @Override
    public String toString() {
        return deviceId + ", " + replyType + ", " + pattern + ", " + response;
    }

}
