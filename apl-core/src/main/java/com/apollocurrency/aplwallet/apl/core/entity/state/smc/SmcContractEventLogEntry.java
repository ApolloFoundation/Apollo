/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.smc;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class SmcContractEventLogEntry extends DerivedEntity {
    private long logId;
    private long eventId;
    private long transactionId;

    private byte[] signature;//SHA256 hash(name+idxCount); 32 bytes
    private String entry;
    /**
     * the sequential index of the event within the blockchain transaction
     */
    private int txIdx;

    @Builder
    public SmcContractEventLogEntry(Long dbId, Integer height, long logId, long eventId, long transactionId, byte[] signature, String entry, int txIdx) {
        super(dbId, height);
        this.logId = logId;
        this.eventId = eventId;
        this.transactionId = transactionId;
        this.signature = signature;
        this.entry = entry;
        this.txIdx = txIdx;
    }
}
