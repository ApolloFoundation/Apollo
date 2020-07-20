/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
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
    private final byte[] payload;
    private final short count;
    private final Map<KeyId, byte[]> signaturesMap;
    private final SignatureParser parser = new Parser();
    private boolean verified = false;

    public MultiSigData(byte[] publicKey, byte[] signature) {
        this(1, new byte[Parser.PAYLOAD_LENGTH]);
        Arrays.fill(payload, (byte) 0x0);
        addSignature(publicKey, signature);
    }

    public MultiSigData(int count) {
        this(count, Parser.PAYLOAD_RESERVED);
    }

    public MultiSigData(int count, byte[] payload) {
        if (count < 1) {
            throw new IllegalArgumentException("The count is less than 1;");
        }
        this.count = (short) count;
        this.payload = payload;
        this.signaturesMap = new HashMap<>();
    }

    void setVerified(boolean verified) {
        this.verified = verified;
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
        return parser.calcDataSize(this.getParticipantCount());
    }

    @Override
    public JSONObject getJsonObject() {
        return parser.getJsonObject(this);
    }

    @Override
    public byte[] getPayload() {
        return payload;
    }

    @Override
    public short getParticipantCount() {
        return count;
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
        private static final int PARSER_VERSION = 2;
        private static final byte[] MAGIC_BYTES = new byte[]{0x4d, 0x53, 0x49, 0x47};//magic='MSIG'
        private static final int MAGIC_DATA_LENGTH = 4;
        private static final int PAYLOAD_LENGTH = 4;
        private static final byte[] PAYLOAD_RESERVED = new byte[PAYLOAD_LENGTH];
        public static final String PAYLOAD_FIELD_NAME = "payload";
        public static final String PARTICIPANT_COUNT_FIELD_NAME = "participantCount";
        public static final String SIGNATURES_FIELD_NAME = "signatures";
        public static final String KEY_ID_FIELD_NAME = "keyId";

        static {
            Arrays.fill(PAYLOAD_RESERVED, (byte) 0x0);
        }

        /**
         * Parse the byte array and build the multisig object
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
                multiSigData = new MultiSigData(count, payload);

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
            ByteBuffer buffer = ByteBuffer.allocate(calcDataSize(multiSig.getParticipantCount()));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.put(MAGIC_BYTES);
            buffer.put(PAYLOAD_RESERVED);
            buffer.putShort(multiSig.getParticipantCount());
            multiSig.signaturesMap().forEach((keyId, bytes) -> {
                buffer.put(keyId.getKey());
                buffer.put(bytes);
            });
            return buffer.array();
        }

        @Override
        public JSONObject getJsonObject(Signature signature) {
            MultiSig multiSig = (MultiSig) signature;
            JSONObject json = new JSONObject();
            json.put(PAYLOAD_FIELD_NAME, Convert.toHexString(multiSig.getPayload()));
            json.put(PARTICIPANT_COUNT_FIELD_NAME, multiSig.getParticipantCount());
            JSONArray signatureArray = new JSONArray();
            multiSig.signaturesMap().forEach((keyId, bytes) -> {
                JSONObject item = new JSONObject();
                item.put(KEY_ID_FIELD_NAME, Convert.toHexString(keyId.getKey()));
                item.put(SIGNATURES_FIELD_NAME, Convert.toHexString(bytes));
                signatureArray.add(item);
            });
            json.put("signatures", signatureArray);

            return json;
        }

        /**
         * Parse the JSON object and build the multisig object
         *
         * @param json input JSONObject
         * @return the multisig object
         */
        @Override
        public Signature parse(JSONObject json) {
            byte[] payload = (byte[]) json.get(PAYLOAD_FIELD_NAME);
            short participantCount = (short) json.get(PARTICIPANT_COUNT_FIELD_NAME);
            MultiSigData multiSigData = new MultiSigData(participantCount, payload);
            JSONArray signatures = (JSONArray) json.get(SIGNATURES_FIELD_NAME);
            for (Object item : signatures) {
                byte[] keyId = Convert.parseHexString((String) ((JSONObject) item).get(KEY_ID_FIELD_NAME));
                byte[] sig = Convert.parseHexString((String) ((JSONObject) item).get(SIGNATURES_FIELD_NAME));
                multiSigData.addSignature(keyId, sig);
            }
            return multiSigData;
        }
    }
}
