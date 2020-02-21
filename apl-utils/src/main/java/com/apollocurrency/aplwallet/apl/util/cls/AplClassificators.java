
package com.apollocurrency.aplwallet.apl.util.cls;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is classificators for certificaqte-related and other itmes.
 * Each item have name and Long value. Items could have sub-classifcators
 * TODO: replace hard-coded initializers with init from JSON files
 * @author alukin@gmail.com
 */
public class AplClassificators {

    public static final String ACTOR_CLS = "ActorTypes";
    public static final String AUTORITY_CLS = "AuthorityCodes";
    public static final String BUSINESS_CLS = "BusinessCodes";
    public static final String OPERATIONS_CLS = "OperationsCodes";
    public static final String SUPL_CLS = "SupplementalCodes";
    
    private static final Logger logger = LoggerFactory.getLogger(AplClassificators.class);
    
    private static final Map<String, BasicClassificator> clsMap = new HashMap<>();
    static {    
        try {
            clsMap.put(ACTOR_CLS, BasicClassificator.fromJsonResource(ACTOR_CLS+".json").setValueComparator(new NumbericValueComparator()));
            clsMap.put(AUTORITY_CLS, BasicClassificator.fromJsonResource(AUTORITY_CLS+".json").setValueComparator(new NumbericValueComparator()));
            clsMap.put(BUSINESS_CLS, BasicClassificator.fromJsonResource(BUSINESS_CLS+".json").setValueComparator(new NumbericValueComparator()));
            clsMap.put(OPERATIONS_CLS, BasicClassificator.fromJsonResource(OPERATIONS_CLS+".json").setValueComparator(new NumbericValueComparator()));
            clsMap.put(SUPL_CLS, BasicClassificator.fromJsonResource(SUPL_CLS+".json").setValueComparator(new NumbericValueComparator()));
        } catch (IOException ex) {
            logger.error("Can not loadclassificator from reources", ex);
        }
    }
    public static BasicClassificator getCls(String cls) {
        return clsMap.get(cls);
    }
}
