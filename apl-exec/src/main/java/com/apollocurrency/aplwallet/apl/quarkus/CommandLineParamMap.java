/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.quarkus;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class CommandLineParamMap {

    private Map<String, Object> cliParams = new HashMap<>();

    public Map<String, Object> getCliParams(){
        return cliParams;
    }

    public void put(String name, Object value){
        cliParams.put(name, value);
    }

}
