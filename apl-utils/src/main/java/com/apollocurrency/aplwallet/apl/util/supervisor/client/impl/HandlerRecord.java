
package com.apollocurrency.aplwallet.apl.util.supervisor.client.impl;

import com.apollocurrency.aplwallet.apl.util.supervisor.client.SvRequestHandler;
import com.fasterxml.jackson.databind.JavaType;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author alukin@gmail.com
 */
public class HandlerRecord {
    public String pathSpec;
    public SvRequestHandler handler;
    public JavaType respMapping;
    public JavaType rqMapping;    
    public Map<String,String> pathParams = new HashMap<>();
}
