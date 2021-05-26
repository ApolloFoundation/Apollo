/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionJsonSerializer;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.NonNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BlockSerializer {

    private final Blockchain blockchain;
    private final TransactionJsonSerializer transactionJsonSerializer;

    @Inject
    public BlockSerializer(@NonNull Blockchain blockchain, @NonNull TransactionJsonSerializer transactionJsonSerializer) {
        this.blockchain = blockchain;
        this.transactionJsonSerializer = transactionJsonSerializer;
    }

    public JSONObject getJSONObject(Block block) {
        JSONObject json = new JSONObject();
        json.put("version", block.getVersion());
        json.put("stringId", block.getStringId());
        json.put("timestamp", block.getTimestamp());
        json.put("previousBlock", Long.toUnsignedString(block.getPreviousBlockId()));
        json.put("totalAmountATM", block.getTotalAmountATM());
        json.put("totalFeeATM", block.getTotalFeeATM());
        json.put("payloadLength", block.getPayloadLength());
        json.put("payloadHash", Convert.toHexString(block.getPayloadHash()));
        json.put("generatorId", Long.toUnsignedString(block.getGeneratorId()));
        json.put("generatorPublicKey", Convert.toHexString(block.getGeneratorPublicKey()));
        json.put("generationSignature", Convert.toHexString(block.getGenerationSignature()));
        json.put("previousBlockHash", Convert.toHexString(block.getPreviousBlockHash()));
        json.put("blockSignature", Convert.toHexString(block.getBlockSignature()));
        json.put("timeout", block.getTimeout());

        JSONArray transactionsData = new JSONArray();
        this.blockchain.loadBlockData(block).getTransactions()
                .forEach(transaction -> transactionsData.add(transactionJsonSerializer.toJson(transaction)));

        json.put("transactions", transactionsData);
        return json;
    }

}
