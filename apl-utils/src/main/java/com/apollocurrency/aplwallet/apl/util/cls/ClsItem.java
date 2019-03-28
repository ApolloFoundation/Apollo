package com.apollocurrency.aplwallet.apl.util.cls;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tree item for classificators.
 *
 * @author alukin@gmail.com
 */
//@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, 
//                  property = "@id")
public class ClsItem {

    public String name = "";
    public String value = "";
    public String descr = "";
    public List<ClsItem> chlds = new ArrayList<>();
    
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(ClsItem.class);
    /**
     * This field is set to true by finders to indicate that item is not found
     */
    @JsonIgnore
    public boolean empty = false;

    @JsonIgnore
    public Integer getIntegerValue() {
        return getLongValue().intValue();
    }

    @JsonIgnore
    public Long getLongValue() {
        Long lv = 0L;
        try {
            lv = Long.parseLong(value);
        } catch (NumberFormatException ex) {
            try {
                lv = Long.parseLong(value, 16);
            } catch (NumberFormatException ex2) {
                try {
                    lv = Long.decode(value);
                } catch (NumberFormatException ex3) {
                   log.error("Can not parse numberic value: {} of node: {}",value,name);
                }
            }
        }
        return lv;
    }

    public BigInteger getBigIntegerVAlue() {
        BigInteger res = BigInteger.ZERO;
        try {
            res = new BigInteger(value);
        } catch (NumberFormatException e10) {
            try {
                res = new BigInteger(value, 16);
            } catch (NumberFormatException e16) {
                try {
                    res = new BigInteger(value, 8);
                } catch (NumberFormatException e8) {
                    log.error("Can not parse numberic value: {} of node: {}",value,name);
                }
            }
        }
        return res;
    }
}
