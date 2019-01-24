/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.dto.ApolloX509Info;
import javax.enterprise.context.RequestScoped;
import javax.inject.Singleton;

/**
 *
 * @author alukin@gmail.com
 */
@Singleton
public class ServerInfoService {
    public ApolloX509Info getX509Info(){
        ApolloX509Info res = new ApolloX509Info();
        res.id = "No ID yet available";
        return res;
    }
}
