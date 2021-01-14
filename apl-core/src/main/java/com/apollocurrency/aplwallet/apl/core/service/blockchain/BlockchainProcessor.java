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

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public interface BlockchainProcessor {

    Peer getLastBlockchainFeeder();

    int getLastBlockchainFeederHeight();

    List<Transaction> getExpectedTransactions(Filter<Transaction> filter);

    boolean isTrimming();

    boolean isScanning();

    boolean isDownloading();

    boolean isProcessingBlock();

    int getMinRollbackHeight();

    int getInitialScanHeight();

    void processPeerBlock(JSONObject request) throws AplException;

    void fullReset();

    SortedSet<UnconfirmedTransaction> getUnconfirmedTransactions(Block previousBlock, int blockTimestamp, int limit);

    void generateBlock(byte[] keySeed, int blockTimestamp, int timeout, int blockVersion) throws BlockNotAcceptedException;

    SortedSet<UnconfirmedTransaction> selectUnconfirmedTransactions(
        Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates, Block previousBlock, int blockTimestamp, int limit);

    void scan(int height, boolean validate);

    void fullScanWithShutdown();

    void setGetMoreBlocks(boolean getMoreBlocks);

    List<Block> popOffTo(int height);

    List<Block> popOffToCommonBlock(Block commonBlock);

    void pushBlock(final Block block) throws BlockNotAcceptedException;

//    int restorePrunedData();

//    Transaction restorePrunedTransaction(long transactionId);

    void waitUntilBlockchainDownloadingStops();

    void suspendBlockchainDownloading();

    void resumeBlockchainDownloading();

    void shutdown();

    void scheduleOneScan();

    class BlockNotAcceptedException extends AplException {

        private final JSONObject block;

        public BlockNotAcceptedException(String message, JSONObject block) {
            super(message);
            this.block = block;
        }

        public BlockNotAcceptedException(Throwable cause, JSONObject block) {
            super(cause);
            this.block = block;
        }

        @Override
        public String getMessage() {
            return block == null ? super.getMessage() :
                super.getMessage() + ", block " + block.get("stringId") + " " + block.toJSONString();
        }

    }

    class TransactionNotAcceptedException extends BlockNotAcceptedException {

        private final Transaction transaction;

        public TransactionNotAcceptedException(String message, Transaction transaction, JSONObject jsonBlock) {
            super(message, jsonBlock);
            this.transaction = transaction;
        }

        public TransactionNotAcceptedException(Throwable cause, Transaction transaction, JSONObject jsonBlock) {
            super(cause, jsonBlock);
            this.transaction = transaction;
        }

        public Transaction getTransaction() {
            return transaction;
        }

        @Override
        public String getMessage() {
            return super.getMessage() + ", transaction " + transaction.getStringId();
        }
    }

    class BlockOutOfOrderException extends BlockNotAcceptedException {

        public BlockOutOfOrderException(String message, JSONObject jsonBlock) {
            super(message, jsonBlock);
        }

    }

}
