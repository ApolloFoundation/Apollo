package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Formatter;

/**
 *
 * @author al
 */
public class ByteArraySerializer extends JsonSerializer<Bytes> {

    public String toHex(byte[] ba) {
        Formatter formatter = new Formatter();
        for (byte b : ba) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    @Override
    public void serialize(Bytes t, JsonGenerator jg, SerializerProvider sp) throws IOException {
        String ba = toHex(t.getBytes());
        jg.writeString(ba);
    }

}
