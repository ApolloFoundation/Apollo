package com.apollocurrency.aplwallet.apl.util.supervisor.client.impl;

import com.apollocurrency.aplwallet.apl.util.supervisor.client.ConnectionStatus;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusRequest;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusResponse;
import java.net.URI;
import java.util.Map;
import com.apollocurrency.aplwallet.apl.util.supervisor.client.MessageDispatcher;
import com.apollocurrency.aplwallet.apl.util.supervisor.client.SvBusService;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusHello;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author al
 */
public class SvBusServiceImpl implements SvBusService{

    private final MessageDispatcherImpl dispatcher;

    public SvBusServiceImpl() {
         SvBusHello hello = new SvBusHello();
         hello.clientPID = ProcessHandle.current().pid();
         hello.clientExePath="";
         hello.clientInfo="SvClient";
         dispatcher = new MessageDispatcherImpl();        
    }

    @Override
    public void addConnection(URI uri, boolean isDefault) {
        dispatcher.addConnection(uri, isDefault);
    }

    @Override
    public Map<URI, ConnectionStatus> getConnections() {
         Map<URI, SvBusClient> clients = dispatcher.getConnections();
         Map<URI,ConnectionStatus> statuses = new HashMap<>();
         for(URI key:clients.keySet()){
             statuses.put(key, clients.get(key).getState());
         }
         return statuses;
    }

    @Override
    public MessageDispatcher getDispatcher() {
       return dispatcher;
    }

    @Override
    public SvBusResponse sendSync(SvBusRequest rq, String path, URI addr) {
        SvBusResponse resp = dispatcher.sendSync(rq, path, addr);
        return resp;
    }

    @Override
    public CompletableFuture<SvBusResponse> sendAsync(SvBusRequest msg, String path, URI addr) {
        return dispatcher.sendAsync(msg, path, addr);
    }

    @Override
    public ConnectionStatus getConnectionStaus(URI uri) {
        Map<URI,ConnectionStatus> statuses = getConnections();
        ConnectionStatus res = statuses.get(uri);
        return res;
    }

    @Override
    public void shutdown() {
        dispatcher.shutdown();
    }

    @Override
    public URI getMyAddress() {
       return  dispatcher.getMyAddress();
    }

    @Override
    public void setMyInfo(SvBusHello info) {
        dispatcher.setMyInfo(info);
    }
    
}
