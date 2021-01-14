/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.appdata.funding;

import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.FundingMonitorInstance;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.MonitoredAccount;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

import java.util.List;

public interface FundingMonitorService {
    /**
     * Minimum monitor amount
     */
    static final long MIN_FUND_AMOUNT = 1;
    /**
     * Minimum monitor threshold
     */
    static final long MIN_FUND_THRESHOLD = 1;
    /**
     * Minimum funding interval
     */
    static final int MIN_FUND_INTERVAL = 10;

    boolean isStopped();

    void processSemaphoreAcquire() throws InterruptedException;

    boolean addPendingEvent(MonitoredAccount account);

    MonitoredAccount pollPendingEvent();

    boolean addAllPendingEvents(List<MonitoredAccount> suspendedEvents);

    boolean containsPendingEvent(MonitoredAccount monitoredAccount);

    boolean isPendingEventsEmpty();

    List<MonitoredAccount> getMonitoredAccountListById(long accountId);

    List<FundingMonitorInstance> getMonitors();

    List<MonitoredAccount> putAccountList(long accountId, List<MonitoredAccount> accountList);

    List<MonitoredAccount> removeByAccountId(long accountId);

    boolean startMonitor(HoldingType holdingType, long holdingId, String property,
                         long amount, long threshold, int interval, byte[] keySeed);

    MonitoredAccount createMonitoredAccount(long accountId, FundingMonitorInstance monitor, String propertyValue);

    int stopAllMonitors();

    boolean stopMonitor(HoldingType holdingType, long holdingId, String property, long accountId);

    List<FundingMonitorInstance> getMonitors(Filter<FundingMonitorInstance> filter);

    List<FundingMonitorInstance> getAllMonitors();

    List<MonitoredAccount> getMonitoredAccounts(FundingMonitorInstance monitor);

    void processAplEvent(MonitoredAccount monitoredAccount, Account targetAccount, Account fundingAccount)
        throws AplException;

    void processAssetEvent(MonitoredAccount monitoredAccount, Account targetAccount, Account fundingAccount)
            throws AplException;

    void processCurrencyEvent(MonitoredAccount monitoredAccount, Account targetAccount, Account fundingAccount)
                throws AplException;
}
