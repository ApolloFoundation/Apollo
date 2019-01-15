package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.Bytes;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.math.BigInteger;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EncryptToResponse {
    public String data;
    public long requestProcessingTime;
    public String nonce;

    /**
     *
     * @author al
     */
    public static class ByteArrayDeserializer extends StdDeserializer<Bytes> {

        public ByteArrayDeserializer() {
            super(Bytes.class);
        }
        @Override
        public Bytes deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            String hex = node.asText();
            BigInteger bi = new BigInteger(hex, 16);
            return new Bytes(bi.toByteArray());
        }

    }
}
