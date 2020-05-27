/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.supervisor.client;

import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusHello;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusRequest;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusResponse;

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
