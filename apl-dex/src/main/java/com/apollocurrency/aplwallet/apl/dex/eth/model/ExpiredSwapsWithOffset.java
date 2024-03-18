package com.apollocurrency.aplwallet.apl.dex.eth.model;

import com.apollocurrency.aplwallet.apl.dex.core.model.ExpiredSwap;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ExpiredSwapsWithOffset extends OffsetModel {
    List<ExpiredSwap> swaps = new ArrayList<>();
}