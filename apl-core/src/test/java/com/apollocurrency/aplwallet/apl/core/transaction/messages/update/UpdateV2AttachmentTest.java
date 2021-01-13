package com.apollocurrency.aplwallet.apl.core.transaction.messages.update;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.env.Arch;
import com.apollocurrency.aplwallet.apl.util.env.OS;
import com.apollocurrency.aplwallet.apl.util.env.PlatformSpec;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateV2AttachmentTest {

    UpdateV2Attachment attachment;

    @BeforeEach
    void setUp() {
        byte[] signature = new byte[256];
        Arrays.fill(signature, (byte) 1);
        attachment = new UpdateV2Attachment("https://update.zip", Level.CRITICAL, new Version("127.3.122"), "appollo.com", BigInteger.ZERO, signature, Set.of(new PlatformSpec(OS.NO_OS, Arch.X86_64), new PlatformSpec(OS.NO_OS, Arch.X86_32), new PlatformSpec(OS.LINUX, Arch.ARM_64)));
    }

    @Test
    void testParseFromBytes() throws AplException.NotValidException {

        ByteBuffer buffer = ByteBuffer.allocate(attachment.getFullSize());
        attachment.putBytes(buffer);
        assertEquals(buffer.position(), attachment.getFullSize());

        buffer.flip();
        UpdateV2Attachment parsedFromBytes = new UpdateV2Attachment(buffer);

        assertEquals(attachment, parsedFromBytes);
    }

    @Test
    void testParseFromJson() throws AplException.NotValidException, IOException, ParseException {

        JSONObject json = attachment.getJSONObject();
        CharArrayWriter out = new CharArrayWriter();
        json.writeJSONString(out);
        JSONObject parsedJson = (JSONObject) JSONValue.parseWithException(new String(out.toCharArray()));

        UpdateV2Attachment parsedFromJson = new UpdateV2Attachment(parsedJson);

        assertEquals(attachment, parsedFromJson);
    }

}