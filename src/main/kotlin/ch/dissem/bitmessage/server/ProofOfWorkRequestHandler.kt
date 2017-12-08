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

import ch.dissem.bitmessage.InternalContext
import ch.dissem.bitmessage.entity.BitmessageAddress
import ch.dissem.bitmessage.entity.CustomMessage
import ch.dissem.bitmessage.entity.MessagePayload
import ch.dissem.bitmessage.entity.valueobject.PrivateKey
import ch.dissem.bitmessage.exception.DecryptionFailedException
import ch.dissem.bitmessage.extensions.CryptoCustomMessage
import ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest
import ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest.Request.CALCULATING
import ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest.Request.COMPLETE
import ch.dissem.bitmessage.ports.CustomCommandHandler
import ch.dissem.bitmessage.ports.ProofOfWorkEngine
import ch.dissem.bitmessage.server.repository.ServerProofOfWorkRepository
import ch.dissem.bitmessage.utils.UnixTime.DAY
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.concurrent.schedule

/**
 * @author Christian Basler
 */
class ProofOfWorkRequestHandler(private val repo: ServerProofOfWorkRepository, clients: Collection<BitmessageAddress>) : CustomCommandHandler, InternalContext.ContextHolder {

    private val decryptionKeys = clients.map { it.publicDecryptionKey }
    private lateinit var engine: ProofOfWorkEngine
    private lateinit var context: InternalContext

    private val identity: BitmessageAddress by lazy {
        context.addressRepository.getIdentities().firstOrNull() ?: BitmessageAddress(PrivateKey(
                false,
                context.streams[0],
                InternalContext.NETWORK_NONCE_TRIALS_PER_BYTE,
                InternalContext.NETWORK_EXTRA_BYTES
        )).also { context.addressRepository.save(it) }
    }

    init {
        Timer().schedule(15000) {
            doMissingProofOfWork()
        } // wait  15 seconds
        Timer().schedule(60000, DAY * 1000) {
            repo.cleanupTasks(7 * DAY)
        } // First time after 1 minute, then daily
    }

    fun doMissingProofOfWork() {
        val incompleteTasks = repo.getIncompleteTasks()
        LOG.info("Doing POW for ${incompleteTasks.size} tasks.")
        for (task in incompleteTasks) {
            engine.calculateNonce(task.initialHash, task.target) { initalHash, nonce -> repo.updateTask(initalHash, nonce) }
        }
    }

    override fun handle(request: CustomMessage): MessagePayload? {
        val cryptoMessage = CryptoCustomMessage.read(request) { client, input ->
            ProofOfWorkRequest.read(client, input)
        }
        val decryptedRequest = decrypt(cryptoMessage) ?: return CustomMessage.error(
                "Unknown sender. Please ask the server's administrator to add you as a client. For this he'll need your identity."
        )
        when (decryptedRequest.request) {
            ProofOfWorkRequest.Request.CALCULATE -> {
                if (!repo.hasTask(decryptedRequest.initialHash)) {
                    repo.storeTask(decryptedRequest)
                    // TODO: This is probably the place to do some book-keeping
                    // if we want to bill our customers.
                    engine.calculateNonce(decryptedRequest.initialHash, decryptedRequest.data) { initalHash, nonce -> repo.updateTask(initalHash, nonce) }
                    return CryptoCustomMessage(
                            ProofOfWorkRequest(identity, decryptedRequest.initialHash, CALCULATING, ByteArray(0))
                    )
                } else {
                    val nonce = repo.getNonce(decryptedRequest)
                    return CryptoCustomMessage(
                            if (nonce != null) {
                                ProofOfWorkRequest(identity, decryptedRequest.initialHash, COMPLETE, nonce)
                            } else {
                                ProofOfWorkRequest(identity, decryptedRequest.initialHash, CALCULATING, ByteArray(0))
                            }
                    ).apply { signAndEncrypt(identity, decryptedRequest.sender.pubkey!!.encryptionKey) }
                }
            }
            else -> return null
        }
    }

    private fun decrypt(cryptoMessage: CryptoCustomMessage<ProofOfWorkRequest>): ProofOfWorkRequest? {
        for (key in decryptionKeys) {
            try {
                return cryptoMessage.decrypt(key)
            } catch (_: DecryptionFailedException) {
            }
        }
        return null
    }

    override fun setContext(context: InternalContext) {
        this.context = context
        this.engine = context.proofOfWorkEngine
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ProofOfWorkRequestHandler::class.java)
    }
}
