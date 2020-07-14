/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.shard.model.PrevBlockData;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Singleton
public class PrevBlockInfoExtractor {

    private DatabaseManager databaseManager;

    @Inject
    public PrevBlockInfoExtractor(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "Database manager is null");
    }

    public PrevBlockData extractPrevBlockData(int height, int limit) {
        return PrevBlockData.builder()
            .generatorIds(Convert.toObjectLongArray(extractObjects(height, limit, "generator_id")))
            .prevBlockTimeouts(Convert.toObjectIntArray(extractObjects(height, limit, "timeout")))
            .prevBlockTimestamps(Convert.toObjectIntArray(extractObjects(height, limit, "timestamp")))
            .build();
    }


    private <T> List<T> extractObjects(int height, int limit, String columnName) {
        List<T> extractedObjects = new ArrayList<>();
        try (Connection con = databaseManager.getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT " + columnName + " FROM block WHERE height < ?  ORDER by height DESC LIMIT ?")) {
            pstmt.setInt(1, height);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    extractedObjects.add((T) rs.getObject(columnName));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return extractedObjects;
    }
}
