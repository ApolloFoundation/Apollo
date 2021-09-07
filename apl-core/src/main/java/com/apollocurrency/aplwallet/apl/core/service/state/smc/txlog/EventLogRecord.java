/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.txlog;

import com.apollocurrency.aplwallet.apl.core.model.smc.AplContractEvent;
import com.apollocurrency.smc.txlog.Record;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class EventLogRecord implements Record {
    AplContractEvent event;

    @Override
    public SmcRecordType type() {
        return SmcRecordType.FIRE_EVENT;
    }
}
