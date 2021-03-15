/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.util.io.WriteBuffer;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionUtils;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpList;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpWriteBuffer;
import lombok.extern.slf4j.Slf4j;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class TxSerializerV3Impl extends AbstractTxSerializer {

    public TxSerializerV3Impl(TxBContextImpl context) {
        super(context);
    }

    @Override
    public int write(Transaction transaction, WriteBuffer buffer) {
        int payloadSize;
        RlpWriteBuffer rlpWriteBuffer;
        if (buffer instanceof RlpWriteBuffer) {
            rlpWriteBuffer = (RlpWriteBuffer) buffer;
            payloadSize = rlpUnsignedTx(transaction, rlpWriteBuffer);
        } else {
            rlpWriteBuffer = new RlpWriteBuffer();
            payloadSize = rlpUnsignedTx(transaction, rlpWriteBuffer);
            buffer.concat(rlpWriteBuffer.toByteArray());
        }
        return payloadSize;
    }

    /**
     * Return RLP encoded transaction bytes.
     */
    private int rlpUnsignedTx(Transaction transaction, RlpWriteBuffer buffer) {
        int payloadSize = 0;
        //header
        buffer
            .write(transaction.getType().getSpec().getType())
            .write(TransactionUtils.getVersionSubtypeByte(transaction))
            .write(transaction.getChainId())
            .write(transaction.getDeadline())
            .write(transaction.getLongTimestamp())
            .write(transaction.getECBlockHeight())
            .write(transaction.getECBlockId())
            .write(transaction.getNonce())
            .write(transaction.getSenderPublicKey())
            .write(transaction.getRecipientId())
            .write(transaction.getAmount())
            .write(transaction.getFuelPrice())
            .write(transaction.getFuelLimit());

        //data part
        buffer.write(transaction.referencedTransactionFullHash() == null ? new byte[0] : transaction.referencedTransactionFullHash());

        payloadSize += buffer.size();

        RlpList.RlpListBuilder attachmentsList = RlpList.builder();
        for (Appendix appendage : transaction.getAppendages()) {
            appendage.putBytes(attachmentsList);
            payloadSize += appendage.getFullSize();
        }
        buffer.write(attachmentsList.build());
        //signature part
        if (transaction.getSignature() != null) {
            buffer.concat(transaction.getSignature().bytes());
            payloadSize += transaction.getSignature().getSize();
        }
        return payloadSize;
    }
}
