/*
 * Copyright (c)  2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransactionEntity;
import com.apollocurrency.aplwallet.apl.core.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.core.io.Result;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionJsonSerializer;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class UnconfirmedTransactionModelToEntityConverter implements Converter<UnconfirmedTransaction, UnconfirmedTransactionEntity> {
    private final BlockchainConfig blockchainConfig;
    private final TransactionJsonSerializer transactionJsonSerializer;
    private final TxBContext txBContext;

    @Inject
    public UnconfirmedTransactionModelToEntityConverter(BlockchainConfig blockchainConfig, TransactionJsonSerializer transactionJsonSerializer) {
        this.blockchainConfig = blockchainConfig;
        this.transactionJsonSerializer = transactionJsonSerializer;
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
    }

    @Override
    public UnconfirmedTransactionEntity apply(UnconfirmedTransaction model) {
        Objects.requireNonNull(model);
        UnconfirmedTransactionEntity.UnconfirmedTransactionEntityBuilder builder = UnconfirmedTransactionEntity.builder()
            .id(model.getId())
            .transactionHeight(model.getHeight())
            .arrivalTimestamp(model.getArrivalTimestamp())
            .feePerByte(model.getFeePerByte())
            .expiration(model.getExpiration());

        JSONObject prunableJSON = transactionJsonSerializer.getPrunableAttachmentJSON(model);
        if (prunableJSON != null) {
            builder.prunableAttachmentJsonString(prunableJSON.toJSONString());
        }
        Result signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(model.getVersion()).serialize(model, signedTxBytes);
        builder.transactionBytes(signedTxBytes.array());

        return builder.build();
    }

}
