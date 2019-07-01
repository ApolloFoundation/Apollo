/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;

@Singleton
public class GeneratorIdsExtractor {

    private DatabaseManager databaseManager;

    @Inject
    public GeneratorIdsExtractor(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "Database manager is null");
    }

    public List<Long> extractGeneratorIdsBefore(int height, int limit) {
        List<Long> generatorIds = new ArrayList<>();
        try(Connection con = databaseManager.getDataSource().getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT generator_id FROM block WHERE height < ?  ORDER by height DESC LIMIT ?")) {
            pstmt.setInt(1, height);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    generatorIds.add(rs.getLong("generator_id"));
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return generatorIds;
    }
}
