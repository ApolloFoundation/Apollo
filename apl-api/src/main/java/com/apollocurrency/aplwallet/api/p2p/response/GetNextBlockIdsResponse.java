/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.response;

import lombok.Data;

import java.util.List;

@Data
public class GetNextBlockIdsResponse extends BaseP2PResponse {

    private List<String> nextBlockIds;

}
