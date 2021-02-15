/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.asset.impl;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.asset.AssetTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetDeleteService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetIssuance;
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
import java.util.Objects;
import java.util.stream.Stream;

import static com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig.DEFAULT_SCHEMA;

@DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
@Slf4j
@Singleton
public class AssetServiceImpl implements AssetService {

    private final AssetTable assetTable;
    private final BlockChainInfoService blockChainInfoService;
    private final AssetDeleteService assetDeleteService;
    private IteratorToStreamConverter<Asset> assetIteratorToStreamConverter;
    private final FullTextSearchUpdater fullTextSearchUpdater;
    private final FullTextSearchService fullTextSearchService;

    @Inject
    public AssetServiceImpl(AssetTable assetTable,
                            BlockChainInfoService blockChainInfoService,
                            AssetDeleteService assetDeleteService,
                            FullTextSearchUpdater fullTextSearchUpdater,
                            FullTextSearchService fullTextSearchService
    ) {
        this.assetTable = assetTable;
        this.blockChainInfoService = blockChainInfoService;
        this.assetDeleteService = assetDeleteService;
        this.assetIteratorToStreamConverter = new IteratorToStreamConverter<>();
        this.fullTextSearchUpdater = fullTextSearchUpdater;
        this.fullTextSearchService = fullTextSearchService;
    }

    /**
     * Constructor for unit tests
     */
    public AssetServiceImpl(AssetTable assetTable,
                            BlockChainInfoService blockChainInfoService,
                            AssetDeleteService assetDeleteService,
                            IteratorToStreamConverter<Asset> assetIteratorToStreamConverter, // for unit tests mostly
                            FullTextSearchUpdater fullTextSearchUpdater,
                            FullTextSearchService fullTextSearchService
    ) {
        this.assetTable = assetTable;
        this.blockChainInfoService = blockChainInfoService;
        this.assetDeleteService = assetDeleteService;
        if (assetIteratorToStreamConverter != null) { // for unit tests
            this.assetIteratorToStreamConverter = assetIteratorToStreamConverter;
        } else {
            this.assetIteratorToStreamConverter = new IteratorToStreamConverter<>();
        }
        this.fullTextSearchUpdater = fullTextSearchUpdater;
        this.fullTextSearchService = fullTextSearchService;
    }

    @Override
    public DbIterator<Asset> getAllAssets(int from, int to) {
        return assetTable.getAll(from, to);
    }

    @Override
    public Stream<Asset> getAllAssetsStream(int from, int to) {
        return assetIteratorToStreamConverter.apply(assetTable.getAll(from, to));
    }

    @Override
    public int getCount() {
        return assetTable.getCount();
    }

    @Override
    public Asset getAsset(long id) {
        return assetTable.get(AssetTable.assetDbKeyFactory.newKey(id));
    }

    @Override
    public Asset getAsset(long id, int height) {
        final DbKey dbKey = AssetTable.assetDbKeyFactory.newKey(id);
        if (height < 0 || blockChainInfoService.doesNotExceed(height)) {
            return assetTable.get(dbKey);
        }
        blockChainInfoService.checkAvailable(height);
        return assetTable.get(dbKey, height);
    }

    @Override
    public DbIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to) {
        return assetTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    @Override
    public Stream<Asset> getAssetsIssuedByStream(long accountId, int from, int to) {
        return assetIteratorToStreamConverter.apply(
            assetTable.getManyBy(
                new DbClause.LongClause("account_id", accountId), from, to));
    }

    @Override
    public DbIterator<Asset> searchAssets(String query, int from, int to) {
//        return assetTable.search(query, DbClause.EMPTY_CLAUSE, from, to, " ORDER BY ft.score DESC ");
        StringBuffer inRangeClause = createDbIdInRangeFromLuceneData(query);
        if (inRangeClause.length() == 2) {
            // no DB_ID were fetched from Lucene index, return empty db iterator
            return DbIterator.EmptyDbIterator();
        }
        DbClause dbClause = DbClause.EMPTY_CLAUSE;
        String sort = " ";
        return fetchAssetByParams(from, to, inRangeClause, dbClause, sort);
    }

    @Override
    public Stream<Asset> searchAssetsStream(String query, int from, int to) {
//        return assetIteratorToStreamConverter.apply(
//            assetTable.search(query, DbClause.EMPTY_CLAUSE, from, to, " ORDER BY ft.score DESC "));
        StringBuffer inRangeClause = createDbIdInRangeFromLuceneData(query);
        if (inRangeClause.length() == 2) {
            // no DB_ID were fetched from Lucene index, return empty db iterator
            return Stream.of();
        }
        DbClause dbClause = DbClause.EMPTY_CLAUSE;
        String sort = " ";
        return assetIteratorToStreamConverter.apply(fetchAssetByParams(from, to, inRangeClause, dbClause, sort));
    }

    @Override
    public void addAsset(Transaction transaction, ColoredCoinsAssetIssuance attachment) {
        Asset asset = new Asset(transaction, attachment, blockChainInfoService.getHeight());
        assetTable.insert(asset);
        createAndFireFullTextSearchDataEvent(asset, FullTextOperationData.OperationType.INSERT_UPDATE);
    }

    @Override
    public void deleteAsset(Transaction transaction, long assetId, long quantityATU) {
        Asset asset = getAsset(assetId);
        asset.setQuantityATU(Math.max(0, asset.getQuantityATU() - quantityATU));
        asset.setHeight(blockChainInfoService.getHeight());
        assetTable.insert(asset);
        assetDeleteService.addAssetDelete(transaction, assetId, quantityATU);
        createAndFireFullTextSearchDataEvent(asset, FullTextOperationData.OperationType.INSERT_UPDATE);
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
        try {
            ResultSet rs = fullTextSearchService.search("public", assetTable.getTableName(), luceneQuery, Integer.MAX_VALUE, 0);
            while (rs.next()) {
                Long DB_ID = rs.getLong(5);
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

    public DbIterator<Asset> fetchAssetByParams(int from, int to,
                                                StringBuffer inRangeClause,
                                                DbClause dbClause,
                                                String sort) {
        Objects.requireNonNull(inRangeClause, "inRangeClause is NULL");
        Objects.requireNonNull(dbClause, "dbClause is NULL");
        Objects.requireNonNull(sort, "sort is NULL");

        Connection con = null;
        TransactionalDataSource dataSource = assetTable.getDatabaseManager().getDataSource();
        final boolean doCache = dataSource.isInTransaction();
        try {
            con = dataSource.getConnection();
            @DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
            PreparedStatement pstmt = con.prepareStatement(
                // select and load full entities from mariadb using prefetched DB_ID list from lucene
                "SELECT " + assetTable.getTableName() + ".* FROM " + assetTable.getTableName()
                    + " WHERE " + assetTable.getTableName() + ".db_id in " + inRangeClause.toString()
                    + (assetTable.isMultiversion() ? " AND " + assetTable.getTableName() + ".latest = TRUE " : " ")
                    + " AND " + dbClause.getClause() + sort
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            i = dbClause.set(pstmt, ++i);
            DbUtils.setLimits(i, pstmt, from, to);
            return new DbIterator<>(con, pstmt, (connection, rs) -> {
                DbKey dbKey = null;
                if (doCache) {
                    dbKey = assetTable.getDbKeyFactory().newKey(rs);
                }
                return assetTable.load(connection, rs, dbKey);
            });
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }


    private void createAndFireFullTextSearchDataEvent(Asset asset, FullTextOperationData.OperationType operationType) {
        FullTextOperationData operationData = new FullTextOperationData(
            DEFAULT_SCHEMA, assetTable.getTableName(), Thread.currentThread().getName());
        operationData.setOperationType(operationType);
        operationData.setDbIdValue(BigInteger.valueOf(asset.getDbId()));
        operationData.addColumnData(asset.getName()).addColumnData(asset.getDescription());
        // send data into Lucene index component
        log.trace("Put lucene index update data = {}", operationData);
        fullTextSearchUpdater.putFullTextOperationData(operationData);
    }

}
