/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

/**
 * Metadata for REST API
 *
 * @author alukin@gmail.com
 */
@OpenAPIDefinition(info =
@Info(
    title = "Apollo REST API",
    version = "0.0.1",
    description = "Apollo REST API, OpenAPI/Swagger 2.0 compatible, Work in progress",
    license = @License(name = "Apollo PUBLIC LICENSE", url = "https://apollocurrency.com/"),
    contact = @Contact(url = "https://github.com/ApolloFoundation/Apollo", name = "Apollo team", email = "info@apollocurrency.com")
)
)
public class Metadata {

}
