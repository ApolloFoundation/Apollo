/*
 * Copyright (c)  2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransactionEntity;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionJsonSerializer;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
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
    private final TransactionBuilderFactory factory;

    @Inject
    public UnconfirmedTransactionModelToEntityConverter(TransactionBuilderFactory factory, BlockchainConfig blockchainConfig, TransactionJsonSerializer transactionJsonSerializer) {
        this.blockchainConfig = blockchainConfig;
        this.transactionJsonSerializer = transactionJsonSerializer;
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
        this.factory = factory;
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
        txBContext.createSerializer(model.getVersion()).serialize(model.getTransactionImpl(), signedTxBytes);
        builder.transactionBytes(signedTxBytes.array());
        if (model.getTransactionImpl().getId() != model.getId()) {
            throw new RuntimeException("Not valid id set, expected " + model.getTransactionImpl().getId() + ", got  " + model.getId());
        }
        Transaction transaction;
        try {
            transaction = factory.newTransaction(signedTxBytes.array());
        } catch (AplException.NotValidException e) {
            throw new RuntimeException(e);
        }

        if (model.getId() != transaction.getId()) {
            PayloadResult bytes = PayloadResult.createLittleEndianByteArrayResult();
            txBContext.createSerializer(transaction.getVersion())
                .serialize(
                    transaction
                    , bytes
                );
            // incorrect deserialization case
            throw new RuntimeException("Transaction " + builder.toString() + ", bytes - " + Convert.toHexString(bytes.array()) +" has different id " + transaction.getId() + ", id from unconfirmed instance " + model.getId() + ",  unconfirmed bytes " + Convert.toHexString(signedTxBytes.array()));
        }
        return builder.build();
    }

}
