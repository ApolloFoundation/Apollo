/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author alukin@gmail.com
 */
public class MyNetworkInterfaces {

    public List<InetAddress> getAdressList() {
        List<InetAddress> res = new ArrayList<>();
        Enumeration<NetworkInterface> intfs = getNetworkInterfaces();
        if (intfs != null) {
            while (intfs.hasMoreElements()) {
                NetworkInterface intf = intfs.nextElement();
                List<InterfaceAddress> intfAddrs = intf.getInterfaceAddresses();
                for (InterfaceAddress intfAddr : intfAddrs) {
                    InetAddress extAddr = intfAddr.getAddress();
                    res.add(extAddr);
                }
            }
        }
        return res;
    }

    public Map<String, byte[]> getInterfaces() {

        Map<String, byte[]> res = new HashMap<>();
        Enumeration<NetworkInterface> intfs = getNetworkInterfaces();
        if (intfs != null) {
            while (intfs.hasMoreElements()) {
                NetworkInterface intf = intfs.nextElement();
                try {
                    byte[] hwaddr = intf.getHardwareAddress();
                    String name = intf.getName();
                    res.put(name, hwaddr);
                } catch (SocketException ex) {
                }
            }
        }
        return res;
    }

    public List<InetAddress> getInterfaceAdressList(String name) {
        List<InetAddress> res = new ArrayList<>();
        Enumeration<NetworkInterface> intfs = getNetworkInterfaces();
        if (intfs != null) {
            while (intfs.hasMoreElements()) {
                NetworkInterface intf = intfs.nextElement();
                if (intf.getName().equalsIgnoreCase(name) || intf.getDisplayName().equalsIgnoreCase(name)) {
                    List<InterfaceAddress> intfAddrs = intf.getInterfaceAddresses();
                    for (InterfaceAddress intfAddr : intfAddrs) {
                        InetAddress extAddr = intfAddr.getAddress();
                        res.add(extAddr);
                    }
                }
            }
        }
        return res;
    }

    private Enumeration<NetworkInterface> getNetworkInterfaces() {
        Enumeration<NetworkInterface> intfs = null;
        try {
            intfs = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            //no interfaces
        }
        return intfs;
    }
}
