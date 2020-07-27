/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.appdata.funding;


import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Setter
@Getter
public class FundingMonitorInstance {

    /**
     * Account monitor holding type
     */
    private final HoldingType holdingType;

    /**
     * Holding identifier
     */
    private final long holdingId;

    /**
     * Account property
     */
    private final String property;

    /**
     * Fund amount
     */
    private final long amount;

    /**
     * Fund threshold
     */
    private final long threshold;

    /**
     * Fund interval
     */
    private final int interval;

    /**
     * Fund account identifier
     */
    private final long accountId;

    /**
     * Fund account name
     */
    private final String accountName;

    /**
     * Fund account secret phrase
     */
    private final byte[] keySeed;

    /**
     * Fund account public key
     */
    private final byte[] publicKey;


    /**
     * Create a monitor instance
     *
     * @param holdingType Holding type
     * @param holdingId   Asset or Currency identifier, ignored for APL monitor
     * @param property    Account property name
     * @param amount      Fund amount
     * @param threshold   Fund threshold
     * @param interval    Fund interval
     * @param accountId   Fund account identifier
     * @param keySeed     Fund account key seed
     */
    public FundingMonitorInstance(HoldingType holdingType, long holdingId, String property,
                           long amount, long threshold, int interval,
                           long accountId, byte[] keySeed) {
        this.holdingType = holdingType;
        this.holdingId = (holdingType != HoldingType.APL ? holdingId : 0);
        this.property = property;
        this.amount = amount;
        this.threshold = threshold;
        this.interval = interval;
        this.accountId = accountId;
        this.accountName = Convert2.rsAccount(accountId);
        this.keySeed = keySeed;
        this.publicKey = Crypto.getPublicKey(keySeed);
    }

    public FundingMonitorInstance(HoldingType holdingType, long holdingId, String property, long amount, long threshold, int interval, long accountId,
                           String accountName, byte[] keySeed, byte[] publicKey) {
        this.holdingType = holdingType;
        this.holdingId = holdingId;
        this.property = property;
        this.amount = amount;
        this.threshold = threshold;
        this.interval = interval;
        this.accountId = accountId;
        this.accountName = accountName;
        this.keySeed = keySeed;
        this.publicKey = publicKey;
    }


    /**
     * Return the hash code
     *
     * @return Hash code
     */
    @Override
    public int hashCode() {
        return holdingType.hashCode() + (int) holdingId + property.hashCode() + (int) accountId;
    }

    /**
     * Check if two monitors are equal
     *
     * @param obj Comparison object
     * @return TRUE if the objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        boolean isEqual = false;
        if ((obj instanceof FundingMonitorInstance)) {
            FundingMonitorInstance monitor = (FundingMonitorInstance) obj;
            if (holdingType == monitor.getHoldingType() && holdingId == monitor.getHoldingId() &&
                property.equals(monitor.getProperty()) && accountId == monitor.getAccountId()) {
                isEqual = true;
            }
        }
        return isEqual;
    }

}
