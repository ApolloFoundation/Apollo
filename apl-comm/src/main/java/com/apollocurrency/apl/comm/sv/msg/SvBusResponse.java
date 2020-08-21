/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.comm.sv.msg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class represents REST response in the channel
 *
 * @author alukin@gmail.com
 */
@EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SvBusResponse extends SvBusMessage {

    /**
     * Error code, 0 or null means success
     */
    private SvBusStatus status;


    @JsonIgnore
    public boolean isSuccessful() {
        return status == null || status.getCode() == 0;
    }
}
