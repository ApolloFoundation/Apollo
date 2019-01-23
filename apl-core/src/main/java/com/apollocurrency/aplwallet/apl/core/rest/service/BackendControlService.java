/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.dto.BackendStatusInfo;

/**
 *
 * @author alukin@gmail.com
 */
public class BackendControlService {
    public BackendStatusInfo getStatus(){
        BackendStatusInfo res = new BackendStatusInfo();
        res.whatever = "Not ready yet, implement!";
        return res;
    } 
}
