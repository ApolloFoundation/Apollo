package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.update.UpdateTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.UpdateV2Attachment;
import com.apollocurrency.aplwallet.apl.testutil.WeldUtils;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.env.Architecture;
import com.apollocurrency.aplwallet.apl.util.env.Platform;
import com.apollocurrency.aplwallet.apl.util.env.PlatformSpec;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnableWeld
public class UpdateTransactionTest {
    @WeldSetup
    WeldInitiator weld = WeldUtils.from(List.of(), List.of(BlockchainConfig.class, PropertiesHolder.class, Blockchain.class, BlockchainImpl.class, NtpTime.class, TimeService.class)).build();
    TransactionType type;

    @BeforeEach
    void setUp() {
        type = UpdateTransactionType.UPDATE_V2;
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.2.3", "32767.32767.32767"})
    void testValidateAttachment_successfully(String v) throws AplException.ValidationException {
        Transaction tx = createUpdateTx(v);

        type.validateAttachment(tx);
    }

    @ParameterizedTest
    @ValueSource(strings = {"32768.1.1", "120.32768.32767", "120.0.33000"})
    void testValidateAttachment_incorrectVersion(String incorrectVersionString) throws AplException.ValidationException {
        Transaction tx = createUpdateTx(incorrectVersionString);

        assertThrows(AplException.NotValidException.class, () -> type.validateAttachment(tx));
    }

    @Test
    void testCallCommonMethods() {
        assertThrows(UnsupportedOperationException.class, () -> ((UpdateTransactionType) type).getLevel());
        assertEquals(type.getName(), "UpdateV2");
        assertEquals(type.getSubtype(), 3);
    }

    private Transaction createUpdateTx(String v) {
        Transaction tx = mock(Transaction.class);
        Version version = new Version(v);
        UpdateV2Attachment attachment = new UpdateV2Attachment("htpps://update.zip", Level.CRITICAL, version, "somesite.com", BigInteger.ONE, new byte[128], Set.of(new PlatformSpec(Platform.ALL, Architecture.AMD64), new PlatformSpec(Platform.ALL, Architecture.X86), new PlatformSpec(Platform.MAC_OS, Architecture.ARM)));
        when(tx.getAttachment()).thenReturn(attachment);
        return tx;
    }
}
