/*
 *
 *  Copyright © 2018-2020 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.UnconfirmedTransactionDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaseBalanceResponse extends ResponseBase {

    private UnconfirmedTransactionDTO transactionJSON;

    private String signatureHash;
    private String unsignedTransactionBytes;
    private Boolean broadcasted;
    private String transactionBytes;
    private String fullHash;
    private String transaction;

}
