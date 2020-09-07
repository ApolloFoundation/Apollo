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
 * @author alukin@gmail.com
 */

public final class PeerAddress implements Comparable {
    private InetAddress ipAddress;
    private String hostName;
    private Integer port;

    private boolean valid = true;

    public PeerAddress(int port, String host) {
        setHost(host);
        this.port = port;
    }

    public PeerAddress() {
        this(Constants.DEFAULT_PEER_PORT, "0.0.0.0");
    }

    public PeerAddress(String hostWithPort) {
        fromString(hostWithPort);
    }

    public String formatIP6Address(String addr) throws UnknownHostException {
        String res = addr;
        InetAddress a = InetAddress.getByName(addr);
        if (a instanceof Inet6Address) {
            res = "[" + a.getHostAddress() + "]";
        }
        return res;
    }

    private void fromString(String addr) {
        if (addr == null || addr.isEmpty()) {
            addr = "localhost";
        }
        try {
            String a = addr.toLowerCase();
            if (!a.startsWith("http")) {
                a = "http://" + a;
            }
            URL u = new URL(a);
            hostName = u.getHost();
            setHost(hostName);
            port = u.getPort();
            if (port == -1) {
                port = Constants.DEFAULT_PEER_PORT;
            }
        } catch (MalformedURLException ex) {
            valid = false;
        }
    }

    public String getHost() {
        if (ipAddress instanceof Inet6Address) {
            return "[" + ipAddress.getHostAddress() + "]";
        } else {
            return ipAddress.getHostAddress();
        }
    }

    private void setHost(String host) {
        hostName = host;
        try {
            this.ipAddress = InetAddress.getByName(host);
        } catch (UnknownHostException ex) {
            valid = false;
        }
    }

    public Integer getPort() {
        return port;
    }

    public final void setPort(Integer port) {
        this.port = port;
    }

    public String getAddrWithPort() {
        if (ipAddress == null || port == null) {
            return "None";
        }
        if (ipAddress instanceof Inet6Address) {
            return "[" + ipAddress.getHostAddress() + "]:" + port.toString();
        } else {
            return ipAddress.getHostAddress() + ":" + port.toString();
        }
    }

    public InetAddress getInetAddress() {
        return ipAddress;
    }

    public String getHostName() {
        return hostName;
    }

    public boolean isLocal() {
        boolean res = (ipAddress.isAnyLocalAddress()
            || ipAddress.isLoopbackAddress()
            || ipAddress.isLinkLocalAddress());
        return res;
    }

    @Override
    public int compareTo(Object t) {
        int res = -1;
        if (t instanceof PeerAddress) {
            PeerAddress pa = (PeerAddress) t;
            if (pa.ipAddress.getHostAddress().equalsIgnoreCase(ipAddress.getHostAddress())
                && this.port.intValue() == pa.port.intValue()
            ) {
                res = 0;
            }

        }
        return res;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public String toString() {
        return "host:" + ipAddress + " name:" + hostName + " port: " + port;
    }

}
