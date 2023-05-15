/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.PeerEntity;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
public class PeerDao {

    private final DatabaseManager databaseManager;

    @Inject
    public PeerDao(DatabaseManager databaseManagerParam) {
        databaseManager = databaseManagerParam;
    }


    public List<PeerEntity> loadPeers() {
        List<PeerEntity> peers = new ArrayList<>();

        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM peer");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                peers.add(new PeerEntity(
                        rs.getString("address"),
                        rs.getLong("services"),
                        rs.getInt("last_updated"),
                        rs.getString("x509pem"),
                        rs.getString("ip_and_port")
                       )
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return peers;
    }


    public void deletePeer(PeerEntity peer) {

        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection()) {
            PreparedStatement pstmt = con.prepareStatement("DELETE FROM peer WHERE address = ?");
            pstmt.setString(1, peer.getAddress());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


    public void deletePeers(Collection<PeerEntity> peers) {

        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM peer WHERE address = ?")) {
            for (PeerEntity peer : peers) {
                pstmt.setString(1, peer.getAddress());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void updatePeers(Collection<PeerEntity> peers) {

        TransactionalDataSource dataSource = databaseManager.getDataSource();
        dataSource.begin();
        try (Connection con = dataSource.getConnection();
             @DatabaseSpecificDml(DmlMarker.MERGE)

             PreparedStatement pstmt = con.prepareStatement("INSERT INTO peer "
                 + "(address, services, last_updated) "
                 + "VALUES(?, ?, ?) "
                 + "ON DUPLICATE KEY UPDATE "
                 + "address = VALUES(address), services = VALUES(services), last_updated = VALUES(last_updated)")
        ) {
            for (PeerEntity peer : peers) {
                pstmt.setString(1, peer.getAddress());
                pstmt.setLong(2, peer.getServices());
                pstmt.setInt(3, peer.getLastUpdated());
                pstmt.executeUpdate();
            }
            dataSource.commit();
        } catch (SQLException e) {
            dataSource.rollback();
            throw new RuntimeException(e.toString(), e);
        }
    }


    public void updatePeer(Peer peer) {

        TransactionalDataSource dataSource = databaseManager.getDataSource();
        dataSource.begin();
        try (Connection con = dataSource.getConnection();
             @DatabaseSpecificDml(DmlMarker.MERGE)
             PreparedStatement pstmt = con.prepareStatement("INSERT INTO peer "
                 + "(address, services, last_updated) VALUES(?, ?, ?) "
                 + "ON DUPLICATE KEY UPDATE "
                 + "address = VALUES(address), services = VALUES(services), last_updated = VALUES(last_updated)")) {
            pstmt.setString(1, peer.getAnnouncedAddress());
            pstmt.setLong(2, peer.getServices());
            pstmt.setInt(3, peer.getLastUpdated());
            pstmt.executeUpdate();
            dataSource.commit();
        } catch (SQLException e) {
            dataSource.rollback();
            throw new RuntimeException(e.toString(), e);
        }
    }

}
