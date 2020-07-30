/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.respons;

import lombok.Data;

import java.util.List;

@Data
public class GetNextBlockIdsResponse implements PeerResponse {

    private List<String> nextBlockIds;

}
