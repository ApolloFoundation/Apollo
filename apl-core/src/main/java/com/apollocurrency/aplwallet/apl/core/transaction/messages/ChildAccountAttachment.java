/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AddressScope;
import com.apollocurrency.aplwallet.apl.core.transaction.ChildAccount;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.Getter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Getter
public class ChildAccountAttachment extends AbstractAttachment {
    private final short childCount;
    private final List<byte[]> childPublicKey;
    private final AddressScope addressScope;

    public ChildAccountAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);

        buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.addressScope = AddressScope.from(buffer.get());
        this.childCount = buffer.getShort();
        childPublicKey = new LinkedList<>();
        for(int i=0;i<childCount;i++){
            byte[] key = new byte[32];
            buffer.get(key);
            childPublicKey.add(key);
        }
    }

    public ChildAccountAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.addressScope = AddressScope.from(((Long) attachmentData.get("addressScope")).intValue());
        this.childCount = (short) (((Long) attachmentData.get("childCount")).intValue());
        childPublicKey = new LinkedList<>();
        JSONArray keys = (JSONArray) attachmentData.get("childPublicKeys");
        for (Object publicKey : keys) {
            byte[] key = Convert.parseHexString(publicKey.toString());
            childPublicKey.add(key);
        }
    }

    public ChildAccountAttachment(AddressScope addressScope, int childCount, List<byte[]> childPublicKey) {
        this.addressScope = addressScope;
        this.childCount = (short) childCount;
        this.childPublicKey = childPublicKey;
    }

    public byte[] dataBytes() {
        ByteBuffer buff = ByteBuffer.allocate(getMySize());
        putDataBytes(buff);
        return buff.array();
    }

    private void putDataBytes(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(addressScope.getCode());
        buffer.putShort(childCount);
        childPublicKey.forEach(buffer::put);
    }

    @Override
    public int getMySize() {
        return 1 + 2 + childCount*32;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        putDataBytes(buffer);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("addressScope", (int) addressScope.getCode());
        attachment.put("childCount", childCount);
        JSONArray childKeyArray = new JSONArray();
        for (byte[] key : this.childPublicKey) {
            childKeyArray.add(Convert.toHexString(key));
        }
        attachment.put("childPublicKeys", childKeyArray);
    }

    @Override
    public TransactionType getTransactionType() {
        return ChildAccount.CREATE_CHILD;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChildAccountAttachment that = (ChildAccountAttachment) o;
        return childCount == that.childCount &&
            Arrays.deepEquals(this.childPublicKey.toArray(), that.childPublicKey.toArray()) &&
            addressScope == that.addressScope;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(new Object[]{childCount, childPublicKey.toArray(), addressScope});
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ChildAccountAttachment.class.getSimpleName() + "[", "]")
            .add("childCount=" + childCount)
            .add("childPublicKey=[" + childPublicKey.stream().map(Convert::toHexString).collect(Collectors.joining(",")) + "]")
            .add("addressScope=" + addressScope.name())
            .toString();
    }
}
