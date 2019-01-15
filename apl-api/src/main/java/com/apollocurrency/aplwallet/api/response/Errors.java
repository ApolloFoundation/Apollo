package com.apollocurrency.aplwallet.api.response;

import java.util.HashMap;
import java.util.Map;

/**
 * Error code ->Description string mapper.
 * TODO: I18n is possible from resources
 * @author al
 */
public class Errors {
    private String lang="en_US";
    private Map<Integer,String> descriptions = new HashMap<>();
    
    public Errors() {
    }
    
//    public Errors(String lang) {
//       if(lang!=null && !lang.isEmpty()){
//           this.lang = lang;
//       }    
//    }
    
    public String getDescription(int code){
        return descriptions.get(code);
    }
}
