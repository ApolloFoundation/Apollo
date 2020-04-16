/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.PeerDTO;
import com.apollocurrency.aplwallet.api.response.ResponseBase;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.testutil.EntityProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PeerConverterTest {

    private static ObjectMapper mapper = new ObjectMapper();

    @Test
    void givenPeerDTO_includeNonNullAnnotation_whenDo_thenCorrect() throws JsonProcessingException {

        Peer peer = EntityProducer.createPeer("192.168.2.68", "10.10.10.10", true, 0);
        assertNotNull(peer);

        PeerConverter converter = new PeerConverter();

        PeerDTO dto = converter.convert(peer);
        assertNotNull(dto);

        checkNonNullInJson(dto);
    }

    @Test
    void givenResponseBase_includeNonNullAnnotation_whenDo_thenCorrect() throws JsonProcessingException {

        ResponseBase response = new ResponseBase();
        assertNotNull(response);

        checkNonNullInJson(response);

    }

    private void checkNonNullInJson(Object object) throws JsonProcessingException {

        String stringJSON = mapper.writeValueAsString(object);

        assertFalse(stringJSON.contains("null"), String.format("Given value contains NULL: %s", stringJSON));
    }

}