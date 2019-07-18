/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.api.dto.DexOfferDto;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferAttachmentV2;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;

import java.math.BigDecimal;

public class DexOffer{
    /**
     * Use transactionId.
     */
    @Deprecated
    private Long id;
    private Long transactionId;
    private Long accountId;
    private String fromAddress;
    private String toAddress;

    private OfferType type;
    private OfferStatus status;
    private DexCurrencies offerCurrency;
    private Long offerAmount;

    private DexCurrencies pairCurrency;
    private BigDecimal pairRate;
    private Integer finishTime;

    public DexOffer() {
    }

    public DexOffer(Transaction transaction, DexOfferAttachment dexOfferAttachment) {
        this.transactionId = transaction.getId();
        this.accountId = transaction.getSenderId();
        this.type = OfferType.getType(dexOfferAttachment.getType());
        this.offerCurrency = DexCurrencies.getType(dexOfferAttachment.getOfferCurrency());
        this.offerAmount = dexOfferAttachment.getOfferAmount();
        this.pairCurrency = DexCurrencies.getType(dexOfferAttachment.getPairCurrency());
        this.pairRate = EthUtil.gweiToEth(dexOfferAttachment.getPairRate());
        this.status = OfferStatus.getType(dexOfferAttachment.getStatus());
        this.finishTime = dexOfferAttachment.getFinishTime();

        if(dexOfferAttachment instanceof DexOfferAttachmentV2){
            this.fromAddress = ((DexOfferAttachmentV2)dexOfferAttachment).getFromAddress();
            this.toAddress = ((DexOfferAttachmentV2)dexOfferAttachment).getToAddress();
        }
    }

    //TODO discuss about this approach
    public DexOfferDto toDto(){
        DexOfferDto dexOfferDto = new DexOfferDto();

        dexOfferDto.id = Long.toUnsignedString(this.getTransactionId());
        dexOfferDto.accountId = Long.toUnsignedString(this.getAccountId());
        dexOfferDto.fromAddress = this.getFromAddress();
        dexOfferDto.toAddress = this.getToAddress();
        dexOfferDto.type = this.getType().ordinal();
        dexOfferDto.offerCurrency = this.getOfferCurrency().ordinal();
        //TODO make changes on UI. Send Apl as apl.
        dexOfferDto.offerAmount = EthUtil.aplToGwei(this.getOfferAmount());
        dexOfferDto.pairCurrency = this.getPairCurrency().ordinal();
        dexOfferDto.finishTime = this.getFinishTime();
        dexOfferDto.status = this.getStatus().ordinal();
        //TODO make changes on UI. Send BigDecimal.
        dexOfferDto.pairRate = EthUtil.ethToGwei(this.getPairRate());

        return dexOfferDto;
    }


    /**
     * Use TransactionId
     */
    @Deprecated
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
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

    public Long getOfferAmount() {
        return offerAmount;
    }

    public void setOfferAmount(Long offerAmount) {
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

    public Integer getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Integer finishTime) {
        this.finishTime = finishTime;
    }

    public OfferStatus getStatus() {
        return status;
    }

    public void setStatus(OfferStatus status) {
        this.status = status;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }
}
