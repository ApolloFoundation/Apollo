/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.observer;

import com.apollocurrency.aplwallet.apl.core.account.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEvent;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class PeerObserver {

    private PeersService peersService;

    @Inject
    public PeerObserver(PeersService peersService) {
        this.peersService = peersService;
    }

    public void onAccountBalance(@Observes @AccountEvent(AccountEventType.BALANCE) Account account) {
        log.trace("Catch event {} accaount={}", AccountEventType.BALANCE, account);
        peersService.getAllConnectablePeers().forEach(peer -> {
            if (peer.getHallmark() != null && peer.getHallmark().getAccountId() == account.getId()) {
                peersService.notifyListeners(peer, PeersService.Event.WEIGHT);
            }
        });
    }

}
