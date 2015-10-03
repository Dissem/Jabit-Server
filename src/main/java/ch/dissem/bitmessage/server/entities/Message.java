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

import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.utils.UnixTime;

import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * Created by chrigu on 30.09.15.
 */
public class Message {
    private Object id;
    private Long received;
    private String subject;
    private String body;

    public Message() {
    }

    public Message(String subject, String body) {
        this.id = 0;
        this.received = UnixTime.now();
        this.subject = subject;
        this.body = body;
    }

    public Message(Plaintext plaintext) {
        this.id = plaintext.getId();
        this.received = plaintext.getReceived();
        this.subject = plaintext.getSubject();
        this.body = plaintext.getText();
    }

    public Object getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public Long getReceived() {
        return received;
    }
}
