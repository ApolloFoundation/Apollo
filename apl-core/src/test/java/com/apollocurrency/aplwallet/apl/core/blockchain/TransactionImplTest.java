/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.blockchain;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.model.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionUtils;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionWrapperHelper;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxSerializer;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ArbitraryMessageAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.types.messaging.ArbitraryMessageTransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;


class TransactionImplTest {
    @Test
    void create() {
        ArbitraryMessageAttachment arbitraryMessage = new ArbitraryMessageAttachment();
        PrunablePlainMessageAppendix prunableAppendix = new PrunablePlainMessageAppendix("Prunable test message", true);
        EncryptToSelfMessageAppendix encryptedAppendix = new EncryptToSelfMessageAppendix(new EncryptedData(new byte[64], new byte[16]), false, false);
        ArbitraryMessageTransactionType txType = new ArbitraryMessageTransactionType(mock(BlockchainConfig.class), mock(AccountService.class));
        Signature sig = mock(Signature.class);
        doReturn(new byte[64]).when(sig).bytes();
        TransactionImpl tx = new TransactionImpl.BuilderImpl((byte) 1, new byte[32], 100L, 5L, (short) 1440, arbitraryMessage, 250, txType)
            .appendix(prunableAppendix)
            .appendix(encryptedAppendix)
            .ecBlockData(new EcBlockData(1111, 200))
            .blockId(2222)
            .blockTimestamp(270)
            .signature(sig)
            .index((short) 0).build();


        // verify unable to get not initialized data (required TransactionImpl#sign method call
        assertThrows(IllegalStateException.class, () -> tx.getFullHash());
        assertThrows(IllegalStateException.class, () -> tx.getId());

        PayloadResult result = PayloadResult.createLittleEndianByteArrayResult();
        TxSerializer serializer = TxBContext.newInstance(mock(Chain.class)).createSerializer(1);
        serializer.serialize(TransactionWrapperHelper.createUnsignedTransaction(tx), result);

        // assert default size of attachments
        assertEquals(294, result.size());

        tx.sign(tx.getSignature(), result);

        // assert prunable full size less than default size
        int fullSize = TransactionUtils.calculateFullSize(tx, result.size());
        assertEquals(283, fullSize);
        assertEquals("0e1027383f91699ea81a4c70b3dd6f3b61eb17decca2768d470b4f7c44f93763", Convert.toHexString(tx.getFullHash()));
        assertEquals(-7031929642451267570L, tx.getId());
    }
}