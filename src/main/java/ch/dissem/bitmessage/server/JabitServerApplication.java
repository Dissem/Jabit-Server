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
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.networking.DefaultNetworkHandler;
import ch.dissem.bitmessage.ports.MemoryNodeRegistry;
import ch.dissem.bitmessage.repository.JdbcAddressRepository;
import ch.dissem.bitmessage.repository.JdbcConfig;
import ch.dissem.bitmessage.repository.JdbcInventory;
import ch.dissem.bitmessage.repository.JdbcMessageRepository;
import ch.dissem.bitmessage.security.bc.BouncySecurity;
import ch.dissem.bitmessage.server.entities.Broadcasts;
import ch.dissem.bitmessage.server.entities.Message;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@CrossOrigin
@RestController
@EnableAutoConfiguration
public class JabitServerApplication {
    private static final int SHORTLIST_SIZE = 5;
    private final Set<String> whitelist;
    private final Set<String> shortlist;
    private final Set<String> blacklist;
    private BitmessageContext ctx;

    @RequestMapping("status")
    public String status() {
        return ctx.status().toString();
    }

    @RequestMapping("read/{broadcastAddress}")
    public Broadcasts read(@PathVariable String broadcastAddress) {
        BitmessageAddress broadcaster = ctx.addresses().getAddress(broadcastAddress);
        if (broadcaster == null) {
            broadcaster = new BitmessageAddress(broadcastAddress);
        }

        if (!whitelist.isEmpty() && !whitelist.contains(broadcaster.getAddress())) {
            return new Broadcasts(broadcaster, new Message("Not Whitelisted", "Messages for " + broadcaster +
                    " can't be shown, as the sender isn't on the whitelist."));
        }
        if (blacklist.contains(broadcaster.getAddress())) {
            return new Broadcasts(broadcaster, new Message("Blacklisted", "Unfortunately, " + broadcaster +
                    " is on the blacklist, so it's messages can't be shown."));
        }

        if (!broadcaster.isSubscribed()) {
            ctx.addSubscribtion(broadcaster);
        }
        List<Plaintext> messages = ctx.messages().findMessages(broadcaster);
        if (shortlist.contains(broadcaster.getAddress())) {
            while (messages.size() > SHORTLIST_SIZE) {
                ctx.messages().remove(messages.get(messages.size() - 1));
                messages.remove(messages.size() - 1);
            }
        }
        return new Broadcasts(broadcaster, messages);
    }

    public JabitServerApplication() {
        whitelist = Utils.readOrCreateList(
                "whitelist.conf",
                "# If there are any Bitmessage addresses in the whitelist, only those will be shown.\n" +
                        "# blacklist.conf will be ignored, but shortlist.conf will be applied to whitelisted addresses.\n"
        );
        shortlist = Utils.readOrCreateList(
                "shortlist.conf",
                "# Broadcasts of these addresses will be restricted to the last " + SHORTLIST_SIZE + " entries.\n\n" +
                        "# Time Service:\n" +
                        "BM-BcbRqcFFSQUUmXFKsPJgVQPSiFA3Xash\n\n" +
                        "# Q's Aktivlist:\n" +
                        "BM-GtT7NLCCAu3HrT7dNTUTY9iDns92Z2ND\n"
        );
        blacklist = Utils.readOrCreateList(
                "blacklist.conf",
                "# Bitmessage addresses in this file are being ignored and their broadcasts won't be returned.\n"
        );
        
        JdbcConfig config = new JdbcConfig();
        ctx = new BitmessageContext.Builder()
                .addressRepo(new JdbcAddressRepository(config))
                .inventory(new JdbcInventory(config))
                .messageRepo(new JdbcMessageRepository(config))
                .nodeRegistry(new MemoryNodeRegistry())
                .networkHandler(new DefaultNetworkHandler())
                .security(new BouncySecurity())
                .port(8445)
                .build();
        ctx.startup(plaintext -> {
        });
    }

    public static void main(String[] args) {
        SpringApplication.run(JabitServerApplication.class, args);
    }
}
