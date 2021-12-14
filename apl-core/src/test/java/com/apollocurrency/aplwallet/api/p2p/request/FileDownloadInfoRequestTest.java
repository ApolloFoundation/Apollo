/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileDownloadInfoRequestTest {
    @Test
    void testConversionFromJson() throws IOException, ParseException {
        UUID chainId = UUID.randomUUID();
        FileDownloadInfoRequest fileDownloadInfoRequest = new FileDownloadInfoRequest(chainId);
        fileDownloadInfoRequest.full = true;
        fileDownloadInfoRequest.fileId = "22";
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(fileDownloadInfoRequest);
        JSONObject jsonObject = (JSONObject) JSONValue.parseWithException(jsonString);
        FileDownloadInfoRequest deserialized = mapper.convertValue(jsonObject, FileDownloadInfoRequest.class);

        assertEquals(fileDownloadInfoRequest, deserialized);


    }

}