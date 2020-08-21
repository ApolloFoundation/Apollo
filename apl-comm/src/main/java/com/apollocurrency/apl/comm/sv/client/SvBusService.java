/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.comm.sv.client;

import com.apollocurrency.apl.comm.sv.msg.SvBusHello;
import com.apollocurrency.apl.comm.sv.msg.SvBusRequest;
import com.apollocurrency.apl.comm.sv.msg.SvBusResponse;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Supervisor bus service, top part of communication API
 *
 * @author alukin@gmailcom
 */
public interface SvBusService {

    void addConnection(URI uri, boolean isDefault);

    <T extends SvBusResponse> void addResponseMapping(String path, Class<T> tClass);

    <T extends SvBusResponse> void addParametrizedResponseMapping(String path, Class<T> tClass, Class<?> paramClass);

    Map<URI, ConnectionStatus> getConnections();

    MessageDispatcher getDispatcher();

    <T extends SvBusResponse> T sendSync(SvBusRequest rq, String path, URI addr);

    CompletableFuture<SvBusResponse> sendAsync(SvBusRequest msg, String path, URI addr);

    ConnectionStatus getConnectionStaus(URI uri);

    void shutdown();

    void setMyInfo(SvBusHello info);

    URI getMyAddress();
}
