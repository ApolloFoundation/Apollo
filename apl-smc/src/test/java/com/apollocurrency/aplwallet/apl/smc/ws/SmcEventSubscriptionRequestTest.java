/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventSubscriptionRequest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;

import static com.apollocurrency.aplwallet.apl.smc.ws.SmcEventSocket.deserializeMessage;
import static com.apollocurrency.aplwallet.apl.smc.ws.SmcEventSocket.serializeMessage;
import static com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventSubscriptionRequest.Operation.SUBSCRIBE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author andrew.zinchenko@gmail.com
 */
class SmcEventSubscriptionRequestTest {

    @SneakyThrows
    @ValueSource(strings = {"{\"$expr\":{\"$not\":{\"$eq\":{\"from\":\"0x25dcf5c92f9e2c8b\"}}}}", "{\"from\":{\"$eq\":\"0x25dcf5c92f9e2c8b\"}}"})
    @ParameterizedTest
    void toJsonFromJson(String pattern) {
        //GIVEN
        var request = SmcEventSubscriptionRequest.builder()
            .operation(SUBSCRIBE)
            .requestId(UUID.randomUUID().toString())
            .events(List.of(SmcEventSubscriptionRequest.Event.builder()
                .subscriptionId("0x01")
                .name("Transfer")
                .signature("0x1234567890")
                .fromBlock("0")
                .build()))
            .build();
        //WHEN
        var str = serializeMessage(request);
        var json = str.replace("null", pattern);
        //THEN
        assertNotNull(json);
        //WHEN
        var obj = deserializeMessage(json);
        request.getEvents().get(0).setFilter(
            obj.getEvents().get(0).getFilter()
        );
        //THEN
        assertEquals(request, obj);

    }

    @SneakyThrows
    @Test
    void fromJson_CASE_INSENSITIVE_ENUMS() {
        //GIVEN
        var json = "{\"operation\":\"sUBscrIBE\",\"requestId\":\"7b89d051-c010-4236-97f3-5a697f3685c3\",\"events\":[{\"name\":\"Transfer\",\"signature\":\"0x1234567890\",\"fromBlock\":\"0\",\"filter\":{\"from\":{\"$eq\":\"0x25dcf5c92f9e2c8b\"}}}]}";
        //WHEN
        var obj = deserializeMessage(json);
        //THEN
        assertNotNull(obj);
        assertEquals(SmcEventSubscriptionRequest.Operation.SUBSCRIBE, obj.getOperation());
    }
}