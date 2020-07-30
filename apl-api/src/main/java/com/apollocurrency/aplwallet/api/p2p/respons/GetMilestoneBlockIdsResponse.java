/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.respons;

import lombok.Data;

import java.util.List;

@Data
public class GetMilestoneBlockIdsResponse extends BaseP2PResponse {
    private List<String> milestoneBlockIds;
    private boolean last;

}
