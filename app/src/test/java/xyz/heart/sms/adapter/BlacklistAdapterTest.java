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

package xyz.heart.sms.adapter;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import xyz.heart.sms.MessengerRobolectricSuite;
import xyz.heart.sms.shared.data.model.Blacklist;

import static org.junit.Assert.assertEquals;

public class BlacklistAdapterTest extends MessengerRobolectricSuite {

    private BlacklistAdapter adapter;

    @Before
    public void setUp() {
        adapter = new BlacklistAdapter(new ArrayList<Blacklist>(), null);
    }

    @Test
    public void cursorCountZero() {
        assertEquals(0, adapter.getItemCount());
    }

}