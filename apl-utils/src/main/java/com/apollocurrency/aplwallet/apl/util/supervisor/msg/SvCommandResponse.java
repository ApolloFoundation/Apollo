/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.supervisor.msg;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

/**
 * Responce to incoming command message
 *
 * @author alukin@gmail.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SvCommandResponse extends SvBusResponse {

    public List<String> out = new ArrayList<>();
    public List<String> err = new ArrayList<>();
}
