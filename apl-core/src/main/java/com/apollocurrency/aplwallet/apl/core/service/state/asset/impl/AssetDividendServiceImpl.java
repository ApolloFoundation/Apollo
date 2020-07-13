/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.asset.impl;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.dao.state.asset.AssetDividendTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetDividend;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AssetEventType;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetDividendService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsDividendPayment;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.app.observer.events.AssetEventBinding.literal;

@Singleton
public class AssetDividendServiceImpl implements AssetDividendService {
    private final Blockchain blockchain;
    private final Event<AssetDividend> assetEvent;
    private final AssetDividendTable assetDividendTable;

    @Inject
    public AssetDividendServiceImpl(Blockchain blockchain, AssetDividendTable assetDividendTable, Event<AssetDividend> assetEvent) {
        this.blockchain = blockchain;
        this.assetDividendTable = assetDividendTable;
        this.assetEvent = assetEvent;
    }

    @Override
    public List<AssetDividend> getAssetDividends(long assetId, int from, int to) {
        return assetDividendTable.getAssetDividends(assetId, from, to);
    }

    @Override
    public AssetDividend getLastDividend(long assetId) {
        return assetDividendTable.getLastDividend(assetId);
    }

    @Override
    public AssetDividend addAssetDividend(long transactionId, ColoredCoinsDividendPayment attachment, long totalDividend, long numAccounts) {
        Block lastBlock = blockchain.getLastBlock();
        AssetDividend assetDividend = new AssetDividend(transactionId, attachment, totalDividend, numAccounts, lastBlock.getHeight(), lastBlock.getTimestamp());
        assetDividendTable.insert(assetDividend);
        //listeners.notify(assetDividend, AssetEventType.ASSET_DIVIDEND);
        assetEvent.select(literal(AssetEventType.ASSET_DIVIDEND)).fire(assetDividend);
        return assetDividend;
    }
}
