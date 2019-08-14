/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.api.dto.DexOfferDto;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferAttachmentV2;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;


@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
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
}
