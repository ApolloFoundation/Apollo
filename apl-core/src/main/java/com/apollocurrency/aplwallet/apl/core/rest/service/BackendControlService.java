/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.dto.NodeHWStatusInfo;
import javax.enterprise.context.ApplicationScoped;

/**
 *
 * @author alukin@gmail.com
 */
@ApplicationScoped
public class BackendControlService {
    
    public NodeHWStatusInfo getHWStatus(){
        NodeHWStatusInfo res = new NodeHWStatusInfo();
        res.cpuCores = 4;
        return res;
    } 
}
