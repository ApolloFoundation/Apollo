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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JAX-RS -like path specifications processor
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class PathParamProcessor {

    private final Map<PathSpecification, HandlerRecord> rqHandlers = new ConcurrentHashMap<>();
    private final Map<PathSpecification, Class<? extends SvBusResponse>> responseMappingClasses = new ConcurrentHashMap<>();
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
        if (findHandler(spec.prefix) != null) {
            log.error("Request handler for path specification with prefix {} is already registered", spec.prefix);
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

    public <T extends SvBusResponse> boolean  registerResponseMapping(String pathSpec, Class<? extends SvBusResponse> respMapping) {
        PathSpecification spec = PathSpecification.fromSpecString(pathSpec);
        boolean success = findResponseMappingClass(spec.prefix) != null;
        if (!success) {
            log.error("Response class for path specification with prefix {} is already registered", spec.prefix);
        } else {
            responseMappingClasses.put(spec, respMapping);
        }
        return success;
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

    public HandlerRecord findHandler(String path) {
        return findOneBySpec(path, rqHandlers);
    }
    public Class<? extends SvBusResponse> findResponseMappingClass(String path) {
        Class<? extends SvBusResponse> oneBySpec = findOneBySpec(path, responseMappingClasses);
        return oneBySpec;
    }

    private <T> T findOneBySpec(String path, Map<PathSpecification, T> map) {
        for (PathSpecification ps : map.keySet()) {
            if (ps.matches(path)) {
                return map.get(ps);
            }
        }
        return null;
    }

    public <T extends SvBusResponse> T  convertResponse(JsonNode body, Class<T> responseClass) {
        T res = mapper.convertValue(body, responseClass);
        return res;
    }
}
