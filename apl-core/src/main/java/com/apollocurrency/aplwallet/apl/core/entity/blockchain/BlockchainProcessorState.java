/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.blockchain;

import com.apollocurrency.aplwallet.apl.core.peer.Peer;

public class BlockchainProcessorState {
    private volatile Peer lastBlockchainFeeder;
    private volatile int lastBlockchainFeederHeight;
    private volatile boolean getMoreBlocks = true;
    private volatile boolean isScanning;
    private volatile boolean isDownloading;
    private volatile boolean isProcessingBlock;
    private volatile boolean isRestoring;
    private volatile int lastRestoreTime = 0;
    private volatile int initialScanHeight;

    public synchronized Peer getLastBlockchainFeeder() {
        return lastBlockchainFeeder;
    }

    public synchronized void setLastBlockchainFeeder(Peer lastBlockchainFeeder) {
        this.lastBlockchainFeeder = lastBlockchainFeeder;
    }

    public synchronized int getLastBlockchainFeederHeight() {
        return lastBlockchainFeederHeight;
    }

    public synchronized void setLastBlockchainFeederHeight(int lastBlockchainFeederHeight) {
        this.lastBlockchainFeederHeight = lastBlockchainFeederHeight;
    }

    public synchronized boolean isGetMoreBlocks() {
        return getMoreBlocks;
    }

    public synchronized void setGetMoreBlocks(boolean getMoreBlocks) {
        this.getMoreBlocks = getMoreBlocks;
    }

    public synchronized boolean isScanning() {
        return isScanning;
    }

    public synchronized void setScanning(boolean scanning) {
        isScanning = scanning;
    }

    public synchronized boolean isDownloading() {
        return isDownloading;
    }

    public synchronized void setDownloading(boolean downloading) {
        isDownloading = downloading;
    }

    public synchronized boolean isProcessingBlock() {
        return isProcessingBlock;
    }

    public synchronized void setProcessingBlock(boolean processingBlock) {
        isProcessingBlock = processingBlock;
    }

    public synchronized boolean isRestoring() {
        return isRestoring;
    }

    public synchronized void setRestoring(boolean restoring) {
        isRestoring = restoring;
    }

    public synchronized int getLastRestoreTime() {
        return lastRestoreTime;
    }

    public synchronized void setLastRestoreTime(int lastRestoreTime) {
        this.lastRestoreTime = lastRestoreTime;
    }

    public synchronized int getInitialScanHeight() {
        return initialScanHeight;
    }

    public synchronized void setInitialScanHeight(int initialScanHeight) {
        this.initialScanHeight = initialScanHeight;
    }
}
