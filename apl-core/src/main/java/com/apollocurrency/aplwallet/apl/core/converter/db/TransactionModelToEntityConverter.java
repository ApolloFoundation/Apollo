/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionEntity;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Prunable;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class TransactionModelToEntityConverter implements Converter<Transaction, TransactionEntity> {

    @Override
    public TransactionEntity apply(Transaction model) {
        TransactionEntity entity = applyWithoutAppendages(model);
        addAppendages(entity, model);
        return entity;
    }

    private TransactionEntity applyWithoutAppendages(Transaction model) {
        TransactionEntity entity = TransactionEntity.builder()
            .version(model.getVersion())
            .type(model.getType().getSpec().getType())
            .subtype(model.getType().getSpec().getSubtype())
            .id(model.getId())
            .timestamp(model.getTimestamp())
            .deadline(model.getDeadline())
            .amountATM(model.getAmountATM())
            .feeATM(model.getFeeATM())
            .referencedTransactionFullHash(model.referencedTransactionFullHash())
            .ecBlockHeight(model.getECBlockHeight())
            .ecBlockId(model.getECBlockId())
            .signatureBytes(model.getSignature().bytes())
            .blockId(model.getBlockId())
            .height(model.getHeight())
            .senderId(model.getSenderId())
            .recipientId(model.getRecipientId())
            .blockTimestamp(model.getBlockTimestamp())
            .fullHash(model.getFullHash())
            .index(model.getIndex())
            .senderPublicKey(model.getSenderPublicKey())

            .hasMessage(model.getMessage() != null)
            .hasEncryptedMessage(model.getEncryptedMessage() != null)
            .hasPublicKeyAnnouncement(model.getPublicKeyAnnouncement() != null)
            .hasEncryptToSelfMessage(model.getEncryptToSelfMessage() != null)
            .phased(model.getPhasing() != null)
            .hasPrunableMessage(model.hasPrunablePlainMessage())
            .hasPrunableEencryptedMessage(model.hasPrunableEncryptedMessage())
            .hasPrunableAttachment(model.getAttachment() instanceof Prunable)
            .errorMessage(model.getErrorMessage().orElse(null))

            .build();

        return entity;
    }

    private void addAppendages(TransactionEntity entity, Transaction model) {
        int bytesLength = 0;
        for (Appendix appendage : model.getAppendages()) {
            bytesLength += appendage.getSize();
        }
        if (bytesLength > 0) {
            ByteBuffer buffer = ByteBuffer.allocate(bytesLength);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            model.getAppendages().forEach(appendix -> appendix.putBytes(buffer));
            entity.setAttachmentBytes(buffer.array());
        }
    }
}
