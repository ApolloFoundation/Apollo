/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.txlog;

import com.apollocurrency.smc.txlog.Record;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Builder
@Getter
@ToString
public class EventLogRecord implements Record {
    private final long address;//contract address
    private final long transactionId;
    private final byte[] signature;
    private final String name;
    private final byte idxCount;
    private final boolean anonymous;

    @Override
    public SmcRecordType type() {
        return SmcRecordType.FIRE_EVENT;
    }
}
