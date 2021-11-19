/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class GetMilestoneBlockIdsResponse extends BaseP2PResponse {
    private List<String> milestoneBlockIds;
    private boolean last;

}
