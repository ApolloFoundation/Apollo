/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.util.cls;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is classificators for certificaqte-related and other itmes.
 * Each item have name and Long value. Items could have sub-classifcators
 * TODO: replace hard-coded initializators with init from JSON files
 * @author alukin@gmail.com
 */
public class AplClassificators {

    public static final String ACTOR_CLS = "ActorTypes";
    public static final String AUTORITY_CLS = "AuthorityCodes";
    public static final String BUSINESS_CLS = "BusinessCodes";
    public static final String OPERATIONS_CLS = "OperationsCodes";
    public static final String REGION_CLS = "RegionCodes"; // Regions incude several countries
    public static final String SUPL_CLS = "SupplementalCodes";
    public static final String COUNTRY_CLS = "Countries"; // With states/provinces and cities
    public static final String CERTPURPOSE_CLS = "CertPurpose";
    
    private static final Logger logger = LoggerFactory.getLogger(AplClassificators.class);
    
    private static final Map<String, BasicClassificator> clsMap = new HashMap<>();
    static {    
        try {
            clsMap.put(ACTOR_CLS, BasicClassificator.fromJsonResource(ACTOR_CLS+".json").setValueComparator(new NumbericValueComparator()));
            clsMap.put(AUTORITY_CLS, BasicClassificator.fromJsonResource(AUTORITY_CLS+".json").setValueComparator(new NumbericValueComparator()));
            clsMap.put(BUSINESS_CLS, BasicClassificator.fromJsonResource(BUSINESS_CLS+".json").setValueComparator(new NumbericValueComparator()));
            clsMap.put(OPERATIONS_CLS, BasicClassificator.fromJsonResource(OPERATIONS_CLS+".json").setValueComparator(new NumbericValueComparator()));
            clsMap.put(REGION_CLS, BasicClassificator.fromJsonResource(REGION_CLS+".json").setValueComparator(new NumbericValueComparator()));
            clsMap.put(SUPL_CLS, BasicClassificator.fromJsonResource(SUPL_CLS+".json").setValueComparator(new NumbericValueComparator()));
            clsMap.put(COUNTRY_CLS, BasicClassificator.fromJsonResource(COUNTRY_CLS+".json"));
            clsMap.put(CERTPURPOSE_CLS, BasicClassificator.fromJsonResource(CERTPURPOSE_CLS+".json").setValueComparator(new NumbericValueComparator()));
        } catch (IOException ex) {
            logger.error("Can not loadclassificator from reources", ex);
        }
    }
    public static BasicClassificator getCls(String cls) {
        return clsMap.get(cls);
    }
}
