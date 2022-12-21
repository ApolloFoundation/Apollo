/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.apollocurrency.aplwallet.apl.smc.events.SmcEvent;
import com.apollocurrency.aplwallet.apl.smc.events.SmcEventType;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractEvent;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractEventService;
import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventMessage;
import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventReceipt;
import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventResponse;
import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventSubscriptionRequest;
import com.apollocurrency.aplwallet.apl.smc.ws.subscription.SubscriptionManager;
import com.apollocurrency.smc.contract.vm.event.EventArguments;
import com.apollocurrency.smc.data.jsonmapper.JsonMapper;
import com.apollocurrency.smc.data.jsonmapper.event.EventJsonMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcEventServer implements SmcEventSocketListener {
    @Getter
    private final SubscriptionManager subscriptionManager;
    private SmcContractEventService eventServiceInt;
    private final JsonMapper jsonMapper;

    public SmcEventServer() {
        this.subscriptionManager = new SubscriptionManager();
        this.jsonMapper = new EventJsonMapper();
    }

    private SmcContractEventService lookupService() {
        return eventServiceInt;
    }

    @PostConstruct
    void init() {
        eventServiceInt = CDI.current().select(SmcContractEventService.class).get();
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
            throw new ContractNotFoundException(socket.getContract());
        }
    }

    @Override
    public void onMessage(SmcEventSocket socket, SmcEventSubscriptionRequest request) {
        SmcEventResponse response = process(socket, request);
        socket.sendWebSocket(response);
        if (request.getOperation() == SmcEventSubscriptionRequest.Operation.SUBSCRIBE_TEST) {
            sendMockEvent(socket, request);
        }
    }

    private SmcEventResponse process(SmcEventSocket socket, SmcEventSubscriptionRequest request) {
        var rc = validateRequest(request);
        if (rc != null) {
            return rc;
        }
        SmcEventResponse response;
        switch (request.getOperation()) {
            case SUBSCRIBE:
                //call subscription routine
                if (!subscriptionManager.addSubscription(socket.getContract(), socket, request)) {
                    response = SmcErrorReceipt.error(request.getRequestId(), SmcEventServerErrors.SUBSCRIPTION_ALREADY_REGISTERED,
                        request.getEvents().get(0).getSubscriptionId());
                } else {
                    response = new SmcEventReceipt(SmcEventReceipt.Status.OK, request.getRequestId());
                }
                break;
            case UNSUBSCRIBE:
                //call unsubscription routine
                if (!subscriptionManager.removeSubscription(socket.getContract(), socket, request)) {
                    response = SmcErrorReceipt.error(request.getRequestId(), SmcEventServerErrors.SUBSCRIPTION_NOT_REGISTERED,
                        request.getEvents().get(0).getSubscriptionId());
                } else {
                    response = new SmcEventReceipt(SmcEventReceipt.Status.OK, request.getRequestId());
                }
                break;
            case SUBSCRIBE_TEST:
                //nothing to do
                response = new SmcEventReceipt(SmcEventReceipt.Status.OK, request.getRequestId());
                break;
            default:
                response = SmcErrorReceipt.error(request.getRequestId(), SmcEventServerErrors.UNSUPPORTED_OPERATION, request.getOperation().name());
        }
        return response;
    }

    public SmcEventResponse validateRequest(SmcEventSubscriptionRequest request) {
        if (request.getRequestId() == null || request.getEvents().isEmpty()) {
            return SmcErrorReceipt.error(request.getRequestId(), SmcEventServerErrors.INVALID_REQUEST_ARGUMENTS);
        } else {
            return null;
        }
    }

    public void onSmcEventEmitted(@ObservesAsync @SmcEvent(SmcEventType.EMIT_EVENT) AplContractEvent event) {
        log.debug("Catch async cdi event={}", event);
        var args = jsonMapper.deserializer().deserialize(event.getState(), EventArguments.class);
        subscriptionManager.fire(event, args);
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
}
