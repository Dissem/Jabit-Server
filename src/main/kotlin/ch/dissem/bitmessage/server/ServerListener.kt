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
import ch.dissem.bitmessage.entity.Plaintext
import ch.dissem.bitmessage.entity.Plaintext.Encoding.EXTENDED
import ch.dissem.bitmessage.entity.Plaintext.Type.MSG
import ch.dissem.bitmessage.entity.valueobject.extended.Message
import ch.dissem.bitmessage.server.Constants.ADMIN_LIST
import ch.dissem.bitmessage.server.Constants.BLACKLIST
import ch.dissem.bitmessage.server.Constants.CLIENT_LIST
import ch.dissem.bitmessage.server.Constants.SHORTLIST
import ch.dissem.bitmessage.server.Constants.WHITELIST
import org.slf4j.LoggerFactory
import java.util.*

/**
 * @author Christian Basler
 */
class ServerListener(private val admins: MutableCollection<BitmessageAddress>,
                     private val clients: MutableCollection<BitmessageAddress>,
                     private val whitelist: MutableCollection<String>,
                     private val shortlist: MutableCollection<String>,
                     private val blacklist: MutableCollection<String>) : BitmessageContext.Listener.WithContext {

    private lateinit var ctx: BitmessageContext
    private val identity: BitmessageAddress by lazy {
        val identities = ctx.addresses.getIdentities()
        if (identities.isEmpty()) {
            ctx.createIdentity(false)
        } else {
            identities[0]
        }
    }

    override fun setContext(ctx: BitmessageContext) {
        this.ctx = ctx
    }

    override fun receive(plaintext: Plaintext) {
        if (admins.contains(plaintext.from)) {
            val command = plaintext.subject!!.trim { it <= ' ' }.toLowerCase().split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val data = plaintext.text
            if (command.size == 1) {
                when (command[0].toLowerCase()) {
                    "status" -> {
                        val response = Plaintext.Builder(MSG)
                        response.from(identity)
                        response.to(plaintext.from)
                        if (plaintext.encoding == EXTENDED) {
                            response.message(
                                    Message.Builder()
                                            .subject("RE: status")
                                            .body(ctx.status().toString())
                                            .addParent(plaintext)
                                            .build()
                            )
                        } else {
                            response.message("RE: status", ctx.status().toString())
                        }
                        ctx.send(response.build())
                    }
                    else -> LOG.info("ignoring  unknown command " + plaintext.subject!!)
                }
            } else if (command.size == 2) {
                when (command[1].toLowerCase()) {
                    "client", "clients" -> updateUserList(CLIENT_LIST, clients, command[0], data)
                    "admin", "admins", "administrator", "administrators" -> updateUserList(ADMIN_LIST, admins, command[0], data)
                    "whitelist" -> updateList(WHITELIST, whitelist, command[0], data)
                    "shortlist" -> updateList(SHORTLIST, shortlist, command[0], data)
                    "blacklist" -> updateList(BLACKLIST, blacklist, command[0], data)
                    else -> LOG.info("ignoring  unknown command " + plaintext.subject!!)
                }
            }
        }
    }

    private fun updateUserList(file: String, list: MutableCollection<BitmessageAddress>, command: String, data: String?) {
        when (command.toLowerCase()) {
            "set" -> {
                list.clear()
                val scanner = Scanner(data!!)
                while (scanner.hasNextLine()) {
                    val line = scanner.nextLine()
                    try {
                        list.add(BitmessageAddress(line))
                    } catch (e: Exception) {
                        LOG.info("$command $file: ignoring line: $line")
                    }

                }
                Utils.saveList(file, list.map { it.address })
            }
            "add" -> {
                val scanner = Scanner(data!!)
                while (scanner.hasNextLine()) {
                    val line = scanner.nextLine()
                    try {
                        list.add(BitmessageAddress(line))
                    } catch (e: Exception) {
                        LOG.info("$command $file: ignoring line: $line")
                    }

                }
                Utils.saveList(file, list.map { it.address })
            }
            "remove" -> {
                list.removeIf { address -> data!!.contains(address.address) }
                Utils.saveList(file, list.map { it.address })
            }
            else -> LOG.info("unknown command $command on list $file")
        }
    }

    private fun updateList(file: String, list: MutableCollection<String>, command: String, data: String?) {
        when (command.toLowerCase()) {
            "set" -> {
                list.clear()
                val scanner = Scanner(data!!)
                while (scanner.hasNextLine()) {
                    val line = scanner.nextLine()
                    try {
                        list.add(BitmessageAddress(line).address)
                    } catch (e: Exception) {
                        LOG.info("$command $file: ignoring line: $line")
                    }

                }
                Utils.saveList(file, list)
            }
            "add" -> {
                val scanner = Scanner(data!!)
                while (scanner.hasNextLine()) {
                    val line = scanner.nextLine()
                    try {
                        list.add(BitmessageAddress(line).address)
                    } catch (e: Exception) {
                        LOG.info("$command $file: ignoring line: $line")
                    }

                }
                Utils.saveList(file, list)
            }
            "remove" -> {
                list.removeAll { data!!.contains(it) }
                Utils.saveList(file, list)
            }
            else -> LOG.info("unknown command $command on list $file")
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ServerListener::class.java)
    }
}
