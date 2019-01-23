/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.dto.ApolloX509Info;

/**
 *
 * @author alukin@gmail.com
 */
public class ServerInfoService {
    public ApolloX509Info getX509Info(){
        ApolloX509Info res = new ApolloX509Info();
        res.id = "No ID yet available";
        return res;
    }
}
