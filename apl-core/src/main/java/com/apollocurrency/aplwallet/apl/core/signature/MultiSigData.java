/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpConverter;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpList;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpWriteBuffer;
import lombok.extern.slf4j.Slf4j;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.apollocurrency.aplwallet.apl.core.signature.MultiSig.KeyId.KEY_LENGTH;

/**
 * Multi-signature is a digital signature scheme.
 * It's a collection of distinct signatures from all users (participants)
 *
 * @author andrii.zinchenko@firstbridge.io
 */
@Slf4j
class MultiSigData implements MultiSig {
    private final SignatureParser parser;
    private final short count;
    private final List<String> signatureParams;
    private final Map<KeyId, byte[]> signaturesMap;
    private boolean verified = false;

    public MultiSigData(byte[] publicKey, byte[] signature) {
        this(1,List.of(), new Parser());
        addSignature(publicKey, signature);
    }

    public MultiSigData(int count, SignatureParser parser) {
        this(count, List.of(), parser);
    }

    public MultiSigData(int count, List<String> signatureParams, SignatureParser parser) {
        if (count < 1) {
            throw new IllegalArgumentException("The count is less than 1;");
        }
        this.count = (short) count;
        //TODO: parse the signatureParams
        this.signatureParams = signatureParams;
        this.parser = Objects.requireNonNull(parser);
        this.signaturesMap = new HashMap<>();
    }

    void setVerified(boolean verified) {
        this.verified = verified;
    }

    @Override
    public String getHexString() {
        return Convert.toHexString(bytes());
    }

    @Override
    public boolean isVerified() {
        return verified;
    }

    @Override
    public byte[] bytes() {
        return parser.bytes(this);
    }

    @Override
    public int getSize() {
        return parser.calcDataSize(this.getThresholdParticipantCount());
    }

    @Override
    public List<String> getParams() {
        return signatureParams;
    }

    @Override
    public int getThresholdParticipantCount() {
        return count;
    }

    @Override
    public int getActualParticipantCount() {
        return signaturesMap.keySet().size();
    }

    @Override
    public byte[] getSignature(byte[] publicKey) {
        return getSignature(new KeyIdImpl(publicKey));
    }

    @Override
    public byte[] getSignature(KeyId publicKeyId) {
        return signaturesMap.get(publicKeyId);
    }

    @Override
    public boolean isParticipant(byte[] publicKey) {
        return isParticipant(new KeyIdImpl(publicKey));
    }

    @Override
    public boolean isParticipant(KeyId publicKeyId) {
        return signaturesMap.containsKey(publicKeyId);
    }

    @Override
    public Set<KeyId> getPublicKeyIdSet() {
        return signaturesMap.keySet();
    }

    @Override
    public Map<KeyId, byte[]> signaturesMap() {
        return signaturesMap;
    }

    @Override
    public void addSignature(byte[] keyId, byte[] signature) {
        addSignature(new KeyIdImpl(keyId), signature);
    }

    @Override
    public void addSignature(KeyId keyId, byte[] signature) {
        signaturesMap.put(Objects.requireNonNull(keyId), Objects.requireNonNull(signature));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiSigData that = (MultiSigData) o;
        return count == that.count && Objects.equals(signatureParams, that.signatureParams) && Objects.equals(signaturesMap, that.signaturesMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(count, signatureParams, signaturesMap);
    }

    public static KeyId createKey(byte[] key) {
        return new KeyIdImpl(key);
    }

    private static class KeyIdImpl implements KeyId {
        private final byte[] key;

        private KeyIdImpl(byte[] key) {
            this.key = Arrays.copyOf(Objects.requireNonNull(key), KEY_LENGTH);
        }

        @Override
        public byte[] getKey() {
            return key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            KeyIdImpl keyId = (KeyIdImpl) o;
            return Arrays.equals(key, keyId.key);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(key);
        }
    }

    static class Parser implements SignatureParser {
        private static final byte[] MAGIC_BYTES = new byte[]{0x4d, 0x53, 0x49, 0x47};//magic='MSIG'
        private static final int MAGIC_DATA_LENGTH = 4;
        private static final int PAYLOAD_LENGTH = 4;
        private static final byte[] PAYLOAD_RESERVED = new byte[PAYLOAD_LENGTH];

        static {
            Arrays.fill(PAYLOAD_RESERVED, (byte) 0x0);
        }

        /**
         * Parse the byte array and build the multisig object
         *
         * @param bytes input data array
         * @return the multisig object
         */
        @Override
        public Signature parse(byte[] bytes) {
            return parse(ByteBuffer.wrap(bytes));
        }

        /**
         * Parse the byte array (V2 structure) and build the multisig object
         *
         * @param buffer input data array
         * @return the multisig object
         */
        @Override
        public Signature parse(ByteBuffer buffer) {
            MultiSigData multiSigData;
            try {
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                byte[] magic = new byte[MAGIC_DATA_LENGTH];
                buffer.get(magic);
                if (!Arrays.equals(MAGIC_BYTES, magic)) {
                    throw new SignatureParseException("The magic bytes don't match the parser version.");
                }
                byte[] payload = new byte[PAYLOAD_LENGTH];
                buffer.get(payload);
                short count = buffer.getShort();
                multiSigData = new MultiSigData(count, List.of("payload="+Convert.toHexString(payload)), this);

                for (int i = 0; i < count; i++) {
                    byte[] pkId = new byte[KEY_LENGTH];
                    buffer.get(pkId);
                    byte[] sig = new byte[ECDSA_SIGNATURE_SIZE];
                    buffer.get(sig);
                    multiSigData.addSignature(pkId, sig);
                }

                if (multiSigData.getPublicKeyIdSet().size() != count) {
                    throw new SignatureParseException("Wrong format of the attached multi-signature data." +
                        " The count doesn't match the data array length.");
                }
            } catch (BufferUnderflowException e) {
                String message = "Can't parse signature bytes, cause: " + e.getMessage();
                log.error(message);
                throw new SignatureParseException(message);
            }
            return multiSigData;
        }

        @Override
        public int calcDataSize(int count) {
            return MAGIC_DATA_LENGTH + PAYLOAD_LENGTH + 2 + (KEY_LENGTH + ECDSA_SIGNATURE_SIZE) * count;
        }

        @Override
        public byte[] bytes(Signature signature) {
            MultiSig multiSig = (MultiSig) signature;
            ByteBuffer buffer = ByteBuffer.allocate(calcDataSize(multiSig.getThresholdParticipantCount()));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.put(MAGIC_BYTES);
            buffer.put(PAYLOAD_RESERVED);
            buffer.putShort((short) multiSig.getThresholdParticipantCount());
            multiSig.signaturesMap().forEach((keyId, bytes) -> {
                buffer.put(keyId.getKey());
                buffer.put(bytes);
            });
            return buffer.array();
        }

    }

    static class ParserV3 extends Parser{

        /**
         * Parse the RLP encoded structure and build the multisig object
         *
         * @param reader the RLP encoded data reader
         * @return the multisig object
         */
        @Override
        public Signature parse(RlpReader reader) {
            MultiSigData multiSigData;
            try {
                List<String> signatureParams = reader.readList(RlpConverter::toString);
                RlpReader signaturesListReader = reader.readListReader();
                int count = signaturesListReader.size();
                multiSigData = new MultiSigData(count, signatureParams, this);

                for (int i = 0; i < count; i++) {
                    RlpReader signatureReader = signaturesListReader.readListReader();
                    if (signatureReader.size()!=2) {
                        throw new SignatureParseException("Wrong format of the attached multi-signature data.");
                    }
                    byte[] pkId = signatureReader.read();
                    byte[] sig = signatureReader.read();
                    multiSigData.addSignature(pkId, sig);
                }
            } catch (Exception e) {
                String message = "Can't parse RLP encoded bytes of signature, cause: " + e.getClass().getName()+":"+e.getMessage();
                log.error(message);
                throw new SignatureParseException(message);
            }
            return multiSigData;
        }

        @Override
        public byte[] bytes(Signature signature) {
            MultiSig multiSig = (MultiSig) signature;
            RlpWriteBuffer buffer = new RlpWriteBuffer();
            buffer.write(RlpList.ofStrings(multiSig.getParams()));
            RlpList.RlpListBuilder listBuilder = RlpList.builder();
            multiSig.signaturesMap()
                .forEach( (keyId, bytes) ->
                    listBuilder.add(RlpList.builder().add(keyId.getKey()).add(bytes).build())
                );
            buffer.write(listBuilder.build());

            return buffer.toByteArray();
        }

    }
}
