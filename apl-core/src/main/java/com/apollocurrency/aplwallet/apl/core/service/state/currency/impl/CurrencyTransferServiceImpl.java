/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency.impl;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyTransferTable;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyTransferService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyTransfer;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class CurrencyTransferServiceImpl implements CurrencyTransferService {

    private final CurrencyTransferTable currencyTransferTable;
    private final BlockChainInfoService blockChainInfoService;
    private final IteratorToStreamConverter<CurrencyTransfer> iteratorToStreamConverter;

    @Inject
    public CurrencyTransferServiceImpl(CurrencyTransferTable currencyTransferTable,
                                       BlockChainInfoService blockChainInfoService) {
        this.currencyTransferTable = currencyTransferTable;
        this.blockChainInfoService = blockChainInfoService;
        this.iteratorToStreamConverter = new IteratorToStreamConverter<>();
    }

    /**
     * for unit test
     */
    public CurrencyTransferServiceImpl(CurrencyTransferTable currencyTransferTable,
                                       BlockChainInfoService blockChainInfoService,
                                       IteratorToStreamConverter<CurrencyTransfer> iteratorToStreamConverter) {
        this.currencyTransferTable = currencyTransferTable;
        this.blockChainInfoService = blockChainInfoService;
        this.iteratorToStreamConverter = iteratorToStreamConverter != null ? iteratorToStreamConverter : new IteratorToStreamConverter<>();
    }

    @Override
    public DbIterator<CurrencyTransfer> getAllTransfers(int from, int to) {
        return currencyTransferTable.getAll(from, to);
    }

    @Override
    public Stream<CurrencyTransfer> getAllTransfersStream(int from, int to) {
        return iteratorToStreamConverter.apply(currencyTransferTable.getAll(from, to));
    }

    @Override
    public int getCount() {
        return currencyTransferTable.getCount();
    }

    @Override
    public DbIterator<CurrencyTransfer> getCurrencyTransfers(long currencyId, int from, int to) {
        return currencyTransferTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to);
    }

    @Override
    public Stream<CurrencyTransfer> getCurrencyTransfersStream(long currencyId, int from, int to) {
        return iteratorToStreamConverter.apply(
            currencyTransferTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to)
        );
    }

    @Override
    public DbIterator<CurrencyTransfer> getAccountCurrencyTransfers(long accountId, int from, int to) {
        return currencyTransferTable.getAccountCurrencyTransfers(accountId, from, to);
    }

    @Override
    public Stream<CurrencyTransfer> getAccountCurrencyTransfersStream(long accountId, int from, int to) {
        return iteratorToStreamConverter.apply(
            currencyTransferTable.getAccountCurrencyTransfers(accountId, from, to)
        );
    }

    @Override
    public DbIterator<CurrencyTransfer> getAccountCurrencyTransfers(long accountId, long currencyId, int from, int to) {
        return currencyTransferTable.getAccountCurrencyTransfers(accountId, currencyId, from, to);
    }

    @Override
    public Stream<CurrencyTransfer> getAccountCurrencyTransfersStream(long accountId, long currencyId, int from, int to) {
        return iteratorToStreamConverter.apply(
            currencyTransferTable.getAccountCurrencyTransfers(accountId, currencyId, from, to)
        );
    }

    @Override
    public int getTransferCount(long currencyId) {
        return currencyTransferTable.getCount(new DbClause.LongClause("currency_id", currencyId));
    }

    @Override
    public CurrencyTransfer addTransfer(Transaction transaction, MonetarySystemCurrencyTransfer attachment) {
        Block lastBlock = blockChainInfoService.getLastBlock();
        CurrencyTransfer transfer = new CurrencyTransfer(transaction, attachment, lastBlock.getTimestamp(), lastBlock.getHeight());
        currencyTransferTable.insert(transfer);
        return transfer;
    }

}
