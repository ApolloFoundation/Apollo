/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.event;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import static com.apollocurrency.smc.util.HexUtils.toHex;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Getter
@Builder
@EqualsAndHashCode
public class SmcContractEvent {
    private long id;
    private long contract; //contract address/id
    private long transactionId;

    private byte[] signature;//SHA256 hash(name+idxCount); 32 bytes
    private String name;
    private int idxCount;//indexed fields count
    private boolean anonymous;//is anonymous event
    private String entry;//serialized event parameters
    private int txIdx;//the sequential number within the transaction

    @Override
    public String toString() {
        return "SmcContractEvent{" +
            "id=" + id +
            ", contract=" + contract +
            ", transactionId=" + transactionId +
            ", signature=" + toHex(signature) +
            ", name='" + name + '\'' +
            ", idxCount=" + idxCount +
            ", anonymous=" + anonymous +
            ", entry='" + entry + '\'' +
            ", txIdx=" + txIdx +
            '}';
    }
}
