/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.endpoint;

public final class Errors {

    public final static String BLACKLISTED = "Your peer is blacklisted";
    public final static String END_OF_FILE = "Unexpected token END OF FILE at position 0.";
    public final static String UNKNOWN_PEER = "Your peer address cannot be resolved";
    public final static String UNSUPPORTED_REQUEST_TYPE = "Unsupported request type!";
    public final static String UNSUPPORTED_PROTOCOL = "Unsupported protocol!";
    public final static String INVALID_ANNOUNCED_ADDRESS = "Invalid announced address";
    public final static String INVALID_CHAINID = "Invalid chain Id";
    public final static String INVALID_APPLICATION = "Invalid Application, must be apl";
    public final static String SEQUENCE_ERROR = "Peer request received before 'getInfo' request";
    public final static String MAX_INBOUND_CONNECTIONS = "Maximum number of inbound connections exceeded";
    public final static String TOO_MANY_BLOCKS_REQUESTED = "Too many blocks requested";
    public final static String NO_BLOCK_ID_LIST = "Block id list not supplied within request";
    public final static String DOWNLOADING = "Blockchain download in progress";
    public final static String LIGHT_CLIENT = "Peer is in light mode";
    public final static String CONNECTION_TIMEOUT = "Connection timed out";
    public final static String CHAIN_ID_ERROR = "Your peer connected to different chain!";
    public final static String UPDATING = "Peer is updating now.";
    public final static String NO_FILE = "File not found";

    private Errors() {
    } // never
}
