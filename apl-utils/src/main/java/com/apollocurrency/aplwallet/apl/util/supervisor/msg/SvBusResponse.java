/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.supervisor.msg;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This class represents REST response in the channel
 *
 * @author alukin@gmail.com
 */
@EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@Data
public class SvBusResponse extends SvBusMessage {

    /**
     * Error code, 0 or null means success
     */
    private final SvBusError error;

    public boolean isSuccessful() {
        return error == null || error.getErrorCode() == 0;
    }
}
