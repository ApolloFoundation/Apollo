/*
 *  Copyright © 2019-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.entity.appdata;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DB record of known peer
 *
 * @author alukin@gmail.com
 */
@Data
@AllArgsConstructor
public class PeerEntity {

    /**
     * In previous version address is public IP, but now it could be IP:port or
     * 256 bit identity in hexadecimal form
     */
    private String address;
    /**
     * Services of node
     */
    private long services;
    /**
     *  time of last update in seconds
     */
    private int lastUpdated;
    /**
     * X.509 certificate of node in PEM format
     */
    private String x509pem;
    /**
     * Last seen on IP and port (IPv4 or IPv6)
     */
    private String ipAndPort;
}
