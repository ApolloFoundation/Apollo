/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata.impl;

import com.apollocurrency.aplwallet.apl.core.dao.appdata.ScanDao;
import com.apollocurrency.aplwallet.apl.core.db.TransactionHelper;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ScanEntity;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class ScanDaoImpl implements ScanDao {
    public static final String DELETE_QUERY = "DELETE from scan";
    private final DatabaseManager databaseManager;
    private static final String INSERT_QUERY = "INSERT INTO scan(rescan, height, validate, current_height, preparation_done, shutdown ) VALUES (?,?,?,?,?,?)  ";
    private static final String SELECT_QUERY = "SELECT * FROM scan";

    @Inject
    public ScanDaoImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void saveOrUpdate(ScanEntity scanEntity) {
        TransactionHelper.executeInTransaction(databaseManager.getDataSource(), ()-> {
            delete();
            try(Connection con = databaseManager.getDataSource().getConnection();
                PreparedStatement pstm = con.prepareStatement(INSERT_QUERY)
            ) {
                pstm.setBoolean(1, scanEntity.isRescan());
                pstm.setInt(2, scanEntity.getFromHeight());
                pstm.setBoolean(3, scanEntity.isValidate());
                pstm.setInt(4, scanEntity.getCurrentScanHeight());
                pstm.setBoolean(5, scanEntity.isPreparationDone());
                pstm.setBoolean(6, scanEntity.isShutdown());
                pstm.executeUpdate();
            }
        });

    }

    int delete() {
        return TransactionHelper.executeInTransaction(databaseManager.getDataSource(), ()-> {
            try(Connection con = databaseManager.getDataSource().getConnection();
                PreparedStatement pstm = con.prepareStatement(DELETE_QUERY)
            ) {
               return pstm.executeUpdate();
            }
        });
    }

    @Override
    public ScanEntity get() {
        return TransactionHelper.executeInTransaction(databaseManager.getDataSource(), ()-> {
            try(Connection con = databaseManager.getDataSource().getConnection();
                PreparedStatement pstm = con.prepareStatement(SELECT_QUERY)
            ) {
                ScanEntity scanEntity = null;
                try (ResultSet rs = pstm.executeQuery()) {
                    if (rs.next()) {
                        scanEntity = map(rs);
                    }
                    if (rs.next()) {
                        throw new RuntimeException("Inconsistent state of scan table. More than 1 entry exist");
                    }
                }
                return scanEntity;
            }
        });
    }

    private ScanEntity map(ResultSet rs) throws SQLException {
        ScanEntity scanEntity = new ScanEntity(rs.getBoolean("validate"), rs.getInt("height"), rs.getBoolean("shutdown"));
        scanEntity.setRescan(rs.getBoolean("rescan"));
        scanEntity.setPreparationDone(rs.getBoolean("preparation_done"));
        scanEntity.setCurrentScanHeight(rs.getInt("current_height"));
        return scanEntity;
    }
}
