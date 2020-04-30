/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.supervisor.msg;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;

/**
 * This class represents command request to supervisor
 *
 * @author alukin@gmail.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SvCommandRequest extends SvBusRequest {

    public String cmd;
    public HashMap<String, String> params = new HashMap<>();
}
