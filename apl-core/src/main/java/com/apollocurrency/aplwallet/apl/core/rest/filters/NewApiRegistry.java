/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.filters;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for implemented endpoints of new API.
 * Should be removed along with ApiSplitFilter after 
 * @author alukin@gmail.com
 */
public class NewApiRegistry {
    private static Map<String,String> apis = new HashMap<>();
    static{
        apis.put("getServerInfo", "/rest/serverinfo"); 
        apis.put("uploadKeyStore", "/rest/keyStore/upload");
        //TODO: add new implemented endpoints
    }
    public static String getRestPath(String rqType) {
        if(rqType==null || rqType.isEmpty()){
            return null;
        }
        return apis.get(rqType);    
    }
    
}
