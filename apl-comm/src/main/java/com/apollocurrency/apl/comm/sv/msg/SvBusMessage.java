/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.comm.sv.msg;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.Setter;

/**
 * Simple message on bus
 *
 * @author alukin@gmail.com
 */
@JsonInclude(Include.NON_NULL)
@Getter
@Setter
public class SvBusMessage {

    private String message;
}
