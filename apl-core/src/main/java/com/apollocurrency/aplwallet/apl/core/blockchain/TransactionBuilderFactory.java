/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.blockchain;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.rest.service.PhasingAppendixFactory;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureParser;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureToolFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionUtils;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionWrapperHelper;
import com.apollocurrency.aplwallet.apl.core.transaction.UnsupportedTransactionVersion;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingProcessingAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TaggedDataExtendAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TaggedDataUploadAttachment;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

@Singleton
@Slf4j
public class TransactionBuilderFactory {
    private final TransactionTypeFactory factory;
    private final BlockchainConfig blockchainConfig;
    private final TxBContext txBContext;

    @Inject
    public TransactionBuilderFactory(TransactionTypeFactory factory, BlockchainConfig blockchainConfig) {
        this.factory = factory;
        this.blockchainConfig = blockchainConfig;
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
    }

    public Transaction.Builder newUnsignedTransactionBuilder(int version, byte[] senderPublicKey, long amountATM, long feeATM, short deadline, Attachment attachment, int timestamp) {
        TransactionTypes.TransactionTypeSpec spec = attachment.getTransactionTypeSpec();
        TransactionType transactionType = factory.findTransactionType(spec.getType(), spec.getSubtype());
        attachment.bindTransactionType(transactionType);
        return new TransactionImpl.BuilderImpl((byte) version, senderPublicKey, amountATM, feeATM, deadline, (AbstractAttachment) attachment, timestamp, transactionType);
    }

    public Transaction newTransaction(byte[] bytes) throws AplException.NotValidException {
        TransactionImpl transaction = newTransactionBuilder(bytes).build();
        reSignTransaction(transaction);
        return transaction;
    }

    public Transaction newTransaction(byte[] bytes, JSONObject prunableAttachments) throws AplException.NotValidException {
        TransactionImpl.BuilderImpl builder = newTransactionBuilder(bytes);
        if (prunableAttachments != null) {
            ShufflingProcessingAttachment shufflingProcessing = ShufflingProcessingAttachment.parse(prunableAttachments);
            if (shufflingProcessing != null) {
                TransactionType transactionType = factory.findTransactionTypeBySpec(shufflingProcessing.getTransactionTypeSpec());
                builder.appendix(shufflingProcessing);
                shufflingProcessing.bindTransactionType(transactionType);
            }
            TaggedDataUploadAttachment taggedDataUploadAttachment = TaggedDataUploadAttachment.parse(prunableAttachments);
            if (taggedDataUploadAttachment != null) {
                TransactionType transactionType = factory.findTransactionTypeBySpec(taggedDataUploadAttachment.getTransactionTypeSpec());
                taggedDataUploadAttachment.bindTransactionType(transactionType);
                builder.appendix(taggedDataUploadAttachment);
            }
            TaggedDataExtendAttachment taggedDataExtendAttachment = TaggedDataExtendAttachment.parse(prunableAttachments);
            if (taggedDataExtendAttachment != null) {
                TransactionType transactionType = factory.findTransactionTypeBySpec(taggedDataExtendAttachment.getTransactionTypeSpec());
                taggedDataExtendAttachment.bindTransactionType(transactionType);
                builder.appendix(taggedDataExtendAttachment);
            }
            PrunablePlainMessageAppendix prunablePlainMessage = PrunablePlainMessageAppendix.parse(prunableAttachments);
            if (prunablePlainMessage != null) {
                builder.appendix(prunablePlainMessage);
            }
            PrunableEncryptedMessageAppendix prunableEncryptedMessage = PrunableEncryptedMessageAppendix.parse(prunableAttachments);
            if (prunableEncryptedMessage != null) {
                builder.appendix(prunableEncryptedMessage);
            }
        }
        TransactionImpl transaction = builder.build();
        reSignTransaction(transaction);
        return transaction;
    }

    public Transaction newTransaction(TransactionDTO txDto) {
        TransactionImpl transaction = newTransactionBuilder(txDto).build();
        reSignTransaction(transaction);
        return transaction;
    }

    /**
     * Use newTransaction(TransactionDTO txDto) or com.apollocurrency.aplwallet.apl.core.rest.converter.TransactionDTOConverter
     */
    @Deprecated
    public Transaction newTransaction(JSONObject transactionData) throws AplException.NotValidException {
        TransactionImpl.BuilderImpl builder = newTransactionBuilder(transactionData);
        TransactionImpl transaction = builder.build();
        reSignTransaction(transaction);
        long id = Long.parseUnsignedLong((String) transactionData.get("id"));
        if (id != transaction.getId()) {
            PayloadResult unsignedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
            txBContext.createSerializer(transaction.getVersion())
                .serialize(
                    TransactionWrapperHelper.createUnsignedTransaction(transaction)
                    , unsignedTxBytes
                );
            // incorrect deserialization case
            throw new AplException.NotValidException("Transaction " + builder.toString() + ", unsigned bytes - " + Convert.toHexString(unsignedTxBytes.getBuffer().toByteArray()) +" has different id " + transaction.getId() + ", id from peer " + id + ", peer data " + transactionData.toJSONString());
        }
        return transaction;
    }

    private void reSignTransaction(TransactionImpl transaction) {
        //re-signe transaction
        PayloadResult unsignedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion())
            .serialize(
                TransactionWrapperHelper.createUnsignedTransaction(transaction)
                , unsignedTxBytes
            );

        transaction.sign(transaction.getSignature(), unsignedTxBytes);
    }

    TransactionImpl.BuilderImpl newTransactionBuilder(byte[] bytes) throws AplException.NotValidException {
        byte version = TransactionUtils.getVersion(bytes[1]);
        if (version < 3) {
            return newTransactionV2Builder(bytes);
        } else {
            RlpReader reader = new RlpReader(bytes);
            return newTransactionV3Builder(reader);
        }
    }

    private TransactionImpl.BuilderImpl newTransactionV2Builder(byte[] bytes) throws AplException.NotValidException {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            byte type = buffer.get();
            byte subtype = buffer.get();
            byte version = TransactionUtils.getVersion(subtype);
            subtype = TransactionUtils.getSubtype(subtype);
            Signature signature = null;
            SignatureParser signatureParser = SignatureToolFactory.selectParser(version).orElseThrow(UnsupportedTransactionVersion::new);
            int timestamp = buffer.getInt();
            short deadline = buffer.getShort();
            byte[] senderPublicKey = new byte[32];
            buffer.get(senderPublicKey);
            long recipientId = buffer.getLong();
            long amountATM = buffer.getLong();
            long feeATM = buffer.getLong();
            byte[] referencedTransactionFullHash = new byte[32];
            buffer.get(referencedTransactionFullHash);
            referencedTransactionFullHash = Convert.emptyToNull(referencedTransactionFullHash);
            if (version < 2) {
                signature = signatureParser.parse(buffer);
            }
            int flags = 0;
            int ecBlockHeight = 0;
            long ecBlockId = 0;
            if (version > 0) {
                flags = buffer.getInt();
                ecBlockHeight = buffer.getInt();
                ecBlockId = buffer.getLong();
            }
            TransactionType transactionType = factory.findTransactionType(type, subtype);
            if (transactionType == null) {
                throw new AplException.NotValidException("Wrong transaction spec: type=" + type + ", subtype=" + subtype + ", version=" + version);
            }

            AbstractAttachment attachment = transactionType.parseAttachment(buffer);
            attachment.bindTransactionType(transactionType);
            TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl(version, senderPublicKey, amountATM, feeATM,
                deadline, attachment, timestamp, transactionType)
                .referencedTransactionFullHash(referencedTransactionFullHash)
                .ecBlockHeight(ecBlockHeight)
                .ecBlockId(ecBlockId);
            if (transactionType.canHaveRecipient()) {
                builder.recipientId(recipientId);
            }
            int position = 1;
            if ((flags & position) != 0 || (version == 0 && transactionType.getSpec() == TransactionTypes.TransactionTypeSpec.ARBITRARY_MESSAGE)) {
                builder.appendix(new MessageAppendix(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new EncryptedMessageAppendix(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new PublicKeyAnnouncementAppendix(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new EncryptToSelfMessageAppendix(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(PhasingAppendixFactory.build(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new PrunablePlainMessageAppendix(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new PrunableEncryptedMessageAppendix(buffer));
            }

            if (version == 2) {//read transaction multi-signature V2
                signature = signatureParser.parse(buffer);
            }
            builder.signature(signature);
            if (buffer.hasRemaining()) {
                throw new AplException.NotValidException("Transaction bytes too long, " + buffer.remaining() + " extra bytes");
            }
            return builder;
        } catch (RuntimeException e) {
            log.debug("Failed to parse transaction bytes: " + Convert.toHexString(bytes));
            throw e;
        }
    }

    private TransactionImpl.BuilderImpl newTransactionV3Builder(RlpReader reader) throws AplException.NotValidException {
        try {
            //header
            byte type = reader.readByte();
            byte subtype = reader.readByte();
            byte version = TransactionUtils.getVersion(subtype);
            subtype = TransactionUtils.getSubtype(subtype);

            String chainId = reader.readString();
            int deadline = reader.readInt();
            long timestamp = reader.readLong();
            int ecBlockHeight = reader.readInt();
            long ecBlockId = reader.readLong();
            BigInteger nonce = reader.readBigInteger();
            byte[] senderPublicKey = reader.read();
            long recipientId = reader.readLong();
            BigInteger amount = reader.readBigInteger();
            BigInteger fuelPrice = reader.readBigInteger();
            BigInteger fuelLimit = reader.readBigInteger();

            //data part
            byte[] referencedTransactionFullHash = reader.read();
            referencedTransactionFullHash = Convert.emptyToNull(referencedTransactionFullHash);

            TransactionType transactionType = factory.findTransactionType(type, subtype);
            if(transactionType == null){
                throw new AplException.NotValidException("Wrong transaction spec: type="+type+", subtype="+subtype+", version="+version);
            }

            //attachments
            AbstractAttachment attachment=null;
            List<AbstractAppendix> appendages = new ArrayList<>();
            RlpReader attachmentsListReader = reader.readListReader();
            while (attachmentsListReader.hasNext()){
                RlpReader attachmentReader = attachmentsListReader.readListReader();
                int appendixFlag = attachmentReader.readInt();
                if (appendixFlag == 0 ){//transaction attachment
                    attachment = transactionType.parseAttachment(attachmentReader);
                    attachment.bindTransactionType(transactionType);
                }else{//transaction appendages
                    AbstractAppendix appendix = readAppendix(appendixFlag, attachmentReader);
                    if(appendix!=null){
                        appendages.add(appendix);
                    }
                }
                if(attachmentReader.hasNext()){
                    throw new AplException.NotValidException("Wrong transaction attachment structure: type="+type+", subtype="+subtype+", version="+version);
                }
            }

            TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl(chainId, transactionType, version,
                senderPublicKey, nonce,amount, fuelLimit, fuelPrice,deadline, timestamp, attachment);

            appendages.forEach(builder::appendix);

            builder.referencedTransactionFullHash(referencedTransactionFullHash)
                .ecBlockHeight(ecBlockHeight)
                .ecBlockId(ecBlockId);

            if (transactionType.canHaveRecipient()) {
                builder.recipientId(recipientId);
            }

            Signature signature = null;
            //read transaction multi-signature V2
            SignatureParser signatureParser = SignatureToolFactory.selectParser(version).orElseThrow(UnsupportedTransactionVersion::new);
            signature = signatureParser.parse(reader);

            builder.signature(signature);
            if (reader.hasNext()) {
                throw new AplException.NotValidException("Transaction bytes too long");
            }
            return builder;
        } catch (RuntimeException e) {
            log.debug("Failed to parse RLP-encoded transaction,  reader: " + reader);
            throw e;
        }
    }

    private AbstractAppendix readAppendix(int flag, RlpReader reader) throws AplException.NotValidException {
        switch (flag){
            case 1:
                return new MessageAppendix(reader);
            case 2:
                return new EncryptedMessageAppendix(reader);
            case 4:
                return new PublicKeyAnnouncementAppendix(reader);
            case 8:
                return new EncryptToSelfMessageAppendix(reader);
            case 16:
                //TODO: should be implemented
                //PhasingAppendixFactory.build(reader));
                break;
            case 32:
                //TODO: should be implemented
                //new PrunablePlainMessageAppendix(reader));
                break;
            case 64:
                //TODO: should be implemented
                //new PrunableEncryptedMessageAppendix(reader));
                break;
            default:
                throw new AplException.NotValidException("Unexpected value: " + flag);
        }
        return null;
    }

    private TransactionImpl.BuilderImpl newTransactionBuilder(TransactionDTO txDto) {
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
            return builder;
        } catch (RuntimeException | AplException.NotValidException e) {
            log.debug("Failed to parse transaction: " + txDto.toString());
            throw new RuntimeException(e);
        }
    }

    /**
     * @deprecated Use newTransactionBuilder(TransactionDTO txDto) or com.apollocurrency.aplwallet.apl.core.rest.converter.TransactionDTOConverter
     */
    @Deprecated
    private TransactionImpl.BuilderImpl newTransactionBuilder(JSONObject transactionData) throws AplException.NotValidException {
        try {
            byte type = ((Number) transactionData.get("type")).byteValue();
            byte subtype = ((Number) transactionData.get("subtype")).byteValue();
            int timestamp = ((Number) transactionData.get("timestamp")).intValue();
            short deadline = ((Number) transactionData.get("deadline")).shortValue();
            byte[] senderPublicKey = Convert.parseHexString((String) transactionData.get("senderPublicKey"));
            long amountATM = transactionData.containsKey("amountATM") ? Convert.parseLong(transactionData.get("amountATM")) : Convert.parseLong(transactionData.get("amountNQT"));
            long feeATM = transactionData.containsKey("feeATM") ? Convert.parseLong(transactionData.get("feeATM")) : Convert.parseLong(transactionData.get("feeNQT"));
            String referencedTransactionFullHash = (String) transactionData.get("referencedTransactionFullHash");
            Number versionValue = (Number) transactionData.get("version");
            byte version = versionValue == null ? 0 : versionValue.byteValue();

            SignatureParser signatureParser = SignatureToolFactory.selectParser(version).orElseThrow(UnsupportedTransactionVersion::new);
            ByteBuffer signatureBuffer = ByteBuffer.wrap(Convert.parseHexString((String) transactionData.get("signature")));
            Signature signature = signatureParser.parse(signatureBuffer);

            JSONObject attachmentData = (JSONObject) transactionData.get("attachment");
            int ecBlockHeight = 0;
            long ecBlockId = 0;
            if (version > 0) {
                ecBlockHeight = ((Number) transactionData.get("ecBlockHeight")).intValue();
                ecBlockId = Convert.parseUnsignedLong((String) transactionData.get("ecBlockId"));
            }

            TransactionType transactionType = factory.findTransactionType(type, subtype);
            if (transactionType == null) {
                throw new AplException.NotValidException("Invalid transaction type: " + type + ", " + subtype);
            }
            AbstractAttachment attachment = transactionType.parseAttachment(attachmentData);
            attachment.bindTransactionType(transactionType);
            TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl(version, senderPublicKey,
                amountATM, feeATM, deadline,
                attachment, timestamp, transactionType)
                .referencedTransactionFullHash(referencedTransactionFullHash)
                .ecBlockHeight(ecBlockHeight)
                .ecBlockId(ecBlockId);
            if (transactionType.canHaveRecipient()) {
                long recipientId = Convert.parseUnsignedLong((String) transactionData.get("recipient"));
                builder.recipientId(recipientId);
            }
            if (attachmentData != null) {
                builder.appendix(MessageAppendix.parse(attachmentData));
                builder.appendix(EncryptedMessageAppendix.parse(attachmentData));
                builder.appendix(PublicKeyAnnouncementAppendix.parse(attachmentData));
                builder.appendix(EncryptToSelfMessageAppendix.parse(attachmentData));
                builder.appendix(PhasingAppendixFactory.parse(attachmentData));
                builder.appendix(PrunablePlainMessageAppendix.parse(attachmentData));
                builder.appendix(PrunableEncryptedMessageAppendix.parse(attachmentData));
            }
            builder.signature(signature);
            return builder;
        } catch (RuntimeException e) {
            log.debug("Failed to parse transaction: " + transactionData.toJSONString());
            throw e;
        }
    }
}
