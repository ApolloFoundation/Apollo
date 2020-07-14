package com.apollocurrency.aplwallet.apl.core.transaction.messages.update;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.testutil.WeldUtils;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.env.Arch;
import com.apollocurrency.aplwallet.apl.util.env.OS;
import com.apollocurrency.aplwallet.apl.util.env.PlatformSpec;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
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
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnableWeld
class UpdateV2AttachmentTest {
    @WeldSetup
    WeldInitiator weld = WeldUtils.from(List.of(), List.of(UpdateV2Transaction.class, BlockchainConfig.class, PropertiesHolder.class, Blockchain.class, BlockchainImpl.class, NtpTime.class, TimeService.class)).build();

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