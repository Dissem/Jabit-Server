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
import ch.dissem.bitmessage.cryptography.bc.BouncyCryptography
import ch.dissem.bitmessage.entity.BitmessageAddress
import ch.dissem.bitmessage.entity.payload.Pubkey
import ch.dissem.bitmessage.networking.nio.NioNetworkHandler
import ch.dissem.bitmessage.repository.*
import ch.dissem.bitmessage.server.Constants.ADMIN_LIST
import ch.dissem.bitmessage.server.Constants.BLACKLIST
import ch.dissem.bitmessage.server.Constants.CLIENT_LIST
import ch.dissem.bitmessage.server.Constants.SHORTLIST
import ch.dissem.bitmessage.server.Constants.WHITELIST
import ch.dissem.bitmessage.server.repository.ServerProofOfWorkRepository
import ch.dissem.bitmessage.utils.Singleton
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket

@Configuration
class JabitServerConfig {

    @Value("\${bitmessage.port}")
    private val port: Int = 0
    @Value("\${bitmessage.connection.ttl.hours}")
    private val connectionTTL: Long = 0
    @Value("\${bitmessage.connection.limit}")
    private val connectionLimit: Int = 0

    @Value("\${database.url}")
    private lateinit var dbUrl: String
    @Value("\${database.user}")
    private lateinit var dbUser: String
    @Value("\${database.password}")
    private var dbPassword: String? = null

    @Bean
    fun jdbcConfig() = JdbcConfig(dbUrl, dbUser, dbPassword)

    @Bean
    fun addressRepo() = JdbcAddressRepository(jdbcConfig())

    @Bean
    fun inventory() = JdbcInventory(jdbcConfig())

    @Bean
    fun labelRepo() = JdbcLabelRepository(jdbcConfig())

    @Bean
    fun messageRepo() = JdbcMessageRepository(jdbcConfig())

    @Bean
    fun proofOfWorkRepo() = JdbcProofOfWorkRepository(jdbcConfig())

    @Bean
    fun nodeRegistry() = JdbcNodeRegistry(jdbcConfig())

    @Bean
    fun networkHandler() = NioNetworkHandler()

    @Bean
    fun cryptography() = BouncyCryptography().also {
        Singleton.initialize(it) // needed for admins and clients
    }

    @Bean
    fun serverListener(): BitmessageContext.Listener =
            ServerListener(
                    admins(), clients(),
                    whitelist(), shortlist(), blacklist()
            )

    @Bean
    fun serverProofOfWorkRepository() = ServerProofOfWorkRepository(jdbcConfig())

    @Bean
    fun commandHandler() = ProofOfWorkRequestHandler(serverProofOfWorkRepository(), clients())

    @Bean
    fun bitmessageContext() = BitmessageContext.build {
        addressRepo = addressRepo()
        inventory = inventory()
        labelRepo = labelRepo()
        messageRepo = messageRepo()
        nodeRegistry = nodeRegistry()
        proofOfWorkRepo = proofOfWorkRepo()
        networkHandler = networkHandler()
        listener = serverListener()
        customCommandHandler = commandHandler()
        cryptography = cryptography()
        preferences.port = port
        preferences.connectionLimit = connectionLimit
        preferences.connectionTTL = connectionTTL
    }

    @Bean
    @DependsOn("cryptography")
    fun admins() = Utils.readOrCreateList(
            ADMIN_LIST,
            """# Admins can send commands to the server.
                |
            """.trimMargin()
    ).map { BitmessageAddress(it) }.toMutableSet()

    @Bean
    @DependsOn("cryptography")
    fun clients() = Utils.readOrCreateList(
            CLIENT_LIST,
            "# Clients may send incomplete objects for proof of work.\n"
    ).map { BitmessageAddress(it) }.toMutableSet()

    @Bean
    fun whitelist() = Utils.readOrCreateList(
            WHITELIST,
            """# If there are any Bitmessage addresses in the whitelist, only those will be shown.
                |# blacklist.conf will be ignored, but shortlist.conf will be applied to whitelisted addresses.
                |
            """.trimMargin()
    )

    @Bean
    fun shortlist() = Utils.readOrCreateList(
            SHORTLIST,
            """"# Broadcasts of these addresses will be restricted to the last $SHORTLIST_SIZE entries.
                |
                |# Time Service:
                |BM-BcbRqcFFSQUUmXFKsPJgVQPSiFA3Xash
                |
                |# Q's Aktivlist:
                |BM-GtT7NLCCAu3HrT7dNTUTY9iDns92Z2ND
                |
            """.trimMargin()
    )

    @Bean
    fun blacklist() = Utils.readOrCreateList(
            BLACKLIST,
            """# Bitmessage addresses in this file are being ignored and their broadcasts won't be returned.
                |
            """.trimMargin()
    )

    @Bean
    fun api(): Docket = Docket(DocumentationType.SWAGGER_2)
            .select()
            .build()

    @Bean
    fun identity(): BitmessageAddress {
        val identities = bitmessageContext().addresses.getIdentities()
        val identity: BitmessageAddress
        if (identities.isEmpty()) {
            LOG.info("Creating new identity...")
            identity = bitmessageContext().createIdentity(false, Pubkey.Feature.DOES_ACK)
            LOG.info("Identity " + identity.address + " created.")
        } else {
            LOG.info("Identities:")
            identities.forEach { LOG.info(it.address) }
            identity = identities[0]
            if (identities.size > 1) {
                LOG.info("Using " + identity)
            }
        }
        LOG.info("QR Code:\n" + Utils.qrCode(identity))
        return identity
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(JabitServerConfig::class.java)

        val SHORTLIST_SIZE = 5
    }
}
