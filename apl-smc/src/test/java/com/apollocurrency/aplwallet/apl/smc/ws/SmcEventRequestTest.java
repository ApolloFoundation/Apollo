/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.apollocurrency.aplwallet.apl.smc.ws.SmcEventRequest.Operation.SUBSCRIBE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author andrew.zinchenko@gmail.com
 */
class SmcEventRequestTest {
    ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    }

    @SneakyThrows
    @Test
    void toJsonFromJson() {
        //GIVEN
        var request = SmcEventRequest.builder()
            .operation(SUBSCRIBE)
            .events(List.of(SmcEventRequest.Event.builder()
                .name("Transfer")
                .signature("0x1234567890")
                .fromBlock("0")
                .filter(List.of(SmcEventRequest.Filter.builder()
                    .parameter("sender")
                    .value("0xaabbccddeeff")
                    .build()))
                .build()))
            .build();
        //WHEN
        var json = mapper.writeValueAsString(request);
        //THEN
        assertNotNull(json);
        //WHEN
        var obj = mapper.readValue(json, SmcEventRequest.class);
        //THEN
        assertEquals(request, obj);

    }

    @SneakyThrows
    @Test
    void fromJson_CASE_INSENSITIVE_ENUMS() {
        //GIVEN
        var json = "{\"operation\":\"SubScribe\",\"events\":[{\"name\":\"Transfer\",\"signature\":\"0x1234567890\",\"fromBlock\":\"0\",\"filter\":[{\"parameter\":\"sender\",\"value\":\"0xaabbccddeeff\"}]}]}";
        //WHEN
        var obj = mapper.readValue(json, SmcEventRequest.class);
        //THEN
        assertNotNull(obj);
        assertEquals(SmcEventRequest.Operation.SUBSCRIBE, obj.getOperation());

    }
}