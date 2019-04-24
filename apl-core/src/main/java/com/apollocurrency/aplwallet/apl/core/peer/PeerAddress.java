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
public class PeerAddress {
    private InetAddress host;
    private String hostName;    
    private Integer port;
    private final PropertiesHolder propertiesHolder;

    
    public PeerAddress(PropertiesHolder propertiesHolder) {
        this.propertiesHolder = propertiesHolder;
        try {
            host = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException ex) {
        }
        port = getDefaultPeerPort();
    }
    
    public final Integer getDefaultPeerPort() {
        return propertiesHolder.getIntProperty("apl.networkPeerServerPort", Constants.DEFAULT_PEER_PORT);
    } 
    
    public void fromString(String addr){
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

    public void setHost(String host){
        hostName=host;
        try {
            this.host = InetAddress.getByName(host);
        } catch (UnknownHostException ex) {
        }
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
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
}
