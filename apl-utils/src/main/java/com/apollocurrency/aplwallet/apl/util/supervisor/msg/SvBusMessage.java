/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.supervisor.msg;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Simple message on bus
 *
 * @author alukin@gmail.com
 */
@JsonInclude(Include.NON_NULL)
public class SvBusMessage {

    public String message;
}
