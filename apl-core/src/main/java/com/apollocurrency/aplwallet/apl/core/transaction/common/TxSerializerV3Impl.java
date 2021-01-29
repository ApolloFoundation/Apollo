/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionUtils;
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
    public void write(Transaction transaction, WriteBuffer buffer) {

        //header
        buffer
            .write(transaction.getType().getSpec().getType())
            .write(TransactionUtils.getVersionSubtypeByte(transaction))
            //.write(transaction.getChainId())
            .write(transaction.getDeadline())
            //.write(transaction.getLongTimestamp())
            .write(transaction.getECBlockHeight())
            .write(transaction.getECBlockId())
            //.write(transaction.getNonce())
            .write(transaction.getSenderPublicKey())
            .write(transaction.getRecipientId())
        //.write(transaction.getAmount())
        //.write(transaction.getFuelPrice())
        //.write(transaction.getFuelLimit())
        ;
        //data part
/*
            buffer.write(referencedTransactionFullHash==null?new byte[0]:referencedTransactionFullHash);
            RlpList.RlpListBuilder attachmentsList = RlpList.builder();
            for (Appendix appendage : appendages) {
                appendage.putBytes(attachmentsList);
            }
            buffer.write(attachmentsList.build());
*/


    }
}
