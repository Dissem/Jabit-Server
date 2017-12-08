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

package ch.dissem.bitmessage.server.repository

import ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest
import ch.dissem.bitmessage.repository.JdbcConfig
import ch.dissem.bitmessage.repository.JdbcHelper
import ch.dissem.bitmessage.utils.UnixTime
import java.util.*

/**
 * @author Christian Basler
 */
class ServerProofOfWorkRepository(config: JdbcConfig) : JdbcHelper(config) {

    fun getIncompleteTasks(): List<Task> {
        config.getConnection().use { connection ->
            val result = LinkedList<Task>()
            val rs = connection.createStatement().executeQuery(
                    "SELECT initial_hash, target FROM ProofOfWorkTask WHERE nonce IS NULL")
            while (rs.next()) {
                result.add(Task(
                        rs.getBytes(1),
                        rs.getBytes(2)
                ))
            }
            return result
        }
    }

    /**
     * client (can be removed once the new IV is returned)
     * IV (without nonce)
     * IV (with nonce, can be removed once the new IV is returned)
     * status: calculating, finished, confirmed
     * data (can be removed once POW calculation is done)
     */
    fun storeTask(request: ProofOfWorkRequest) {
        config.getConnection().use { connection ->
            val ps = connection.prepareStatement(
                    "INSERT INTO ProofOfWorkTask (initial_hash, client, target, timestamp) VALUES (?, ?, ?, ?)")
            ps.setBytes(1, request.initialHash)
            ps.setString(2, request.sender.address)
            ps.setBytes(3, request.data)
            ps.setLong(4, UnixTime.now)
            ps.executeUpdate()
        }
    }

    fun updateTask(initalHash: ByteArray, nonce: ByteArray) {
        config.getConnection().use { connection ->
            val ps = connection.prepareStatement(
                    "UPDATE ProofOfWorkTask SET nonce = ? WHERE initial_hash = ?")
            ps.setBytes(1, nonce)
            ps.setBytes(2, initalHash)
            ps.executeUpdate()
        }
    }

    fun getNonce(request: ProofOfWorkRequest): ByteArray? {
        config.getConnection().use { connection ->
            val ps = connection.prepareStatement("SELECT nonce FROM ProofOfWorkTask WHERE initial_hash = ?")
            ps.setBytes(1, request.initialHash)
            val rs = ps.executeQuery()
            return if (rs.next()) {
                rs.getBytes(1)
            } else {
                null
            }
        }
    }

    fun hasTask(initialHash: ByteArray): Boolean {
        config.getConnection().use { connection ->
            val ps = connection.prepareStatement("SELECT count(1) FROM ProofOfWorkTask WHERE initial_hash = ?")
            ps.setBytes(1, initialHash)
            val rs = ps.executeQuery()
            rs.next()
            return rs.getInt(1) > 0
        }
    }

    fun cleanupTasks(ageInSeconds: Long) {
        config.getConnection().use { connection ->
            val ps = connection.prepareStatement(
                    "DELETE FROM ProofOfWorkTask WHERE timestamp < ?")
            ps.setLong(1, UnixTime.now - ageInSeconds)
            ps.executeUpdate()
        }
    }

    class Task internal constructor(val initialHash: ByteArray, val target: ByteArray)
}
