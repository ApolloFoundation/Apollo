/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto;

public class DexOfferDto {

    /**
     * id for entity offer is a transaction id.
     */
    public String id;
    public String accountId;

    public Integer type;
    public Integer offerCurrency;
    public Long offerAmount;
    public Long pairRate;

    public Integer pairCurrency;
    public Integer status;
    public Integer finishTime;

}
