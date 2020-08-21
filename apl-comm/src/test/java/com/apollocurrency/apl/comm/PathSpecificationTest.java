/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.apl.comm;

import com.apollocurrency.apl.comm.sv.client.impl.PathSpecification;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author al
 */
public class PathSpecificationTest {
    
    public PathSpecificationTest() {
    }
    

    /**
     * Test of fromSpecString method, of class PathSpecification.
     */
    @Test
    public void testFromSpecString() {
        String pathSpec1 = "/test/one/{param1}/{param2}";
        PathSpecification result1 = PathSpecification.fromSpecString(pathSpec1);
        
        assertEquals("/test/one", result1.prefix);
        assertEquals("param1", result1.paramNames.get(0));
        assertEquals("param2", result1.paramNames.get(1));
        
        String pathSpec2 = "/test/two/one";
        PathSpecification result2 = PathSpecification.fromSpecString(pathSpec2);
        
        assertEquals("/test/two/one", result2.prefix);
        assertEquals(0, result2.paramNames.size());
        
        String pathSpec3 = "/test/one/{param1/{param2}";
        PathSpecification result3 = PathSpecification.fromSpecString(pathSpec3);
        
        assertEquals("/test/one", result3.prefix);
        assertEquals("param1", result3.paramNames.get(0));
        assertEquals("param2", result3.paramNames.get(1));
    }

    /**
     * Test of matches method, of class PathSpecification.
     */
    @Test
    public void testMatches() {
        String pathSpec = "/test/one/{param1}/{param2}";
        PathSpecification ps = PathSpecification.fromSpecString(pathSpec);
        boolean result1 = ps.matches("/test/one/1");
        assertEquals(true,result1);
        boolean result2 = ps.matches("/test/one/1/2/3");
        assertEquals(true,result2);    
    }

    /**
     * Test of parseParams method, of class PathSpecification.
     */
    @Test
    public void testParseParams() {
        String pathSpec = "/test/one/{param1}/{param2}";
        PathSpecification ps = PathSpecification.fromSpecString(pathSpec);
        Map<String,String> params = ps.parseParams("/test/one/1");        
        assertEquals("1", params.get("param1"));
        assertEquals(null, params.get("param2"));
        
        Map<String,String> params1 = ps.parseParams("/test/one/1/2");        
        assertEquals("1", params1.get("param1"));
        assertEquals("2", params1.get("param2"));
        
        Map<String,String> params2 = ps.parseParams("test/one/1/2");        
        assertEquals("1", params1.get("param1"));
        assertEquals("2", params1.get("param2"));
    }
    
}
