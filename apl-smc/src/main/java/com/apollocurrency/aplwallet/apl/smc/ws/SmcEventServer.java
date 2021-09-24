/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.apollocurrency.aplwallet.apl.smc.events.SmcEvent;
import com.apollocurrency.aplwallet.apl.smc.events.SmcEventType;
import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventMessage;
import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventReceipt;
import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventResponse;
import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventSubscriptionRequest;
import com.apollocurrency.aplwallet.apl.smc.ws.subscription.SubscriptionManager;
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
        if (request.getOperation() == SmcEventSubscriptionRequest.Operation.SUBSCRIBE_TEST) {
            sendMockEvent(socket, request);
        }
    }

    private void sendMockEvent(SmcEventSocket socket, SmcEventSubscriptionRequest request) {
        var event = request.getEvents().get(0);
        socket.sendWebSocket(SmcEventMessage.builder()
            .subscriptionId(event.getSubscriptionId())
            .address(socket.getContract().getHex())
            .name(event.getName())
            .signature(event.getSignature())
            .data("{\"sender\":\"0x111111111111\",\"receiver\":\"0x222222222222\",\"value\":\"0x12345\"}")
            .build());
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
            case SUBSCRIBE_TEST:
                //call subscription routine

                break;
            default:
                response = SmcErrorReceipt.error(request.getRequestId(), SmcEventServerErrors.UNSUPPORTED_OPERATION, request.getOperation().name());

        }
        return response;
    }

    public void validateRequest(SmcEventSocket socket, SmcEventSubscriptionRequest request) {
        var response = SmcErrorReceipt.error(request.getRequestId(), SmcEventServerErrors.INVALID_REQUEST_ARGUMENTS);
        socket.sendWebSocket(response);
    }

    public void onSmcEventEmitted(@Observes @SmcEvent(SmcEventType.EMIT_EVENT) SmcContractEvent contractEvent) {
        log.debug("Subscription: fire event={}", contractEvent);
        subscriptionManager.fire(contractEvent);
    }
}
