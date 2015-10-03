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
import ch.dissem.bitmessage.entity.Plaintext;

import java.util.Collection;

/**
 * Created by chrigu on 30.09.15.
 */
public class Broadcasts {
    private final Sender sender;
    private final Message[] messages;

    public Broadcasts(BitmessageAddress sender, Collection<Plaintext> messages) {
        this.sender = new Sender(sender);
        this.messages = new Message[messages.size()];
        int i = 0;
        for (Plaintext msg : messages) {
            this.messages[i] = new Message(msg);
            i++;
        }
    }

    public Broadcasts(BitmessageAddress sender, Message... messages) {
        this.sender = new Sender(sender);
        this.messages = messages;
    }

    public Sender getSender() {
        return sender;
    }

    public Message[] getMessages() {
        return messages;
    }
}
