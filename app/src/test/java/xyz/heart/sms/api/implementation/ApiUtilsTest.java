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

package xyz.heart.sms.api.implementation;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import xyz.heart.sms.MessengerRobolectricSuite;
import xyz.heart.sms.R;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ApiUtilsTest extends MessengerRobolectricSuite {

    private ApiUtils apiUtils;

    @Before
    public void setUp() {
        apiUtils = ApiUtils.INSTANCE;
    }

    @Test
    public void baseUrl() {
        String url = apiUtils.getApi().baseUrl();
        String environment = RuntimeEnvironment.application.getString(R.string.environment);

        // we always use the release url now.
//        if (environment.equals("debug")) {
//            assertTrue(url.startsWith("http://192.168."));
//            assertTrue(url.endsWith(":3000/api/v1/"));
//        } else if (environment.equals("staging")) {
//            assertEquals("https://klinkerapps-messenger-staging.herokuapp.com/api/v1/", url);
//        } else {
            assertEquals("https://api.messenger.klinkerapps.com/api/v1/", url);
//        }
    }

}
