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
import com.apollocurrency.aplwallet.apl.core.converter.Converter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Converter;
import com.apollocurrency.aplwallet.apl.core.rest.service.NetworkService;
import com.apollocurrency.aplwallet.apl.util.builder.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Apollo network endpoint
 */
@OpenAPIDefinition(info = @Info(description = "Network operation"))
@SecurityScheme(type = SecuritySchemeType.APIKEY, name = "admin_api_key", in = SecuritySchemeIn.QUERY, paramName = "adminPassword")
@NoArgsConstructor
@Path("/networking")
public class NetworkController {

    @Inject
    @Setter
    private Converter<Peer, PeerDTO> peerConverter;

    @Inject
    @Setter
    private NetworkService service;

    public NetworkController(Converter<Peer, PeerDTO> peerConverter, NetworkService service) {
        this.peerConverter = peerConverter;
        this.service = service;
    }

    @Path("/peer/mypeerinfo")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Return host information",
        description = "Return hostname and address of the requesting node.",
        tags = {"networking"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = MyPeerInfoDTO.class)))
        })
    @PermitAll
    public Response getMyInfo(@Context HttpServletRequest request) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        MyPeerInfoDTO dto = new MyPeerInfoDTO(request.getRemoteHost(), request.getRemoteAddr());
        return response.bind(dto).build();
    }

    @Path("/peer")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns information about a given peer.",
        description = "Returns peer information by host address.",
        tags = {"networking"},
        parameters = {},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PeerDTO.class)))
        })
    @PermitAll
    public Response getPeer(
        @Parameter(description = "The certain peer IP address.", required = true)
        @QueryParam("peer") String peerAddress) {

        ResponseBuilder response = ResponseBuilder.startTiming();

        if (peerAddress == null) {
            return response.error(ApiErrors.MISSING_PARAM, "peer").build();
        }

        Peer peer = service.findPeerByAddress(peerAddress);

        if (peer == null) {
            return response.error(ApiErrors.UNKNOWN_VALUE, "peer", peerAddress).build();
        }

        return response.bind(peerConverter.convert(peer)).build();
    }

    @Path("/peer")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
        summary = "Add new peer or replace existing.",
        description = "Add a peer to the list of known peers and attempt to connect to it.",
        security = @SecurityRequirement(name = "admin_api_key"),
        method = "POST",
        tags = {"networking"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "text/html",
                    schema = @Schema(implementation = PeerDTO.class)))
        }
    )
    @RolesAllowed("admin")
    public Response addOrReplacePeer(@Parameter(schema = @Schema(implementation = String.class, description = "the IP address or domain name of the peer")) @FormParam("peer") String peerAddress, @Context SecurityContext sc) {
        ResponseBuilder response = ResponseBuilder.ok();

        if (peerAddress == null) {
            return response.error(ApiErrors.MISSING_PARAM, "peer").build();
        }

        Peer peer = service.findOrCreatePeerByAddress(peerAddress);

        if (peer == null) {
            return response.error(ApiErrors.FAILED_TO_ADD_PEER, peerAddress).build();
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
    @PermitAll
    //TODO: need to be divided into two separated methods
    // cause currently, this GET returns two different responses (GetPeersResponse or GetPeersSimpleResponse)
    // that depend on the value of the includePeerInfo parameter.
    public Response getPeersList(
        @Parameter(description = "include active only peers") @QueryParam("active") @DefaultValue("false") Boolean active,
        @Parameter(description = "include peers in certain state, one of NON_CONNECTED, CONNECTED, DISCONNECTED  (optional).",
            schema = @Schema(allowableValues = {"NON_CONNECTED", "CONNECTED"}))
        @QueryParam("state") String stateValue,
        @Parameter(description = "include peer which provides services (HALLMARK, PRUNABLE, API, API_SSL, CORS)")
        @QueryParam("service") List<String> serviceValues,
        @Parameter(description = "include additional peer information otherwise the host only.")
        @QueryParam("includePeerInfo") @DefaultValue("false") Boolean includePeerInfo) {

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
        summary = "Returns a list of inbound peers.",
        description = "Returns all peers that have sent a request to this peer in the last 30 minutes.",
        tags = {"networking"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = GetPeersResponse.class)))
        })
    @PermitAll
    //TODO: need to be divided into two separated path
    // cause currently, this GET returns two different responses (GetPeersResponse or GetPeersSimpleResponse)
    // that depend on the value of the includePeerInfo parameter.
    public Response getInboundPeersList(
        @Parameter(description = "include additional peer information otherwise the host only.")
        @QueryParam("includePeerInfo") @DefaultValue("false") Boolean includePeerInfo) {

        ResponseBuilder response = ResponseBuilder.startTiming();

        List<Peer> peers = service.getInboundPeers();

        return response.bind(mapResponse(peers, includePeerInfo)).build();
    }

    @Path("/peer/blacklist")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
        summary = "Blacklist a peer.",
        description = "Blacklist a peer for the default blacklisting period.",
        security = @SecurityRequirement(name = "admin_api_key"),
        method = "POST",
        tags = {"networking"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "text/html",
                    schema = @Schema(implementation = ResponseDone.class)))
        }
    )
    @RolesAllowed("admin")
    public Response addPeerInBlackList(@Parameter(schema = @Schema(implementation = String.class, description = "the IP address or domain name of the peer"))
                                       @FormParam("peer")
                                       @NotNull
                                           String peerAddress) {
        ResponseBuilder response = ResponseBuilder.done();

        Peer peer = service.putPeerInBlackList(peerAddress);

        if (peer == null) {
            return response.error(ApiErrors.UNKNOWN_VALUE, "peer", peerAddress).build();
        }

        return response.build();
    }

    @Path("/peer/proxyblacklist")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
        summary = "Blacklist a remote node.",
        description = "Blacklist a remote node from the UI, so it won't be used when in roaming and light client modes.",
        security = @SecurityRequirement(name = "admin_api_key"),
        method = "POST",
        tags = {"networking"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "text/html",
                    schema = @Schema(implementation = ResponseDone.class)))
        }
    )
    @RolesAllowed("admin")
    public Response addPeerInProxyBlackList(@Parameter(schema = @Schema(implementation = String.class, description = "the IP address or domain name of the peer"))
                                            @FormParam("peer")
                                            @NotNull
                                                String peerAddress) {
        ResponseBuilder response = ResponseBuilder.startTiming();

        Peer peer = service.findOrCreatePeerByAddress(peerAddress);

        if (peer == null) {
            return response.error(ApiErrors.UNKNOWN_VALUE, "peer", peerAddress).build();
        }

        ResponseDone body = new ResponseDone(service.putPeerInProxyBlackList(peer));

        return response.bind(body).build();
    }

    @Path("/peer/setproxy")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
        summary = "Set the remote node as a proxy.",
        description = "Set the remote node to use when in roaming and light client modes.",
        security = @SecurityRequirement(name = "admin_api_key"),
        method = "POST",
        tags = {"networking"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "text/html",
                    schema = @Schema(implementation = PeerDTO.class)))
        }
    )
    @RolesAllowed("admin")
    public Response setAPIProxyPeer(@Parameter(schema = @Schema(implementation = String.class, description = "the IP address or domain name of the peer"))
                                    @FormParam("peer")
                                        String peerAddress) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        Peer peer;
        if (peerAddress == null) {
            peer = service.setForcedPeer(null);
            if (peer == null) {
                return response.error(ApiErrors.MISSING_PARAM, "peer").build();
            }
        } else {
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

    private ResponseBase mapResponse(List<Peer> peers, boolean includePeerInfo) {
        if (includePeerInfo) {
            GetPeersResponse peersResponse = new GetPeersResponse();
            peersResponse.setPeers(peerConverter.convert(peers));
            return peersResponse;
        } else {
            GetPeersSimpleResponse peersSimpleResponse = new GetPeersSimpleResponse();
            List<String> hosts = peers.stream().map(Peer::getHost).collect(Collectors.toList());
            peersSimpleResponse.setPeers(hosts);
            return peersSimpleResponse;
        }
    }

}
