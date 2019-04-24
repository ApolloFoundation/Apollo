/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 *
 * @author alukin@gmail.com
 */
public final class PeerAddress {
    private InetAddress host;
    private String hostName;    
    private Integer port;
    private final PropertiesHolder propertiesHolder;
    
    public PeerAddress(PropertiesHolder propertiesHolder, int port, String host){
        this.propertiesHolder = propertiesHolder;
        setHost(host);
        this.port = port;      
    }
    
    public PeerAddress(PropertiesHolder propertiesHolder) {
       this(propertiesHolder, 0, "127.0.0.1");
       setPort(getDefaultPeerPort());
    }
    
    public PeerAddress(PropertiesHolder propertiesHolder, String hostWithPort){
        this(propertiesHolder);
        fromString(hostWithPort);
    }
    
    public final Integer getDefaultPeerPort() {
        return propertiesHolder.getIntProperty("apl.networkPeerServerPort", Constants.DEFAULT_PEER_PORT);
    } 
    
    public final void fromString(String addr){
        try {
            String a=addr.toLowerCase();
            if(!a.startsWith("http")){
                a="http://"+a;
            }
            URL u = new URL(a);
            host=InetAddress.getByName(u.getHost());
            port=u.getPort();
            if(port==-1){
                port=getDefaultPeerPort();
            }
        } catch (MalformedURLException | UnknownHostException ex) {           
        }
    }

    public String getHost() {
        return host.getHostAddress();
    }

    public final void setHost(String host){
        hostName=host;
        try {
            this.host = InetAddress.getByName(host);
        } catch (UnknownHostException ex) {
        }
    }

    public Integer getPort() {
        return port;
    }

    public final void setPort(Integer port) {
        this.port = port;
    }
    
    public String getAddrWithPort(){
        if (host instanceof Inet6Address){
          return "["+host.getHostAddress()+"]:"+port.toString();   
        } else{
          return host.getHostAddress()+":"+port.toString();
        }
    }
    public InetAddress getInetAddress(){
        return host;
    }
    public String getHostName(){
        return hostName;
    }
}
