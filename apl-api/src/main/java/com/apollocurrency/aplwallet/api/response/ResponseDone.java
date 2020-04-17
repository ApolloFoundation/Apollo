/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class ResponseDone extends ResponseBase {

    private Boolean done;

    public ResponseDone() {
        this(true);
    }

    public ResponseDone(Boolean done) {
        super(0, null, 0L);
        this.done = done;
    }

}
