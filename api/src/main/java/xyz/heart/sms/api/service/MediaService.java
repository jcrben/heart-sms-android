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

package xyz.heart.sms.api.service;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import xyz.heart.sms.api.entity.AddMediaRequest;
import xyz.heart.sms.api.entity.MediaBody;

public interface MediaService {

    @POST("media/add")
    Call<Void> add(@Query("account_id") String accountId, @Body AddMediaRequest request);

    @GET("media/{message_id}")
    Call<MediaBody> download(@Path("message_id") long messageId, @Query("account_id") String accountId);

}
