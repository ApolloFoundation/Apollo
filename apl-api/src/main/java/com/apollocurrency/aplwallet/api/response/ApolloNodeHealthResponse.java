/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.NodeHWStatusInfo;
import com.apollocurrency.aplwallet.api.dto.NodeHealthInfo;



/**
 *
 * @author alukin@gmail.com
 */
public class ApolloNodeHealthResponse extends ResponseBase{
    Boolean rebootRequired = false;
    public NodeHealthInfo healthInfo;
    public NodeHWStatusInfo hwInfo;
}
