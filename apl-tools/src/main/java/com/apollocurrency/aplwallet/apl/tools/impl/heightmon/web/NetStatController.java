/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon.web;

import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.HeightMonitorService;
import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.NetworkStats;
import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.PeerInfo;

import java.net.UnknownHostException;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/netstat")
@Singleton
public class NetStatController {

    @Inject
    private HeightMonitorService heightMonitorService;
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStats() {
        NetworkStats last = heightMonitorService.getLastStats();
        if (last == null) {
            return Response.serverError()
                    .entity("Network statistics is not ready yet").build();
        }
        return Response.ok(last).build();
    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response addPeer(@NotNull @QueryParam("ip") String ip) {
        try {
            return Response.ok(heightMonitorService.addPeer(new PeerInfo(ip))).build();
        }
        catch (UnknownHostException e) {
            return Response.status(422).build();
        }
    }
}
