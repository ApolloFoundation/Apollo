
package com.apollocurrency.aplwallet.apl.util.supervisor.client;

import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusHello;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusRequest;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusResponse;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 
 * @author alukin@gmailcom
 */
public interface SvBusService {
    public void addConnection(URI uri, boolean isDefault);
    public Map<URI,ConnectionStatus> getConnections();
    public MessageDispatcher getDispatcher();
    public SvBusResponse sendSync(SvBusRequest rq, String path, URI addr);
    public CompletableFuture<SvBusResponse> sendAsync(SvBusRequest msg, String path, URI addr);
    public ConnectionStatus getConnectionStaus(URI uri);
    public void shutdown();
    public void setMyInfo(SvBusHello info);
    public URI getMyAddress();
}
