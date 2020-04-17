/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.http;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author al
 */
class NetworkAddress {

    private BigInteger netAddress;
    private BigInteger netMask;

    public NetworkAddress(String address) throws UnknownHostException {
        String[] addressParts = address.split("/");
        if (addressParts.length == 2) {
            InetAddress targetHostAddress = InetAddress.getByName(addressParts[0]);
            byte[] srcBytes = targetHostAddress.getAddress();
            netAddress = new BigInteger(1, srcBytes);
            int maskBitLength = Integer.valueOf(addressParts[1]);
            int addressBitLength = (targetHostAddress instanceof Inet4Address) ? 32 : 128;
            netMask = BigInteger.ZERO.setBit(addressBitLength).subtract(BigInteger.ONE).subtract(BigInteger.ZERO.setBit(addressBitLength - maskBitLength).subtract(BigInteger.ONE));
        } else {
            throw new IllegalArgumentException("Invalid address: " + address);
        }
    }

    boolean contains(BigInteger hostAddressToCheck) {
        return hostAddressToCheck.and(netMask).equals(netAddress);
    }

}
