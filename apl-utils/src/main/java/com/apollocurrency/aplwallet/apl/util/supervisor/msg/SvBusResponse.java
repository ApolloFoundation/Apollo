package com.apollocurrency.aplwallet.apl.util.supervisor.msg;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents REST response in the channel
 * @author alukin@gmail.com
 */
@JsonInclude(Include.NON_NULL)
public class SvBusResponse extends SvBusMessage{
    /**
     * Error code, 0 or null means success
     */
    public SvBusError error;    
}
