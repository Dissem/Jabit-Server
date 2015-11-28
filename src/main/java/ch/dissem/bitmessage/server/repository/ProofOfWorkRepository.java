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

import ch.dissem.bitmessage.InternalContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.valueobject.InventoryVector;
import ch.dissem.bitmessage.extensions.pow.ProofOfWorkRequest;
import ch.dissem.bitmessage.repository.JdbcConfig;
import ch.dissem.bitmessage.repository.JdbcHelper;
import ch.dissem.bitmessage.server.entities.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static ch.dissem.bitmessage.server.repository.ProofOfWorkRepository.Status.*;

/**
 * @author Christian Basler
 */
public class ProofOfWorkRepository extends JdbcHelper implements InternalContext.ContextHolder {
    private static final Logger LOG = LoggerFactory.getLogger(ProofOfWorkRepository.class);

    private InternalContext context;

    protected ProofOfWorkRepository(JdbcConfig config) {
        super(config);
    }

    @Override
    public void setContext(InternalContext context) {
        this.context = context;
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
                    "INSERT INTO ProofOfWorkTask (initial_hash, client, target, status) VALUES (?, ?, ?, ?)");
            ps.setBytes(1, request.getInitialHash());
            ps.setString(2, request.getClient().getAddress());
            ps.setBytes(3, request.getData());
            ps.setString(4, CALCULATING.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateTask(InventoryVector temporaryIV, InventoryVector newIV) {
        try (Connection connection = config.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE ProofOfWorkTask SET IV = ?, status = ?, data = NULL WHERE temporaryIV = ?");
            ps.setBytes(1, newIV.getHash());
            ps.setString(2, FINISHED.name());
            ps.setBytes(3, temporaryIV.getHash());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<Update<InventoryVector>> getUnconfirmed(BitmessageAddress client) {
        List<Update<InventoryVector>> result = new LinkedList<>();
        try (Connection connection = config.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("SELECT temporaryIV, IV FROM ProofOfWorkTask WHERE client = ? AND status = ?");
            ps.setString(1, client.getAddress());
            ps.setString(2, FINISHED.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                InventoryVector temporaryIV = new InventoryVector(rs.getBytes(1));
                InventoryVector iv = new InventoryVector(rs.getBytes(2));
                result.add(new Update<>(temporaryIV, iv));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public void confirm(Stream<InventoryVector> unconfirmed) {
        try (Connection connection = config.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE ProofOfWorkTask SET status = ?, IV = NULL, client = NULL WHERE IV = ANY(?)");
            ps.setString(1, CONFIRMED.name());
            ps.setArray(2, connection.createArrayOf("BINARY", unconfirmed.map(InventoryVector::getHash).toArray()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public enum Status {
        CALCULATING, FINISHED, CONFIRMED
    }
}
