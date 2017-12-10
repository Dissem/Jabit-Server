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

package ch.dissem.bitmessage.server

import ch.dissem.bitmessage.entity.BitmessageAddress
import ch.dissem.bitmessage.entity.Plaintext
import ch.dissem.bitmessage.server.entities.Broadcasts
import ch.dissem.bitmessage.server.entities.Message
import ch.dissem.bitmessage.server.entities.Sender
import ch.dissem.bitmessage.utils.UnixTime

object Converter {
    fun broadcasts(sender: BitmessageAddress, messages: Collection<Plaintext>) = Broadcasts().also {
        it.sender = sender(sender)
        it.messages = messages.map { message(it) }.toTypedArray()
    }

    fun broadcasts(sender: BitmessageAddress, vararg messages: Message) = Broadcasts().also {
        it.sender = sender(sender)
        it.messages = arrayOf(*messages)
    }

    fun sender(sender: BitmessageAddress) = Sender().apply {
        address = sender.address
        alias = sender.toString()
    }

    fun message(subject: String, body: String) = Message().also {
        it.id = 0
        it.received = UnixTime.now
        it.subject = subject
        it.body = body
    }

    fun message(plaintext: Plaintext) = Message().apply {
        id = plaintext.id
        received = plaintext.received
        subject = plaintext.subject
        body = plaintext.text
    }
}
