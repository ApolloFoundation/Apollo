package com.apollocurrency.aplwallet.apl.core.entity.blockchain;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

@Getter
public class MandatoryTransactionEntity {
    private final long dbId;
    private final long id;
    private final byte[] transactionBytes;
    private final byte[] requiredTxHash;

    public MandatoryTransactionEntity(long id, byte[] transactionBytes, byte[] requiredTxHash) {
        this(0, id, transactionBytes, requiredTxHash);
    }

    public MandatoryTransactionEntity(long dbId, long id, byte[] transactionBytes, byte[] requiredTxHash) {
        this.dbId = dbId;
        this.id = id;
        this.requiredTxHash = requiredTxHash;
        this.transactionBytes = transactionBytes;
    }

    @Override
    public String toString() {
        return "MandatoryTransaction{" +
                "transaction id=" + id +
                ", dbId=" + dbId +
                ", requiredTxHash=" + Convert.toHexString(requiredTxHash) +
                ", transactionBytes=" + Convert.toHexString(transactionBytes) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MandatoryTransactionEntity)) return false;
        MandatoryTransactionEntity that = (MandatoryTransactionEntity) o;
        return dbId == that.dbId &&
                id == that.id &&
                Arrays.equals(requiredTxHash, that.requiredTxHash) &&
                Arrays.equals(transactionBytes, that.transactionBytes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(dbId, id);
        result = 31 * result + Arrays.hashCode(requiredTxHash);
        result = 31 * result + Arrays.hashCode(transactionBytes);
        return result;
    }

}
