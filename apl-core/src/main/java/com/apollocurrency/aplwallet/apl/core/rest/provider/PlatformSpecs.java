/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.provider;

import com.apollocurrency.aplwallet.apl.util.env.PlatformSpec;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
public class PlatformSpecs {
    private List<PlatformSpec> specList = new ArrayList<>();

}
