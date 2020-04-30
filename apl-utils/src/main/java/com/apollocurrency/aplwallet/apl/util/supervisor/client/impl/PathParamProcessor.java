/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.supervisor.client.impl;

import com.apollocurrency.aplwallet.apl.util.supervisor.client.SvRequestHandler;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusMessage;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusRequest;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * JAX-RS -like path specifications processor
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class PathParamProcessor {

    private final Map<PathSpecification, HandlerRecord> rqHandlers = new ConcurrentHashMap<>();
    @Getter
    private final ObjectMapper mapper;

    public PathParamProcessor() {
        //init mapper
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    boolean canDistinguish(String pathSpec) {
        return true;
    }

    public boolean registerRqHandler(String pathSpec, Class<? extends SvBusMessage> rqMapping, Class<? extends SvBusMessage> respMapping, SvRequestHandler handler) {
        boolean res = false;
        PathSpecification spec = PathSpecification.fromSpecString(pathSpec);
        if (find(spec.prefix) != null) {
            log.error("Path specification with prefix {} is already registered", spec.prefix);
        } else {
            res = true;
            HandlerRecord hrec = new HandlerRecord();
            hrec.pathSpec = pathSpec;
            hrec.handler = handler;
            hrec.rqMapping = mapper.constructType(rqMapping);
            hrec.respMapping = mapper.constructType(respMapping);
            rqHandlers.putIfAbsent(spec, hrec);
        }
        return res;
    }

    public HandlerRecord findAndParse(String path) {
        HandlerRecord res = null;
        for (PathSpecification ps : rqHandlers.keySet()) {
            if (ps.matches(path)) {
                res = rqHandlers.get(ps);
                res.pathParams.clear();
                res.pathParams.putAll(ps.parseParams(path));
                break;
            }
        }
        return res;
    }

    void remove(String pathSpec) {
        for (PathSpecification ps : rqHandlers.keySet()) {
            if (ps.pathSpec.equals(pathSpec)) {
                rqHandlers.remove(ps);
                break;
            }
        }
    }

    public SvBusRequest convertRequest(JsonNode body, HandlerRecord hr) {
        SvBusRequest res = mapper.convertValue(body, hr.rqMapping);
        return res;
    }

    public HandlerRecord find(String path) {
        HandlerRecord res = null;
        for (PathSpecification ps : rqHandlers.keySet()) {
            if (ps.matches(path)) {
                res = rqHandlers.get(ps);
                break;
            }
        }
        return res;
    }

    public SvBusResponse convertResponse(JsonNode body, HandlerRecord hr) {
        SvBusResponse res = mapper.convertValue(body, hr.respMapping);
        return res;
    }
}
