/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.blockchain;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

@Getter
@Builder
public class UnconfirmedTransactionEntity extends DerivedEntity {

    private long id;
    private int transactionHeight;
    private long arrivalTimestamp;
    private long feePerByte;
    private int expiration;
    private byte[] transactionBytes;
    private String prunableAttachmentJsonString;

    public UnconfirmedTransactionEntity() {
        super(null, -1);
    }

    public UnconfirmedTransactionEntity(long id
            , int transactionHeight, long arrivalTimestamp, long feePerByte, int expiration
            , byte[] transactionBytes, String prunableAttachmentJsonString) {
        super(null, transactionHeight);
        this.id = id;
        this.transactionHeight = transactionHeight;
        this.arrivalTimestamp = arrivalTimestamp;
        this.feePerByte = feePerByte;
        this.expiration = expiration;
        this.transactionBytes = transactionBytes;
        this.prunableAttachmentJsonString = prunableAttachmentJsonString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnconfirmedTransactionEntity)) return false;
        if (!super.equals(o)) return false;
        UnconfirmedTransactionEntity entity = (UnconfirmedTransactionEntity) o;
        return id == entity.id &&
                transactionHeight == entity.transactionHeight &&
                Arrays.equals(transactionBytes, entity.transactionBytes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), id, transactionHeight);
        result = 31 * result + Arrays.hashCode(transactionBytes);
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UnconfirmedTransactionEntity.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("transactionHeight=" + transactionHeight)
                .add("arrivalTimestamp=" + arrivalTimestamp)
                .add("feePerByte=" + feePerByte)
                .add("expiration=" + expiration)
                //.add("transactionBytes=" + Arrays.toString(transactionBytes))
                //.add("prunableAttachmentJsonString='" + prunableAttachmentJsonString + "'")
                .toString();
    }
}
