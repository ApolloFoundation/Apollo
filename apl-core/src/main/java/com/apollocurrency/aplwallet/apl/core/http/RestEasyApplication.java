/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.apl.core.rest.endpoint.BackendControlEndpoint;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.ServerInfoEndpoint;
import io.swagger.jaxrs.config.BeanConfig;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * REST and Swagger configuration and fire-up
 * @author alukin@gmail.com
 */
@ApplicationPath("/rest")
public class RestEasyApplication extends Application  {
    
    public RestEasyApplication() {
        configureSwagger();
    }
  
    @Override
    public Set<Class<?>> getClasses() {

        HashSet<Class<?>> set = new HashSet<Class<?>>();
        
        set.add( ServerInfoEndpoint.class);        
        set.add(BackendControlEndpoint.class);               
        //TODO: add all
        
        set.add(io.swagger.jaxrs.listing.ApiListingResource.class);
        set.add(io.swagger.jaxrs.listing.SwaggerSerializers.class);

        return set;
    }

    private void configureSwagger() {
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion("0.0.1-Alpha");
        beanConfig.setSchemes(new String[]{"http"});
        beanConfig.setHost("localhost:7876");
        beanConfig.setBasePath("/rest");
        beanConfig.setResourcePackage(ServerInfoEndpoint.class.getPackage().getName());
        beanConfig.setTitle("Vimana blockchain API");
        beanConfig.setDescription("Work in progress of Vimana API documenting");
        beanConfig.setScan(true);
    }
    
}
