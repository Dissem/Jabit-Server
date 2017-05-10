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

package ch.dissem.bitmessage.server;

import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.cryptography.bc.BouncyCryptography;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.payload.Pubkey;
import ch.dissem.bitmessage.networking.nio.NioNetworkHandler;
import ch.dissem.bitmessage.ports.*;
import ch.dissem.bitmessage.repository.*;
import ch.dissem.bitmessage.server.repository.ServerProofOfWorkRepository;
import ch.dissem.bitmessage.utils.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.List;
import java.util.Set;

import static ch.dissem.bitmessage.server.Constants.*;
import static java.util.stream.Collectors.toSet;

@Configuration
public class JabitServerConfig {
    private static final Logger LOG = LoggerFactory.getLogger(JabitServerConfig.class);

    public static final int SHORTLIST_SIZE = 5;

    @Value("${bitmessage.port}")
    private int port;
    @Value("${bitmessage.connection.ttl.hours}")
    private int connectionTTL;
    @Value("${bitmessage.connection.limit}")
    private int connectionLimit;

    @Value("${database.url}")
    private String dbUrl;
    @Value("${database.user}")
    private String dbUser;
    @Value("${database.password}")
    private String dbPassword;

    @Bean
    public JdbcConfig jdbcConfig() {
        return new JdbcConfig(dbUrl, dbUser, dbPassword);
    }

    @Bean
    public AddressRepository addressRepo() {
        return new JdbcAddressRepository(jdbcConfig());
    }

    @Bean
    public Inventory inventory() {
        return new JdbcInventory(jdbcConfig());
    }

    @Bean
    public MessageRepository messageRepo() {
        return new JdbcMessageRepository(jdbcConfig());
    }

    @Bean
    public ProofOfWorkRepository proofOfWorkRepo() {
        return new JdbcProofOfWorkRepository(jdbcConfig());
    }

    @Bean
    public NodeRegistry nodeRegistry() {
        return new JdbcNodeRegistry(jdbcConfig());
    }

    @Bean
    public NetworkHandler networkHandler() {
        return new NioNetworkHandler();
    }

    @Bean
    public Cryptography cryptography() {
        Cryptography cryptography = new BouncyCryptography();
        Singleton.initialize(cryptography); // needed for admins and clients
        return cryptography;
    }

    @Bean
    public BitmessageContext.Listener serverListener() {
        return new ServerListener(admins(), clients(), whitelist(), shortlist(), blacklist());
    }

    @Bean
    public ServerProofOfWorkRepository serverProofOfWorkRepository() {
        return new ServerProofOfWorkRepository(jdbcConfig());
    }

    @Bean
    public CustomCommandHandler commandHandler() {
        return new ProofOfWorkRequestHandler(serverProofOfWorkRepository(), clients());
    }

    @Bean
    public BitmessageContext bitmessageContext() {
        return new BitmessageContext.Builder()
                .addressRepo(addressRepo())
                .inventory(inventory())
                .messageRepo(messageRepo())
                .nodeRegistry(nodeRegistry())
                .powRepo(proofOfWorkRepo())
                .networkHandler(networkHandler())
                .listener(serverListener())
                .customCommandHandler(commandHandler())
                .cryptography(cryptography())
                .port(port)
                .connectionLimit(connectionLimit)
                .connectionTTL(connectionTTL)
                .build();
    }

    @Bean
    public Set<BitmessageAddress> admins() {
        cryptography();
        return Utils.readOrCreateList(
                ADMIN_LIST,
                "# Admins can send commands to the server.\n"
        ).stream().map(BitmessageAddress::new).collect(toSet());
    }

    @Bean
    public Set<BitmessageAddress> clients() {
        cryptography();
        return Utils.readOrCreateList(
                CLIENT_LIST,
                "# Clients may send incomplete objects for proof of work.\n"
        ).stream().map(BitmessageAddress::new).collect(toSet());
    }

    @Bean
    public Set<String> whitelist() {
        return Utils.readOrCreateList(
                WHITELIST,
                "# If there are any Bitmessage addresses in the whitelist, only those will be shown.\n" +
                        "# blacklist.conf will be ignored, but shortlist.conf will be applied to whitelisted addresses.\n"
        );
    }

    @Bean
    public Set<String> shortlist() {
        return Utils.readOrCreateList(
                SHORTLIST,
                "# Broadcasts of these addresses will be restricted to the last " + SHORTLIST_SIZE + " entries.\n\n" +
                        "# Time Service:\n" +
                        "BM-BcbRqcFFSQUUmXFKsPJgVQPSiFA3Xash\n\n" +
                        "# Q's Aktivlist:\n" +
                        "BM-GtT7NLCCAu3HrT7dNTUTY9iDns92Z2ND\n"
        );
    }

    @Bean
    public Set<String> blacklist() {
        return Utils.readOrCreateList(
                BLACKLIST,
                "# Bitmessage addresses in this file are being ignored and their broadcasts won't be returned.\n"
        );
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .build();
    }

    @Bean
    public BitmessageAddress identity() {
        List<BitmessageAddress> identities = bitmessageContext().addresses().getIdentities();
        BitmessageAddress identity;
        if (identities.isEmpty()) {
            LOG.info("Creating new identity...");
            identity = bitmessageContext().createIdentity(false, Pubkey.Feature.DOES_ACK);
            LOG.info("Identity " + identity.getAddress() + " created.");
        } else {
            LOG.info("Identities:");
            identities.stream().map(BitmessageAddress::getAddress).forEach(LOG::info);
            identity = identities.get(0);
            if (identities.size() > 1) {
                LOG.info("Using " + identity);
            }
        }
        return identity;
    }
}
