/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.core.io.Result;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UnconfirmedTransactionCreator {
    private final BlockchainConfig blockchainConfig;
    private final TimeService timeService;
    private final TxBContext txBContext;

    @Inject
    public UnconfirmedTransactionCreator(BlockchainConfig blockchainConfig, TimeService timeService) {
        this.blockchainConfig = blockchainConfig;
        this.timeService = timeService;
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
    }

    public UnconfirmedTransaction from(Transaction transaction) {
        return from(transaction, timeService.getEpochTime());
    }

    public UnconfirmedTransaction from(Transaction transaction, long arrivalTimestamp) {
        Result byteArrayTx = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion()).serialize(transaction, byteArrayTx);
        int fullSize = byteArrayTx.payloadSize();

        return new UnconfirmedTransaction(transaction, arrivalTimestamp, transaction.getFeeATM() / fullSize, fullSize);
    }
}
