/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.export;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.apollocurrency.aplwallet.apl.updater.export.event.StartUpdateEvent;
import com.apollocurrency.aplwallet.apl.updater.export.event.StartUpdateEventData;
import com.apollocurrency.aplwallet.apl.updater.export.event.StartUpdateEventType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class StartUpdateEventObserver {

    @Inject
    public StartUpdateEventObserver() {
    }

    public void onStartUpdateBefore(
        @Observes @StartUpdateEvent(StartUpdateEventType.BEFORE_SCRIPT) StartUpdateEventData startUpdateEventData) {
        log.debug("onStartUpdateBefore...");


        log.debug("onStartUpdateBefore DONE");
    }
}
