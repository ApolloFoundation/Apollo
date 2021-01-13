/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.order.impl;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.order.BidOrderTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.BidOrder;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.service.state.qualifier.BidOrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderPlacement;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Stream;

@Slf4j
@Singleton
@BidOrderService
public class BidOrderServiceImpl implements OrderService<BidOrder, ColoredCoinsBidOrderPlacement> {
    static final String ORDER = " ORDER BY price DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ";
    private final DatabaseManager databaseManager;
    private final BidOrderTable bidOrderTable;
    private final IteratorToStreamConverter<BidOrder> converter;
    private final Blockchain blockchain;

    /**
     * Constructor for unit tests
     *
     * @param databaseManager
     * @param bidOrderTable
     * @param blockchain
     * @param converter
     */
    public BidOrderServiceImpl(
        final DatabaseManager databaseManager,
        final BidOrderTable bidOrderTable,
        final Blockchain blockchain,
        final IteratorToStreamConverter<BidOrder> converter
    ) {
        this.databaseManager = databaseManager;
        this.bidOrderTable = bidOrderTable;
        this.blockchain = blockchain;
        this.converter = converter;
    }

    @Inject
    public BidOrderServiceImpl(
        final DatabaseManager databaseManager,
        final BidOrderTable bidOrderTable,
        final Blockchain blockchain
    ) {
        this.databaseManager = databaseManager;
        this.bidOrderTable = bidOrderTable;
        this.blockchain = blockchain;
        this.converter = new IteratorToStreamConverter<>();
    }

    @Override
    public int getCount() {
        return bidOrderTable.getCount();
    }

    @Override
    public BidOrder getOrder(long orderId) {
        return bidOrderTable.getBidOrder(orderId);
    }

    @Override
    public Stream<BidOrder> getAll(int from, int to) {
        return converter.convert(bidOrderTable.getAll(from, to));
    }

    @Override
    public Stream<BidOrder> getOrdersByAccount(long accountId, int from, int to) {
        return converter.convert(
            bidOrderTable.getManyBy(
                new DbClause.LongClause("account_id", accountId), from, to
            )
        );
    }

    @Override
    public Stream<BidOrder> getOrdersByAccountAsset(final long accountId, final long assetId, int from, int to) {
        return converter.convert(
            bidOrderTable.getManyBy(
                new DbClause.LongClause("account_id", accountId)
                    .and(new DbClause.LongClause("asset_id", assetId)), from, to
            )
        );
    }

    @Override
    public Stream<BidOrder> getSortedOrders(long assetId, int from, int to) {
        return converter.convert(
            bidOrderTable.getManyBy(new DbClause.LongClause("asset_id", assetId),
                from,
                to,
                ORDER
            )
        );
    }

    @Override
    public BidOrder getNextOrder(long assetId) {
        final TransactionalDataSource dataSource = databaseManager.getDataSource();
        return bidOrderTable.getNextOrder(dataSource, assetId);
    }

    @Override
    public void addOrder(Transaction transaction, ColoredCoinsBidOrderPlacement attachment) {
        final BidOrder order = new BidOrder(transaction, attachment, blockchain.getHeight());
        log.trace(">> addOrder() bidOrder={}", order);
        bidOrderTable.insert(order);
    }

    @Override
    public void removeOrder(long orderId) {
        int height = blockchain.getHeight();
        BidOrder order = getOrder(orderId);
        // IMPORTANT! update new height in order for correct saving duplicated record and correct trim
        order.setHeight(height); // do not remove
        boolean result = bidOrderTable.deleteAtHeight(order, height);
        log.trace("<< removeOrder() result={}, bidOrderId={}, height={}", result, orderId, height);
    }

    @Override
    public void updateQuantityATU(long quantityATU, BidOrder orderBid) {
        orderBid.setQuantityATU(quantityATU);
        int height = blockchain.getHeight();
        orderBid.setHeight(height);
        insertOrDeleteOrder(bidOrderTable, quantityATU, orderBid, height);
    }
}
