/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.supervisor.msg;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Error message body on the bus
 *
 * @author alukin@gmail.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SvBusError {

    public Integer errorCode;
    public String descritption;
}
