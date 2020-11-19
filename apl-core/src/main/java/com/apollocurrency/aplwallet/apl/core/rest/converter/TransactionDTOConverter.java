/*
 *  Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.rest.service.PhasingAppendixFactory;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureParser;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureToolFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.UnsupportedTransactionVersion;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class TransactionDTOConverter implements Converter<TransactionDTO, Transaction> {

    private final TransactionTypeFactory factory;

    @Inject
    public TransactionDTOConverter(TransactionTypeFactory factory) {
        this.factory = factory;
    }

    @Override
    public Transaction apply(TransactionDTO txDto) {
        try {
            byte[] senderPublicKey = Convert.parseHexString(txDto.getSenderPublicKey());
            byte version = txDto.getVersion() == null ? 0 : txDto.getVersion();

            SignatureParser signatureParser = SignatureToolFactory.selectParser(version).orElseThrow(UnsupportedTransactionVersion::new);
            Signature signature = signatureParser.parse(Convert.parseHexString(txDto.getSignature()));

            int ecBlockHeight = 0;
            long ecBlockId = 0;
            if (version > 0) {
                ecBlockHeight = txDto.getEcBlockHeight();
                ecBlockId = Convert.parseUnsignedLong(txDto.getEcBlockId());
            }

            TransactionType transactionType = factory.findTransactionType(txDto.getType(), txDto.getSubtype());
            if (transactionType == null) {
                throw new AplException.NotValidException("Invalid transaction type: " + txDto.getType() + ", " + txDto.getSubtype());
            }

            JSONObject attachmentData;
            if (!CollectionUtil.isEmpty(txDto.getAttachment())) {
                attachmentData = new JSONObject(txDto.getAttachment());
            } else {
                throw new AplException.NotValidException("Transaction dto {" + txDto + "} has no attachment");
            }

            //TODO APL-1663 Refactoring TransactionType. (make parseAttachment works with dto instead of json)
            AbstractAttachment attachment = transactionType.parseAttachment(attachmentData);
            attachment.bindTransactionType(transactionType);
            TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl(version, senderPublicKey,
                Convert.parseLong(txDto.getAmountATM()),
                Convert.parseLong(txDto.getFeeATM()),
                txDto.getDeadline(),
                attachment, txDto.getTimestamp(), transactionType)
                .referencedTransactionFullHash(txDto.getReferencedTransactionFullHash())
                .signature(signature)
                .ecBlockHeight(ecBlockHeight)
                .ecBlockId(ecBlockId);
            if (transactionType.canHaveRecipient()) {
                long recipientId = Convert.parseUnsignedLong(txDto.getRecipient());
                builder.recipientId(recipientId);
            }

            builder.appendix(MessageAppendix.parse(attachmentData));
            builder.appendix(EncryptedMessageAppendix.parse(attachmentData));
            builder.appendix(PublicKeyAnnouncementAppendix.parse(attachmentData));
            builder.appendix(EncryptToSelfMessageAppendix.parse(attachmentData));
            builder.appendix(PhasingAppendixFactory.parse(attachmentData));
            builder.appendix(PrunablePlainMessageAppendix.parse(attachmentData));
            builder.appendix(PrunableEncryptedMessageAppendix.parse(attachmentData));
            builder.signature(signature);

            return builder.build();
        } catch (RuntimeException | AplException.NotValidException e) {
            log.debug("Failed to parse transaction: " + txDto.toString());
            throw new RuntimeException(e);
        }
    }
}
