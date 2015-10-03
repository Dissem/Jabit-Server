/*
 * Copyright 2015 Christian Basler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.dissem.bitmessage.server.entities;

import ch.dissem.bitmessage.entity.BitmessageAddress;

/**
 * Created by chrigu on 30.09.15.
 */
public class Sender {
    private final String address;
    private final String alias;

    public Sender(BitmessageAddress address) {
        this.address = address.getAddress();
        this.alias = address.toString();
    }

    public String getAddress() {
        return address;
    }

    public String getAlias() {
        return alias;
    }
}
