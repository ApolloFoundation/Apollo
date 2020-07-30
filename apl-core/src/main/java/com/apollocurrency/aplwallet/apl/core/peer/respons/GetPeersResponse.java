/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.respons;

import lombok.Data;

import java.util.List;

@Data
public class GetPeersResponse implements PeerResponse {
    private List<String> peers;
    private List<String> services;
}
