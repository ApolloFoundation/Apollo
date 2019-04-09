/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferAttachment;

import java.math.BigDecimal;

public class DexOffer{
    private long id;
    private long transactionId;
    private long accountId;

    private OfferType type;
    private DexCurrencies offerCurrency;
    private long offerAmount;

    private DexCurrencies pairCurrency;
    private BigDecimal pairRate;
    //TODO change it
    private int finishTime;

    public DexOffer() {
    }

    public DexOffer(Transaction transaction, DexOfferAttachment dexOfferAttachment) {
        this.transactionId = transaction.getId();
        this.accountId = transaction.getSenderId();
        this.type = OfferType.getType(dexOfferAttachment.getType());
        this.offerCurrency = DexCurrencies.getType(dexOfferAttachment.getOfferCurrency());
        this.offerAmount = dexOfferAttachment.getOfferAmount();
        this.pairCurrency = DexCurrencies.getType(dexOfferAttachment.getPairCurrency());
        this.pairRate = getPairRate();
        this.finishTime = getFinishTime();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public OfferType getType() {
        return type;
    }

    public void setType(OfferType type) {
        this.type = type;
    }

    public DexCurrencies getOfferCurrency() {
        return offerCurrency;
    }

    public void setOfferCurrency(DexCurrencies offerCurrency) {
        this.offerCurrency = offerCurrency;
    }

    public long getOfferAmount() {
        return offerAmount;
    }

    public void setOfferAmount(long offerAmount) {
        this.offerAmount = offerAmount;
    }

    public DexCurrencies getPairCurrency() {
        return pairCurrency;
    }

    public void setPairCurrency(DexCurrencies pairCurrency) {
        this.pairCurrency = pairCurrency;
    }

    public BigDecimal getPairRate() {
        return pairRate;
    }

    public void setPairRate(BigDecimal pairRate) {
        this.pairRate = pairRate;
    }

    public int getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(int finishTime) {
        this.finishTime = finishTime;
    }
}
