/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account.impl;

import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountInfoTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountInfo;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountInfoService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import static com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig.DEFAULT_SCHEMA;
import static com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil.toList;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class AccountInfoServiceImpl implements AccountInfoService {

    private final Blockchain blockchain;
    private final AccountInfoTable accountInfoTable;
    private final FullTextSearchUpdater fullTextSearchUpdater;
    private final FullTextSearchService fullTextSearchService;

    @Inject
    public AccountInfoServiceImpl(Blockchain blockchain,
                                  AccountInfoTable accountInfoTable,
                                  FullTextSearchUpdater fullTextSearchUpdater,
                                  FullTextSearchService fullTextSearchService) {
        this.blockchain = blockchain;
        this.accountInfoTable = accountInfoTable;
        this.fullTextSearchUpdater = fullTextSearchUpdater;
        this.fullTextSearchService = fullTextSearchService;
    }

    @Override
    public void update(AccountInfo accountInfo) {
        Objects.requireNonNull(accountInfo);
        // prepare Event instance with data
        FullTextOperationData operationData = new FullTextOperationData(
            DEFAULT_SCHEMA, accountInfoTable.getTableName(), Thread.currentThread().getName());

        if (accountInfo.getName() != null || accountInfo.getDescription() != null) {
            log.debug("1. {}", accountInfo);
            accountInfoTable.insert(accountInfo);
            // put relevant data into Event instance
            operationData.setOperationType(FullTextOperationData.OperationType.INSERT_UPDATE);
            operationData.setDbIdValue(accountInfo.getDbId());
            operationData.addColumnData(accountInfo.getName()).addColumnData(accountInfo.getDescription());
            log.debug("2. {}", accountInfo);
        } else {
            accountInfoTable.deleteAtHeight(accountInfo, blockchain.getHeight());
            operationData.setDbIdValue(accountInfo.getDbId());
            operationData.setOperationType(FullTextOperationData.OperationType.DELETE);
        }
        // send data into Lucene index component
        log.trace("Put lucene index update data = {}", operationData);
        fullTextSearchUpdater.putFullTextOperationData(operationData);
    }

    @Override
    public AccountInfo getAccountInfo(Account account) {
        return accountInfoTable.get(AccountTableInterface.newKey(account));
    }

    @Override
    public void updateAccountInfo(Account account, String name, String description) {
        name = Convert.emptyToNull(name.trim());
        description = Convert.emptyToNull(description.trim());
        AccountInfo accountInfo = getAccountInfo(account);
        if (accountInfo == null) {
            accountInfo = new AccountInfo(account.getId(), name, description, blockchain.getHeight());
        } else {
            accountInfo.setName(name);
            accountInfo.setDescription(description);
            accountInfo.setHeight(blockchain.getHeight());
        }
        update(accountInfo);
    }

    @Override
    public List<AccountInfo> searchAccounts(String query, int from, int to) {
//        return toList(accountInfoTable.searchAccounts(query, from, to));
        StringBuffer inRangeClause = createDbIdInRangeFromLuceneData(query);
        if (inRangeClause.length() == 2) {
            // no DB_ID were fetched from Lucene index, return empty db iterator
            return List.of();
        }
        DbClause dbClause = DbClause.EMPTY_CLAUSE;
        String sort = " ";
        return toList(fetchAccountInfoByParams(from, to, inRangeClause, dbClause, sort));

    }

    /**
     * compose db_id list for in (id,..id) SQL luceneQuery
     *
     * @param luceneQuery lucene language luceneQuery pattern
     * @return composed sql luceneQuery part
     */
    private StringBuffer createDbIdInRangeFromLuceneData(String luceneQuery) {
        Objects.requireNonNull(luceneQuery, "luceneQuery is empty");
        StringBuffer inRange = new StringBuffer("(");
        int index = 0;
        try (ResultSet rs = fullTextSearchService.search("public", accountInfoTable.getTableName(), luceneQuery, Integer.MAX_VALUE, 0)) {
            while (rs.next()) {
                Long DB_ID = rs.getLong(4);
                if (index == 0) {
                    inRange.append(DB_ID);
                } else {
                    inRange.append(",").append(DB_ID);
                }
                index++;
            }
            inRange.append(")");
            log.debug("{}", inRange.toString());
        } catch (SQLException e) {
            log.error("FTS failed", e);
            throw new RuntimeException(e);
        }
        return inRange;
    }

    public DbIterator<AccountInfo> fetchAccountInfoByParams(int from, int to,
                                                            StringBuffer inRangeClause,
                                                            DbClause dbClause,
                                                            String sort) {
        Objects.requireNonNull(inRangeClause, "inRangeClause is NULL");
        Objects.requireNonNull(dbClause, "dbClause is NULL");
        Objects.requireNonNull(sort, "sort is NULL");

        Connection con = null;
        TransactionalDataSource dataSource = accountInfoTable.getDatabaseManager().getDataSource();
        final boolean doCache = dataSource.isInTransaction();
        try {
            con = dataSource.getConnection();
            @DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
            PreparedStatement pstmt = con.prepareStatement(
                // select and load full entities from mariadb using prefetched DB_ID list from lucene
                "SELECT " + accountInfoTable.getTableName() + ".* FROM " + accountInfoTable.getTableName()
                    + " WHERE " + accountInfoTable.getTableName() + ".db_id in " + inRangeClause.toString()
                    + (accountInfoTable.isMultiversion() ? " AND " + accountInfoTable.getTableName() + ".latest = TRUE " : " ")
                    + " AND " + dbClause.getClause() + sort
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            i = dbClause.set(pstmt, ++i);
            DbUtils.setLimits(i, pstmt, from, to);
            return new DbIterator<>(con, pstmt, (connection, rs) -> {
                DbKey dbKey = null;
                if (doCache) {
                    dbKey = accountInfoTable.getDbKeyFactory().newKey(rs);
                }
                return accountInfoTable.load(connection, rs, dbKey);
            });
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
}
