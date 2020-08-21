/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.comm.sv.msg;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.Map;

/**
 * Request (incoming message) in SV channel
 *
 * @author alukin@gmail.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SvBusRequest extends SvBusMessage {

    private final Map<String, String> parameters = new HashMap<>();

    public SvBusRequest(Map<String, String> parameters) {
        this.parameters.putAll(parameters);
    }

    public SvBusRequest() {}

    public  <T, V> void add(T t, V v) {
        parameters.put(t.toString(), v.toString());
    }

    public String get(String param) {
        return parameters.get(param);
    }

}
