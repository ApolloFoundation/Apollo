/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.MyPeerInfoDTO;
import com.apollocurrency.aplwallet.api.dto.PeerDTO;
import com.apollocurrency.aplwallet.api.response.GetPeersResponse;
import com.apollocurrency.aplwallet.api.response.GetPeersSimpleResponse;
import com.apollocurrency.aplwallet.api.response.ResponseBase;
import com.apollocurrency.aplwallet.api.response.ResponseDone;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Converter;
import com.apollocurrency.aplwallet.apl.core.rest.service.NetworkService;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.Setter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Apollo network endpoint
 */

@Path("/networking")
public class NetworkController {

    @Inject @Setter
    private Converter<Peer, PeerDTO> peerConverter;

    @Inject @Setter
    private NetworkService service;



    @Path("/peer/mypeerinfo")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Return host information",
            description = "Return the remote host name and address.",
            tags = {"networking"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = MyPeerInfoDTO.class)))
            })
    public Response getMyInfo(@Context HttpServletRequest request) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        MyPeerInfoDTO dto = new MyPeerInfoDTO(request.getRemoteHost(), request.getRemoteAddr());
        return response.bind(dto).build();
    }

    @Path("/peer")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Returns peer information",
            description = "Returns peer information by host address.",
            tags = {"networking"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PeerDTO.class)))
    })
    public Response getPeer(
            @Parameter(description = "The certain peer IP address.", required = true)
            @QueryParam("peer") String peerAddress ) {

        ResponseBuilder response = ResponseBuilder.startTiming();

        if (peerAddress == null) {
            return response.error( ApiErrors.MISSING_PARAM, "peer").build();
        }

        Peer peer = service.findPeerByAddress(peerAddress);

        if (peer == null) {
            return response.error( ApiErrors.UNKNOWN_VALUE, "peer", peerAddress).build();
        }

        return response.bind(peerConverter.convert(peer)).build();
    }

    @Path("/peer")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Add new peer or replace existing.",
            description = "Add new peer or replace existing.",
            method = "POST",
            tags = {"networking"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "text/html",
                                    schema = @Schema(implementation = PeerDTO.class)))
            }
    )
    public Response addOrReplacePeer( @FormParam("peer") String peerAddress ) {
        ResponseBuilder response = ResponseBuilder.ok();

        if (peerAddress == null) {
            return response.error( ApiErrors.MISSING_PARAM, "peer").build();
        }

        Peer peer = service.findOrCreatePeerByAddress(peerAddress);

        if (peer == null) {
            return response.error( ApiErrors.FAILED_TO_ADD,peerAddress).build();
        }

        boolean isNewlyAdded = service.addPeer(peer, peerAddress);
        PeerDTO dto = peerConverter.convert(peer);
        dto.setIsNewlyAdded(isNewlyAdded);

        return response.bind(dto).build();
    }

    @Path("/peer/all")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Returns peers list",
            description = "Returns all peers list by supplied parameters.",
            tags = {"networking"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = GetPeersResponse.class)))
    })
    //TODO: need to be divided into two separated methods
    // cause currently, this GET returns two different responses (GetPeersResponse or GetPeersSimpleResponse)
    // that depend on the value of the includePeerInfo parameter.
    public Response getPeersList(
            @Parameter(description = "include active only peers") @QueryParam("active") @DefaultValue("false") Boolean active,
            @Parameter(description = "include peers in certain state (NON_CONNECTED, CONNECTED).",
                    schema = @Schema(allowableValues = {"NON_CONNECTED", "CONNECTED"}))
                        @QueryParam("state") String stateValue,
            @Parameter(description = "include peer which provides services (HALLMARK, PRUNABLE, API, API_SSL, CORS)")
                        @QueryParam("service") List<String> serviceValues,
            @Parameter(description = "include additional peer information otherwise the host only.")
                        @QueryParam("includePeerInfo") @DefaultValue("false") Boolean includePeerInfo ) {

        ResponseBuilder response = ResponseBuilder.startTiming();

        PeerState state;
        if (stateValue != null) {
            try {
                state = PeerState.valueOf(stateValue);
            } catch (RuntimeException exc) {
                return response.error(ApiErrors.INCORRECT_VALUE, "state", stateValue).build();
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
                    return response.error(ApiErrors.INCORRECT_VALUE, "service", serviceValue).build();
                }
            }
        }

        List<Peer> peers = service.getPeersByStateAndService(active, state, serviceCodes);

        return response.bind(mapResponse(peers, includePeerInfo)).build();
    }

    @Path("/peer/inbound")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Returns inbound peers list.",
            description = "Returns a list of inbound peers. An inbound peer is a peer that has sent a request" +
                         " to this peer within the previous 30 minutes.",
            tags = {"networking"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema( implementation = GetPeersResponse.class)))
    })
    //TODO: need to be divided into two separated path
    // cause currently, this GET returns two different responses (GetPeersResponse or GetPeersSimpleResponse)
    // that depend on the value of the includePeerInfo parameter.
    public Response getInboundPeersList(
            @Parameter(description = "include additional peer information otherwise the host only.")
            @QueryParam("includePeerInfo") @DefaultValue("false") Boolean includePeerInfo ) {

        ResponseBuilder response = ResponseBuilder.startTiming();

        List<Peer> peers = service.getInboundPeers();

        return response.bind(mapResponse(peers, includePeerInfo)).build();
    }

    @Path("/peer/blacklist")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Add peer in the black list.",
            description = "Add peer in the black list.",
            method = "POST",
            tags = {"networking"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "text/html",
                                    schema = @Schema(implementation = ResponseDone.class)))
            }
    )
    public Response addPeerInBlackList( @FormParam("peer") String peerAddress ) {
        ResponseBuilder response = ResponseBuilder.done();

        if (peerAddress == null) {
            return response.error( ApiErrors.MISSING_PARAM, "peer").build();
        }

        Peer peer = service.putPeerInBlackList(peerAddress);

        if (peer == null) {
            return response.error( ApiErrors.UNKNOWN_VALUE, "peer", peerAddress).build();
        }

        return response.build();
    }

    @Path("/peer/proxyblacklist")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Add peer in the proxy black list.",
            description = "Add peer in the proxy black list.",
            method = "POST",
            tags = {"networking"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "text/html",
                                    schema = @Schema(implementation = ResponseDone.class)))
            }
    )
    public Response addPeerInProxyBlackList(@FormParam("peer") String peerAddress ) {
        ResponseBuilder response = ResponseBuilder.startTiming();

        if (peerAddress == null) {
            return response.error( ApiErrors.MISSING_PARAM, "peer").build();
        }

        Peer peer = service.findOrCreatePeerByAddress(peerAddress);

        if (peer == null) {
            return response.error( ApiErrors.UNKNOWN_VALUE, "peer", peerAddress).build();
        }

        ResponseDone body = new ResponseDone(service.putPeerInProxyBlackList(peer));

        return response.bind(body).build();
    }

    @Path("/peer/setproxy")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Set peer as a proxy.",
            description = "Set peer as a proxy.",
            method = "POST",
            tags = {"networking"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "text/html",
                                    schema = @Schema(implementation = PeerDTO.class)))
            }
    )
    public Response setAPIProxyPeer( @FormParam("peer") String peerAddress ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        Peer peer;
        if (peerAddress == null) {
            peer = service.setForcedPeer(null);
            if ( peer == null ) {
                return response.error(ApiErrors.MISSING_PARAM, "peer").build();
            }
        }else {
            peer = service.findPeerByAddress(peerAddress);

            if (peer == null) {
                return response.error(ApiErrors.UNKNOWN_VALUE, "peer", peerAddress).build();
            }

            if (peer.getState() != PeerState.CONNECTED) {
                return response.error(ApiErrors.PEER_NOT_CONNECTED).build();
            }
            if (!peer.isOpenAPI()) {
                return response.error(ApiErrors.PEER_NOT_OPEN_API).build();
            }

            service.setForcedPeer(peer);
        }

        return response.bind(peerConverter.convert(peer)).build();
    }

    private ResponseBase mapResponse(List<Peer> peers, boolean includePeerInfo){
        if (includePeerInfo){
            GetPeersResponse peersResponse = new GetPeersResponse();
            peersResponse.setPeers(peerConverter.convert(peers));
            return peersResponse;
        }else{
            GetPeersSimpleResponse peersSimpleResponse = new GetPeersSimpleResponse();
            List<String> hosts = peers.stream().map(Peer::getHost).collect(Collectors.toList());
            peersSimpleResponse.setPeers(hosts);
            return peersSimpleResponse;
        }
    }

}
