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

package ch.dissem.bitmessage.server.repository;

import ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest;
import ch.dissem.bitmessage.repository.JdbcConfig;
import ch.dissem.bitmessage.repository.JdbcHelper;
import ch.dissem.bitmessage.utils.UnixTime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Christian Basler
 */
public class ServerProofOfWorkRepository extends JdbcHelper {

    public ServerProofOfWorkRepository(JdbcConfig config) {
        super(config);
    }

    /**
     * client (can be removed once the new IV is returned)
     * IV (without nonce)
     * IV (with nonce, can be removed once the new IV is returned)
     * status: calculating, finished, confirmed
     * data (can be removed once POW calculation is done)
     */
    public void storeTask(ProofOfWorkRequest request) {
        try (Connection connection = config.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO ProofOfWorkTask (initial_hash, client, target, timestamp) VALUES (?, ?, ?, ?)");
            ps.setBytes(1, request.getInitialHash());
            ps.setString(2, request.getSender().getAddress());
            ps.setBytes(3, request.getData());
            ps.setLong(4, UnixTime.now());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateTask(byte[] initalHash, byte[] nonce) {
        try (Connection connection = config.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE ProofOfWorkTask SET nonce = ? WHERE initial_hash = ?");
            ps.setBytes(1, nonce);
            ps.setBytes(2, initalHash);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getNonce(ProofOfWorkRequest request) {
        try (Connection connection = config.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("SELECT nonce FROM ProofOfWorkTask WHERE initial_hash = ?");
            ps.setBytes(1, request.getInitialHash());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBytes(1);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasTask(byte[] initialHash) {
        try (Connection connection = config.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("SELECT count(1) FROM ProofOfWorkTask WHERE initial_hash = ?");
            ps.setBytes(1, initialHash);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Task> getIncompleteTasks() {
        try (Connection connection = config.getConnection()) {
            List<Task> result = new LinkedList<>();
            ResultSet rs = connection.createStatement().executeQuery(
                    "SELECT initial_hash, target FROM ProofOfWorkTask WHERE nonce IS NULL");
            while (rs.next()) {
                result.add(new Task(
                        rs.getBytes(1),
                        rs.getBytes(2)
                ));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void cleanupTasks(long ageInSeconds) {
        try (Connection connection = config.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM ProofOfWorkTask WHERE timestamp < ?");
            ps.setLong(1, UnixTime.now() - ageInSeconds);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Task {
        public final byte[] initialHash;
        public final byte[] target;

        private Task(byte[] initialHash, byte[] target) {
            this.initialHash = initialHash;
            this.target = target;
        }
    }
}
