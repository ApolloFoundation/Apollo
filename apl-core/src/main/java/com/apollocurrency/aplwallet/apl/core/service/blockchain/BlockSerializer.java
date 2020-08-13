/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

@Singleton
public class BlockSerializer {

    private final Blockchain blockchain;

    @Inject
    public BlockSerializer(Blockchain blockchain) {
        this.blockchain = blockchain;
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
        this.blockchain.getOrLoadTransactions(block).forEach(transaction -> transactionsData.add(transaction.getJSONObject()));
        json.put("transactions", transactionsData);
        return json;
    }

}
