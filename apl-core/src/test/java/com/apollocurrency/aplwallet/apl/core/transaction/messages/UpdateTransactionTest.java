package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.UpdateV2Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.types.update.UpdateTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.update.UpdateV2TransactionType;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.env.Arch;
import com.apollocurrency.aplwallet.apl.util.env.OS;
import com.apollocurrency.aplwallet.apl.util.env.PlatformSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UpdateTransactionTest {
    TransactionType type;

    @BeforeEach
    void setUp() {
        type = new UpdateV2TransactionType(mock(BlockchainConfig.class), mock(AccountService.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.2.3", "32767.32767.32767"})
    void testValidateAttachment_successfully(String v) throws AplException.ValidationException {
        Transaction tx = createUpdateTx(v);

        type.doStateIndependentValidation(tx);
    }

    @ParameterizedTest
    @ValueSource(strings = {"32768.1.1", "120.32768.32767", "120.0.33000"})
    void testValidateAttachment_incorrectVersion(String incorrectVersionString) throws AplException.ValidationException {
        Transaction tx = createUpdateTx(incorrectVersionString);

        assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));
    }

    @Test
    void testCallCommonMethods() {
        assertThrows(UnsupportedOperationException.class, () -> ((UpdateTransactionType) type).getLevel());
        assertEquals(type.getName(), "UpdateV2");
        assertEquals(type.getSpec().getSubtype(), 3);
    }

    private Transaction createUpdateTx(String v) {
        Transaction tx = mock(Transaction.class);
        Version version = new Version(v);
        UpdateV2Attachment attachment = new UpdateV2Attachment("htpps://update.zip", Level.CRITICAL, version, "somesite.com", BigInteger.ONE, new byte[128], Set.of(new PlatformSpec(OS.NO_OS, Arch.X86_64), new PlatformSpec(OS.NO_OS, Arch.X86_32), new PlatformSpec(OS.MAC_OS, Arch.ARM_32)));
        when(tx.getAttachment()).thenReturn(attachment);
        return tx;
    }
}
