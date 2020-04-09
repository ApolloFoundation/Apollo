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

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class AddPeerResponse extends ResponseBase {

    private Boolean done;

    public AddPeerResponse(Integer newErrorCode, String errorDescription, Long errorCode) {
        super(newErrorCode, errorDescription, errorCode);
        this.done = true;
    }
}
