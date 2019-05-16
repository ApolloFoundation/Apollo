/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.PeerDTO;
import com.apollocurrency.aplwallet.api.response.GetPeersResponse;
import com.apollocurrency.aplwallet.api.response.GetPeersSimpleResponse;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Converter;
import com.apollocurrency.aplwallet.apl.core.rest.service.NetworkService;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Apollo network endpoint
 */

@Path("/networking")
public class NetworkEndpoint {

    @Inject
    private Converter<Peer, PeerDTO> converter;

    @Inject
    private NetworkService service;

    @Path("/peer")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Returns peer information",
            notes = "Returns peer information by host address.",
            tags = "networking",
            response = PeerDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                    message = "Successful execution", response = PeerDTO.class)
    })
    public Response getPeer(
            @ApiParam(value = "The requested peer IP address.")
            @QueryParam("peer") String peerAddress) {

        ResponseBuilder response = ResponseBuilder.startTiming();

        Peer peer = service.findPeerByAddress(peerAddress);

        if (peer == null) {
            return ResponseBuilder.apiError( ApiErrors.UNKNOWN_VALUE, "peer", peerAddress);
        }

        return response.bind(converter.convert(peer)).build();
    }

    @Path("/peer/all")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Returns peers list",
            notes = "Returns all peers list by supplied parameters.",
            tags = "networking",
            response = GetPeersResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                    message = "Successful execution", response = GetPeersResponse.class)
    })
    public Response getPeersList(
            @ApiParam(value = "include active only peers")
            @QueryParam("active") @DefaultValue("false") Boolean active,
            @ApiParam(value = "include peers in certain state (NON_CONNECTED, CONNECTED, DISCONNECTED)")
            @QueryParam("state") String stateValue,
            @ApiParam(value = "include peer which provides services (HALLMARK, PRUNABLE, API, API_SSL, CORS)")
            @QueryParam("service") List<String> serviceValues,
            @ApiParam(value = "include additional peer information otherwise the host only.")
            @QueryParam("includePeerInfo") @DefaultValue("false") Boolean includePeerInfo
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();

        Peer.State state;
        if (stateValue != null) {
            try {
                state = Peer.State.valueOf(stateValue);
            } catch (RuntimeException exc) {
                return ResponseBuilder.apiError(ApiErrors.INCORRECT_VALUE, "state", stateValue);
            }
        } else {
            state = null;
        }

        long serviceCodes = 0;
        if (serviceValues != null) {
            for (String serviceValue : serviceValues) {
                try {
                    serviceCodes |= Peer.Service.valueOf(serviceValue).getCode();
                } catch (RuntimeException exc) {
                    return ResponseBuilder.apiError(ApiErrors.INCORRECT_VALUE, "service", serviceValue);
                }
            }
        }

        List<Peer> peers = service.getPeersByStateAndService(active, state, serviceCodes);

        if (includePeerInfo){
            GetPeersResponse peersResponse = new GetPeersResponse();
            peersResponse.setPeers(converter.convert(peers));
            response.bind(peersResponse);
        }else{
            GetPeersSimpleResponse peersSimpleResponse = new GetPeersSimpleResponse();
            List<String> hosts = peers.stream().map(Peer::getHost).collect(Collectors.toList());
            peersSimpleResponse.setPeers(hosts);
            response.bind(peersSimpleResponse);
        }

        return response.build();
    }
}
