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
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.app.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.util.Observable;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public interface BlockchainProcessor extends Observable<Block,BlockchainProcessor.Event> {

    enum Event {
        BLOCK_PUSHED, BLOCK_POPPED, BLOCK_GENERATED, BLOCK_SCANNED,
        RESCAN_BEGIN, RESCAN_END,
        BEFORE_BLOCK_ACCEPT, AFTER_BLOCK_ACCEPT,
        BEFORE_BLOCK_APPLY, AFTER_BLOCK_APPLY
    }

    List<DerivedDbTable> getDerivedTables();

    Peer getLastBlockchainFeeder();

    int getLastBlockchainFeederHeight();

    boolean isScanning();

    boolean isDownloading();

    boolean isProcessingBlock();

    int getMinRollbackHeight();

    int getInitialScanHeight();

    void processPeerBlock(JSONObject request) throws AplException;

    void fullReset();

    SortedSet<UnconfirmedTransaction> getUnconfirmedTransactions(Block previousBlock, int blockTimestamp);

    void generateBlock(byte[] keySeed, int blockTimestamp, int timeout, int blockVersion) throws BlockNotAcceptedException;

    SortedSet<UnconfirmedTransaction> selectUnconfirmedTransactions(
            Map<TransactionType, Map<String, Integer>> duplicates, Block previousBlock, int blockTimestamp);

    void scan(int height, boolean validate);

    void fullScanWithShutdown();

    void setGetMoreBlocks(boolean getMoreBlocks);

    List<Block> popOffTo(int height);

    List<Block> popOffTo(Block commonBlock);

    void registerDerivedTable(DerivedDbTable table);

    void trimDerivedTables();

    int restorePrunedData();

    Transaction restorePrunedTransaction(long transactionId);

    long getGenesisBlockId();

    class BlockNotAcceptedException extends AplException {

        private final Block block;

        BlockNotAcceptedException(String message, Block block) {
            super(message);
            this.block = block;
        }

        BlockNotAcceptedException(Throwable cause, Block block) {
            super(cause);
            this.block = block;
        }

        @Override
        public String getMessage() {
            return block == null ? super.getMessage() : super.getMessage() + ", block " + block.getStringId() + " " + block.getJSONObject().toJSONString();
        }

    }

    class TransactionNotAcceptedException extends BlockNotAcceptedException {

        private final Transaction transaction;

        TransactionNotAcceptedException(String message, Transaction transaction) {
            super(message, transaction.getBlock());
            this.transaction = transaction;
        }

        TransactionNotAcceptedException(Throwable cause, Transaction transaction) {
            super(cause, transaction.getBlock());
            this.transaction = transaction;
        }

        public Transaction getTransaction() {
            return transaction;
        }

        @Override
        public String getMessage() {
            return super.getMessage() + ", transaction " + transaction.getStringId() + " " + transaction.getJSONObject().toJSONString();
        }
    }

    class BlockOutOfOrderException extends BlockNotAcceptedException {

        BlockOutOfOrderException(String message, Block block) {
            super(message, block);
        }

	}

	class InvalidTransactionException extends BlockNotAcceptedException {

        InvalidTransactionException(String message, Block block) {
            super(message, block);
        }

        InvalidTransactionException(Throwable cause, Block block) {
            super(cause, block);
        }

        public InvalidTransactionException(String message) {
            super(message, null);
        }
    }

    void suspendBlockchainDownloading();

    void resumeBlockchainDownloading();

    void shutdown();
}
