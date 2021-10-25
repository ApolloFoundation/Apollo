/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.dex.eth.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EthChainGasInfoImplTest {
    String gasNowResponse = "{\"code\":200,\"data\":{\"rapid\":83000000000,\"fast\":66186654643,\"standard\":46000000000,\"slow\":40000000000,\"timestamp\":1634805517872,\"priceUSD\":4182.36}}";
    EthChainGasInfoImpl expected = new EthChainGasInfoImpl(66L, 46L, 40L);

    @Test
    void deserialize() throws JsonProcessingException {
        EthChainGasInfoImpl actual = new ObjectMapper().readValue(gasNowResponse, EthChainGasInfoImpl.class);

        assertEquals(expected, actual);
    }

    @Test
    void serialize() throws JsonProcessingException {
        String json = new ObjectMapper().writeValueAsString(expected.toDto());

        assertEquals("{\"fast\":\"66\",\"average\":\"46\",\"safeLow\":\"40\"}", json);
    }

}