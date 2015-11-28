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

import ch.dissem.bitmessage.entity.CustomMessage;
import ch.dissem.bitmessage.entity.MessagePayload;
import ch.dissem.bitmessage.exception.DecryptionFailedException;
import ch.dissem.bitmessage.extensions.CryptoCustomMessage;
import ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest;
import ch.dissem.bitmessage.ports.CustomCommandHandler;
import ch.dissem.bitmessage.ports.ProofOfWorkEngine;
import ch.dissem.bitmessage.server.repository.ProofOfWorkRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Christian Basler
 */
public class ProofOfWorkRequestHandler implements CustomCommandHandler {
    private final List<byte[]> decryptionKeys = new ArrayList<>();
    private ProofOfWorkRepository repo;
    private ProofOfWorkEngine engine;

    @Override
    public MessagePayload handle(CustomMessage message) {
        try {
            CryptoCustomMessage<ProofOfWorkRequest> cryptoMessage = CryptoCustomMessage.read(message.getData(),
                    (sender, in) -> ProofOfWorkRequest.read(sender, in));
            ProofOfWorkRequest request = decrypt(cryptoMessage);
            if (request == null) return error("Unknown encryption key.");
            switch (request.getRequest()) {
                case CALCULATE:
                    repo.storeTask(request); // FIXME
                    engine.calculateNonce(request.getInitialHash(), request.getData(), nonce -> {

                    });
            }
            return null;
        } catch (IOException e) {
            return error(e.getMessage());
        }
    }

    private MessagePayload error(String message) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write("ERROR\n".getBytes("UTF-8"));
            out.write(message.getBytes("UTF-8"));
            return new CustomMessage(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

}
