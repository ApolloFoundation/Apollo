/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.appdata.funding;

import com.apollocurrency.aplwallet.apl.core.service.appdata.funding.FundingMonitorService;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import lombok.ToString;

@ToString
public class MonitoredAccount {

    /**
     * Account identifier
     */
    private final long accountId;

    /**
     * Account name
     */
    private final String accountName;

    /**
     * Associated monitor
     */
    private final FundingMonitorInstance monitor;

    /**
     * Fund amount
     */
    private long amount;

    /**
     * Fund threshold
     */
    private long threshold;

    /**
     * Fund interval
     */
    private int interval;

    /**
     * Last fund height
     */
    private int height;


    /**
     * Create a new monitored account
     *
     * @param accountId Account identifier
     * @param monitor   Account monitor
     * @param amount    Fund amount
     * @param threshold Fund threshold
     * @param interval  Fund interval
     */
    public MonitoredAccount(long accountId, FundingMonitorInstance monitor, long amount, long threshold, int interval) {
        if (amount < FundingMonitorService.MIN_FUND_AMOUNT) {
            throw new IllegalArgumentException("Minimum fund amount is " + FundingMonitorService.MIN_FUND_AMOUNT);
        }
        if (threshold < FundingMonitorService.MIN_FUND_THRESHOLD) {
            throw new IllegalArgumentException("Minimum fund threshold is " + FundingMonitorService.MIN_FUND_THRESHOLD);
        }
        if (interval < FundingMonitorService.MIN_FUND_INTERVAL) {
            throw new IllegalArgumentException("Minimum fund interval is " + FundingMonitorService.MIN_FUND_INTERVAL);
        }
        this.accountId = accountId;
        this.accountName = Convert2.rsAccount(accountId);
        this.monitor = monitor;
        this.amount = amount;
        this.threshold = threshold;
        this.interval = interval;
    }

    /**
     * Get the account identifier
     *
     * @return Account identifier
     */
    public long getAccountId() {
        return accountId;
    }

    /**
     * Get the account name (Reed-Solomon encoded account identifier)
     *
     * @return Account name
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * Get the funding amount
     *
     * @return Funding amount
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Get the funding threshold
     *
     * @return Funding threshold
     */
    public long getThreshold() {
        return threshold;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    /**
     * Get the funding interval
     *
     * @return Funding interval
     */
    public int getInterval() {
        return interval;
    }

    public FundingMonitorInstance getMonitor() {
        return monitor;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
