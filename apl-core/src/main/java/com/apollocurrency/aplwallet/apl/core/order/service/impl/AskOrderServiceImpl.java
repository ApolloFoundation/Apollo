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
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.order.service.impl;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.converter.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.order.dao.AskOrderTable;
import com.apollocurrency.aplwallet.apl.core.order.entity.AskOrder;
import com.apollocurrency.aplwallet.apl.core.order.service.OrderService;
import com.apollocurrency.aplwallet.apl.core.order.service.qualifier.AskOrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAskOrderPlacement;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Stream;

@Slf4j
@Singleton
@AskOrderService
public class AskOrderServiceImpl implements OrderService<AskOrder, ColoredCoinsAskOrderPlacement> {
    static final String ORDER = " ORDER BY price ASC, creation_height ASC, transaction_height ASC, transaction_index ASC ";
    private final DatabaseManager databaseManager;
    private final AskOrderTable askOrderTable;
    private final IteratorToStreamConverter<AskOrder> converter;
    private final Blockchain blockchain;

    /**
     * Constructor for unit tests
     *
     * @param databaseManager
     * @param orderAskTable
     * @param blockchain
     * @param converter
     */
    public AskOrderServiceImpl(
        final DatabaseManager databaseManager,
        final AskOrderTable orderAskTable,
        final Blockchain blockchain,
        final IteratorToStreamConverter<AskOrder> converter
    ) {
        this.databaseManager = databaseManager;
        this.askOrderTable = orderAskTable;
        this.blockchain = blockchain;
        this.converter = converter;
    }

    @Inject
    public AskOrderServiceImpl(
        final DatabaseManager databaseManager,
        final AskOrderTable orderAskTable,
        final Blockchain blockchain
    ) {
        this.databaseManager = databaseManager;
        this.askOrderTable = orderAskTable;
        this.blockchain = blockchain;
        this.converter = new IteratorToStreamConverter<>();
    }

    @Override
    public int getCount() {
        return askOrderTable.getCount();
    }

    @Override
    public AskOrder getOrder(long orderId) {
        return askOrderTable.getAskOrder(orderId);
    }

    @Override
    public Stream<AskOrder> getAll(int from, int to) {
        return converter.convert(askOrderTable.getAll(from, to));
    }

    @Override
    public Stream<AskOrder> getOrdersByAccount(long accountId, int from, int to) {
        return converter.convert(
            askOrderTable.getManyBy(
                new DbClause.LongClause("account_id", accountId), from, to
            )
        );
    }

    @Override
    public Stream<AskOrder> getOrdersByAccountAsset(final long accountId, final long assetId, int from, int to) {
        return converter.convert(
            askOrderTable.getManyBy(
                new DbClause.LongClause("account_id", accountId)
                    .and(new DbClause.LongClause("asset_id", assetId)), from, to
            )
        );
    }

    @Override
    public Stream<AskOrder> getSortedOrders(long assetId, int from, int to) {
        return converter.convert(
            askOrderTable.getManyBy(
                new DbClause.LongClause("asset_id", assetId),
                from,
                to,
                ORDER
            )
        );
    }

    @Override
    public AskOrder getNextOrder(long assetId) {
        final TransactionalDataSource dataSource = databaseManager.getDataSource();
        return askOrderTable.getNextOrder(dataSource, assetId);
    }

    @Override
    public void addOrder(Transaction transaction, ColoredCoinsAskOrderPlacement attachment) {
        final AskOrder order = new AskOrder(transaction, attachment, blockchain.getHeight());
        log.trace(">> addOrder() askOrder={}", order);
        askOrderTable.insert(order);
    }

    @Override
    public void removeOrder(long orderId) {
        int height = blockchain.getHeight();
        boolean result = askOrderTable.deleteAtHeight(getOrder(orderId), height);
        log.trace(">> removeOrder() result={}, askOrderId={}, height={}", result, orderId, height);
    }

    @Override
    public void updateQuantityATU(long quantityATU, AskOrder orderAsk) {
        orderAsk.setQuantityATU(quantityATU);
        int height = blockchain.getHeight();
        if (quantityATU > 0) {
            log.trace("Update POSITIVE quantity = {}, height={}", orderAsk, height);
        } else if (quantityATU == 0) {
            log.trace("Delete ZERO quantity = {}, height={}", orderAsk, height);
        }
        insertOrDeleteOrder(askOrderTable, quantityATU, orderAsk, height);
    }
}
