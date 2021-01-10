package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.dex.config.DexConfig;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@EnableWeld
    // setup weld only for Dex tx types instantiation
class DexContractAttachmentTest {
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from()
        .addBeans(
            MockBean.of(mock(DexConfig.class), DexConfig.class),
            MockBean.of(mock(DexService.class), DexService.class),
            MockBean.of(mock(BlockchainConfig.class), BlockchainConfig.class),
            MockBean.of(mock(PhasingPollService.class), PhasingPollService.class),
            MockBean.of(mock(BlockchainImpl.class), Blockchain.class, BlockchainImpl.class),
            MockBean.of(mock(TimeService.class), TimeService.class)
        ).build();
    private String hash = "f0af17449a83681de22db7ce16672f16f37131bec0022371d4ace5d1854301e0";
    private String encryptedSecret = "ce6b20ee7f7797e102f68d15099e7d5b0e8d4c50f98a7865ea168717539ec3aace6b20ee7f7797e102f68d15099e7d5b0e8d4c50f98a7865ea168717539ec3aa";
    private DexContractAttachment attachment = new DexContractAttachment(1L, 2L, Convert.parseHexString(hash), "3", "4", Convert.parseHexString(encryptedSecret), ExchangeContractStatus.STEP_1, 7200);

    @Test
    void testSerializeToJson() {
        JSONObject serialized = attachment.getJSONObject();
        DexContractAttachment deserialized = new DexContractAttachment(serialized);
        assertEquals(attachment, deserialized);
    }

    @Test
    void testSerializeToJsonWithoutSecret() {
        attachment.setSecretHash(null);

        JSONObject serialized = attachment.getJSONObject();
        DexContractAttachment deserialized = new DexContractAttachment(serialized);

        attachment.setEncryptedSecret(null);
        assertEquals(attachment, deserialized);
    }

    @Test
    void testSerializeToByteBuffer() throws AplException.NotValidException {
        testSerializeToByteBuffer(125);
    }

    @Test
    void testSerializeToByteBufferWithoutSecret() throws AplException.NotValidException {
        attachment.setSecretHash(null);
        attachment.setEncryptedSecret(null);
        testSerializeToByteBuffer(29);
    }

    @Test
    void testSerializeToByteBufferWithoutCounterTransferTx() throws AplException.NotValidException {
        attachment.setCounterTransferTxId(null);
        testSerializeToByteBuffer(124);
    }

    private void testSerializeToByteBuffer(int size) throws AplException.NotValidException {
        int fullSize = attachment.getFullSize();
        assertEquals(size, fullSize);
        ByteBuffer byteBuffer = ByteBuffer.allocate(fullSize);
        attachment.putBytes(byteBuffer);
        byteBuffer.rewind();

        DexContractAttachment deserialized = new DexContractAttachment(byteBuffer);

        assertEquals(attachment, deserialized);
    }

}