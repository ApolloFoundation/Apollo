/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.txlog;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
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
public class TransferRecord implements Record {
    private final long sender;
    private final long recipient;
    private final long value;
    private final LedgerEvent event;
    private final long transaction;

    @Override
    public SmcRecordType type() {
        return SmcRecordType.TRANSFER;
    }
}
