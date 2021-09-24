/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventSubscriptionRequest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

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
    @Test
    void toJsonFromJson() {
        //GIVEN
        var request = SmcEventSubscriptionRequest.builder()
            .operation(SUBSCRIBE)
            .requestId(UUID.randomUUID().toString())
            .events(List.of(SmcEventSubscriptionRequest.Event.builder()
                .name("Transfer")
                .signature("0x1234567890")
                .fromBlock("0")
                .filter(List.of(SmcEventSubscriptionRequest.Filter.builder()
                    .parameter("sender")
                    .value("0xaabbccddeeff")
                    .build()))
                .build()))
            .build();
        //WHEN
        var json = serializeMessage(request);
        //THEN
        assertNotNull(json);
        //WHEN
        var obj = deserializeMessage(json);
        //THEN
        assertEquals(request, obj);

    }

    @SneakyThrows
    @Test
    void fromJson_CASE_INSENSITIVE_ENUMS() {
        //GIVEN
        var json = "{\"operation\":\"SUBSCRIBE\",\"requestId\":\"7b89d051-c010-4236-97f3-5a697f3685c3\",\"events\":[{\"name\":\"Transfer\",\"signature\":\"0x1234567890\",\"fromBlock\":\"0\",\"filter\":[{\"parameter\":\"sender\",\"value\":\"0xaabbccddeeff\"}]}]}";
        //WHEN
        var obj = deserializeMessage(json);
        //THEN
        assertNotNull(obj);
        assertEquals(SmcEventSubscriptionRequest.Operation.SUBSCRIBE, obj.getOperation());
    }
}