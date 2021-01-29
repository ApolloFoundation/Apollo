/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
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
    public void write(Transaction transaction, WriteBuffer buffer) {
        buffer
            .write(transaction.getType().getSpec().getType())
            .write(getVersionSubtypeByte(transaction))
            .write(transaction.getTimestamp())
            .write(transaction.getDeadline())
            .write(transaction.getSenderPublicKey())
            .write(transaction.getType().canHaveRecipient() ? transaction.getRecipientId() : GenesisImporter.CREATOR_ID)
            .write(transaction.getAmountATM())
            .write(transaction.getFeeATM());

        if (transaction.referencedTransactionFullHash() != null) {
            buffer.write(transaction.referencedTransactionFullHash());
        } else {
            buffer.write(new byte[32]);
        }
        if (transaction.getVersion() < 2) {
            if (transaction.getSignature() != null) {
                buffer.write(transaction.getSignature().bytes());
            } else {
                buffer.write(new byte[Signature.ECDSA_SIGNATURE_SIZE]);
            }
        }

        buffer
            .write(getTransactionFlags(transaction))
            .write(transaction.getECBlockHeight())
            .write(transaction.getECBlockId());

        for (Appendix appendage : transaction.getAppendages()) {
            appendage.putBytes(buffer);
        }
        if (transaction.getVersion() >= 2) {
            if (transaction.getSignature() != null) {
                buffer.concat(transaction.getSignature().bytes());
            }
        }
    }
}
