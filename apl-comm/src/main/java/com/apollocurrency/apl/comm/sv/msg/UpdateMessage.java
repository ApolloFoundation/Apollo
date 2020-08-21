/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.comm.sv.msg;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Update indication message TODO: adapt it for uptater2
 *
 * @author alukin@gmail.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateMessage extends SvBusMessage {

    public String version;
    public Integer priority;
}
