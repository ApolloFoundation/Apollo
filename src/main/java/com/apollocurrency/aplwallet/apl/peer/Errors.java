/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl.peer;

final class Errors {

    final static String BLACKLISTED = "Your peer is blacklisted";
    final static String END_OF_FILE = "Unexpected token END OF FILE at position 0.";
    final static String UNKNOWN_PEER = "Your peer address cannot be resolved";
    final static String UNSUPPORTED_REQUEST_TYPE = "Unsupported request type!";
    final static String UNSUPPORTED_PROTOCOL = "Unsupported protocol!";
    final static String INVALID_ANNOUNCED_ADDRESS = "Invalid announced address";
    final static String SEQUENCE_ERROR = "Peer request received before 'getInfo' request";
    final static String MAX_INBOUND_CONNECTIONS = "Maximum number of inbound connections exceeded";
    final static String TOO_MANY_BLOCKS_REQUESTED = "Too many blocks requested";
    final static String DOWNLOADING = "Blockchain download in progress";
    final static String LIGHT_CLIENT = "Peer is in light mode";
    final static String CONNECTION_TIMEOUT = "Connection timed out";
    final static String UPDATING = "Peer is updating now.";

    private Errors() {} // never
}
