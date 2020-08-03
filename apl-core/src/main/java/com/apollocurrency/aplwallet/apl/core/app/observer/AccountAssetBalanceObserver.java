/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.MonitoredAccount;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.service.appdata.funding.FundingMonitorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class AccountAssetBalanceObserver {

    private final FundingMonitorService fundingMonitorService;

    @Inject
    public AccountAssetBalanceObserver(FundingMonitorService fundingMonitorService) {
        this.fundingMonitorService = fundingMonitorService;
    }

    /**
     * Asset event notification
     *
     * @param asset Account asset
     */
    public void onAccountAssetBalance(@Observes @AccountEvent(AccountEventType.ASSET_BALANCE) AccountAsset asset) {
        log.trace("Catch event {} asset={}", AccountEventType.ASSET_BALANCE, asset);
        if (fundingMonitorService.isStopped()) {
            return;
        }
        long balance = asset.getQuantityATU();
        long assetId = asset.getAssetId();
        //
        // Check the asset balance for monitored accounts
        //
        synchronized (fundingMonitorService.getMonitors()) {
            List<MonitoredAccount> accountList = fundingMonitorService.getMonitoredAccountListById(asset.getAccountId());
            if (accountList != null) {
                accountList.forEach((maccount) -> {
                    if (maccount.getMonitor().getHoldingType() == HoldingType.ASSET &&
                        maccount.getMonitor().getHoldingId() == assetId &&
                        balance < maccount.getThreshold() &&
                        !fundingMonitorService.containsPendingEvent(maccount)) {
                        fundingMonitorService.addPendingEvent(maccount);
                    }
                });
            }
        }
    }

}
