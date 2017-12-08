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

import ch.dissem.bitmessage.BitmessageContext
import ch.dissem.bitmessage.entity.BitmessageAddress
import ch.dissem.bitmessage.server.Converter.broadcasts
import ch.dissem.bitmessage.server.Converter.message
import ch.dissem.bitmessage.server.entities.Broadcasts
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RestController
import javax.annotation.PostConstruct
import javax.annotation.Resource
import javax.inject.Inject

/**
 * @author Christian Basler
 */
@CrossOrigin
@RestController
class JabitServerController {

    @Resource
    private lateinit var whitelist: Set<String>
    @Resource
    private lateinit var shortlist: Set<String>
    @Resource
    private lateinit var blacklist: Set<String>
    @Inject
    private lateinit var identity: BitmessageAddress
    @Inject
    private lateinit var ctx: BitmessageContext

    @RequestMapping(value = ["identity"], method = [GET], produces = ["application/json"])
    fun identity() = """{
    "address": "${identity.address}",
    "uri":     "${Utils.getURL(identity, true)}"
}"""


    @RequestMapping(value = ["status"], method = [GET], produces = ["application/json"])
    fun status() = "{${ctx.status()}}"

    @RequestMapping(value = ["read/{broadcastAddress}"], method = [GET])
    fun read(@PathVariable broadcastAddress: String): Broadcasts {
        if ("test".equals(broadcastAddress, ignoreCase = true)) {
            return broadcasts(
                    BitmessageAddress("BM-2cWhyaPxydemCeM8dWJUBmEo8iu7v2JptK"),
                    message("Test", "This is a test message. The rest service is running."),
                    message("Another Test", "And because it's such fun, a second message.")
            )
        }

        var broadcaster = ctx.addresses.getAddress(broadcastAddress)
        if (broadcaster == null) {
            broadcaster = BitmessageAddress(broadcastAddress)
        }

        if (!whitelist.isEmpty() && !whitelist.contains(broadcaster.address)) {
            return broadcasts(broadcaster, message("Not Whitelisted", "Messages for " + broadcaster +
                    " can't be shown, as the sender isn't on the whitelist."))
        }
        if (blacklist.contains(broadcaster.address)) {
            return broadcasts(broadcaster, message("Blacklisted", "Unfortunately, " + broadcaster +
                    " is on the blacklist, so it's messages can't be shown."))
        }

        if (!broadcaster.isSubscribed) {
            ctx.addSubscribtion(broadcaster)
        }
        val messages = ctx.messages.findMessages(broadcaster)
        return if (shortlist.contains(broadcaster.address) && messages.size > SHORTLIST_SIZE) {
            messages.listIterator(SHORTLIST_SIZE).forEach { ctx.messages.remove(it) }
            broadcasts(broadcaster, messages.subList(0, SHORTLIST_SIZE))
        } else {
            broadcasts(broadcaster, messages)
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    fun broadcastStatus() = ctx.broadcast(identity, "Status", ctx.status().toString())

    @Scheduled(cron = "0 0 2 * * *")
    fun cleanup() = ctx.cleanup()

    @PostConstruct
    fun setUp() = ctx.startup()

    companion object {
        private val SHORTLIST_SIZE = 5
    }
}
