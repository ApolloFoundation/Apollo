/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer.events;

public enum BlockEventType {
    BLOCK_PUSHED,
    BLOCK_POPPED,
    BLOCK_GENERATED,
    BLOCK_SCANNED,
    RESCAN_BEGIN,
    RESCAN_END,
    BEFORE_BLOCK_ACCEPT,
    AFTER_BLOCK_ACCEPT,
    BEFORE_BLOCK_APPLY,
    AFTER_BLOCK_APPLY,
}
