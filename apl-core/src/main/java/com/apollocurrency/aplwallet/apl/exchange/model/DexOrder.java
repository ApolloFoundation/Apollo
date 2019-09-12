/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.api.dto.DexOrderDto;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOrderAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOrderAttachmentV2;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import lombok.Getter;


@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class DexOrder {

    private Long dbId;
    private Long id;
    private Long accountId;
    private String fromAddress;
    private String toAddress;

    private OrderType type;
    private OrderStatus status;
    private DexCurrencies orderCurrency;
    private Long orderAmount;

    private DexCurrencies pairCurrency;
    private BigDecimal pairRate;
    private Integer finishTime;

    public DexOrder() {
    }

    public DexOrder(Transaction transaction, DexOrderAttachment dexOrderAttachment) {
        this.id = transaction.getId();
        this.accountId = transaction.getSenderId();
        this.type = OrderType.getType(dexOrderAttachment.getType());
        this.orderCurrency = DexCurrencies.getType(dexOrderAttachment.getOrderCurrency());
        this.orderAmount = dexOrderAttachment.getOrderAmount();
        this.pairCurrency = DexCurrencies.getType(dexOrderAttachment.getPairCurrency());
        this.pairRate = EthUtil.gweiToEth(dexOrderAttachment.getPairRate());
        this.status = OrderStatus.getType(dexOrderAttachment.getStatus());
        this.finishTime = dexOrderAttachment.getFinishTime();

        if (dexOrderAttachment instanceof DexOrderAttachmentV2) {
            this.fromAddress = ((DexOrderAttachmentV2) dexOrderAttachment).getFromAddress();
            this.toAddress = ((DexOrderAttachmentV2) dexOrderAttachment).getToAddress();
        }
    }

    public DexOrderDto toDto() {
        DexOrderDto dexOrderDto = new DexOrderDto();

        dexOrderDto.id = Long.toUnsignedString(this.getId());
        dexOrderDto.accountId = Long.toUnsignedString(this.getAccountId());
        dexOrderDto.fromAddress = this.getFromAddress();
        dexOrderDto.toAddress = this.getToAddress();
        dexOrderDto.type = this.getType().ordinal();
        dexOrderDto.offerCurrency = this.getOrderCurrency().ordinal();
        //TODO make changes on UI. Send Apl as apl.
        dexOrderDto.offerAmount = EthUtil.atmToGwei(this.getOrderAmount());
        dexOrderDto.pairCurrency = this.getPairCurrency().ordinal();
        dexOrderDto.finishTime = this.getFinishTime();
        dexOrderDto.status = this.getStatus().ordinal();
        //TODO make changes on UI. Send BigDecimal.
        dexOrderDto.pairRate = EthUtil.ethToGwei(this.getPairRate());

        return dexOrderDto;
    }

}
