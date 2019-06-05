package com.apollocurrency.aplwallet.apl.core.rest.endpoint;





import com.apollocurrency.aplwallet.api.response.TransportStatusResponse;
import com.apollocurrency.aplwallet.apl.core.rest.service.TransportInteractionService;
import com.apollocurrency.aplwallet.apl.core.rest.service.TransportInteractionServiceImpl;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Path("/transportinfo")
public class TransportInteractionController {

    private static final Logger log = LoggerFactory.getLogger(TransportInteractionController.class);

    @Inject @Setter
    private TransportInteractionService  tiService;

    
     
     public TransportInteractionController( ) {
        
     }


    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns transport status",
            description = "Returns transport status in JSON format.",
            tags = {"transportstatus"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = TransportStatusResponse.class)))
            }
    )
    public TransportStatusResponse getTransportStatusResponse(){        
                        
        TransportStatusResponse transportStatusResponse = tiService.getTransportStatusResponse();
        
        transportStatusResponse.connected = true; 
        return transportStatusResponse;        
    }



}