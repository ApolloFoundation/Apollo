/*
 * Copyright (c)  2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.MandatoryTransaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.MandatoryTransactionEntity;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class MandatoryTransactionModelToEntityConverter implements Converter<MandatoryTransaction, MandatoryTransactionEntity> {
    private final BlockchainConfig blockchainConfig;
    private final TxBContext txBContext;

    @Inject
    public MandatoryTransactionModelToEntityConverter(BlockchainConfig blockchainConfig) {
        this.blockchainConfig = blockchainConfig;
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
    }

    @Override
    public MandatoryTransactionEntity apply(MandatoryTransaction model) {
        Objects.requireNonNull(model);
        Result signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(model.getVersion()).serialize(model.getTransactionImpl(), signedTxBytes);
        return new MandatoryTransactionEntity(model.getId(), signedTxBytes.array(), model.getRequiredTxHash());
    }
}
