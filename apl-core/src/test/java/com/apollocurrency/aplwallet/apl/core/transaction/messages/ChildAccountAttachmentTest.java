/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.AddressScope;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
class ChildAccountAttachmentTest {
    private static final byte[] CHILD_PUBLIC_KEY_1 = Crypto.getPublicKey("1234567890");
    private static final byte[] CHILD_PUBLIC_KEY_2 = Crypto.getPublicKey("0987654321");

    ChildAccountAttachment attachment;

    @BeforeEach
    void setUp() {
        attachment = new ChildAccountAttachment(AddressScope.IN_FAMILY, 2, List.of(CHILD_PUBLIC_KEY_1, CHILD_PUBLIC_KEY_2));
    }

    @Test
    void testParseFromBytes() throws AplException.NotValidException {

        ByteBuffer buffer = ByteBuffer.allocate(attachment.getFullSize());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        attachment.putBytes(buffer);
        assertEquals(buffer.position(), attachment.getFullSize());

        buffer.rewind();
        ChildAccountAttachment parsedFromBytes = new ChildAccountAttachment(buffer);

        assertEquals(attachment, parsedFromBytes);
    }

    @Test
    void testParseFromJson() throws IOException, ParseException {

        JSONObject json = attachment.getJSONObject();
        CharArrayWriter out = new CharArrayWriter();
        json.writeJSONString(out);
        JSONObject parsedJson = (JSONObject) JSONValue.parseWithException(new String(out.toCharArray()));

        ChildAccountAttachment parsedFromJson = new ChildAccountAttachment(parsedJson);

        assertEquals(attachment, parsedFromJson);
    }
}