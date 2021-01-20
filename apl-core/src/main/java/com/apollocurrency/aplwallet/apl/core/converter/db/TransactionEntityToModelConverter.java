/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.converter.Converter;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionEntity;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.rest.service.PhasingAppendixFactory;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureParser;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureToolFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionBuilder;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.UnsupportedTransactionVersion;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class TransactionEntityToModelConverter implements Converter<TransactionEntity, Transaction> {
    private final TransactionTypeFactory factory;
    private final TransactionBuilder transactionBuilder;

    @Inject
    public TransactionEntityToModelConverter(TransactionTypeFactory factory, TransactionBuilder transactionBuilder) {
        this.factory = factory;
        this.transactionBuilder = transactionBuilder;
    }

    @Override
    public Transaction apply(TransactionEntity entity) {
        try {

            SignatureParser parser = SignatureToolFactory.selectParser(entity.getVersion()).orElseThrow(UnsupportedTransactionVersion::new);
            Signature signature = parser.parse(entity.getSignatureBytes());

            ByteBuffer buffer = null;
            if (entity.getAttachmentBytes() != null) {
                buffer = ByteBuffer.wrap(entity.getAttachmentBytes());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
            }
            TransactionType transactionType = factory.findTransactionType(entity.getType(), entity.getSubtype());
            TransactionImpl.BuilderImpl builder = transactionBuilder.newTransactionBuilder(entity.getVersion(), entity.getSenderPublicKey(),
                entity.getAmountATM(), entity.getFeeATM(), entity.getDeadline(),
                transactionType != null ? transactionType.parseAttachment(buffer) : null,
                entity.getTimestamp())

                .referencedTransactionFullHash(entity.getReferencedTransactionFullHash())
                .blockId(entity.getBlockId())
                .height(entity.getHeight())
                .id(entity.getId())
                .senderId(entity.getSenderId())
                .blockTimestamp(entity.getBlockTimestamp())
                .fullHash(entity.getFullHash())
                .ecBlockHeight(entity.getEcBlockHeight())
                .ecBlockId(entity.getEcBlockId())
                .dbId(entity.getDbId())
                .index(entity.getIndex())
                .recipientId(entity.getRecipientId());

            if (entity.isHasMessage()) {
                builder.appendix(new MessageAppendix(buffer));
            }
            if (entity.isHasEncryptedMessage()) {
                builder.appendix(new EncryptedMessageAppendix(buffer));
            }
            if (entity.isHasPublicKeyAnnouncement()) {
                builder.appendix(new PublicKeyAnnouncementAppendix(buffer));
            }
            if (entity.isHasEncryptToSelfMessage()) {
                builder.appendix(new EncryptToSelfMessageAppendix(buffer));
            }
            if (entity.isPhased()) {
                builder.appendix(PhasingAppendixFactory.build(buffer));
            }
            if (entity.isHasPrunableMessage()) {
                builder.appendix(new PrunablePlainMessageAppendix(buffer));
            }
            if (entity.isHasPrunableEencryptedMessage()) {
                builder.appendix(new PrunableEncryptedMessageAppendix(buffer));
            }

            Transaction transaction = builder.build();
            transaction.sign(signature);

            return transaction;

        } catch (AplException.NotValidException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
