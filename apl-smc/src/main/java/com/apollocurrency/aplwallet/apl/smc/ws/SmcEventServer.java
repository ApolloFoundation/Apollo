/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.apollocurrency.aplwallet.apl.smc.events.SmcEvent;
import com.apollocurrency.aplwallet.apl.smc.events.SmcEventType;
import com.apollocurrency.smc.contract.vm.event.SmcContractEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.WebSocketException;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcEventServer implements SmcEventSocketListener {

    @Getter
    private final SubscriptionManager subscriptionManager;
    private SmcEventService eventServiceInt;

    public SmcEventServer() {
        this.subscriptionManager = new SubscriptionManager();
    }

    private SmcEventService lookupService() {
/*
        if(eventServiceInt == null){
            eventServiceInt = CDI.current().select(SmcEventService.class).get();
        }
*/
        return eventServiceInt;
    }

    @PostConstruct
    void init() {
        eventServiceInt = CDI.current().select(SmcEventService.class).get();
    }

    @Override
    public void onClose(SmcEventSocket socket, int code, String reason) {
        subscriptionManager.remove(socket.getContract(), socket);
    }

    @Override
    public void onOpen(SmcEventSocket socket) {
        if (lookupService().isContractExist(socket.getContract())) {
            subscriptionManager.register(socket.getContract(), socket);
        } else {
            throw new WebSocketException("Contract not found, address=" + socket.getContract());
        }
    }

    @Override
    public void onMessage(SmcEventSocket socket, SmcEventSubscriptionRequest request) {
        SmcEventResponse response = process(request);
        socket.sendWebSocket(response);
    }

    private SmcEventResponse process(SmcEventSubscriptionRequest request) {
        SmcEventResponse response = SmcEventReceipt.OK;
        SmcEventReceipt.OK.setRequestId(request.getRequestId());
        switch (request.getOperation()) {
            case SUBSCRIBE:
                //call subscription routine

                break;
            case UNSUBSCRIBE:
                //call unsubscription routine

                break;
            default:
                response = SmcErrorReceipt.error(SmcEventServerErrors.UNSUPPORTED_OPERATION, request.getOperation().name());

        }
        return response;
    }

    public void validateRequest(SmcEventSocket socket, SmcEventSubscriptionRequest request) {
        var response = SmcErrorReceipt.error(SmcEventServerErrors.UNSUPPORTED_OPERATION, request.getOperation().name());
        socket.sendWebSocket(response);
    }

    public void onSmcEventEmitted(@Observes @SmcEvent(SmcEventType.EMIT_EVENT) SmcContractEvent contractEvent) {
        log.debug("Subscription: fire event={}", contractEvent);
        subscriptionManager.fire(contractEvent);
    }
}
