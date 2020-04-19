/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.NodeHealthInfo;
import com.apollocurrency.aplwallet.api.dto.NodeNetworkingInfo;
import com.apollocurrency.aplwallet.api.dto.NodeStatusInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


/**
 * @author alukin@gmail.com
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class NodeHealthResponse extends ResponseBase {
    public NodeHealthInfo healthInfo;
    public NodeStatusInfo statusInfo;
    public NodeNetworkingInfo networkingInfo;
    Boolean rebootRequired = false;
}
