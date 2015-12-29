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

import ch.dissem.bitmessage.InternalContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.CustomMessage;
import ch.dissem.bitmessage.entity.MessagePayload;
import ch.dissem.bitmessage.entity.valueobject.PrivateKey;
import ch.dissem.bitmessage.exception.DecryptionFailedException;
import ch.dissem.bitmessage.extensions.CryptoCustomMessage;
import ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest;
import ch.dissem.bitmessage.ports.CustomCommandHandler;
import ch.dissem.bitmessage.ports.ProofOfWorkEngine;
import ch.dissem.bitmessage.server.repository.ServerProofOfWorkRepository;
import ch.dissem.bitmessage.utils.UnixTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import static ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest.Request.CALCULATING;
import static ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest.Request.COMPLETE;
import static ch.dissem.bitmessage.utils.UnixTime.DAY;

/**
 * @author Christian Basler
 */
public class ProofOfWorkRequestHandler implements CustomCommandHandler, InternalContext.ContextHolder {
    private final static Logger LOG = LoggerFactory.getLogger(ProofOfWorkRequestHandler.class);

    private final List<byte[]> decryptionKeys;
    private final ServerProofOfWorkRepository repo;
    private BitmessageAddress serverIdentity;
    private ProofOfWorkEngine engine;
    private InternalContext context;

    public ProofOfWorkRequestHandler(ServerProofOfWorkRepository repo, Collection<BitmessageAddress> clients) {
        this.repo = repo;
        decryptionKeys = clients.stream().map(BitmessageAddress::getPublicDecryptionKey).collect(Collectors.toList());
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                doMissingProofOfWork();
            }
        }, 15_000); // After 15 seconds
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                repo.cleanupTasks(7 * DAY);
            }
        }, 60_000, DAY * 1000); // First time after 1 minute, then daily
    }

    public void doMissingProofOfWork() {
        List<ServerProofOfWorkRepository.Task> incompleteTasks = repo.getIncompleteTasks();
        LOG.info("Doing POW for " + incompleteTasks.size() + " tasks.");
        for (ServerProofOfWorkRepository.Task task : incompleteTasks) {
            engine.calculateNonce(task.initialHash, task.target, repo::updateTask);
        }
    }

    @Override
    public MessagePayload handle(CustomMessage message) {
        try {
            CryptoCustomMessage<ProofOfWorkRequest> cryptoMessage = CryptoCustomMessage.read(message,
                    ProofOfWorkRequest::read);
            ProofOfWorkRequest request = decrypt(cryptoMessage);
            if (request == null) {
                return CustomMessage.error(
                        "Unknown sender. Please ask the server's administrator to add you as a client. " +
                                "For this he'll need your identity."
                );
            }
            switch (request.getRequest()) {
                case CALCULATE:
                    if (!repo.hasTask(request.getInitialHash())) {
                        repo.storeTask(request);
                        // TODO: This is probably the place to do some book-keeping
                        // if we want to bill our customers.
                        engine.calculateNonce(request.getInitialHash(), request.getData(), repo::updateTask);
                        return new CryptoCustomMessage<>(
                                new ProofOfWorkRequest(getIdentity(), request.getInitialHash(), CALCULATING, new byte[0])
                        );
                    } else {
                        byte[] nonce = repo.getNonce(request);
                        CryptoCustomMessage<ProofOfWorkRequest> response;
                        if (nonce != null) {
                            response = new CryptoCustomMessage<>(
                                    new ProofOfWorkRequest(getIdentity(), request.getInitialHash(), COMPLETE, nonce)
                            );
                        } else {
                            response = new CryptoCustomMessage<>(
                                    new ProofOfWorkRequest(getIdentity(), request.getInitialHash(), CALCULATING, new byte[0])
                            );
                        }
                        response.signAndEncrypt(serverIdentity, request.getSender().getPubkey().getEncryptionKey());
                        return response;
                    }
            }
            return null;
        } catch (IOException e) {
            return CustomMessage.error(e.getMessage());
        }
    }

    private BitmessageAddress getIdentity() {
        if (serverIdentity == null) {
            synchronized (this) {
                if (serverIdentity == null) {
                    serverIdentity = context.getAddressRepository().getIdentities().stream().findFirst().orElseGet(() -> {
                        final BitmessageAddress identity = new BitmessageAddress(new PrivateKey(
                                false,
                                context.getStreams()[0],
                                context.getNetworkNonceTrialsPerByte(),
                                context.getNetworkExtraBytes()
                        ));
                        context.getAddressRepository().save(identity);
                        return identity;
                    });
                }
            }
        }
        return serverIdentity;
    }

    private ProofOfWorkRequest decrypt(CryptoCustomMessage<ProofOfWorkRequest> cryptoMessage) {
        for (byte[] key : decryptionKeys) {
            try {
                return cryptoMessage.decrypt(key);
            } catch (DecryptionFailedException ignore) {
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    @Override
    public void setContext(InternalContext context) {
        this.context = context;
        this.engine = context.getProofOfWorkEngine();
    }
}
