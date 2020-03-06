package com.apollocurrency.aplwallet.apl.core.transaction.messages.update;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.testutil.WeldUtils;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Architecture;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.Platform;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
@EnableWeld
class UpdateV2AttachmentTest {
    @WeldSetup
    WeldInitiator weld = WeldUtils.from(List.of(), List.of(BlockchainConfig.class, PropertiesHolder.class, Blockchain.class, BlockchainImpl.class, NtpTime.class, TimeService.class)).build();

    UpdateV2Attachment attachment = new UpdateV2Attachment("https://update.zip", Level.CRITICAL, Platform.ALL, Architecture.AMD64, new Version("127.3.122"));

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