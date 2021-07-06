/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Prunable;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.util.io.JsonBuffer;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransactionJsonSerializerImpl implements TransactionJsonSerializer {
    private final PrunableLoadingService prunableService;
    private final TxBContext txBContext;

    @Inject
    public TransactionJsonSerializerImpl(PrunableLoadingService prunableService, BlockchainConfig blockchainConfig) {
        this.prunableService = prunableService;
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
    }

    @Override
    public byte[] serialize(Transaction transaction) {
        Result byteArrayTx = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion())
            .serialize(transaction, byteArrayTx);
        return byteArrayTx.array();
    }

    @Override
    public byte[] serializeUnsigned(Transaction transaction) {
        Result byteArrayTx = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion())
            .serialize(TransactionWrapperHelper.createUnsignedTransaction(transaction), byteArrayTx);
        return byteArrayTx.array();
    }

    @Override
    public JSONObject toJson(Transaction transaction) {
        //load not expired prunable attachments
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            prunableService.loadPrunable(transaction, appendage, false);
        }
        JsonBuffer buffer = new JsonBuffer();
        Result byteArrayTx = PayloadResult.createJsonResult(buffer);
        txBContext.createSerializer(transaction.getVersion()).serialize(transaction, byteArrayTx);
        return buffer.getJsonObject();
    }

    @Override
    public JSONObject getPrunableAttachmentJSON(Transaction transaction) {
        JSONObject prunableJSON = null;
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            prunableService.loadPrunable(transaction, appendage, false);
            if (appendage instanceof Prunable) {
                if (prunableJSON == null) {
                    prunableJSON = appendage.getJSONObject();
                } else {
                    prunableJSON.putAll(appendage.getJSONObject());
                }
            }
        }
        return prunableJSON;
    }
}
