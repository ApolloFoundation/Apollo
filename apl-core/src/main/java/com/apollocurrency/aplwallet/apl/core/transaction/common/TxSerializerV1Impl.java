/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.io.WriteBuffer;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import lombok.extern.slf4j.Slf4j;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionUtils.getTransactionFlags;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionUtils.getVersionSubtypeByte;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class TxSerializerV1Impl extends AbstractTxSerializer {

    public TxSerializerV1Impl(TxBContextImpl context) {
        super(context);
    }

    @Override
    public int write(Transaction transaction, WriteBuffer buffer) {
        int payloadSize = 0;
        buffer.write(transaction.getType().getSpec().getType());
        verifyBuffSize(1, buffer);
        buffer.write(getVersionSubtypeByte(transaction));
        verifyBuffSize(2, buffer);
        buffer.write(transaction.getTimestamp());
        verifyBuffSize(6, buffer);
        buffer.write(transaction.getDeadline());
        verifyBuffSize(8, buffer);
        buffer.write(transaction.getSenderPublicKey());
        verifyBuffSize(40, buffer);
        buffer.write(transaction.getType().canHaveRecipient() ? transaction.getRecipientId() : GenesisImporter.CREATOR_ID);
        verifyBuffSize(48, buffer);
        buffer.write(transaction.getAmountATM());
        verifyBuffSize(56, buffer);
        buffer.write(transaction.getFeeATM());
        verifyBuffSize(64, buffer);

        if (transaction.referencedTransactionFullHash() != null) {
            buffer.write(transaction.referencedTransactionFullHash());
        } else {
            buffer.write(new byte[32]);
        }
        verifyBuffSize(96, buffer);
        if (transaction.getVersion() < 2) {
            if (transaction.getSignature() != null) {
                buffer.write(transaction.getSignature().bytes());
            } else {
                buffer.write(new byte[Signature.ECDSA_SIGNATURE_SIZE]);
            }
        }
        verifyBuffSize(160, buffer);

        buffer
            .write(getTransactionFlags(transaction))
            .write(transaction.getECBlockHeight())
            .write(transaction.getECBlockId());
        verifyBuffSize(176, buffer);

        for (Appendix appendage : transaction.getAppendages()) {
            appendage.putBytes(buffer);
        }
        if (transaction.getVersion() >= 2) {
            if (transaction.getSignature() != null) {
                buffer.concat(transaction.getSignature().bytes());
            }
        }
        return buffer.size();
    }

    void verifyBuffSize(int bytes, WriteBuffer buffer) {
        int actualLength = buffer.toByteArray().length;
        if (actualLength != bytes) {
            log.error("Byte array serialization error: expected {}, actual {}, buff {}, trace {}", bytes, actualLength, buffer
                , ThreadUtils.lastNStacktrace(20));
        }
    }
}
