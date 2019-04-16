/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto;

public class DexOfferDto {

    public long id;
    public long accountId;

    public Integer type;
    public Integer offerCurrency;
    public Long offerAmount;
    public Long pairRate;

    public Integer pairCurrency;
    public Integer status;
    public Integer finishTime;

}
