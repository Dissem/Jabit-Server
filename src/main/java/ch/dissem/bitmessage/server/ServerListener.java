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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Scanner;

import static ch.dissem.bitmessage.server.Constants.*;

/**
 * @author Christian Basler
 */
public class ServerListener implements BitmessageContext.Listener {
    private final static Logger LOG = LoggerFactory.getLogger(ServerListener.class);

    private final Collection<BitmessageAddress> admins;
    private final Collection<BitmessageAddress> clients;

    private final Collection<String> whitelist;
    private final Collection<String> shortlist;
    private final Collection<String> blacklist;

    public ServerListener(Collection<BitmessageAddress> admins,
                          Collection<BitmessageAddress> clients,
                          Collection<String> whitelist,
                          Collection<String> shortlist,
                          Collection<String> blacklist) {
        this.admins = admins;
        this.clients = clients;
        this.whitelist = whitelist;
        this.shortlist = shortlist;
        this.blacklist = blacklist;
    }

    @Override
    public void receive(Plaintext message) {
        if (admins.contains(message.getFrom())) {
            String[] command = message.getSubject().trim().toLowerCase().split("\\s+");
            String data = message.getText();
            if (command.length == 2) {
                switch (command[1]) {
                    case "client":
                    case "clients":
                        updateUserList(CLIENT_LIST, clients, command[0], data);
                        break;
                    case "admin":
                    case "admins":
                    case "administrator":
                    case "administrators":
                        updateUserList(ADMIN_LIST, admins, command[0], data);
                        break;
                    case "whitelist":
                        updateList(WHITELIST, whitelist, command[0], data);
                        break;
                    case "shortlist":
                        updateList(SHORTLIST, shortlist, command[0], data);
                        break;
                    case "blacklist":
                        updateList(BLACKLIST, blacklist, command[0], data);
                        break;
                    default:
                        LOG.trace("ignoring  unknown command " + message.getSubject());
                }
            }
        }
    }

    private void updateUserList(String file, Collection<BitmessageAddress> list, String command, String data) {
        switch (command) {
            case "set":
                list.clear();
            case "add":
                Scanner scanner = new Scanner(data);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    try {
                        list.add(new BitmessageAddress(line));
                    } catch (Exception e) {
                        LOG.info(command + " " + file + ": ignoring line: " + line);
                    }
                }
                Utils.saveList(file, list.stream().map(BitmessageAddress::getAddress));
                break;
            case "remove":
                list.removeIf(address -> data.contains(address.getAddress()));
                Utils.saveList(file, list.stream().map(BitmessageAddress::getAddress));
                break;
            default:
                LOG.info("unknown command " + command + " on list " + file);
        }
    }

    private void updateList(String file, Collection<String> list, String command, String data) {
        switch (command) {
            case "set":
                list.clear();
            case "add":
                Scanner scanner = new Scanner(data);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    try {
                        list.add(new BitmessageAddress(line).getAddress());
                    } catch (Exception e) {
                        LOG.info(command + " " + file + ": ignoring line: " + line);
                    }
                }
                Utils.saveList(file, list.stream());
                break;
            case "remove":
                list.removeIf(data::contains);
                Utils.saveList(file, list.stream());
                break;
            default:
                LOG.info("unknown command " + command + " on list " + file);
        }
    }
}
