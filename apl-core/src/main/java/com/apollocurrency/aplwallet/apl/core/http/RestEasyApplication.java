/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.http;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.apollocurrency.aplwallet.apl.core.rest.endpoint.BackendControlEndpoint;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.ServerInfoEndpoint;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

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
        
//        set.add(io.swagger.jaxrs.listing.ApiListingResource.class);
        set.add(io.swagger.v3.jaxrs2.SwaggerSerializers.class);

        return set;
    }

    private void configureSwagger() {
        OpenAPI oas = new OpenAPI();
        Info info = new Info()
                .title("Apollo App Swagger API bootstrap code")
                .description("This is a sample server Apollo server.  You can find out more about Swagger " +
                        "at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/).  For this sample, " +
                        "you can use the api key `special-key` to test the authorization filters.")
                .termsOfService("http://swagger.io/terms/")
                .contact(new Contact()
                        .email("api-team@swagger.io"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("http://www.apache.org/licenses/LICENSE-2.0.html"));
        oas.info(info);

        SwaggerConfiguration beanConfig = new SwaggerConfiguration();
//        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setOpenAPI(oas);
//        beanConfig.setSchemes(new String[]{"http"});
//        beanConfig.setHost("localhost:7876");
//        beanConfig.setBasePath("/rest");
//        beanConfig.setResourcePackage(ServerInfoEndpoint.class.getPackage().getName());
        beanConfig.setResourcePackages(Stream.of(ServerInfoEndpoint.class.getPackage().getName()).collect(Collectors.toSet()));
//        beanConfig.setTitle("Vimana blockchain API");
//        beanConfig.setDescription("Work in progress of Vimana API documenting");
//        beanConfig.setScan(true);
    }
    
}
