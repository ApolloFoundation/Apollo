/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.comm.sv.client.impl;

import com.apollocurrency.apl.comm.sv.client.SvRequestHandler;
import com.fasterxml.jackson.databind.JavaType;
import java.util.HashMap;
import java.util.Map;

/**
 * Record to save handler data and mappings
 *
 * @author alukin@gmail.com
 */
public class HandlerRecord {

    public String pathSpec;
    public SvRequestHandler handler;
    public JavaType respMapping;
    public JavaType rqMapping;
    public Map<String, String> pathParams = new HashMap<>();
}
