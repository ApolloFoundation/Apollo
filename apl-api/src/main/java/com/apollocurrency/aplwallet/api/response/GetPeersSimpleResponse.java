/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class GetPeersSimpleResponse extends ResponseBase {

    private List<String> peers;

}
