/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */
package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class AccountCurrentAssetAskOrderIdsResponse extends ResponseBase {
    private List<String> askOrderIds;

    public AccountCurrentAssetAskOrderIdsResponse(List<String> askOrderIds) {
        this.askOrderIds = askOrderIds;
    }
}
