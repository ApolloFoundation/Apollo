package com.apollocurrency.aplwallet.apl.core.service.state.smc.txlog;

import com.apollocurrency.smc.txlog.RecordType;

/**
 * @author andrew.zinchenko@gmail.com
 */
public enum SmcRecordType implements RecordType {
    TRANSFER,
    FIRE_EVENT;
}
