/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.blockchain.BlockImpl;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class BlockParserImpl implements BlockParser {

    protected AccountService accountService;
    private final TransactionBuilderFactory transactionBuilderFactory;
    private final TransactionValidator transactionValidator;

    @Inject
    public BlockParserImpl(AccountService accountService, TransactionBuilderFactory transactionBuilderFactory, TransactionValidator transactionValidator) {
        this.accountService = accountService;
        this.transactionBuilderFactory = transactionBuilderFactory;
        this.transactionValidator = transactionValidator;
    }

    @Override
    public BlockImpl parseBlock(JSONObject blockData, long baseTarget) throws AplException.NotValidException, AplException.NotCurrentlyValidException {
        try {
            int version = ((Number) blockData.get("version")).intValue();
            int timestamp = ((Number) blockData.get("timestamp")).intValue();
            long previousBlock = Convert.parseUnsignedLong((String) blockData.get("previousBlock"));
            long totalAmountATM = blockData.containsKey("totalAmountATM") ? Convert.parseLong(blockData.get("totalAmountATM")) : Convert.parseLong(blockData.get("totalAmountNQT"));
            long totalFeeATM = blockData.containsKey("totalFeeATM") ? Convert.parseLong(blockData.get("totalFeeATM")) : Convert.parseLong(blockData.get("totalFeeNQT"));
            int payloadLength = ((Number) blockData.get("payloadLength")).intValue();
            byte[] payloadHash = Convert.parseHexString((String) blockData.get("payloadHash"));
            byte[] generatorPublicKey = Convert.parseHexString((String) blockData.get("generatorPublicKey"));
            byte[] generationSignature = Convert.parseHexString((String) blockData.get("generationSignature"));
            byte[] blockSignature = Convert.parseHexString((String) blockData.get("blockSignature"));
            byte[] previousBlockHash = version == 1 ? null : Convert.parseHexString((String) blockData.get("previousBlockHash"));
            Object timeoutJsonValue = blockData.get("timeout");

            //TODO https://firstb.atlassian.net/browse/APL-1634
            if (generatorPublicKey == null) {
                long generatorId = Long.parseUnsignedLong((String) blockData.get("generatorId"));
                generatorPublicKey = accountService.getPublicKeyByteArray(generatorId);
            }
            int timeout = !requireTimeout(version) ? 0 : ((Number) timeoutJsonValue).intValue();
            List<Transaction> blockTransactions = new ArrayList<>();
            for (Object transactionData : (JSONArray) blockData.get("transactions")) {
                blockTransactions.add(parseTransaction((JSONObject) transactionData));
            }
            BlockImpl block = new BlockImpl(version, timestamp, previousBlock, totalAmountATM, totalFeeATM,
                payloadLength, payloadHash, generatorPublicKey,
                generationSignature, blockSignature, previousBlockHash, timeout, blockTransactions, baseTarget);
            if (!block.checkSignature()) {
                throw new AplException.NotValidException("Invalid block signature");
            }
            return block;
        } catch (RuntimeException e) {
            log.debug("Failed to parse block: " + blockData.toJSONString(), e);
            throw e;
        }
    }

    private Transaction parseTransaction(JSONObject jsonObject) throws AplException.NotValidException, AplException.NotCurrentlyValidException {
        Transaction tx = transactionBuilderFactory.newTransaction(jsonObject);
        if (!transactionValidator.checkSignature(null, tx)) {
            throw new AplException.NotValidException("Invalid signature of tx: " + tx.getStringId());
        }
        return tx;
    }

    private boolean requireTimeout(int version) {
        return Block.ADAPTIVE_BLOCK_VERSION == version || Block.INSTANT_BLOCK_VERSION == version;
    }

}
