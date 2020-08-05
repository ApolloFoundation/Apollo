/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.request;

import java.util.UUID;

public class GetPeersRequest extends BaseP2PRequest {

    private static final String requestType = "getPeers";

    public GetPeersRequest(UUID chainId) {
        super(requestType, chainId);
    }

}
