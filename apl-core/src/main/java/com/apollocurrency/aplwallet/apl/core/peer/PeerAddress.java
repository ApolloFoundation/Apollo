/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.util.Constants;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 *
 * @author alukin@gmail.com
 */
public final class PeerAddress implements Comparable{
    private InetAddress host;
    private String hostName;    
    private Integer port;
    
    public PeerAddress(int port, String host){
        setHost(host);
        this.port = port;      
    }
    
    public PeerAddress() {
       this(Constants.DEFAULT_PEER_PORT, "0.0.0.0");
    }
    
    public PeerAddress(String hostWithPort){
        fromString(hostWithPort);
    }
    
    
    public final void fromString(String addr){
        if(addr==null || addr.isEmpty()){
            addr="localhost";
        }
        try {
            String a=addr.toLowerCase();
            if(!a.startsWith("http")){
                a="http://"+a;
            }
            URL u = new URL(a);
            hostName=u.getHost();
            host=InetAddress.getByName(hostName);
            port=u.getPort();
            if(port==-1){
                port=Constants.DEFAULT_PEER_PORT;
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
    
    public boolean isLocal(){
       boolean res = ( host.isAnyLocalAddress() 
                     || host.isLoopbackAddress() 
                     || host.isLinkLocalAddress() );
      return res;
    }
    
    @Override
    public int compareTo(Object t) {
       int res = -1; 
       if(t!=null && t instanceof PeerAddress){
         PeerAddress pa = (PeerAddress)t;
         if( pa.host.getHostAddress().equalsIgnoreCase(host.getHostAddress()) 
             && this.port.intValue() == pa.port.intValue()
         ){
             res=0;
         }
             
       }  
       return res;
    }
    
    @Override
    public String toString(){
        return "host:"+host+" name:"+hostName+" port: "+port;
    }
}
