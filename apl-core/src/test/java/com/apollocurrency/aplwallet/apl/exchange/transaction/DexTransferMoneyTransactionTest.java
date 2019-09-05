package com.apollocurrency.aplwallet.apl.exchange.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexControlOfFrozenMoneyAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
@EnableWeld
class DexTransferMoneyTransactionTest {
    DexControlOfFrozenMoneyAttachment attachment = new DexControlOfFrozenMoneyAttachment(64, 100);
    DexService dexService = mock(DexService.class);
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from()
            .addBeans(
                    MockBean.of(mock(BlockchainConfig.class), BlockchainConfig.class),
                    MockBean.of(mock(BlockchainImpl.class), Blockchain.class, BlockchainImpl.class),
                    MockBean.of(dexService, DexService.class),
                    MockBean.of(mock(TimeService.class), TimeService.class)
            ).build();

    DexTransferMoneyTransaction transactionType;
    @BeforeEach
    void setUp() {
        transactionType = new DexTransferMoneyTransaction();
    }

    @Test
    void parseByteAttachment() throws AplException.NotValidException {
        ByteBuffer buffer = ByteBuffer.allocate(17);
        buffer.put((byte) 1);
        buffer.putLong(64);
        buffer.putLong(100);
        buffer.rewind();
        AbstractAttachment parsedAttachment = transactionType.parseAttachment(buffer);

        assertEquals(attachment, parsedAttachment);
    }

    @Test
    void testJsonParseAttachment() throws AplException.NotValidException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("contractId", 64L);
        jsonObject.put("offerAmount", 100L);
        jsonObject.put("version.DexTransferMoney", 1);

        AbstractAttachment parsedAttachment = transactionType.parseAttachment(jsonObject);

        assertEquals(attachment, parsedAttachment);
    }

    @Test
    void testSerializeDeserializeAttachmentToByteBuffer() throws AplException.NotValidException {
        int myFullSize = attachment.getFullSize();
        ByteBuffer buffer = ByteBuffer.allocate(myFullSize);
        attachment.putBytes(buffer);
        buffer.rewind();

        AbstractAttachment parsedAttachment = transactionType.parseAttachment(buffer);

        assertEquals(attachment, parsedAttachment);
    }

    @Test
    void testSerializeDeserializeAttachmentToJson() throws AplException.NotValidException {
        JSONObject jsonObject = attachment.getJSONObject();

        AbstractAttachment parsedAttachment = transactionType.parseAttachment(jsonObject);

        assertEquals(attachment, parsedAttachment);
    }

    @Test
    void testValidateAttachment() {
        Transaction tx = mock(Transaction.class);
        doReturn(attachment).when(tx).getAttachment();

        assertThrows(AplException.NotValidException.class, () -> transactionType.validateAttachment(tx)); // no contract

        ExchangeContract contract = new ExchangeContract(1L, 64L, 200L, 300L, 1000L, 2000L, ExchangeContractStatus.STEP_3, new byte[32], null, null, new byte[32]);
        doReturn(contract).when(dexService).getDexContract(DexContractDBRequest.builder().id(64L).build());
        assertThrows(AplException.NotValidException.class, () -> transactionType.validateAttachment(tx));
    }


    @Test
    void applyAttachment() {
    }

    @Test
    void isDuplicate() {
    }
}