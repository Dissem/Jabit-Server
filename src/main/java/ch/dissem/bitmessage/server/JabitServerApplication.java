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
import ch.dissem.bitmessage.server.entities.Broadcasts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.Timer;

import static ch.dissem.bitmessage.server.Converter.broadcasts;
import static ch.dissem.bitmessage.server.Converter.message;

@CrossOrigin
@RestController
@SpringBootApplication
public class JabitServerApplication {
    private static final Logger LOG = LoggerFactory.getLogger(JabitServerApplication.class);

    private static final long HOUR = 60 * 60 * 1000l; // in ms

    private static final String CONFIG_FILE = "config.properties";
    private static final String PROPERTY_PORT = "port";

    private static final int SHORTLIST_SIZE = 5;

    @Resource
    private Set<String> whitelist;
    @Resource
    private Set<String> shortlist;
    @Resource
    private Set<String> blacklist;
    @Inject
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
            return broadcasts(broadcaster, message("Not Whitelisted", "Messages for " + broadcaster +
                    " can't be shown, as the sender isn't on the whitelist."));
        }
        if (blacklist.contains(broadcaster.getAddress())) {
            return broadcasts(broadcaster, message("Blacklisted", "Unfortunately, " + broadcaster +
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
        return broadcasts(broadcaster, messages);
    }

    @PostConstruct
    public void setUp() {
        ctx.startup();
        new Timer().scheduleAtFixedRate(new CleanupJob(ctx), 1 * HOUR, 24 * HOUR);
    }

    public static void main(String[] args) {
        SpringApplication.run(JabitServerApplication.class, args);
    }
}
