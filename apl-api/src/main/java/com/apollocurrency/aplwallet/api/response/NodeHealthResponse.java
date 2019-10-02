/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.NodeHealthInfo;
import com.apollocurrency.aplwallet.api.dto.NodeNetworkingInfo;
import com.apollocurrency.aplwallet.api.dto.NodeStatusInfo;


/**
 * @author alukin@gmail.com
 */
public class NodeHealthResponse extends ResponseBase {
    Boolean rebootRequired = false;
    public NodeHealthInfo healthInfo;
    public NodeStatusInfo statusInfo;
    public NodeNetworkingInfo networkingInfo;
}
