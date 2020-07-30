/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetCumulativeDifficultyRequestTest {

    private static final String jsonExample = "{\"requestType\":\"getCumulativeDifficulty\",\"chainId\":\"00000000-0000-007b-0000-000000000141\"}";

    @Test
    void toJson() throws JsonProcessingException {
        GetCumulativeDifficultyRequest getCumulativeDifficulty = new GetCumulativeDifficultyRequest(UUID.fromString("00000000-0000-007b-0000-000000000141"));

        String json = getCumulativeDifficulty.toJson();

        assertEquals(jsonExample, json);
    }
}